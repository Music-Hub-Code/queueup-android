package org.louiswilliams.queueupplayer.queueup;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Louis on 7/21/2015.
 */
public class QueueupStore {

    public static final String STORE_NAME = "authStore";
    public static final String USER_ID = "userId";
    public static final String CLIENT_TOKEN = "clientToken";
    public static final String FACEBOOK_ID = "facebookId";
    public static final String SPOTIFY_ID = "spotifyId";
    public static final String SPOTIFY_REFRESH_TOKEN = "spotifyRefreshToken";

    private SharedPreferences prefs;

    public QueueupStore(Context context) {
        prefs = context.getSharedPreferences(STORE_NAME, 0);
    }

    public static QueueupStore with(Context c) {
        return new QueueupStore(c);
    }

    public void put(String field, String value) {
        prefs.edit().putString(field, value).apply();
    }

    public String get(String field) {
        return prefs.getString(field, null);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

}
