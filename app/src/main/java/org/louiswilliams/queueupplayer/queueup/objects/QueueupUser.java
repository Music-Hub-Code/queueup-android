package org.louiswilliams.queueupplayer.queueup.objects;

import org.json.JSONObject;

/**
 * Created by Louis on 5/23/2015.
 */
public class QueueUpUser extends QueueUpObject {
    public String name;

    public String facebookId;
    public QueueUpUser(JSONObject obj) {
        super(obj);
//        try {
//            name = obj.getString("name");
//            facebookId = obj.getJSONObject("facebook").getString("id");
//
//        } catch (JSONException e) {
//            Log.e(Queueup.LOG_TAG, "JSON Error: " + e.getMessage());
//        }
    }
}

