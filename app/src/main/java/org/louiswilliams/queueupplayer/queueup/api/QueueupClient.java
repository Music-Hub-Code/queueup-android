package org.louiswilliams.queueupplayer.queueup.api;

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

import org.louiswilliams.queueupplayer.queueup.PlaybackReceiver;
import org.louiswilliams.queueupplayer.queueup.PlaylistClient;
import org.louiswilliams.queueupplayer.queueup.PlaylistPlayer;
import org.louiswilliams.queueupplayer.queueup.QueueUp;
import org.louiswilliams.queueupplayer.queueup.QueueUpException;
import org.louiswilliams.queueupplayer.queueup.objects.QueueUpApiCredential;
import org.louiswilliams.queueupplayer.queueup.objects.QueueUpPlaylist;
import org.louiswilliams.queueupplayer.queueup.objects.QueueUpUser;
import org.louiswilliams.queueupplayer.queueup.objects.SpotifyTrack;

public class QueueUpClient {

    private String clientToken, userId;
    private PlaylistPlayer playlistPlayer;
    private static HttpGet searchGetRequest;



    public QueueUpClient(String clientToken, String userId) {
        this.clientToken = clientToken;
        this.userId = userId;
    }


    public static void login(JSONObject json, final QueueUp.CallReceiver<QueueUpApiCredential> receiver){
        sendPost("/auth/login", json, new QueueUp.CallReceiver<JSONObject>() {
            @Override
            public void onResult(JSONObject result) {
                try {
                    String clientToken = result.getString("client_token");
                    String userId = result.getString("user_id");
                    receiver.onResult(new QueueUpApiCredential(userId, clientToken));
                } catch (JSONException e) {
                    receiver.onException(new QueueUpException(e));
                }
            }

            @Override
            public void onException(Exception e) {
                receiver.onException(e);
            }
        });
    }

    public void register(String name, String email, String password, final QueueUp.CallReceiver<QueueUpApiCredential> receiver){
        JSONObject json = new JSONObject();

        try {
            json.put("user_id", userId);
            json.put("client_token", clientToken);
            json.put("name", name);
            json.put("email", email);
            json.put("password", password);
        } catch (JSONException e) {
            receiver.onException(new QueueUpException("Error sending JSON: " + e.getMessage()));
            return;
        }

        sendPost("/auth/register", json, new QueueUp.CallReceiver<JSONObject>() {
            @Override
            public void onResult(JSONObject result) {
                try {
                    String userId = result.getString("user_id");
                    String clientToken = result.getString("client_token");
                    receiver.onResult(new QueueUpApiCredential(userId, clientToken));
                } catch (JSONException e) {
                    receiver.onException(new QueueUpException(e));
                }
            }

            @Override
            public void onException(Exception e) {
                receiver.onException(e);
            }
        });
    }

    public static void loginEmail(String email, String password, QueueUp.CallReceiver<QueueUpApiCredential> receiver) {
        JSONObject json = new JSONObject();

        try {
            json.put("email", email);
            json.put("password", password);
        } catch (JSONException e) {
            receiver.onException(new QueueUpException("Error sending JSON: " + e.getMessage()));
            return;
        }

        login(json, receiver);
    }

    public void loginFacebook (String accessToken, QueueUp.CallReceiver<QueueUpApiCredential> receiver) {
        JSONObject json = new JSONObject();

        try {
            json.put("facebook_access_token", accessToken);
            json.put("user_id", userId);
            json.put("client_token", clientToken);
        } catch (JSONException e) {
            receiver.onException(new QueueUpException("Error sending JSON: " + e.getMessage()));
            return;
        }

        login(json, receiver);
    }

    public static void loginInit(String deviceId, final QueueUp.CallReceiver<QueueUpApiCredential> receiver) {
        JSONObject json = new JSONObject();

        try {
            JSONObject device = new JSONObject();
            device.put("id", deviceId);
            json.put("device", device);
        } catch (JSONException e) {
            receiver.onException(new QueueUpException("Error sending JSON: " + e.getMessage()));
            return;
        }

        sendPost("/auth/init", json, new QueueUp.CallReceiver<JSONObject>() {
            @Override
            public void onResult(JSONObject result) {
                try {
                    String clientToken = result.getString("client_token");
                    String userId = result.getString("user_id");
                    receiver.onResult(new QueueUpApiCredential(userId, clientToken));
                } catch (JSONException e) {
                    receiver.onException(new QueueUpException(e));
                }
            }

            @Override
            public void onException(Exception e) {
                receiver.onException(e);
            }
        });
    }

    public void playlistGetList(final QueueUp.CallReceiver<List<QueueUpPlaylist>> receiver) {


        sendApiGet("/playlists", new QueueUp.CallReceiver<JSONObject>() {
            @Override
            public void onResult(JSONObject result) {
                try {
                    JSONArray playlistsJson = result.getJSONArray("playlists");
                    List<QueueUpPlaylist> playlistList = new ArrayList<>();

                    for (int i = 0; i < playlistsJson.length(); i++) {
                        playlistList.add(new QueueUpPlaylist(playlistsJson.getJSONObject(i)));
                    }

                    receiver.onResult(playlistList);
                } catch (JSONException e) {
                    receiver.onException(new QueueUpException("Invalid JSON  received: " + e.getMessage()));
                }
            }

            @Override
            public void onException(Exception e) {
                receiver.onException(e);
            }
        });

    }

    public void playlistGet(String playlistId, final QueueUp.CallReceiver<QueueUpPlaylist> receiver) {
        sendApiGet("/playlists/" + playlistId, new QueueUp.CallReceiver<JSONObject>() {
            @Override
            public void onResult(JSONObject result) {
                try {
                    JSONObject playlistJson = result.getJSONObject("playlist");
                    receiver.onResult(new QueueUpPlaylist(playlistJson));
                } catch (JSONException e) {
                    receiver.onException(new QueueUpException("Invalid JSON  received: " + e.getMessage()));
                }
            }

            @Override
            public void onException(Exception e) {
                receiver.onException(e);
            }
        });
    }

    public void playlistCreate(String playlistName, final QueueUp.CallReceiver<QueueUpPlaylist> receiver) {
        try {
            JSONObject playlist = new JSONObject();
            playlist.put("name", playlistName);

            JSONObject request = new JSONObject();
            request.put("playlist", playlist);

            sendApiPost("/playlists/new", request, new QueueUp.CallReceiver<JSONObject>() {
                @Override
                public void onResult(JSONObject result) {
                    try {
                        JSONObject playlistJSON = result.getJSONObject("playlist");
                        QueueUpPlaylist playlist = new QueueUpPlaylist(playlistJSON);
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
            Log.e(QueueUp.LOG_TAG, "JSON error adding track: " + e.getMessage());
        }
    }

    public void playlistRename(String playlistId, String newName, final QueueUp.CallReceiver<QueueUpPlaylist> receiver) {
        try {
            JSONObject request = new JSONObject();
            request.put("name", newName);
            sendApiPost("/playlists/" + playlistId + "/rename", request, new QueueUp.CallReceiver<JSONObject>() {
                @Override
                public void onResult(JSONObject result) {
                    try {
                        QueueUpPlaylist playlist = new QueueUpPlaylist(result.getJSONObject("playlist"));
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
            Log.e(QueueUp.LOG_TAG, "JSON error adding track: " + e.getMessage());
        }
    }

    public void playlistVoteOnTrack(String playlistId, String trackId, boolean vote, final QueueUp.CallReceiver<QueueUpPlaylist> receiver) {
        try {
            JSONObject request = new JSONObject();
            request.put("track_id", trackId);
            request.put("vote", vote);

            sendApiPost("/playlists/" + playlistId + "/vote", request, new QueueUp.CallReceiver<JSONObject>() {
                @Override
                public void onResult(JSONObject result) {
                    try {
                        QueueUpPlaylist playlist = new QueueUpPlaylist(result.getJSONObject("playlist"));
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
            Log.e(QueueUp.LOG_TAG, "JSON error adding track: " + e.getMessage());
        }
    }

    public void playlistDelete(String playlistId, final QueueUp.CallReceiver<Boolean> receiver) {
        JSONObject request = new JSONObject();
        sendApiPost("/playlists/" + playlistId + "/delete", request, new QueueUp.CallReceiver<JSONObject>() {
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

    public static void playlistAddTrack(String playlistId, String spotifyUri, final QueueUp.CallReceiver<JSONObject> receiver) {
        try {
            JSONObject request = new JSONObject();
            request.put("track_id", spotifyUri);
            sendPost("/playlists/" + playlistId + "/add", request, receiver);
        } catch (JSONException e) {
            Log.e(QueueUp.LOG_TAG, "JSON error adding track: " + e.getMessage());
        }
    }

    public void playlistDeleteTrack(String playlistId, String trackId, final QueueUp.CallReceiver<QueueUpPlaylist> receiver) {
        try {
            JSONObject request = new JSONObject();
            request.put("track_id", trackId);

            sendApiPost("/playlists/" + playlistId + "/delete/track", request, new QueueUp.CallReceiver<JSONObject>() {
                @Override
                public void onResult(JSONObject result) {
                    QueueUpPlaylist playlist = new QueueUpPlaylist(result.optJSONObject("playlist"));
                    receiver.onResult(playlist);
                }

                @Override
                public void onException(Exception e) {
                    receiver.onException(e);
                }
            });
        } catch (JSONException e) {
            Log.e(QueueUp.LOG_TAG, "JSON error deleting track:" + e.getMessage());
        }
    }

    public void getFriendsPlaylists(List<String> fbIds, final QueueUp.CallReceiver<List<QueueUpPlaylist>> receiver) {

        JSONArray jsonIds = new JSONArray();
        for (String id : fbIds) {
            jsonIds.put(id);
        }

        try {
            JSONObject request = new JSONObject();
            request.put("fb_ids", jsonIds);
            sendApiPost("/users/friends/playlists", request, new QueueUp.CallReceiver<JSONObject>() {
                @Override
                public void onResult(JSONObject result) {
                    JSONArray jsonPlaylists = result.optJSONArray("playlists");
                    List<QueueUpPlaylist> playlists = new ArrayList<QueueUpPlaylist>();
                    if (jsonPlaylists != null) {
                        for (int i = 0; i < jsonPlaylists.length(); i++) {
                            playlists.add(new QueueUpPlaylist(jsonPlaylists.optJSONObject(i)));
                        }
                    }
                    receiver.onResult(playlists);
                }

                @Override
                public void onException(Exception e) {
                    receiver.onException(e);
                }
            });
        } catch (JSONException e) {
            Log.e(QueueUp.LOG_TAG, "JSON error addind IDS: " + e.getMessage());
        }
    }

    public void getUserPlaylists(String userId, final QueueUp.CallReceiver<List<QueueUpPlaylist>> receiver) {
        sendApiGet("/users/" + userId + "/playlists", new QueueUp.CallReceiver<JSONObject>() {
            @Override
            public void onResult(JSONObject result) {
                Log.d(QueueUp.LOG_TAG, result.toString());
                JSONArray jsonPlaylists = result.optJSONArray("playlists");
                List<QueueUpPlaylist> playlists = new ArrayList<QueueUpPlaylist>();
                if (jsonPlaylists != null) {
                    for (int i = 0; i < jsonPlaylists.length(); i++) {
                        playlists.add(new QueueUpPlaylist(jsonPlaylists.optJSONObject(i)));
                    }
                }
                receiver.onResult(playlists);
            }

            @Override
            public void onException(Exception e) {
                receiver.onException(e);
            }
        });
    }

    public void userGet(String userId, final QueueUp.CallReceiver<QueueUpUser> receiver) {
        sendApiGet("/users/" + userId, new QueueUp.CallReceiver<JSONObject>() {
            @Override
            public void onResult(JSONObject result) {
                Log.d(QueueUp.LOG_TAG, result.toString());
                try {
                    receiver.onResult(new QueueUpUser(result.getJSONObject("user")));
                } catch (JSONException e) {
                    receiver.onException(new QueueUpException("Invalid JSON  received: " + e.getMessage()));
                }
            }

            @Override
            public void onException(Exception e) {
                receiver.onException(e);
            }
        });
    }


    public static void searchTracks(String query, int offset, final QueueUp.CallReceiver<List<SpotifyTrack>> receiver) {

        /* Short circuit if the string is empty */
        if (query.length() == 0) {
            receiver.onResult(new ArrayList<SpotifyTrack>());
            return;
        }

        if (searchGetRequest != null) {
            searchGetRequest.abort();
            searchGetRequest = null;
        }

        String encodedQuery;

        try {
            encodedQuery = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return;
        }

        searchGetRequest = sendGet("/search/tracks/" + encodedQuery + "/" + offset, new QueueUp.CallReceiver<JSONObject>() {
            @Override
            public void onResult(JSONObject result) {
                List<SpotifyTrack> resultsList = new ArrayList<>();
                try {
                    JSONArray tracks = result.getJSONArray("tracks");
                    for (int i = 0; i < tracks.length(); i++) {
                        JSONObject track = tracks.getJSONObject(i);
                        resultsList.add(new SpotifyTrack(track));
                    }
                    receiver.onResult(resultsList);
                } catch (JSONException e){
                    receiver.onException(new QueueUpException("Invalid JSON received: " + e.getMessage()));
                }
            }

            @Override
            public void onException(Exception e) {
                receiver.onException(e);
            }
        });
    }

    public static void searchPlaylists(String query, final QueueUp.CallReceiver<List<QueueUpPlaylist>> receiver) {
        /* Short circuit if the string is empty */
        if (query.length() == 0) {
            receiver.onResult(new ArrayList<QueueUpPlaylist>());
            return;
        }

        if (searchGetRequest != null) {
            searchGetRequest.abort();
            searchGetRequest = null;
        }

        String encodedQuery;

        try {
            encodedQuery = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return;
        }

        searchGetRequest = sendGet("/search/playlists/" + encodedQuery, new QueueUp.CallReceiver<JSONObject>() {
            @Override
            public void onResult(JSONObject result) {
                List<QueueUpPlaylist> resultsList = new ArrayList<>();
                try {
                    JSONArray tracks = result.getJSONArray("playlists");
                    for (int i = 0; i < tracks.length(); i++) {
                        JSONObject playlist = tracks.getJSONObject(i);
                        resultsList.add(new QueueUpPlaylist(playlist));
                    }
                    receiver.onResult(resultsList);
                } catch (JSONException e){
                    receiver.onException(new QueueUpException("Invalid JSON received: " + e.getMessage()));
                }
            }

            @Override
            public void onException(Exception e) {
                receiver.onException(e);
            }
        });
    }

    private HttpGet sendApiGet(String uri, final QueueUp.CallReceiver<JSONObject> receiver) {
        HttpGet get = new HttpGet(QueueUp.API_URL + uri);

        /* Use API credientials only when available */
        if (clientToken != null && userId != null) {
            ApiHmac.hmacSha1(clientToken).setHeadersForUser(get, userId);
        }

        return sendGet(get, receiver);
    }

    private HttpPost sendApiPost(String uri, JSONObject json, final QueueUp.CallReceiver<JSONObject> receiver) {
        HttpPost post = new HttpPost(QueueUp.API_URL + uri);

        /* Use API credientials only when available */
        if (clientToken != null && userId != null) {
            ApiHmac.hmacSha1(clientToken).setHeadersForUser(post, userId);
        }

        return sendPost(post, json, receiver);

    }

    private static HttpGet sendGet(String url, final QueueUp.CallReceiver<JSONObject> receiver) {
        return sendGet(new HttpGet(QueueUp.API_URL + url), receiver);
    }

    private static HttpGet sendGet(HttpGet get, final QueueUp.CallReceiver<JSONObject> receiver) {
        sendApiRequest(get, receiver);
        return get;
    }


    private static HttpRequestBase sendApiRequest(final HttpRequestBase request, final QueueUp.CallReceiver<JSONObject> receiver) {
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
                                Log.e(QueueUp.LOG_TAG, e.getMessage());
                                throw new IOException(e);
                            }

                            /* Anything other than 200 is an error */
                            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                                receiver.onResult(json);
                            } else {

                                /* Attempt to getString the error message */
                                JSONObject error = json.optJSONObject("error");
                                String message = "Error: ";
                                if (error != null) {
                                    message += error.optString("message", "UNKNOWN");
                                } else {
                                    message += httpResponse.getStatusLine().getStatusCode();
                                }

                                receiver.onException(new QueueUpException(message));
                            }
                            return null;
                        }
                    });
                } catch (IOException e) {
                    Log.e(QueueUp.LOG_TAG, "Http execution error: " + e.getMessage());
                    receiver.onException(e);
                }
            }
        }).start();

        return request;
    }

    private static HttpPost sendPost(String uri, JSONObject json, QueueUp.CallReceiver<JSONObject> receiver) {
        return sendPost(new HttpPost(QueueUp.API_URL + uri), json, receiver);
    }

    private static HttpPost sendPost(HttpPost post, final JSONObject json, final QueueUp.CallReceiver<JSONObject> receiver ) {

        post.setHeader("Content-type", "application/json");

        try {
            post.setEntity(new StringEntity(json.toString()));
        } catch (UnsupportedEncodingException e) {
            Log.e(QueueUp.LOG_TAG, e.getMessage());
            receiver.onException(new QueueUpException(e));
            return post;
        }

        sendApiRequest(post, receiver);

        return post;
    }

    public PlaylistClient getNewPlaylistClient(QueueUp.CallReceiver<PlaylistClient> receiver, PlaybackReceiver playbackReceiver) {
        return new PlaylistPlayer(clientToken, userId, receiver, playbackReceiver);
    }

    public PlaylistPlayer getPlaylistPlayer(QueueUp.CallReceiver<PlaylistClient> receiver, PlaybackReceiver playbackReceiver) {
        if (playlistPlayer != null) {
            playlistPlayer.disconnect();
        }
        PlaylistPlayer player = new PlaylistPlayer(clientToken, userId, receiver, playbackReceiver);
        playlistPlayer = player;
        return player;
    }

    public String getClientToken() {
        return clientToken;
    }

    public String getUserId() {
        return userId;
    }
}
