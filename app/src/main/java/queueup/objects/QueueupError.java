package queueup.objects;

import org.json.JSONObject;

import queueup.QueueupException;

/**
 * Created by Louis on 5/25/2015.
 */
public class QueueupError {
    private String message;
    private String jsonString;

    public QueueupError(String message) {
        this.message = message;
    }

    public QueueupError(JSONObject error) {
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
