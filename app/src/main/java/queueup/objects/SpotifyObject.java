package queueup.objects;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import queueup.Queueup;

/**
 * Created by Louis on 5/23/2015.
 */
public abstract class SpotifyObject {
    public String id;
    public String uri;
    public String name;


    public SpotifyObject(JSONObject obj) {
        try {
            id = obj.getString("id");
            uri = obj.getString("uri");
            name = obj.getString("name");
        } catch (JSONException e) {
            Log.e(Queueup.LOG_TAG, "JSON problem: " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "SpotifyObject[id=" + id + ", uri=" + uri + ", name=" + name + "]";
    }
}