package queueup;

import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import queueup.objects.QueueupCredential;
import queueup.objects.QueueupPlaylist;
import queueup.objects.SpotifyTrack;

/**
 * Created by Louis on 5/23/2015.
 */
public class QueueupClient {

    private String clientToken, userId;
    private PlaylistClient playlistClient;
    private static HttpGet searchGetRequest;
//    private PlaylistPlayer playlistPlayer;

    public QueueupClient(String clientToken, String userId) {
        this.clientToken = clientToken;
        this.userId = userId;
    }

    public static void login(JSONObject json, final Queueup.CallReceiver<QueueupCredential> receiver){
        sendPost("/api/auth/login", json, new Queueup.CallReceiver<JSONObject>() {
            @Override
            public void onResult(JSONObject result) {
                try {
                    String clientToken = result.getString("client_token");
                    String userId = result.getString("user_id");
                    receiver.onResult(new QueueupCredential(userId, clientToken));
                } catch (JSONException e) {
                    receiver.onException(new QueueupException(e));
                }
            }

            @Override
            public void onException(Exception e) {
                receiver.onException(e);
            }
        });

    }

    public static void register(String email, String password, String name, final Queueup.CallReceiver<QueueupCredential> receiver){
        JSONObject json = new JSONObject();

        try {
            json.put("email", email);
            json.put("password", password);
            json.put("name", name);
        } catch (JSONException e) {
            receiver.onException(new QueueupException("Error sending JSON: " + e.getMessage()));
            return;
        }

        sendPost("/api/auth/register", json, new Queueup.CallReceiver<JSONObject>() {
            @Override
            public void onResult(JSONObject result) {
                try {
                    String userId = result.getString("user_id");
                    String clientToken = result.getString("client_token");
                    receiver.onResult(new QueueupCredential(userId, clientToken));
                } catch (JSONException e) {
                    receiver.onException(new QueueupException(e));
                }
            }

            @Override
            public void onException(Exception e) {
                receiver.onException(e);
            }
        });
    }

    public static void loginEmail(String email, String password, Queueup.CallReceiver<QueueupCredential> receiver) {
        JSONObject json = new JSONObject();

        try {
            json.put("email", email);
            json.put("password", password);
        } catch (JSONException e) {
            receiver.onException(new QueueupException("Error sending JSON: " + e.getMessage()));
            return;
        }

        login(json, receiver);
    }

    public static void loginFacebook (String accessToken, Queueup.CallReceiver<QueueupCredential> receiver) {
        JSONObject json = new JSONObject();

        try {
            json.put("facebook_access_token", accessToken);
        } catch (JSONException e) {
            receiver.onException(new QueueupException("Error sending JSON: " + e.getMessage()));
            return;
        }

        login(json, receiver);
    }

    public static void playlistGetList(final Queueup.CallReceiver<List<QueueupPlaylist>> receiver) {
        sendGet("/api/playlists", new Queueup.CallReceiver<JSONObject>() {
            @Override
            public void onResult(JSONObject result) {
                try {
                    JSONArray playlistsJson = result.getJSONArray("playlists");
                    List<QueueupPlaylist> playlistList = new ArrayList<>();

                    for (int i = 0; i < playlistsJson.length(); i++) {
                        playlistList.add(new QueueupPlaylist(playlistsJson.getJSONObject(i)));
                    }

                    receiver.onResult(playlistList);
                } catch (JSONException e) {
                    receiver.onException(new QueueupException("Invalid JSON  received: " + e.getMessage()));
                }
            }

            @Override
            public void onException(Exception e) {
                receiver.onException(e);
            }
        });

    }

    public static void playlistGet(String playlistId, final Queueup.CallReceiver<QueueupPlaylist> receiver) {
        sendGet("/api/playlists/" + playlistId, new Queueup.CallReceiver<JSONObject>() {
            @Override
            public void onResult(JSONObject result) {
                try {
                    JSONObject playlistJson = result.getJSONObject("playlist");
                    receiver.onResult(new QueueupPlaylist(playlistJson));
                } catch (JSONException e) {
                    receiver.onException(new QueueupException("Invalid JSON  received: " + e.getMessage()));
                }
            }

            @Override
            public void onException(Exception e) {
                receiver.onException(e);
            }
        });
    }

    public void playlistCreate(String playlistName, final Queueup.CallReceiver<QueueupPlaylist> receiver) {
        try {
            JSONObject playlist = new JSONObject();
            playlist.put("name", playlistName);

            JSONObject request = new JSONObject();
            request.put("playlist", playlist);

            sendApiPost("/api/playlists/new", request, new Queueup.CallReceiver<JSONObject>() {
                @Override
                public void onResult(JSONObject result) {
                    try {
                        JSONObject playlistJSON = result.getJSONObject("playlist");
                        QueueupPlaylist playlist = new QueueupPlaylist(playlistJSON);
                        receiver.onResult(playlist);
                    } catch (JSONException e) {
                        receiver.onException(e);
                    }
                }

                @Override
                public void onException(Exception e) {
                    receiver.onException(e);
                }
            });
        } catch (JSONException e) {
            Log.e(Queueup.LOG_TAG, "JSON error adding track: " + e.getMessage());
        }
    }

    public void playlistRename(String playlistId, String newName, final Queueup.CallReceiver<QueueupPlaylist> receiver) {
        try {
            JSONObject request = new JSONObject();
            request.put("name", newName);
            sendApiPost("/api/playlists/" + playlistId + "/rename", request, new Queueup.CallReceiver<JSONObject>() {
                @Override
                public void onResult(JSONObject result) {
                    try {
                        QueueupPlaylist playlist = new QueueupPlaylist(result.getJSONObject("playlist"));
                        receiver.onResult(playlist);
                    } catch (JSONException e) {
                        receiver.onException(e);
                    }
                }

                @Override
                public void onException(Exception e) {
                    receiver.onException(e);
                }
            });
        } catch (JSONException e) {
            Log.e(Queueup.LOG_TAG, "JSON error adding track: " + e.getMessage());
        }
    }

    public void playlistDelete(String playlistId, final Queueup.CallReceiver<Boolean> receiver) {
        JSONObject request = new JSONObject();
        sendApiPost("/api/playlists/" + playlistId + "/delete", request, new Queueup.CallReceiver<JSONObject>() {
            @Override
            public void onResult(JSONObject result) {
                boolean success = result.optBoolean("success", false);
                receiver.onResult(success);
            }

            @Override
            public void onException(Exception e) {
                receiver.onException(e);
            }
        });
    }

    public static void playlistAddTrack(String playlistId, String spotifyUri, final Queueup.CallReceiver<JSONObject> receiver) {
        try {
            JSONObject request = new JSONObject();
            request.put("track_id", spotifyUri);
            sendPost("/api/playlists/" + playlistId + "/add", request, receiver);
        } catch (JSONException e) {
            Log.e(Queueup.LOG_TAG, "JSON error adding track: " + e.getMessage());
        }
    }

    public static void searchTracks(String query, int offset, final Queueup.CallReceiver<List<SpotifyTrack>> receiver) {

        /* Short circuit if the string is empty */
        if (query.length() == 0) {
            receiver.onResult(new ArrayList<SpotifyTrack>());
            return;
        }

        if (searchGetRequest != null) {
            searchGetRequest.abort();
            searchGetRequest = null;
        }

        String encodedQuery = "";

        try {
            encodedQuery = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return;
        }

        searchGetRequest = sendGet("/api/search/tracks/" + encodedQuery + "/" + offset, new Queueup.CallReceiver<JSONObject>() {
            @Override
            public void onResult(JSONObject result) {
                List<SpotifyTrack> resultsList = new ArrayList<SpotifyTrack>();
                try {
                    JSONArray tracks = result.getJSONArray("tracks");
                    for (int i = 0; i < tracks.length(); i++) {
                        JSONObject track = tracks.getJSONObject(i);
                        resultsList.add(new SpotifyTrack(track));
                    }
                    receiver.onResult(resultsList);
                } catch (JSONException e){
                    receiver.onException(new QueueupException("Invalid JSON received: " + e.getMessage()));
                }
            }

            @Override
            public void onException(Exception e) {
                receiver.onException(e);
            }
        });
    }

    private HttpPost sendApiPost(String uri, JSONObject json, final Queueup.CallReceiver<JSONObject> receiver) {
        try {
            json.put("client_token", clientToken);
            json.put("user_id", userId);
        } catch (JSONException e) {
            Log.e(Queueup.LOG_TAG, "JSON Error: " + e.getMessage());
            receiver.onException(new QueueupException(e));
            return null;
        }

        return sendPost(uri, json, new Queueup.CallReceiver<JSONObject>() {
            @Override
            public void onResult(JSONObject result) {
                if (result == null) {
                    receiver.onException(new QueueupException(""));
                    return;
                }

                JSONObject error = result.optJSONObject("error");
                if (error != null) {
                    receiver.onException(new QueueupException(error));
                    return;
                }

                receiver.onResult(result);
            }

            @Override
            public void onException(Exception e) {
                receiver.onException(e);
            }
        });

    }

    private static HttpGet sendGet(final String uri, final Queueup.CallReceiver<JSONObject> receiver) {
        final HttpGet get = new HttpGet(Queueup.API_URL + uri);

        new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject response = null;

                try {
                    response = new DefaultHttpClient().execute(get, new ResponseHandler<JSONObject> () {
                        @Override
                        public JSONObject handleResponse(HttpResponse httpResponse) throws IOException {
                            JSONObject json = null;
                            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                                try {
                                    json = new JSONObject(EntityUtils.toString(httpResponse.getEntity()));
                                } catch (JSONException e) {
                                    Log.e(Queueup.LOG_TAG, e.getMessage());
                                    throw new IOException(e);
                                }
                            } else {
                                throw new IOException("Received response code from server: " + httpResponse.getStatusLine().getStatusCode());
                            }
                            return json;
                        }
                    });
                } catch (IOException e) {
                    Log.e(Queueup.LOG_TAG, "Http execution error: " + e.getMessage());
                    receiver.onException(e);
                    return;
                }
                receiver.onResult(response);
            }
        }).start();

        return get;
    }

    private static HttpPost sendPost(final String uri, final JSONObject json, final Queueup.CallReceiver<JSONObject> receiver ) {

        final HttpPost post = new HttpPost(Queueup.API_URL + uri);

        new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject response = null;

                post.setHeader("Content-type", "application/json");

                try {
                    post.setEntity(new StringEntity(json.toString()));
                } catch (UnsupportedEncodingException e) {
                    Log.e(Queueup.LOG_TAG, e.getMessage());
                    receiver.onException(new QueueupException(e));
                    return;
                }

                try {
                    response = new DefaultHttpClient().execute(post, new ResponseHandler<JSONObject> () {
                        @Override
                        public JSONObject handleResponse(HttpResponse httpResponse) throws IOException {
                            JSONObject json = null;
                            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                                try {
                                    json = new JSONObject(EntityUtils.toString(httpResponse.getEntity()));
                                } catch (JSONException e) {
                                    Log.e(Queueup.LOG_TAG, e.getMessage());
                                    throw new IOException(e);
                                }
                            } else {
                                throw new IOException("Received response code: " + httpResponse.getStatusLine().getStatusCode());
                            }
                            return json;
                        }
                    });
                } catch (IOException e) {
                    Log.e(Queueup.LOG_TAG, "Http execution error: " + e.getMessage());
                    receiver.onException(e);
                    return;
                }
                receiver.onResult(response);
            }

        }).start();

        return post;
    }

    public PlaylistClient getPlaylistClient(Queueup.CallReceiver<PlaylistClient> receiver) {
        if (playlistClient != null) {
            playlistClient.disconnect();
        }
        playlistClient = new PlaylistClient(clientToken, userId, receiver);
        return playlistClient;
    }

    public PlaylistPlayer getPlaylistPlayer(Queueup.CallReceiver<PlaylistClient> receiver) {
        if (playlistClient != null) {
            playlistClient.disconnect();
        }
        PlaylistPlayer player = new PlaylistPlayer(clientToken, userId, receiver);
        playlistClient = player;
        return player;
    }

    public String getClientToken() {
        return clientToken;
    }

    public String getUserId() {
        return userId;
    }
}
