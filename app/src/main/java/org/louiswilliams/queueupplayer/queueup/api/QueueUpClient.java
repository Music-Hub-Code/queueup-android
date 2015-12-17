package org.louiswilliams.queueupplayer.queueup.api;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.louiswilliams.queueupplayer.R;
import org.louiswilliams.queueupplayer.queueup.PlaybackReceiver;
import org.louiswilliams.queueupplayer.queueup.PlaylistClient;
import org.louiswilliams.queueupplayer.queueup.PlaylistPlayer;
import org.louiswilliams.queueupplayer.queueup.QueueUp;
import org.louiswilliams.queueupplayer.queueup.QueueUpException;
import org.louiswilliams.queueupplayer.queueup.objects.QueueUpApiCredential;
import org.louiswilliams.queueupplayer.queueup.objects.QueueUpPlaylist;
import org.louiswilliams.queueupplayer.queueup.objects.QueueUpUser;
import org.louiswilliams.queueupplayer.queueup.objects.SpotifyTrack;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class QueueUpClient {

    private String clientToken, userId;
    private PlaylistPlayer playlistPlayer;
    private static SSLContext sslContext;
    private static HttpsURLConnection searchGetRequest;

    private static final String CF_CSR = "X.509";
    private static final String TLS = "TLS";


    public QueueUpClient(Context context, String clientToken, String userId) throws QueueUpException {
        this.clientToken = clientToken;
        this.userId = userId;

        if (sslContext == null) {
            sslContext = getSslContext(context);
        }
    }

    public static SSLContext getSslContext(Context applicationContext) throws QueueUpException {
        SSLContext sslContext = null;
        try {
            CertificateFactory cf = CertificateFactory.getInstance(CF_CSR);
            InputStream in = applicationContext.getResources().openRawResource(R.raw.queueup_crt);
            Certificate ca = cf.generateCertificate(in);
            in.close();

            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            sslContext = SSLContext.getInstance(TLS);
            sslContext.init(null, tmf.getTrustManagers(), null);

        } catch (CertificateException | IOException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
            throw new QueueUpException("Error loading SSL certificate!", e);
        }
        return sslContext;
    }

    public void login(JSONObject json, final QueueUp.CallReceiver<QueueUpApiCredential> receiver){
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

    public void loginEmail(String email, String password, QueueUp.CallReceiver<QueueUpApiCredential> receiver) {
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

    public void loginInit(String deviceId, final QueueUp.CallReceiver<QueueUpApiCredential> receiver) {
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

    public void playlistCreate(String playlistName, Location userLocation, final QueueUp.CallReceiver<QueueUpPlaylist> receiver) {
        try {
            JSONObject location = new JSONObject();
            location.put("latitude", userLocation.getLatitude());
            location.put("longitude", userLocation.getLongitude());
            location.put("accuracy", userLocation.getAccuracy());
            location.put("altitude", userLocation.getAltitude());

            JSONObject playlist = new JSONObject();
            playlist.put("name", playlistName);
            playlist.put("location", location);

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

    public void playlistRelocate(String playlistId, Location location, final QueueUp.CallReceiver<QueueUpPlaylist> receiver) {
        try {
            JSONObject locationJson = new JSONObject();
            JSONObject request = new JSONObject();

            locationJson.put("longitude", location.getLongitude());
            locationJson.put("latitude", location.getLatitude());
            request.put("location", locationJson);
            sendApiPost("/playlists/" + playlistId + "/relocate", request, new QueueUp.CallReceiver<JSONObject>() {
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
            Log.e(QueueUp.LOG_TAG, "JSON error relocating: " + e.getMessage());
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

    public void playlistAddTrack(String playlistId, String spotifyUri, final QueueUp.CallReceiver<JSONObject> receiver) {
        try {
            JSONObject request = new JSONObject();
            request.put("track_id", spotifyUri);
            sendApiPost("/playlists/" + playlistId + "/add", request, receiver);
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

    public void getNearbyPlaylists(Location location, final QueueUp.CallReceiver<List<QueueUpPlaylist>> receiver) {

        JSONObject locationJson = new JSONObject();

        try {
            locationJson.put("longitude", location.getLongitude());
            locationJson.put("latitude", location.getLatitude());
            JSONObject request = new JSONObject();
            request.put("location", locationJson);
            sendApiPost("/playlists/nearby", request, new QueueUp.CallReceiver<JSONObject>() {
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
            Log.e(QueueUp.LOG_TAG, "JSON error adding location: " + e.getMessage());
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


    public void searchTracks(String query, int offset, final QueueUp.CallReceiver<List<SpotifyTrack>> receiver) {

        /* Short circuit if the string is empty */
        if (query.length() == 0) {
            receiver.onResult(new ArrayList<SpotifyTrack>());
            return;
        }

        if (searchGetRequest != null) {
            searchGetRequest.disconnect();
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

    public void searchPlaylists(String query, final QueueUp.CallReceiver<List<QueueUpPlaylist>> receiver) {
        /* Short circuit if the string is empty */
        if (query.length() == 0) {
            receiver.onResult(new ArrayList<QueueUpPlaylist>());
            return;
        }

        if (searchGetRequest != null) {
            searchGetRequest.disconnect();
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

    private void sendApiGet(String uri, final QueueUp.CallReceiver<JSONObject> receiver) {
        try {
            URL get = new URL(QueueUp.API_URL + uri);

            sendApiRequest((HttpsURLConnection) get.openConnection(), null, receiver);
        } catch (IOException e) {
            receiver.onException(e);
        }
    }

    private HttpsURLConnection sendApiPost(String uri, JSONObject json, final QueueUp.CallReceiver<JSONObject> receiver) {
        HttpsURLConnection connection = null;
        try {
            URL post = new URL(QueueUp.API_URL + uri);
            connection = (HttpsURLConnection) post.openConnection();
            connection.setRequestMethod("POST");
            sendApiRequest(connection, json, receiver);
        } catch (IOException e) {
            receiver.onException(e);
        }
        return connection;
    }

    private HttpsURLConnection sendPost(String uri, JSONObject object, final QueueUp.CallReceiver<JSONObject> receiver) {
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(QueueUp.API_URL + uri);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            sendRequest(connection, object, receiver);
        } catch (IOException e) {
            receiver.onException(e);
        }
        return connection;
    }

    private HttpsURLConnection sendGet(String uri, final QueueUp.CallReceiver<JSONObject> receiver) {
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(QueueUp.API_URL + uri);
            connection = (HttpsURLConnection) url.openConnection();
            sendRequest(connection, null, receiver);
        } catch (IOException e) {
            receiver.onException(e);
        }
        return connection;
    }

    private HttpsURLConnection sendApiRequest(final HttpsURLConnection connection, final JSONObject data, final QueueUp.CallReceiver<JSONObject> receiver) {
        if (clientToken != null && userId != null) {
            ApiHmac.hmacSha1(clientToken).setAuthHeadersForUser(connection, userId);
        }
        return sendRequest(connection, data, receiver);
    }

    public HttpsURLConnection sendRequest(final HttpsURLConnection connection, final JSONObject data, final QueueUp.CallReceiver<JSONObject> receiver) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    connection.setSSLSocketFactory(sslContext.getSocketFactory());

                    boolean isPost = data != null;

                    /* POST data */
                    if (isPost) {
                        writeJsonToConnection(data, connection);
                    } else {
                        connection.connect();
                    }

                    JSONObject response = readJsonFromConnection(connection);
                    if (connection.getResponseCode() == 200) {

                        receiver.onResult(response);
                    } else {

                        /* Attempt to getString the error message */
                        JSONObject error = response.optJSONObject("error");
                        String message = "Error (" + connection.getResponseCode() + "): ";
                        if (error != null) {
                            message += error.optString("message", "UNKNOWN");
                        }

                        receiver.onException(new QueueUpException(message));
                    }


                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    receiver.onException(e);
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        }).start();

        return connection;
    }

    public JSONObject readJsonFromConnection(HttpURLConnection connection) throws IOException, JSONException {
        InputStream inputStream;
        try {
            inputStream = connection.getInputStream();
        } catch (FileNotFoundException e) {

            /* In the case the server sent data on an error */
            inputStream = connection.getErrorStream();
        }
        if (inputStream == null) return new JSONObject();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return new JSONObject(response.toString());
    }

    public void writeJsonToConnection(JSONObject json, HttpURLConnection connection) throws IOException {
        String content = json.toString();
        connection.setDoOutput(true);
        connection.setFixedLengthStreamingMode(content.length());
        connection.setRequestProperty("Content-Type", "application/json");

        DataOutputStream writer = new DataOutputStream(connection.getOutputStream());
        writer.write(content.getBytes());
        writer.close();
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
