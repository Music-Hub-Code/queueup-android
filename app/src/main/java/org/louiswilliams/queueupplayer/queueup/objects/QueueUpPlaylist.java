package org.louiswilliams.queueupplayer.queueup.objects;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import org.louiswilliams.queueupplayer.queueup.QueueUp;

public class QueueUpPlaylist extends QueueUpObject {
    public String name;
    public String adminId;
    public String adminName;
    public SpotifyTrack current;
    public double latitude;
    public double longitude;
    public double altitude;
    public double accuracy;
    public List<QueueUpTrack> tracks;
    public boolean playing;


    public QueueUpPlaylist(JSONObject obj) {
        super(obj);
        tracks = new ArrayList<>();
        try {
            name = obj.optString("name");
            adminId = obj.optString("admin");
            adminName = obj.optString("admin_name");
            playing = obj.optBoolean("play", false);

            latitude = obj.optDouble("latitude", -1);
            longitude = obj.optDouble("longitude", -1);
            altitude = obj.optDouble("altitude", -1);
            accuracy = obj.optDouble("accuracy", -1);

            JSONObject currentJson = obj.optJSONObject("current");
            if (currentJson != null ) {
                current = new SpotifyTrack(currentJson);
            }

            JSONArray jsonTracks = obj.optJSONArray("tracks");
            if (jsonTracks != null) {
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
        return "QueueupPlaylist[id=" + id +
                ", name=" + name +
                ", adminId=" + adminId +
                ", playing=" + playing +
                ", current=" + current + "]";
    }
}