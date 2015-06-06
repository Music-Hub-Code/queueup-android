package queueup;

import android.util.Log;

import com.github.nkzawa.emitter.Emitter;

import org.json.JSONException;
import org.json.JSONObject;

import queueup.objects.QueueupStateChange;

/**
 * Created by Louis on 6/1/2015.
 */
public class PlaylistPlayer extends PlaylistClient {

    public PlaylistPlayer(String clientId, String email, Queueup.CallReceiver<PlaylistClient> receiver) {
        super(clientId, email, receiver);
    }

    public void subscribe(String playlistId, boolean force, final StateChangeListener listener) {
        if (mSocket != null) {
            JSONObject params = new JSONObject();
            try {
                params.put("playlist_id", playlistId);
                params.put("force", force);

                mSubcription = playlistId;

                mSocket.on("state_change", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        QueueupStateChange state = new QueueupStateChange((JSONObject) args[0]);
                        listener.onStateChange(state);
                    }
                });

                mSocket.on("player_subscribe_response", new Emitter.Listener() {
                    @Override
                    public void call(Object... args)  {
                        JSONObject error = ((JSONObject) args[0]).optJSONObject("error");

                        if (error != null) {
                            String message = "Problem subscribing: " + error.toString();
                            listener.onError(message);
                        }
                    }
                });

                mSocket.emit("player_subscribe", params);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void subscribe(String playlistId, final StateChangeListener listener) {
        subscribe(playlistId, false, listener);
    }

    public void updateTrackPlaying(boolean playing) {
        if (isConnected()) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("playing", playing);
                mSocket.emit("track_play_pause", obj);
            } catch (JSONException e) {
                Log.e(Queueup.LOG_TAG, e.getMessage());
            }
        }
    }

    public void updateTrackDone() {
        if (isConnected()) {
            mSocket.emit("track_finished");
        }
    }

    public void updateTrackProgress(int progress, int duration) {
        if (isConnected()) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("progress", progress);
                obj.put("duration", duration);
                mSocket.emit("track_progress", obj);
            } catch (JSONException e) {
                Log.e(Queueup.LOG_TAG, e.getMessage());
            }
        }
    }

}
