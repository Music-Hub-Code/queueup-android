package org.louiswilliams.queueupplayer.queueup.objects;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import org.louiswilliams.queueupplayer.queueup.QueueUp;

/**
 * Created by Louis on 5/23/2015.
 */
public class QueueUpTrack extends QueueUpObject {
    public SpotifyTrack track;
    public int votes;
    public List<String> voters;


    public QueueUpTrack(JSONObject obj) {
        super(obj);
        try {
            track = new SpotifyTrack(obj.getJSONObject("track"));
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
