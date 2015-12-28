package org.louiswilliams.queueupplayer.queueup.objects;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SpotifyPlaylist extends SpotifyObject {

    public List<String> imageUrls;
    public List<SpotifyTrack> tracks;
    public int totalTracks;
    public SpotifyUser owner;


    public SpotifyPlaylist(JSONObject obj) {
        super(obj);

        try {
            imageUrls = new ArrayList<>();
            tracks = new ArrayList<>();

            JSONArray images = (JSONArray) obj.getJSONArray("images");
            for (int i = 0; i < images.length(); i++) {
                JSONObject image = (JSONObject) images.get(i);
                imageUrls.add(image.getString("url"));
            }

            JSONObject ownerJson = obj.optJSONObject("owner");
            owner = new SpotifyUser(ownerJson);

            JSONObject tracksJson = obj.optJSONObject("tracks");
            totalTracks = tracksJson.optInt("total", 0);

            JSONArray items = tracksJson.optJSONArray("items");
            if (items != null) {
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i).getJSONObject("track");
                    tracks.add(new SpotifyTrack(item));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
