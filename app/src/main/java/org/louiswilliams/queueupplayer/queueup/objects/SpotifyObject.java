package org.louiswilliams.queueupplayer.queueup.objects;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.louiswilliams.queueupplayer.queueup.QueueUp;

public abstract class SpotifyObject {
    public String id;
    public String uri;
    public String name;


    public SpotifyObject(JSONObject obj) {
        id = obj.optString("id");
        uri = obj.optString("uri");
        name = obj.optString("name");
    }

    @Override
    public String toString() {
        return "SpotifyObject[id=" + id + ", uri=" + uri + ", name=" + name + "]";
    }
}