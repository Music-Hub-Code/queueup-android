package org.louiswilliams.queueupplayer.queueup.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.louiswilliams.queueupplayer.queueup.QueueUp;
import org.louiswilliams.queueupplayer.queueup.objects.SpotifyPlaylist;
import org.louiswilliams.queueupplayer.queueup.objects.SpotifyUser;

import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

public class SpotifyClient {

    public static final String SPOTIFY_API_FORMAT = "https://api.spotify.com/v1/%s";

    String accessToken;

    public SpotifyClient(String accessToken) {
        this.accessToken = accessToken;
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

    public void apiRequest(final String uri, final JSONObject data, final QueueUp.CallReceiver<JSONObject> receiver) {
        final String urlString = String.format(SPOTIFY_API_FORMAT, uri);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(urlString);
                    HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                    connection.setRequestProperty("Authorization", "Bearer " + accessToken);

                    QueueUpClient.sendRequest(connection, data, receiver);
                } catch (IOException e) {
                    receiver.onException(e);
                }
            }
        }).start();
    }

}
