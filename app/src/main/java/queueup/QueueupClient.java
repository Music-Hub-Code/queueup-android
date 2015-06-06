package queueup;

import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import queueup.objects.QueueupCredential;
import queueup.objects.QueueupPlaylist;

/**
 * Created by Louis on 5/23/2015.
 */
public class QueueupClient {

    private String clientToken, userId;
    private PlaylistClient playlistClient;
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

    public void playlistGetList(final Queueup.CallReceiver<List<QueueupPlaylist>> receiver) {
        sendApiPost("/api/playlists", new JSONObject(), new Queueup.CallReceiver<JSONObject>() {
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
        sendApiPost("/api/playlists/" + playlistId, new JSONObject(), new Queueup.CallReceiver<JSONObject>() {
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

    private void sendApiPost(String uri, JSONObject json, final Queueup.CallReceiver<JSONObject> receiver) {
        try {
            json.put("client_token", clientToken);
            json.put("user_id", userId);
        } catch (JSONException e) {
            Log.e(Queueup.LOG_TAG, "JSON Error: " + e.getMessage());
            receiver.onException(new QueueupException(e));
            return;
        }

        sendPost(uri, json, new Queueup.CallReceiver<JSONObject>() {
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

    private static void sendPost(final String uri, final JSONObject json, final Queueup.CallReceiver<JSONObject> receiver ) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject response = null;

                HttpPost post = new HttpPost(Queueup.API_URL + uri);
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
                            }
                            return json;
                        }
                    });
                } catch (IOException e) {
                    Log.e(Queueup.LOG_TAG, e.getMessage());
                    receiver.onException(e);
                    return;
                }
                receiver.onResult(response);
            }

        }).start();
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
