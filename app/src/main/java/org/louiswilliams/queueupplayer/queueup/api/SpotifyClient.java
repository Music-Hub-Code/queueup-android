package org.louiswilliams.queueupplayer.queueup.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.louiswilliams.queueupplayer.queueup.QueueUp;
import org.louiswilliams.queueupplayer.queueup.objects.SpotifyAlbum;
import org.louiswilliams.queueupplayer.queueup.objects.SpotifyPlaylist;
import org.louiswilliams.queueupplayer.queueup.objects.SpotifyTrack;
import org.louiswilliams.queueupplayer.queueup.objects.SpotifyUser;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.HttpsURLConnection;

public class SpotifyClient {

    public static final String SPOTIFY_API_FORMAT = "https://api.spotify.com/v1/%s";

    private String accessToken;
    private Map<String,String> eTags;

    public SpotifyClient(String accessToken) {
        this.accessToken = accessToken;
        this.eTags = new ConcurrentHashMap<>();
    }

    public static SpotifyClient with(String accessToken) {
        return new SpotifyClient(accessToken);
    }

    public void getMe(final QueueUp.CallReceiver<SpotifyUser> receiver) {
        apiRequest("me", null, new QueueUp.CallReceiver<JSONObject>() {
            @Override
            public void onResult(JSONObject result) {
                SpotifyUser me = new SpotifyUser(result);
                receiver.onResult(me);
            }

            @Override
            public void onException(Exception e) {
                receiver.onException(e);
            }
        });
    }

    public void getMyPlaylists(final QueueUp.CallReceiver<List<SpotifyPlaylist>> receiver) {
        apiRequest("me/playlists", null, new QueueUp.CallReceiver<JSONObject>() {

            @Override
            public void onResult(JSONObject result) {
                try {
                    List<SpotifyPlaylist> playlists = new ArrayList<>();
                    JSONArray items = result.getJSONArray("items");
                    for (int i = 0; i < items.length(); i++) {
                        playlists.add(new SpotifyPlaylist(items.getJSONObject(i)));
                    }
                    receiver.onResult(playlists);
                } catch (JSONException e) {
                    receiver.onException(e);
                }
            }

            @Override
            public void onException(Exception e) {
                receiver.onException(e);
            }
        });
    }

    public void getUser(String userId, final QueueUp.CallReceiver<SpotifyUser> receiver) {
        apiRequest("users/" + userId, null, new QueueUp.CallReceiver<JSONObject>() {
            @Override
            public void onResult(JSONObject result) {
                SpotifyUser user = new SpotifyUser(result);
                receiver.onResult(user);
            }

            @Override
            public void onException(Exception e) {
                receiver.onException(e);
            }
        });
    }

    public void getUserPlaylist(String userId, final String playlistId, final QueueUp.CallReceiver<SpotifyPlaylist> receiver) {
        apiRequest("users/" + userId + "/playlists/" + playlistId, null, new QueueUp.CallReceiver<JSONObject>() {
            @Override
            public void onResult(JSONObject result) {
                receiver.onResult(new SpotifyPlaylist(result));
            }

            @Override
            public void onException(Exception e) {
                receiver.onException(e);
            }
        });
    }

    public void apiRequest(final String uri, final JSONObject data, final QueueUp.CallReceiver<JSONObject> receiver) {
        final String urlString = String.format(SPOTIFY_API_FORMAT, uri);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(urlString);
                    final HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                    connection.setRequestProperty("Authorization", "Bearer " + accessToken);
//                    String etag = eTags.get(urlString);
//                    if (etag != null) {
//                        connection.setRequestProperty("If-None-Match", etag);
//                    }

                    /* Handle the ETag caching */
                    QueueUpClient.sendRequest(connection, data, new QueueUp.CallReceiver<JSONObject>() {
                        @Override
                        public void onResult(JSONObject result) {
//                            String etag = connection.getHeaderField("ETag");
//                            if (etag != null) {
//                                eTags.put(urlString, etag);
//                            }
                            receiver.onResult(result);
                        }

                        @Override
                        public void onException(Exception e) {
                            receiver.onException(e);
                        }
                    });
                } catch (IOException e) {
                    receiver.onException(e);
                }
            }
        }).start();
    }

}
