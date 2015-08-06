package org.louiswilliams.queueupplayer.queueup;

import org.json.JSONObject;

public class QueueUpException extends Exception {

    public QueueUpException(Throwable throwable) {
        super(throwable);
    }

    public QueueUpException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public QueueUpException(String message) {
        super(message);
    }

    public QueueUpException(JSONObject error) {
        super(error.optString("message"));
    }
}
