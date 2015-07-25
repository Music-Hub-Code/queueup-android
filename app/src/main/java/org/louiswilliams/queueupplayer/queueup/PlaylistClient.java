package org.louiswilliams.queueupplayer.queueup;

import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import org.louiswilliams.queueupplayer.queueup.objects.QueueupStateChange;

public class PlaylistClient {

    private static final String SOCKET_URL = Queueup.SOCKET_URL;
    protected String mClientToken;
    protected String mUserId;
    protected Socket mSocket;
    protected String mSubscription;

    protected int currentProgress;
    protected int currentDuration;

    public PlaylistClient(String clientToken, String userId, final Queueup.CallReceiver<PlaylistClient> authReceiver) {
        mClientToken = clientToken;
        mUserId = userId;

        try {
            mSocket = IO.socket(SOCKET_URL);

            final Emitter.Listener onConnectListener = new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    mSubscription = null;

                    Log.d(Queueup.LOG_TAG, "CONNECTED");
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
                                    authReceiver.onException(new QueueupException("Error authenticating: " + error));
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
                        Log.d(Queueup.LOG_TAG, "Emitting 'auth'");
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

    protected void subscribe(String playlistId, final StateChangeListener receiver) {

        if (mSocket != null) {
            JSONObject params = new JSONObject();
            try {
                params.put("playlist_id", playlistId);

                mSubscription = playlistId;

                mSocket.on("state_change", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        QueueupStateChange state = new QueueupStateChange((JSONObject) args[0]);
                        receiver.onStateChange(state);
                    }
                });

                mSocket.emit("client_subscribe", params);
            } catch (JSONException e) {
                Log.e(Queueup.LOG_TAG, e.getMessage());
            }
        }
    }

    public void unsubscribe() {
        mSubscription = null;
        mSocket.off("state_change");
        mSocket.emit("client_unsubscribe");
    }

    public boolean isConnected() {
        return (mSocket != null && mSocket.connected());
    }

    public boolean isSubscribed() {
        return mSubscription != null;
    }

    public String getPlaylistId() {
        return mSubscription;
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

    public interface StateChangeListener {
        void onStateChange(QueueupStateChange state);
        void onError(String message);
    }
}
