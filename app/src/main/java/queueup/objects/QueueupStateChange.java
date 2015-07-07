package queueup.objects;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import queueup.Queueup;

/**
 * Created by Louis on 6/1/2015.
 */
public class QueueupStateChange {
    public String trigger;
    public SpotifyTrack current;
    public List<QueueupTrack> tracks;
    public boolean playing;


    public QueueupStateChange(JSONObject obj) {
        try {
            trigger = obj.optString("trigger");
            playing = obj.optBoolean("play");
            JSONObject currentJson = obj.optJSONObject("track");
            if (currentJson != null ) {
                current = new SpotifyTrack(currentJson);
            }

            JSONArray jsonTracks = obj.optJSONArray("queue");
            if (jsonTracks != null) {
                tracks = new ArrayList<QueueupTrack>();

                for (int i = 0; i < jsonTracks.length(); i++) {
                    JSONObject track = (JSONObject) jsonTracks.get(i);
                    tracks.add(new QueueupTrack(track));
                }
            }
        } catch (JSONException e) {
            Log.e(Queueup.LOG_TAG, "JSON Error: " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "QueueupPlaylist[trigger=" + trigger +
                ", tracks=" + tracks +
                ", playing=" + playing +
                ", current=" + current + "]";
    }
}
