package queueup;

/**
 * Created by Louis on 5/23/2015.
 */
public class Queueup {
    public static final String LOG_TAG = "QUEUEUP";
    public static final String API_URL= "http://dev.qup.louiswilliams.org";
    public static final String STORE_NAME = "authStore";

    public interface CallReceiver<T> {
        void onResult(T result);
        void onException(Exception e);
    }

}
