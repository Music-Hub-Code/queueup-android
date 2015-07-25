package org.louiswilliams.queueupplayer.queueup;

import android.content.Context;
import android.content.SharedPreferences;

public class QueueupStore {

    public static final String STORE_NAME = "authStore";
    public static final String USER_ID = "userId";
    public static final String CLIENT_TOKEN = "clientToken";
    public static final String FACEBOOK_ID = "facebookId";
    public static final String SPOTIFY_ID = "spotifyId";
    public static final String SPOTIFY_ENCRYPTED_REFRESH_TOKEN = "spotifyRefreshToken";
    public static final String SPOTIFY_ACCESS_TOKEN = "spotifyAccessToken";
    public static final String SPOTIFY_TOKEN_EXPIRATION_TIME_SECONDS = "spotifyExpiresIn";

    private SharedPreferences prefs;

    public QueueupStore(Context context) {
        prefs = context.getSharedPreferences(STORE_NAME, 0);
    }

    public static QueueupStore with(Context c) {
        return new QueueupStore(c);
    }

    public void putString(String field, String value) {
        prefs.edit().putString(field, value).apply();
    }

    public String getString(String field) {
        return prefs.getString(field, null);
    }

    public long getLong(String field) {
        return prefs.getLong(field, 0);
    }

    public void putLong(String field, long value) {
        prefs.edit().putLong(field, value).apply();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

}
