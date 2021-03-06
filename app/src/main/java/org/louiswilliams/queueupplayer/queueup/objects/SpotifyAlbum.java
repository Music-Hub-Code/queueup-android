package org.louiswilliams.queueupplayer.queueup.objects;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.louiswilliams.queueupplayer.queueup.QueueUp;

import java.io.IOError;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SpotifyAlbum extends SpotifyObject {
    public List<String> imageUrls;

    public SpotifyAlbum (JSONObject obj) {
        super(obj);
        imageUrls = new ArrayList<String>();
        try {
            JSONArray images = (JSONArray) obj.getJSONArray("images");
            for (int i = 0; i < images.length(); i++) {
                JSONObject image = (JSONObject) images.get(i);
                imageUrls.add(image.getString("url"));
            }

        } catch (JSONException e) {
            Log.e(QueueUp.LOG_TAG, "JSON Error: " + e.getMessage());
        }

    }

    @Override
    public String toString() {
        return "SpotifyAlbum[id=" + id +
                ", uri=" + uri +
                ", name=" + name +
                ", imageUrls=" + imageUrls + "]";
    }
}