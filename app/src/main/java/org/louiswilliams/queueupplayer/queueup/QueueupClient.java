package org.louiswilliams.queueupplayer.queueup;

import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
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

import org.louiswilliams.queueupplayer.queueup.crypto.ApiHmac;
import org.louiswilliams.queueupplayer.queueup.objects.QueueupApiCredential;
import org.louiswilliams.queueupplayer.queueup.objects.QueueupPlaylist;
import org.louiswilliams.queueupplayer.queueup.objects.SpotifyTrack;

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


    public static void login(JSONObject json, final Queueup.CallReceiver<QueueupApiCredential> receiver){
        sendPost("/auth/login", json, new Queueup.CallReceiver<JSONObject>() {
            @Override
            public void onResult(JSONObject result) {
                try {
                    String clientToken = result.getString("client_token");
                    String userId = result.getString("user_id");
                    receiver.onResult(new QueueupApiCredential(userId, clientToken));
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

    public static void register(String email, String password, String name, final Queueup.CallReceiver<QueueupApiCredential> receiver){
        JSONObject json = new JSONObject();

        try {
            json.put("email", email);
            json.put("password", password);
            json.put("name", name);
        } catch (JSONException e) {
            receiver.onException(new QueueupException("Error sending JSON: " + e.getMessage()));
            return;
        }

        sendPost("/auth/register", json, new Queueup.CallReceiver<JSONObject>() {
            @Override
            public void onResult(JSONObject result) {
                try {
                    String userId = result.getString("user_id");
                    String clientToken = result.getString("client_token");
                    receiver.onResult(new QueueupApiCredential(userId, clientToken));
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

    public static void loginEmail(String email, String password, Queueup.CallReceiver<QueueupApiCredential> receiver) {
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

    public static void loginFacebook (String accessToken, Queueup.CallReceiver<QueueupApiCredential> receiver) {
        JSONObject json = new JSONObject();

        try {
            json.put("facebook_access_token", accessToken);
        } catch (JSONException e) {
            receiver.onException(new QueueupException("Error sending JSON: " + e.getMessage()));
            return;
        }

        login(json, receiver);
    }

    public void playlistGetList(final Queueup.CallReceiver<List<QueueupPlaylist>> receiver) {


        sendApiGet("/playlists", new Queueup.CallReceiver<JSONObject>() {
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

    public void playlistGet(String playlistId, final Queueup.CallReceiver<QueueupPlaylist> receiver) {
        sendApiGet("/playlists/" + playlistId, new Queueup.CallReceiver<JSONObject>() {
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

            sendApiPost("/playlists/new", request, new Queueup.CallReceiver<JSONObject>() {
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
            sendApiPost("/playlists/" + playlistId + "/rename", request, new Queueup.CallReceiver<JSONObject>() {
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

    public void playlistVoteOnTrack(String playlistId, String trackId, boolean vote, final Queueup.CallReceiver<QueueupPlaylist> receiver) {
        try {
            JSONObject request = new JSONObject();
            request.put("track_id", trackId);
            request.put("vote", vote);

            sendApiPost("/playlists/" + playlistId + "/vote", request, new Queueup.CallReceiver<JSONObject>() {
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
        sendApiPost("/playlists/" + playlistId + "/delete", request, new Queueup.CallReceiver<JSONObject>() {
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
            sendPost("/playlists/" + playlistId + "/add", request, receiver);
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

        searchGetRequest = sendGet("/search/tracks/" + encodedQuery + "/" + offset, new Queueup.CallReceiver<JSONObject>() {
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

    private HttpGet sendApiGet(String uri, final Queueup.CallReceiver<JSONObject> receiver) {
        HttpGet get = new HttpGet(Queueup.API_URL + uri);

        /* Use API credientials only when available */
        if (clientToken != null && userId != null) {
            ApiHmac.hmacSha1(clientToken).setHeadersForUser(get, userId);
        }

        return sendGet(get, receiver);
    }

    private HttpPost sendApiPost(String uri, JSONObject json, final Queueup.CallReceiver<JSONObject> receiver) {
        HttpPost post = new HttpPost(Queueup.API_URL + uri);

        /* Use API credientials only when available */
        if (clientToken != null && userId != null) {
            ApiHmac.hmacSha1(clientToken).setHeadersForUser(post, userId);
        }

        return sendPost(post, json, receiver);

    }

    private static HttpGet sendGet(String url, final Queueup.CallReceiver<JSONObject> receiver) {
        return sendGet(new HttpGet(Queueup.API_URL + url), receiver);
    }

    private static HttpGet sendGet(HttpGet get, final Queueup.CallReceiver<JSONObject> receiver) {
        sendApiRequest(get, receiver);
        return get;
    }


    private static HttpRequestBase sendApiRequest(final HttpRequestBase request, final Queueup.CallReceiver<JSONObject> receiver) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new DefaultHttpClient().execute(request, new ResponseHandler<Void>() {
                        @Override
                        public Void handleResponse(HttpResponse httpResponse) throws IOException {
                            JSONObject json;

                            /* Get the JSON response*/
                            try {
                                json = new JSONObject(EntityUtils.toString(httpResponse.getEntity()));
                            } catch (JSONException e) {
                                Log.e(Queueup.LOG_TAG, e.getMessage());
                                throw new IOException(e);
                            }

                            /* Anything other than 200 is an error */
                            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                                receiver.onResult(json);
                            } else {

                                /* Attempt to get the error message */
                                JSONObject error = json.optJSONObject("error");
                                String message = "Error: ";
                                if (error != null) {
                                    message += error.optString("message", "UNKNOWN");
                                } else {
                                    message += httpResponse.getStatusLine().getStatusCode();
                                }

                                receiver.onException(new QueueupException(message));
                            }
                            return null;
                        }
                    });
                } catch (IOException e) {
                    Log.e(Queueup.LOG_TAG, "Http execution error: " + e.getMessage());
                    receiver.onException(e);
                    return;
                }
            }
        }).start();

        return request;
    }

    private static HttpPost sendPost(String uri, JSONObject json, Queueup.CallReceiver<JSONObject> receiver) {
        return sendPost(new HttpPost(Queueup.API_URL + uri), json, receiver);
    }

    private static HttpPost sendPost(HttpPost post, final JSONObject json, final Queueup.CallReceiver<JSONObject> receiver ) {

        post.setHeader("Content-type", "application/json");

        try {
            post.setEntity(new StringEntity(json.toString()));
        } catch (UnsupportedEncodingException e) {
            Log.e(Queueup.LOG_TAG, e.getMessage());
            receiver.onException(new QueueupException(e));
            return post;
        }

        sendApiRequest(post, receiver);

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
