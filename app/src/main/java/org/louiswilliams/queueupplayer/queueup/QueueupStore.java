package org.louiswilliams.queueupplayer.queueup;

import android.content.Context;
import android.content.SharedPreferences;

public class QueueUpStore {

    public static final String STORE_NAME = "authStore";
    public static final String USER_ID = "userId";
    public static final String CLIENT_TOKEN = "clientToken";
    public static final String FACEBOOK_ID = "facebookId";
    public static final String DEVICE_ID = "deviceId";
    public static final String EMAIL_ADDRESS = "emailAddress";
    public static final String SPOTIFY_ENCRYPTED_REFRESH_TOKEN = "spotifyRefreshToken";
    public static final String SPOTIFY_ACCESS_TOKEN = "spotifyAccessToken";
    public static final String SPOTIFY_TOKEN_EXPIRATION_TIME_SECONDS = "spotifyExpiresIn";

    private SharedPreferences prefs;

    public QueueUpStore(Context context) {
        prefs = context.getSharedPreferences(STORE_NAME, 0);
    }

    public static QueueUpStore with(Context c) {
        return new QueueUpStore(c);
    }

    public void putString(String field, String value) {
        prefs.edit().putString(field, value).apply();
    }

    public String getString(String field) {
        return prefs.getString(field, null);
    }

    public boolean getBoolean(String field) {
        return prefs.getBoolean(field, false);
    }

    public void putBoolean(String field, boolean value) {
        prefs.edit().putBoolean(field, value).apply();
    }

    public long getLong(String field) {
        return prefs.getLong(field, 0);
    }

    public void putLong(String field, long value) {
        prefs.edit().putLong(field, value).apply();
    }

    public void clear() {
        prefs.edit().clear().commit();
    }

}
