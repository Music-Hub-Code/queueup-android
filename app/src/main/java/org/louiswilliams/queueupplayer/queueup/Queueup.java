package org.louiswilliams.queueupplayer.queueup;

public class Queueup {
    public static final String LOG_TAG = "QUEUEUP";
    public static final String QUEUEUP_HOST = "dev.qup.louiswilliams.org";
    public static final String API_URL = "http://" + QUEUEUP_HOST + "/api/v2";
    public static final String SOCKET_URL = "http://" + QUEUEUP_HOST + "/";
    public static final String STORE_NAME = "authStore";
    public static final String STORE_CLIENT_TOKEN = "clientToken";
    public static final String STORE_USER_ID = "userId";
    public static final String STORE_FACEBOOK_ID = "facebookId";

    public interface CallReceiver<T> {
        void onResult(T result);
        void onException(Exception e);
    }

}
