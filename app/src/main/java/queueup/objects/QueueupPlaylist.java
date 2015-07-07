package queueup.objects;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import queueup.Queueup;

public class QueueupPlaylist extends QueueupObject {
    public String name;
    public String adminId;
    public String adminName;
    public SpotifyTrack current;
    public List<QueueupTrack> tracks;
    public boolean playing;


    public QueueupPlaylist(JSONObject obj) {
        super(obj);
        tracks = new ArrayList<QueueupTrack>();
        try {
            name = obj.optString("name");
            adminId = obj.optString("admin");
            adminName = obj.optString("admin_name");
            playing = obj.optBoolean("play", false);
            JSONObject currentJson = obj.optJSONObject("current");
            if (currentJson != null ) {
                current = new SpotifyTrack(currentJson);
            }

            JSONArray jsonTracks = obj.optJSONArray("tracks");
            if (jsonTracks != null) {
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
        return "QueueupPlaylist[id=" + id +
                ", name=" + name +
                ", adminId=" + adminId +
                ", playing=" + playing +
                ", current=" + current + "]";
    }
}