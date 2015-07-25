package org.louiswilliams.queueupplayer.queueup;

import android.drm.DrmStore;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.louiswilliams.queueupplayer.queueup.objects.QueueupStateChange;

public class PlaylistPlayer extends PlaylistClient {

    private QueueupStateChange currentState;
    private Queue<PlaylistListener> playlistListeners;
    private PlaybackReceiver playbackReceiver;

    public PlaylistPlayer(String clientToken, String userId, Queueup.CallReceiver<PlaylistClient> receiver, PlaybackReceiver playbackReceiver) {
        super(clientToken, userId, receiver);

        /* The playback receiver receives events about the end of playback on the server side */
        this.playbackReceiver = playbackReceiver;

        /* We need a thread-safe data structure, because the application can be iterating while simultaneously remove or adding a listener */
        playlistListeners = new ConcurrentLinkedQueue<>();
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
                        QueueupStateChange state = new QueueupStateChange((JSONObject) args[0]);
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

    public void updatePlaybackReady() {
        Log.d(Queueup.LOG_TAG, "Playback ready");

        for(PlaylistListener listener : playlistListeners) {
            listener.onPlayerReady();
        }
    }

    public void updateTrackPlaying(boolean playing) {

        Log.d(Queueup.LOG_TAG, "Player playing: " + playing);
        for (PlaylistListener listener : playlistListeners) {
            listener.onPlayingChanged(playing);
        }

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
        Log.d(Queueup.LOG_TAG, "Track done");

        if (isConnected()) {
            mSocket.emit("track_finished");
        }
    }

    public void addPlaylistListener(PlaylistListener listener) {
        playlistListeners.add(listener);
    }

    public void removePlaylistListener(PlaylistListener listener) {
        playlistListeners.remove(listener);
    }

    public void removeAllPlaylistListeners() {
        playlistListeners.clear();
    }

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
                Log.e(Queueup.LOG_TAG, e.getMessage());
            }
        }

    }

    public QueueupStateChange getCurrentState() {
        return currentState;
    }

    /* Listen to updates from the server about the playlist */
    private PlaylistClient.StateChangeListener stateChangeListener = new PlaylistClient.StateChangeListener() {
        @Override
        public void onStateChange(final QueueupStateChange state) {
            Log.d(Queueup.LOG_TAG, "State change: " + state);

            /* This signals end of playback */
            if (state.current == null) {
                playbackReceiver.onPlaybackEnd();
                return;
            }

            /* New track */
            if (currentState == null ||
                    currentState.current == null ||
                    !currentState.current.uri.equals(state.current.uri)) {
                Log.d(Queueup.LOG_TAG, "Changing tracks...");

                /* Update every listener */
                for (PlaylistListener listener : playlistListeners) {
                    listener.onTrackChanged(state.current);
                }
            }

            /* If the playing state is not what it currently is (it changed) */

            if (currentState == null ||
                    (currentState.playing && !state.playing) ||
                    (!currentState.playing && state.playing)) {

                /* Update every listener */
                for (PlaylistListener listener : playlistListeners) {
                    listener.onPlayingChanged(state.playing);
                }

            }

            /* New queue */
            if (state.tracks != null) {

                /* Update every listener */
                for (PlaylistListener listener : playlistListeners) {
                    listener.onQueueChanged(state.tracks);
                }
            }

        }

        @Override
        public void onError(String message) {
            Log.e(Queueup.LOG_TAG, message);
        }
    };
}
