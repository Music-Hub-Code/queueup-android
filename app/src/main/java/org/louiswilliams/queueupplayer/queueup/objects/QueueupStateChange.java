package org.louiswilliams.queueupplayer.queueup.objects;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import org.louiswilliams.queueupplayer.queueup.QueueUp;

/**
 * Created by Louis on 6/1/2015.
 */
public class QueueUpStateChange {
    public String trigger;
    public SpotifyTrack current;
    public List<QueueUpTrack> tracks;
    public boolean playing;


    public QueueUpStateChange(JSONObject obj) {
        try {
            trigger = obj.optString("trigger");
            playing = obj.optBoolean("play");
            JSONObject currentJson = obj.optJSONObject("track");
            if (currentJson != null ) {
                current = new SpotifyTrack(currentJson);
            }

            JSONArray jsonTracks = obj.optJSONArray("queue");
            if (jsonTracks != null) {
                tracks = new ArrayList<QueueUpTrack>();

                for (int i = 0; i < jsonTracks.length(); i++) {
                    JSONObject track = (JSONObject) jsonTracks.get(i);
                    tracks.add(new QueueUpTrack(track));
                }
            }
        } catch (JSONException e) {
            Log.e(QueueUp.LOG_TAG, "JSON Error: " + e.getMessage());
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
