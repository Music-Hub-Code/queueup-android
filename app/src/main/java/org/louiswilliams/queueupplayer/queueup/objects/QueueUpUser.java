package org.louiswilliams.queueupplayer.queueup.objects;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.louiswilliams.queueupplayer.queueup.QueueUp;

/**
 * Created by Louis on 5/23/2015.
 */
public class QueueUpUser extends QueueUpObject {
    public String name;

    public QueueUpUser(JSONObject obj) {
        super(obj);
        try {
            name = obj.getString("name");
        } catch (JSONException e) {
            Log.e(QueueUp.LOG_TAG, "JSON Error: " + e.getMessage());
        }
    }
}

