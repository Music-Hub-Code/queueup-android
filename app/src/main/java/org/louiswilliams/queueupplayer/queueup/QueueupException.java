package org.louiswilliams.queueupplayer.queueup;

import org.json.JSONObject;

public class QueueupException extends Exception {

    public QueueupException(Throwable throwable) {
        super(throwable);
    }

    public QueueupException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public QueueupException(String message) {
        super(message);
    }

    public QueueupException(JSONObject error) {
        super(error.optString("message"));
    }
}
