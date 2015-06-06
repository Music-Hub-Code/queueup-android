package queueup;

import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import queueup.objects.QueueupStateChange;

public class PlaylistClient {

    private static final String API_URL = Queueup.API_URL;
    protected String mClientToken;
    protected String mUserId;
    protected Socket mSocket;
    protected String mSubcription;
    protected StateChangeListener mListener;

    public PlaylistClient(String clientToken, String userId, final Queueup.CallReceiver<PlaylistClient> authReceiver) {
        mClientToken = clientToken;
        mUserId = userId;

        try {
            mSocket = IO.socket(API_URL);

            mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {

                    mSubcription = null;

                    Log.d(Queueup.LOG_TAG, "CONNECTED");
                    String event = "auth";

                    mSocket.on("auth_response", new Emitter.Listener() {
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
                        mSocket.emit(event, obj);
                    } catch(JSONException e) {
                        authReceiver.onException(e);
                        mSocket.off();
                        mSocket.disconnect();
                    }

                }
            });

            mSocket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    mSocket.off();
                }
            });

            mSocket.connect();

        } catch (URISyntaxException e) {
            authReceiver.onException(e);
        }

    }

    public void subscribe(String playlistId, final StateChangeListener receiver) {

        if (mSocket != null) {
            JSONObject params = new JSONObject();
            try {
                params.put("playlist_id", playlistId);

                mSubcription = playlistId;

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
        mSocket.off("state_change");
        mSocket.emit("client_unsubscribe");
    }

    public boolean isConnected() {
        return (mSocket != null && mSocket.connected());
    }

    public boolean isSubscribed() {
        return mSubcription != null;
    }

    public void disconnect() {
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
