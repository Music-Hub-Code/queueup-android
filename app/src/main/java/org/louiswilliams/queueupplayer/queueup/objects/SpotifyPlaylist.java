package org.louiswilliams.queueupplayer.queueup.objects;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SpotifyPlaylist extends SpotifyObject {

    List<String> imageUrls;
    int totalTraacks;
    SpotifyUser owner;


    public SpotifyPlaylist(JSONObject obj) {
        super(obj);

        try {
            imageUrls = new ArrayList<>();
            JSONArray images = (JSONArray) obj.getJSONArray("images");
            for (int i = 0; i < images.length(); i++) {
                JSONObject image = (JSONObject) images.get(i);
                imageUrls.add(image.getString("url"));
            }

            JSONObject ownerJson = obj.optJSONObject("owner");
            owner = new SpotifyUser(ownerJson);

            JSONObject tracks = obj.optJSONObject("tracks");
            totalTraacks = tracks.optInt("total", 0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
