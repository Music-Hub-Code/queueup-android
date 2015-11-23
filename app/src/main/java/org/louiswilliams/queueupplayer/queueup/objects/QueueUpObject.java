package org.louiswilliams.queueupplayer.queueup.objects;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import org.louiswilliams.queueupplayer.queueup.QueueUp;

/**
 * Created by Louis on 5/23/2015.
 */
public abstract class QueueUpObject {
    public String id;

    public QueueUpObject(JSONObject obj) {
        try {
            id = obj.getString("_id");
        } catch (JSONException e) {
            Log.e(QueueUp.LOG_TAG, "JSON problem: " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "QueueupObject[id=" + id + "]";
    }
}
