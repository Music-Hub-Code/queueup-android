package org.louiswilliams.queueupplayer.queueup.objects;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import org.louiswilliams.queueupplayer.queueup.Queueup;

/**
 * Created by Louis on 5/23/2015.
 */
public class SpotifyTrack extends SpotifyObject {
    public long durationMs;
    public SpotifyAlbum album;
    public List<SpotifyArtist> artists;

    public SpotifyTrack (JSONObject obj) {
        super(obj);

        artists = new ArrayList<SpotifyArtist>();
        try {
            durationMs = obj.optLong("duration_ms");
            album = new SpotifyAlbum(obj.getJSONObject("album"));
            JSONArray jsonArtists = (JSONArray) obj.getJSONArray("artists");
            for (int i = 0; i < jsonArtists.length(); i++) {
                JSONObject artist = (JSONObject) jsonArtists.get(i);
                artists.add(new SpotifyArtist(artist));
            }
        } catch (JSONException e) {
            Log.e(Queueup.LOG_TAG, "JSON problem: " + e.getMessage());
        }
    }

    public String toString() {
        return "SpotifyTrack[id=" + id +
                ", uri=" + uri +
                ", name=" + name +
                ", durationMs=" + durationMs +
                ", album=" + album +
                ", artists=" + artists + "]";
    }
}