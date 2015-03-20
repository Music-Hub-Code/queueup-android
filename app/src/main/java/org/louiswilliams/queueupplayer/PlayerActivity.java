package org.louiswilliams.queueupplayer;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.spotify.sdk.android.Spotify;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.playback.Config;
import com.spotify.sdk.android.playback.ConnectionStateCallback;
import com.spotify.sdk.android.playback.Player;
import com.spotify.sdk.android.playback.PlayerNotificationCallback;
import com.spotify.sdk.android.playback.PlayerState;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;


public class PlayerActivity extends ActionBarActivity
    implements PlayerNotificationCallback, ConnectionStateCallback {

    private static final int REQUEST_CODE = 1234;
    private static final String CLIENT_ID = "16cfe9a1c56f488cbcd7b845a0e655b3";
    private static final String REDIRECT_URI = "queueup://callback";
    private static final String LOG_TAG = "QUEUEUP";
    private static Player mPlayer;
    private static Socket mSocket;
    private static boolean isPlaying;
    private static boolean isDisabled;
    private static  String currentTrack;
    private static String[] queue;
    private static String playlistId;
    private Button playButton;
    private Button stopButton;
    private TextView updateTextView;

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(LOG_TAG,"NEW INTENT:" + intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        String id = getIntent().getStringExtra("playlist_id");
        if (playlistId == null) {
            playlistId = id;
        } else if (!playlistId.equals(id)) {
            closeSocket();
            playlistId = id;
            initSocket(playlistId);
        }

        if (mPlayer == null || mPlayer.isShutdown()) {
            spotifyLogin();
        }


        playButton = (Button) findViewById(R.id.player_play_button);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updatePlaying(!isPlaying);
            }
        });

        stopButton = (Button) findViewById(R.id.player_stop_button);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (isDisabled) {
                    initSocket(playlistId);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            isDisabled = false;
                            ((TextView) v).setText("Disable Updates");
                        }
                    });
                } else {
                    closeSocket();
                    updatePlaying(false);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            isDisabled = true;
                            ((TextView) v).setText("Enable Updates");
                        }
                    });
                }


            }
        });
        updatePlayButton(isPlaying);

        updateTextView = (TextView) findViewById(R.id.player_update_view);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == REQUEST_CODE) {

            Log.d(LOG_TAG, "REQUEST_CODE");
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);

            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                initPlayer(response.getAccessToken());
            }
        }
    }

    @Override
    public void onPlaybackEvent(PlayerNotificationCallback.EventType eventType, PlayerState playerState) {
        Log.d(LOG_TAG, "Playback event: " + eventType.name());

        /* When the track has finished */
        if (eventType == PlayerNotificationCallback.EventType.END_OF_CONTEXT ) {
            Log.d(LOG_TAG, "Track finished!");
            mSocket.emit("track_finished");
        }
    }

    @Override
    public void onPlaybackError(PlayerNotificationCallback.ErrorType errorType, String s) {
        Log.d(LOG_TAG, "Playback error: " + errorType.name());
    }

    @Override
    public void onLoggedIn() {
        Log.d(LOG_TAG, "Logged in");
    }

    @Override
    public void onLoggedOut() {
        Log.d(LOG_TAG, "Logged out");
    }

    @Override
    public void onLoginFailed(Throwable throwable) {
        Log.d(LOG_TAG, "Logged failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d(LOG_TAG, "Temp error");
    }

    @Override
    public void onConnectionMessage(String s) {
        Log.d(LOG_TAG, "Connection message: " + s);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        Spotify.destroyPlayer(this);
    }


    private void spotifyLogin() {
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    private void initPlayer(String accessToken) {

            Config playerConfig = new Config(this, accessToken, CLIENT_ID);
            mPlayer = Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {

                @Override
                public void onInitialized(Player player) {
                    Log.d(LOG_TAG, "Iinit Player");
                    player.addConnectionStateCallback(PlayerActivity.this);
                    player.addPlayerNotificationCallback(PlayerActivity.this);
                    isPlaying = false;

                    // Only initialize one the player has been
                    if (mSocket != null) {
                        mSocket.close();
                    }
                    initSocket(playlistId);
                }

                @Override
                public void onError(Throwable throwable) {
                    Log.e(LOG_TAG, "Could not initialize: " + throwable.getMessage());
                }
            });
    }

    private void initSocket(final String playlistId) {

        try {
            mSocket = IO.socket("http://queueup.louiswilliams.org/");
        } catch (URISyntaxException e) {
            Log.e(LOG_TAG, e.getMessage());
        }

        if (mSocket != null) {


            mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

                @Override
                public void call(Object...args) {
                    logEvent("Received connection on socket");
                    Log.d(LOG_TAG, "RECEIVED CONNECTION");
                }
            });

            mSocket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    logEvent("Server disconnected");
                    Log.d(LOG_TAG,"Server Disconnected");
                }
            });

            mSocket.on("auth_request", new Emitter.Listener() {

                @Override
                public void call(Object ... args) {
                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("id", playlistId);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    mSocket.emit("auth_send", obj);
                }
            });

            mSocket.on("auth_success", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    logEvent("Authenticated with server");
                    Log.d(LOG_TAG,"Socket auth success");
                }
            });

            mSocket.on("auth_fail", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    logEvent("Authentication failed with server");
                    Log.d(LOG_TAG,"Socket auth failure: " + args.toString());
                }
            });

            mSocket.on("state_change", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    Log.d(LOG_TAG, "STATE CHANGE");
                    logEvent("New state!");

                    JSONObject state = (JSONObject)args[0];
                    try {
                        double volume = state.getDouble("volume");
                        Log.d(LOG_TAG, "Volume: " + volume);
                        // TODO: Change Volume
                    } catch (JSONException e) {
                        Log.d(LOG_TAG, e.getMessage());
                    }
                    try {
                        JSONObject track = state.getJSONObject("track");
                        String trackUri = track.getString("uri");
                        Log.d(LOG_TAG,"Current track: " + trackUri);

                        // If the playlist state changed
                        if (!trackUri.equals(currentTrack)) {
                            logEvent("Track changed to " + trackUri);
                            currentTrack = trackUri;
                            mPlayer.play(currentTrack);
                            updatePlaying(true);
                        }

                        // TODO: Update current track
                    } catch (JSONException e) {
                        Log.d(LOG_TAG, e.getMessage());
                    }
                    try {
                        boolean play = state.getBoolean("play");
                        Log.d(LOG_TAG, "Playing: " + play);
                        updatePlaying(play);
                    } catch (JSONException e) {
                        Log.d(LOG_TAG, e.getMessage());
                    }

                    try {
                        JSONArray queue = state.getJSONArray("queue");
                        Log.d(LOG_TAG, "Queue: " + queue);
                        // TODO: Update queue
                    } catch (JSONException e) {
                        Log.d(LOG_TAG, e.getMessage());
                    }

                }
            });
            logEvent("Created socket");
            Log.d(LOG_TAG,"CONNECTING...");
            mSocket.connect();

        } else {
            Log.d(LOG_TAG, "Socket not created");
        }
    }

    private void closeSocket() {
        mSocket.off();
        mSocket.disconnect();
        logEvent("Socket closed");
    }

    private void updatePlaying(boolean play) {
        if (!isPlaying && play) {
            logEvent("Playing");
            mPlayer.resume();
            isPlaying = true;
        } else if (!play && isPlaying) {
            logEvent("Paused");
            mPlayer.pause();
            isPlaying = false;
        }
        updatePlayButton(isPlaying);

    }

    private void updatePlayButton(final boolean play) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String text = (play) ? "Pause" : "Play";
                playButton.setText(text); }
        });
    }

    private void logEvent(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
                if (updateTextView != null) {
                    updateTextView.append(df.format(new Date()) + ": " + text + "\n");
                } else {
                    Log.d(LOG_TAG, "Update view not available. Message: " + text);
                }
            }
        });
    }
}
