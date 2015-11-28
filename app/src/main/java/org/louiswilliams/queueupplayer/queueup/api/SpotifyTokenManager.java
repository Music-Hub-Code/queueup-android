package org.louiswilliams.queueupplayer.queueup.api;

import org.json.JSONException;
import org.json.JSONObject;
import org.louiswilliams.queueupplayer.queueup.QueueUp;
import org.louiswilliams.queueupplayer.queueup.QueueUpStore;

import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;


public class SpotifyTokenManager{

    private static final String SWAP_URL = QueueUp.API_URL + "/spotify/swap";
    private static final String REFRESH_URL = QueueUp.API_URL + "/spotify/refresh";

    private QueueUpStore store;
    private QueueUpClient client;

    public SpotifyTokenManager(QueueUpClient client, QueueUpStore store) {
        this.store = store;
        this.client = client;
    }

    public static SpotifyTokenManager with(QueueUpClient client, QueueUpStore store) {
        return new SpotifyTokenManager(client, store);
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
                HttpsURLConnection connection = null;
                try {
                    connection = (HttpsURLConnection) swapUrl.openConnection();

                    JSONObject postJSON = new JSONObject();
                    postJSON.put("code", code);

                    client.sendRequest(connection, postJSON, new QueueUp.CallReceiver<JSONObject>() {
                        @Override
                        public void onResult(JSONObject response) {

                            try {
                                String accessToken = response.getString("access_token");
                                String refreshToken = response.getString("refresh_token");
                                long expiresIn = response.getLong("expires_in");

                                storeAccessToken(accessToken);
                                storeEncryptedRefreshToken(refreshToken);
                                storeTokenExpiresIn(expiresIn);

                                receiver.onResult(accessToken);
                            } catch (JSONException e) {
                                receiver.onException(e);
                            }

                        }

                        @Override
                        public void onException(Exception e) {
                            receiver.onException(e);
                        }
                    });

                } catch (Exception e) {
                    receiver.onException(e);
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

                HttpsURLConnection connection = null;
                try {
                    connection = (HttpsURLConnection) refreshUrl.openConnection();

                    JSONObject postJSON = new JSONObject();
                    postJSON.put("refresh_token", getEncryptedRefreshToken());

                    client.sendRequest(connection, postJSON, new QueueUp.CallReceiver<JSONObject>() {
                        @Override
                        public void onResult(JSONObject response) {
                            try {
                                String accessToken = response.getString("access_token");
                                long expiresIn = response.getLong("expires_in");

                                storeAccessToken(accessToken);
                                storeTokenExpiresIn(expiresIn);

                                receiver.onResult(accessToken);
                            } catch (JSONException e) {
                                receiver.onException(e);
                            }
                        }

                        @Override
                        public void onException(Exception e) {
                            receiver.onException(e);
                        }
                    });

                } catch (Exception e) {
                    receiver.onException(e);
                }

            }
        }).start();
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
