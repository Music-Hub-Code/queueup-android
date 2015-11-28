package org.louiswilliams.queueupplayer.queueup.objects;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.louiswilliams.queueupplayer.queueup.QueueUp;

import java.util.ArrayList;
import java.util.List;

public class QueueUpTrack extends QueueUpObject {
    public SpotifyTrack track;
    public int votes;
    public String addedByUserId;
    public String addedByName;
    public List<String> voters;


    public QueueUpTrack(JSONObject obj) {
        super(obj);
        try {
            track = new SpotifyTrack(obj.getJSONObject("track"));

            JSONObject addedBy = obj.optJSONObject("addedBy");
            if (addedBy != null) {
                addedByUserId = addedBy.optString("_id");
                addedByName = addedBy.optString("name");
            }

            votes = obj.optInt("votes");
            voters = new ArrayList<String>();
            JSONArray votersJson = obj.optJSONArray("voters");
            if (votersJson != null) {
                for (int i = 0; i < votersJson.length(); i++) {
                    String id = votersJson.getJSONObject(i).getString("_id");
                    voters.add(id);
                }
            }

        } catch (JSONException e) {
            Log.e(QueueUp.LOG_TAG, "JSON Problem: " + e.getMessage());
        }
    }
}
