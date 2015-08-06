package org.louiswilliams.queueupplayer.queueup.objects;

import org.json.JSONObject;

/**
 * Created by Louis on 5/25/2015.
 */
public class QueueUpError {
    private String message;
    private String jsonString;

    public QueueUpError(String message) {
        this.message = message;
    }

    public QueueUpError(JSONObject error) {
        this.jsonString = error.toString();
        this.message = error.optString("message");
    }

    public String getJsonString() {
        return jsonString;
    }

    public String getMessage() {
        return message;
    }
}
