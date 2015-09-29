package org.louiswilliams.queueupplayer.queueup;

import android.util.Log;


import org.json.JSONException;
import org.json.JSONObject;

import org.louiswilliams.queueupplayer.queueup.objects.QueueUpStateChange;

import io.socket.emitter.Emitter;

public class PlaylistPlayer extends PlaylistClient implements PlaybackController {

    public PlaylistPlayer(String clientToken, String userId, QueueUp.CallReceiver<PlaylistClient> receiver, PlaybackReceiver playbackReceiver) {
        super(clientToken, userId, receiver, playbackReceiver);
    }

    public void subscribe(String playlistId, boolean force) {
        if (mSocket != null) {
            JSONObject params = new JSONObject();
            try {
                params.put("playlist_id", playlistId);
                params.put("force", force);

                mSubscription = playlistId;

                mSocket.on("state_change", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        QueueUpStateChange state = new QueueUpStateChange((JSONObject) args[0]);
                        stateChangeListener.onStateChange(state);
                        currentState = state;
                    }
                });

                mSocket.on("player_subscribe_response", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        JSONObject error = ((JSONObject) args[0]).optJSONObject("error");

                        if (error != null) {
                            String message = "Problem subscribing: " + error.toString();
                            stateChangeListener.onError(message);
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
    public void updatePlaybackReady() {
        Log.d(QueueUp.LOG_TAG, "Playback ready");

        for(PlaylistListener listener : playlistListeners) {
            listener.onPlayerReady();
        }
    }

    @Override
    public void updateTrackPlaying(boolean playing) {

        Log.d(QueueUp.LOG_TAG, "Player playing: " + playing);
        for (PlaylistListener listener : playlistListeners) {
            listener.onPlayingChanged(playing);
        }

        if (isConnected()) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("playing", playing);
                mSocket.emit("track_play_pause", obj);
            } catch (JSONException e) {
                Log.e(QueueUp.LOG_TAG, e.getMessage());
            }
        }
    }

    @Override
    public void updateTrackDone() {
        Log.d(QueueUp.LOG_TAG, "Track done");

        if (isConnected()) {
            mSocket.emit("track_finished");
        }
    }

    @Override
    public void stopPlayback() {
        disconnect();
    }

    @Override
    public void updateTrackProgress(int progress, int duration) {

        this.currentProgress = progress;
        this.currentDuration = duration;

        for (PlaylistListener listener : playlistListeners) {
            listener.onTrackProgress(progress, duration);
        }

        if (isConnected()) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("progress", progress);
                obj.put("duration", duration);
                mSocket.emit("track_progress", obj);
            } catch (JSONException e) {
                Log.e(QueueUp.LOG_TAG, e.getMessage());
            }
        }
    }

}
