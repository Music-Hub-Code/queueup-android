package org.louiswilliams.queueupplayer;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.usage.UsageEvents;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.PlayerStateCallback;
import com.spotify.sdk.android.player.Spotify;

import java.util.Timer;
import java.util.TimerTask;

import queueup.PlaylistClient;
import queueup.PlaylistPlayer;
import queueup.Queueup;
import queueup.QueueupClient;
import queueup.objects.QueueupStateChange;
import queueup.objects.SpotifyTrack;


public class MainActivity extends Activity implements FragmentManager.OnBackStackChangedListener, ConnectionStateCallback, PlayerNotificationCallback {

    private static final int REQUEST_CODE = 1234;
    private static final String REDIRECT_URI = "queueup://callback";
    private String[] navigationTitles = {"Hot","Me"};
    private String CLIENT_ID;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private QueueupClient mQueueupClient;
    private String mUserId;
    private String mClientToken;
    private Timer mProgressTimer;

    private PlaylistListener mPlaylistListener;
    private PlaylistPlayer mPlaylistPlayer;
    private Player mPlayer;

    private static final String STORE_NAME = "authStore";
    private static final String LOG_TAG = "QUEUEUP";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CLIENT_ID = getString(R.string.spotify_client_id);

        getFragmentManager().addOnBackStackChangedListener(this);

        SharedPreferences prefs = getSharedPreferences(STORE_NAME, 0);
        mClientToken = prefs.getString("clientToken", null);
        mUserId = prefs.getString("userId", null);

        if (mClientToken != null && mUserId != null) {

            mQueueupClient = new QueueupClient(mClientToken, mUserId);

            if (savedInstanceState == null) {
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                PlaylistListFragment playlistListFragment = new PlaylistListFragment();

                transaction.replace(R.id.content_frame, playlistListFragment );
                transaction.commit();
            }

        } else {

            goToLogin();
        }


        setContentView(R.layout.drawer_main);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_main);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        mDrawerList.setAdapter(new ArrayAdapter<String>(
                this, R.layout.drawer_list_item, navigationTitles) {

        });
        mDrawerList.setOnItemClickListener(new ListView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(LOG_TAG, "Clicked " + position);
            }
        });

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                displayHomeUp();
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                mDrawerToggle.setDrawerIndicatorEnabled(true);
            }

        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        displayHomeUp();


    }

    public  void goToLogin() {
        Intent intent = new Intent(getBaseContext(), LoginActivity.class);
        startActivity(intent);
        finish();
    }

    public void showPlaylistFragment(String playlistId) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Bundle bundle = new Bundle();
        bundle.putString("playlist_id", playlistId);

        PlaylistFragment playlistFragment = new PlaylistFragment();
        playlistFragment.setArguments(bundle);

        transaction.replace(R.id.content_frame, playlistFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void setPlaylistListener(PlaylistListener listener) {
        mPlaylistListener = listener;
    }

    public String getCurrentUserId() {
        return mUserId;
    }

    public PlaylistPlayer getPlaylistPlayer() {
        return mPlaylistPlayer;
    }

    public PlaylistPlayer subscribeToPlaylist(final String playlistId) {

        /* Listen to updates from the server about the playlist */
        final PlaylistClient.StateChangeListener stateChangeListener = new PlaylistClient.StateChangeListener() {
            @Override
            public void onStateChange(final QueueupStateChange state) {
                Log.d(Queueup.LOG_TAG, "State change: " + state);

                if (mPlayer!= null) {

                    mPlayer.getPlayerState(new PlayerStateCallback() {
                        @Override
                        public void onPlayerState(PlayerState playerState) {

                            /* If the player's playlist is the same as the listener's  */
                            if (mPlaylistListener != null && mPlaylistPlayer.getPlaylistId().equals(mPlaylistListener.getPlaylistId())) {

                                /* New track */
                                if (!playerState.trackUri.equals(state.current.uri)) {
                                    Log.d(Queueup.LOG_TAG, "Changing tracks...");
                                    mPlaylistListener.onTrackChanged(state.current);
                                    mPlayer.play(state.current.uri);
                                }

                                /* If the playing state is not what it currently is (changed) */
                                if (playerState.playing && !state.playing) {
                                    Log.d(Queueup.LOG_TAG, "Pausing...");
                                    mPlaylistListener.onPlayingChanged(false);
                                    mPlayer.pause();
                                } else if (!playerState.playing && state.playing) {
                                    Log.d(Queueup.LOG_TAG, "Resuming...");
                                    mPlaylistListener.onPlayingChanged(true);
                                    mPlayer.resume();
                                }

                                /* New queue */
                                if (state.tracks != null) {
                                    mPlaylistListener.onQueueChanged(state.tracks);
                                }
                            }

                        }
                    });
                }

            }

            @Override
            public void onError(String message) {
                toast(message);
                Log.e(Queueup.LOG_TAG, message);
            }
        };

        unsubscribeFromCurrentPlaylist();

        /* Initial authentication to get the player */
        mPlaylistPlayer = mQueueupClient.getPlaylistPlayer(new Queueup.CallReceiver<PlaylistClient>() {
            @Override
            public void onResult(PlaylistClient result) {
                Log.d(Queueup.LOG_TAG, "AUTH SUCCESS");
                PlaylistPlayer player  = (PlaylistPlayer) result;

                /* Perform the subscription */
                player.subscribe(playlistId, true, stateChangeListener);
            }

            @Override
            public void onException(Exception e) {
                toast(e.getMessage());
                Log.e(Queueup.LOG_TAG, "AUTH PROBLEM: " + e.getMessage());
            }
        });

        return mPlaylistPlayer;
    }

    public void unsubscribeFromCurrentPlaylist() {
        if (mPlaylistPlayer != null) {
            mPlaylistPlayer.disconnect();
        }
    }

    public QueueupClient getQueueupClient() {
        return mQueueupClient;
    }

    public void spotifyLogin() {
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginInBrowser(this, request);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Uri uri = intent.getData();
        if (uri != null) {
            AuthenticationResponse response =AuthenticationResponse.fromUri(uri);
            switch (response.getType()) {
                case TOKEN:
                    initPlayer(response.getAccessToken());
                    break;
                case ERROR:
                    Log.e(Queueup.LOG_TAG, "Login Error");
                    break;
            }
        }
    }

    public void initPlayer(String accessToken) {
        Config playerConfig = new Config(this, accessToken, CLIENT_ID);
        mPlayer = Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {

            @Override
            public void onInitialized(Player player) {
                Log.d(LOG_TAG, "Init Player");
                player.addConnectionStateCallback(MainActivity.this);
                player.addPlayerNotificationCallback(MainActivity.this);

                SpotifyTrack current = mPlaylistPlayer.getCurrentState().current;
                player.play(current.uri);

                if (!mPlaylistPlayer.getCurrentState().playing) {
                    player.pause();
                }
            }

            @Override
            public void onError(Throwable throwable) {
                Log.e(LOG_TAG, "Could not initialize: " + throwable.getMessage());
            }
        });
    }

    public Player getSpotifyPlayer() {
        return mPlayer;
    }

    public void startProgressUpdater() {
        stopProgressUpdater();
        mProgressTimer = new Timer();

        mProgressTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mPlayer.getPlayerState(new PlayerStateCallback() {
                    @Override
                    public void onPlayerState(PlayerState playerState) {
                        int duration = playerState.durationInMs;
                        int progress = playerState.positionInMs;

                        Log.d(Queueup.LOG_TAG, "Progress: " + progress + "/" + duration);

                        mPlaylistPlayer.updateTrackProgress(progress, duration);
                        if (mPlaylistListener != null) {
                            mPlaylistListener.onTrackProgress(progress, duration);
                        }
                    }
                });
            }
        }, 0, 1000);
    }

    public void stopProgressUpdater() {
        if (mProgressTimer != null) {
            mProgressTimer.cancel();
        }
    }

    public void displayHomeUp() {
        mDrawerToggle.setDrawerIndicatorEnabled(getFragmentManager().getBackStackEntryCount() == 0);

    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        mDrawerToggle.onConfigurationChanged(config);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            SharedPreferences prefs = getSharedPreferences(STORE_NAME,0);
            prefs.edit().clear().commit();

            Intent intent = new Intent(getBaseContext(), LoginActivity.class);
            if (FacebookSdk.isInitialized()) {
                LoginManager.getInstance().logOut();
            }
            startActivity(intent);
            finish();
            return true;
        } else if (mDrawerToggle.isDrawerIndicatorEnabled() && mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        } else if (id == android.R.id.home && getFragmentManager().popBackStackImmediate()) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if (mPlayer != null) {
            Spotify.destroyPlayer(this);
        }
        if (mPlaylistPlayer != null) {
            mPlaylistPlayer.disconnect();
        }

        super.onDestroy();
    }


    @Override
    public void onBackStackChanged() {
        displayHomeUp();
    }

    /* ConnectionStateCallback */

    @Override
    public void onLoggedIn() {

    }

    @Override
    public void onLoggedOut() {

    }

    @Override
    public void onLoginFailed(Throwable throwable) {

    }

    @Override
    public void onTemporaryError() {

    }

    @Override
    public void onConnectionMessage(String s) {

    }

    /* PlayerNotificationCallback */

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
        if (eventType == EventType.END_OF_CONTEXT) {
            if (mPlaylistPlayer != null) {
                mPlaylistPlayer.updateTrackDone();
            }
        } else if (eventType == EventType.PAUSE) {
            stopProgressUpdater();
        } else if (eventType == EventType.PLAY) {
            startProgressUpdater();
        }
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String s) {

    }

    public void toast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
