package org.louiswilliams.queueupplayer.queueup.objects;

import org.json.JSONObject;

/**
 * Created by Louis on 5/23/2015.
 */
public class QueueupUser extends QueueupObject {
    public String name;

    public String facebookId;
    public QueueupUser(JSONObject obj) {
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

