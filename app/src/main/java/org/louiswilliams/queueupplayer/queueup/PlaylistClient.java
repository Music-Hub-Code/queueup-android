package org.louiswilliams.queueupplayer.queueup;

import android.util.Log;

import io.socket.client.Socket;
import io.socket.client.IO;
import io.socket.emitter.Emitter;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.louiswilliams.queueupplayer.queueup.objects.QueueUpStateChange;

public class PlaylistClient {

    private static final String SOCKET_URL = QueueUp.SOCKET_URL;
    protected String mClientToken;
    protected String mUserId;
    protected Socket mSocket;
    protected String mSubscription;

    protected int currentProgress;
    protected int currentDuration;

    protected PlaybackReceiver playbackReceiver;
    protected QueueUpStateChange currentState;

    protected Queue<PlaylistListener> playlistListeners;

    public PlaylistClient(String clientToken, String userId, final QueueUp.CallReceiver<PlaylistClient> authReceiver, PlaybackReceiver playbackReceiver) {
        mClientToken = clientToken;
        mUserId = userId;

        /* The playback receiver receives events about the end of playback on the server side */
        this.playbackReceiver = playbackReceiver;

        /* We need a thread-safe data structure, because the application can be iterating while simultaneously remove or adding a listener */
        playlistListeners = new ConcurrentLinkedQueue<>();
        try {

            /* Force to create a new connection if there are multiple on the same URL */
            IO.Options opts = new IO.Options();
            opts.forceNew = true;
            mSocket = IO.socket(SOCKET_URL, opts);

            final Emitter.Listener onConnectListener = new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    mSubscription = null;

                    Log.d(QueueUp.LOG_TAG, "CONNECTED");
                    String event = "auth";

                    mSocket.once("auth_response", new Emitter.Listener() {
                                @Override
                                public void call(Object... args) {
                            if (args.length > 0) {
                                JSONObject obj = (JSONObject) args[0];
                                JSONObject error = obj.optJSONObject("error");

                                if (error == null) {
                                    authReceiver.onResult(PlaylistClient.this);
                                } else {
                                    authReceiver.onException(new QueueUpException("Error authenticating: " + error));
                                    mSocket.off();
                                    mSocket.disconnect();
                                }
                            } else {
                                authReceiver.onResult(PlaylistClient.this);
                            }
                                }
                        }
                    );

                    try {
                        JSONObject obj = new JSONObject();
                        obj.put("client_token", mClientToken);
                        obj.put("user_id", mUserId);
                        Log.d(QueueUp.LOG_TAG, "Emitting 'auth'");
                        mSocket.emit(event, obj);
                    } catch(JSONException e) {
                        authReceiver.onException(e);
                        mSocket.off();
                        mSocket.disconnect();
                    }


                }
            };

            /* The first time attach the listener */
            mSocket.once(Socket.EVENT_CONNECT, onConnectListener);

            /* After a disconnect, remove all listeners then reattach */
            mSocket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    mSocket.off();
                    mSocket.on(Socket.EVENT_CONNECT, onConnectListener);
                }
            });

            mSocket.connect();

        } catch (URISyntaxException e) {
            authReceiver.onException(e);
        }

    }

    public void subscribe(String playlistId) {

        if (mSocket != null) {
            JSONObject params = new JSONObject();
            try {
                params.put("playlist_id", playlistId);

                mSubscription = playlistId;

                mSocket.on("state_change", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        QueueUpStateChange state = new QueueUpStateChange((JSONObject) args[0]);
                        stateChangeListener.onStateChange(state);
                        currentState = state;
                    }
                });

                mSocket.emit("client_subscribe", params);
            } catch (JSONException e) {
                Log.e(QueueUp.LOG_TAG, e.getMessage());
            }
        }
    }

    public void unsubscribe() {
        mSubscription = null;
        mSocket.off("state_change");
        mSocket.emit("client_unsubscribe");
    }


    /* Listen to updates from the server about the playlist */
    protected PlaylistClient.StateChangeListener stateChangeListener = new PlaylistClient.StateChangeListener() {
        @Override
        public void onStateChange(final QueueUpStateChange state) {
            Log.d(QueueUp.LOG_TAG, "State change: " + state);

            /* This signals end of playback */
            if (state.current == null) {
                playbackReceiver.onPlaybackEnd();
                return;
            }

            /* New track */
            if (currentState == null ||
                    currentState.current == null ||
                    !currentState.current.uri.equals(state.current.uri)) {
                Log.d(QueueUp.LOG_TAG, "Changing tracks...");

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
            Log.e(QueueUp.LOG_TAG, message);
        }
    };


    public boolean isConnected() {
        return (mSocket != null && mSocket.connected());
    }

    public boolean isSubscribed() {
        return mSubscription != null;
    }

    public String getPlaylistId() {
        return mSubscription;
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


    public int getCurrentProgress() {
        return currentProgress;
    }

    public int getCurrentDuration() {
        return currentDuration;
    }

    public void disconnect() {
        mSubscription = null;
        if (mSocket != null) {
            mSocket.off();
            mSocket.disconnect();
        }
    }

    public QueueUpStateChange getCurrentState() {
        return currentState;
    }


    public interface StateChangeListener {
        void onStateChange(QueueUpStateChange state);
        void onError(String message);
    }


}
