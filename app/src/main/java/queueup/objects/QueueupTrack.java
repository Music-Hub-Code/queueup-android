package queueup.objects;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import queueup.Queueup;

/**
 * Created by Louis on 5/23/2015.
 */
public class QueueupTrack extends QueueupObject{
    public SpotifyTrack track;

    public QueueupTrack(JSONObject obj) {
        super(obj);
        try {
            track = new SpotifyTrack(obj.getJSONObject("track"));

        } catch (JSONException e) {
            Log.e(Queueup.LOG_TAG, "JSON Problem: " + e.getMessage());
        }
    }
}
