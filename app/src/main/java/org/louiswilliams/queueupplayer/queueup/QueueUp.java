package org.louiswilliams.queueupplayer.queueup;

public class QueueUp {
    public static final String LOG_TAG = "QUEUEUP";
    public static final String QUEUEUP_HOST = "stage.queueup.io";
    public static final String API_URL = "https://" + QUEUEUP_HOST + "/api/v2";
    public static final String SOCKET_URL = "http://" + QUEUEUP_HOST + "/";
    public static final String SPOTIFY_LOGIN_REDIRECT_URI = "queueup://callback";

    public interface CallReceiver<T> {
        void onResult(T result);
        void onException(Exception e);
    }

}
