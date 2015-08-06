package org.louiswilliams.queueupplayer.queueup.api;

import org.json.JSONException;
import org.json.JSONObject;
import org.louiswilliams.queueupplayer.queueup.QueueUp;
import org.louiswilliams.queueupplayer.queueup.QueueUpException;
import org.louiswilliams.queueupplayer.queueup.QueueUpStore;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class SpotifyTokenManager{

    private static final String SWAP_URL = QueueUp.API_URL + "/spotify/swap";
    private static final String REFRESH_URL = QueueUp.API_URL + "/spotify/refresh";

    private QueueUpStore store;

    public SpotifyTokenManager(QueueUpStore store) {
        this.store = store;
    }

    public static SpotifyTokenManager with(QueueUpStore store) {
        return new SpotifyTokenManager(store);
    }

    /* If there is an access token at all */
    public boolean haveAccessToken() {
        return getAccessToken() != null;
    }

    /* If we have an access token and it hasn't expired */
    public boolean haveValidAccessToken() {
        return (haveAccessToken() && !isExpired(getTokenExpirationTime()));
    }

    /* If the token's expiration time comes before the current time */
    public static boolean isExpired(long expireDate) {
        long now = System.currentTimeMillis() / 1000l;
        return (now > expireDate);
    }

    /* Exchange an authentication code for an access token and refresh token */
    public void swapCodeForToken(final String code, final QueueUp.CallReceiver<String> receiver) {
        final URL swapUrl;
        try {
            swapUrl = new URL(SWAP_URL);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }

        /* Create a new thread to open a connection */
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    connection = (HttpURLConnection) swapUrl.openConnection();
                    JSONObject postJSON = new JSONObject();
                    postJSON.put("code", code);

                    writeJsonToConnection(postJSON, connection);

                    JSONObject response = readJsonFromConnection(connection);

                    if (connection.getResponseCode() == 200) {

                        String accessToken = response.getString("access_token");
                        String refreshToken = response.getString("refresh_token");
                        long expiresIn = response.getLong("expires_in");

                        /* We store these tokens to cache future Spotify logins */
                        storeAccessToken(accessToken);
                        storeEncryptedRefreshToken(refreshToken);
                        storeTokenExpiresIn(expiresIn);

                        receiver.onResult(accessToken);
                    } else {

                        receiver.onException(new QueueUpException("Bad request: " + response));
                    }

                    connection.disconnect();
                } catch (Exception e) {
                    receiver.onException(e);
                } finally {
                    if (connection != null) connection.disconnect();
                }

            }
        }).start();
    }

    /* Exchange a refresh token for a new access token */
    public void refreshToken(final QueueUp.CallReceiver<String> receiver) {
        final URL refreshUrl;
        try {
            refreshUrl = new URL(REFRESH_URL);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {

                HttpURLConnection connection = null;
                try {
                    connection = (HttpURLConnection) refreshUrl.openConnection();
                    JSONObject postJSON = new JSONObject();
                    postJSON.put("refresh_token", getEncryptedRefreshToken());

                    writeJsonToConnection(postJSON, connection);

                    JSONObject response = readJsonFromConnection(connection);

                    if (connection.getResponseCode() == 200) {

                        String accessToken = response.getString("access_token");
                        long expiresIn = response.getLong("expires_in");

                        storeAccessToken(accessToken);
                        storeTokenExpiresIn(expiresIn);

                        receiver.onResult(accessToken);
                    } else {
                        receiver.onException(new QueueUpException("Bad request: " + response));
                    }

                    connection.disconnect();
                } catch (Exception e) {
                    receiver.onException(e);
                } finally {
                    if (connection != null) connection.disconnect();
                }

            }
        }).start();
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

    public String getAccessToken() {
        return store.getString(QueueUpStore.SPOTIFY_ACCESS_TOKEN);
    }

    public void storeAccessToken(String token) {
        store.putString(QueueUpStore.SPOTIFY_ACCESS_TOKEN, token);
    }

    public long getTokenExpirationTime() {
        return store.getLong(QueueUpStore.SPOTIFY_TOKEN_EXPIRATION_TIME_SECONDS);
    }

    public void storeTokenExpiresIn(long expiresIn) {
        store.putLong(QueueUpStore.SPOTIFY_TOKEN_EXPIRATION_TIME_SECONDS, expiresIn + (System.currentTimeMillis() / 1000l));
    }

    public String getEncryptedRefreshToken() {
        return store.getString(QueueUpStore.SPOTIFY_ENCRYPTED_REFRESH_TOKEN);
    }

    public void storeEncryptedRefreshToken(String token) {
        store.putString(QueueUpStore.SPOTIFY_ENCRYPTED_REFRESH_TOKEN, token);
    }

}
