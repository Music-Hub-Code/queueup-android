package org.louiswilliams.queueupplayer.activity;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.gc.materialdesign.views.ButtonFlat;
import com.github.nkzawa.socketio.client.Socket;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.Spotify;
import com.squareup.picasso.Picasso;

import org.louiswilliams.queueupplayer.AndroidLoggingHandler;
import org.louiswilliams.queueupplayer.R;
import org.louiswilliams.queueupplayer.fragment.AddTrackFragment;
import org.louiswilliams.queueupplayer.fragment.PlaylistFragment;
import org.louiswilliams.queueupplayer.fragment.PlaylistListFragment;
import org.louiswilliams.queueupplayer.widget.PlayerNotification;

import java.util.Arrays;
import java.util.logging.Level;

import org.louiswilliams.queueupplayer.queueup.PlaylistClient;
import org.louiswilliams.queueupplayer.queueup.PlaylistPlayer;
import org.louiswilliams.queueupplayer.queueup.Queueup;
import org.louiswilliams.queueupplayer.queueup.QueueupClient;
import org.louiswilliams.queueupplayer.queueup.objects.QueueupPlaylist;
import org.louiswilliams.queueupplayer.queueup.SpotifyPlayer;


public class MainActivity
        extends Activity
        implements
            FragmentManager.OnBackStackChangedListener {

    private String[] navigationTitles = {"Hot Playlists","Me"};
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private DrawerListAdapter mDrawerAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private QueueupClient mQueueupClient;
    private String mUserId;
    private String mClientToken;
    private String mFacebookId;
    private boolean showNewTrackOnPlaylistLoad;
    private PlaylistPlayer mPlaylistPlayer;
    private SpotifyPlayer mSpotifyPlayer;
    private String CLIENT_ID;
    private PlayerNotification mPlayerNotification;

    private static final String REDIRECT_URI = "queueup://callback";
    private static final String STORE_NAME = Queueup.STORE_NAME;
    private static final String LOG_TAG = Queueup.LOG_TAG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CLIENT_ID = getString(R.string.spotify_client_id);

        AndroidLoggingHandler.reset(new AndroidLoggingHandler());
        java.util.logging.Logger.getLogger(Socket.class.getName()).setLevel(Level.FINEST);


        getFragmentManager().addOnBackStackChangedListener(this);


        SharedPreferences prefs = getSharedPreferences(STORE_NAME, 0);
        mClientToken = prefs.getString(Queueup.STORE_CLIENT_TOKEN, null);
        mUserId = prefs.getString(Queueup.STORE_USER_ID, null);
        mFacebookId = prefs.getString(Queueup.STORE_FACEBOOK_ID, null);

        if (isLoggedIn()) {
            mQueueupClient = new QueueupClient(mClientToken, mUserId);
        } else {
            mQueueupClient = new QueueupClient(null, null);
        }

        if (savedInstanceState == null) {
            showPlaylistListFragment();
        }

        setContentView(R.layout.drawer_main);

        /* Make sure FB SDK is initialized before doing FB stuff later on */
        ensureSdkInitialized();

        /* Redo the facebook login if this is a cold startup */
        if (isLoggedInWithFacebook()) {
            maybeDoFacebookLogin();
        }

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_main);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        mDrawerAdapter = new DrawerListAdapter(this, R.layout.drawer_list_item, Arrays.asList(navigationTitles));
        mDrawerAdapter.setSelection(0);

        mDrawerList.setAdapter(mDrawerAdapter);
        mDrawerList.setOnItemClickListener(new ListView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(LOG_TAG, "Clicked " + position);
            }
        });

        LinearLayout headerView;

        /* Display different navigation headers if a user is logged in or not */
        if (isLoggedIn()) {
            headerView = (LinearLayout) getLayoutInflater().inflate(R.layout.drawer_header, null, false);

            ButtonFlat logoutButton = (ButtonFlat) headerView.findViewById(R.id.drawer_logout_button);

            logoutButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doLogout();
                }
            });

            populateHeaderWithProfileInfo(headerView);


        } else {
            headerView = (LinearLayout) getLayoutInflater().inflate(R.layout.drawer_header_default, null, false);

            ButtonFlat loginButton = (ButtonFlat) headerView.findViewById(R.id.drawer_login_button);
            loginButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doLogin();
                }
            });
        }


        mDrawerList.addHeaderView(headerView, null, false);

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
        transaction.addToBackStack(playlistFragment.getClass().getName());

        transaction.commit();
    }

    public  void showPlaylistListFragment() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        PlaylistListFragment playlistListFragment = new PlaylistListFragment();

        transaction.replace(R.id.content_frame, playlistListFragment);
        transaction.commit();
    }


    public void showAddTrackFragment(String playlistId) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Bundle bundle = new Bundle();
        bundle.putString("playlist_id", playlistId);

        AddTrackFragment fragment = new AddTrackFragment();
        fragment.setArguments(bundle);

        transaction.replace(R.id.content_frame, fragment);
        transaction.addToBackStack(fragment.getClass().getName());
        transaction.commit();
    }

    public void reloadCurrentFragment() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        Fragment current = getFragmentManager().findFragmentById(R.id.content_frame);
        transaction.remove(current);
        transaction.replace(R.id.content_frame, current);
        transaction.commit();
    }

    public void onAddTrackFinshed(final boolean trackAdded) {
        showNewTrackOnPlaylistLoad = trackAdded;
        FragmentManager fm = getFragmentManager();
        fm.popBackStackImmediate();
    }


    public String getCurrentUserId() {
        return mUserId;
    }

    public PlaylistPlayer getPlaylistPlayer() {
        return mPlaylistPlayer;
    }

    public boolean getShowNewTrackAndReset() {
        boolean tmp = showNewTrackOnPlaylistLoad;
        showNewTrackOnPlaylistLoad = false;
        return tmp;
    }

    public PlaylistPlayer subscribeToPlaylist(final String playlistId) {

        unsubscribeFromCurrentPlaylist();

        /* Initial authentication to get the player */
        mPlaylistPlayer = mQueueupClient.getPlaylistPlayer(new Queueup.CallReceiver<PlaylistClient>() {
            @Override
            public void onResult(PlaylistClient result) {
                Log.d(Queueup.LOG_TAG, "AUTH SUCCESS");
                PlaylistPlayer player  = (PlaylistPlayer) result;

                /* Attach the notification listener... */
                mPlaylistPlayer.addPlaylistListener(getPlayerNotification());


                /* Start log into spotify sequence */
                spotifyLogin();

                /* Perform the subscription */
                player.subscribe(playlistId, true);

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
            Log.d(Queueup.LOG_TAG, "Unsubscribing from preview player");
            mPlaylistPlayer.removeAllPlaylistListeners();
            if (mSpotifyPlayer != null) {
                mSpotifyPlayer.stopReceivingPlaybackNotificactions();
            }
            mPlaylistPlayer.disconnect();
            mPlaylistPlayer = null;
        }
    }


    public QueueupClient getQueueupClient() {
        return mQueueupClient;
    }

    public void spotifyLogin() {

        /* If we want to skip logging in again... */
        if (mSpotifyPlayer != null && mSpotifyPlayer.getPlayer().isLoggedIn()) {
            Log.d(Queueup.LOG_TAG, "Not re-logging in, no need");

            /* Make sure we create the player on the main thread */
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    beginPlayback();
                }
            });

        } else {

            AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
                    AuthenticationResponse.Type.TOKEN,
                    REDIRECT_URI);
            builder.setScopes(new String[]{"user-read-private", "streaming"});
            AuthenticationRequest request = builder.build();

            AuthenticationClient.openLoginInBrowser(this, request);
        }

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


        /* Don't get another player if there's already one initialized */
        if (mSpotifyPlayer != null &&  mSpotifyPlayer.getPlayer().isInitialized()) {
            beginPlayback();
        } else {


            Player player = Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {

                @Override
                public void onInitialized(Player player) {
                    beginPlayback();
                }

                @Override
                public void onError(Throwable throwable) {
                    Log.e(LOG_TAG, "Could not initialize: " + throwable.getMessage());
                }
            });

            /* Construct our player wrapper */
            mSpotifyPlayer = new SpotifyPlayer(mPlaylistPlayer, player);
        }
    }


    public void beginPlayback() {
        Log.d(LOG_TAG, "Init Player");

        /* Attach the player listener */
        mSpotifyPlayer.startReceivingPlaybackNotifications();
        mPlaylistPlayer.addPlaylistListener(mSpotifyPlayer);
        mPlaylistPlayer.updatePlaybackReady();

    }

    /* Just recreate the activity, which resets everything to a clean slate */
    public void stopPlayback() {
//        if (mSpotifyPlayer != null) {
//            mSpotifyPlayer.getPlayer().pause();
//            mSpotifyPlayer.stopReceivingPlaybackNotificactions();
//        }
//
//        mPlaylistPlayer.removeAllPlaylistListeners();
//
//        unsubscribeFromCurrentPlaylist();

        recreate();
    }

    public void displayHomeUp() {
        mDrawerToggle.setDrawerIndicatorEnabled(getFragmentManager().getBackStackEntryCount() == 0);

    }

    public boolean isLoggedIn() {
        return mUserId != null && mClientToken != null;
    }

    public boolean isLoggedInWithFacebook() {
        return mFacebookId != null;
    }


    public void populateHeaderWithProfileInfo(View headerView) {
        ImageView userImage = (ImageView) headerView.findViewById(R.id.drawer_user_image);
        TextView userName = (TextView) headerView.findViewById(R.id.drawer_user_name);

        Profile profile = Profile.getCurrentProfile();
        AccessToken accessToken = AccessToken.getCurrentAccessToken();

        Log.d(Queueup.LOG_TAG, "Access token: " + accessToken);
        /* If logged in with Facebook*/
        if (profile != null) {
            Uri proPicUri = profile.getProfilePictureUri(userImage.getLayoutParams().width, userImage.getLayoutParams().height);
            Picasso.with(this).load(proPicUri).into(userImage);

            userName.setText(profile.getFirstName() + " " + profile.getLastName());
        } else {
            toast("Profile null");
        }


    }

    public PlayerNotification getPlayerNotification() {
        if (mPlayerNotification == null) {
            mPlayerNotification = new PlayerNotification(this);
        }
        return mPlayerNotification;
    }


    public void doLogin() {
        Intent loginIntent = new Intent(getBaseContext(), LoginActivity.class);

        startActivityForResult(loginIntent, LoginActivity.QUEUEUP_LOGIN_REQUEST_CODE);

    }

    public void doLogout() {
        SharedPreferences prefs = getSharedPreferences(STORE_NAME, 0);
        prefs.edit().clear().commit();

        LoginManager.getInstance().logOut();

        recreate();
    }

    /* Initialize the FB SDK if it isn't already */
    public void ensureSdkInitialized() {
        if (!FacebookSdk.isInitialized()) {
            FacebookSdk.sdkInitialize(getApplicationContext());
        }
    }

    /* Only redo the FB login if the access token is null */
    public void maybeDoFacebookLogin() {
        if (AccessToken.getCurrentAccessToken() == null) {
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            intent.putExtra("DO_FB_LOGIN", true);
        }
    }

    public void hideKeyboard() {
        hideKeyboard(getCurrentFocus());
    }

    /* Hide the keyboard if focused */
    public void hideKeyboard(View focus) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (focus != null) {
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        } else{
            Log.e(Queueup.LOG_TAG, "Cant get focus to hide keyboard!");
        }

    }

    /* Show keyboard */
    public void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

    }

    /* Create a new playlist */
    public void doCreatePlaylist(String name) {
        mQueueupClient.playlistCreate(name, new Queueup.CallReceiver<QueueupPlaylist>() {
            @Override
            public void onResult(QueueupPlaylist result) {
                toast("Created " + result.name);
                showPlaylistFragment(result.id);
            }

            @Override
            public void onException(Exception e) {
                Log.e(Queueup.LOG_TAG, "Problem creating playlist: " + e.getMessage());
                toast("Problem creating playlist");
            }
        });

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
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (mDrawerToggle.isDrawerIndicatorEnabled() && mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        } else if (id == android.R.id.home && getFragmentManager().popBackStackImmediate()) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if (mPlayerNotification != null) {
                mPlayerNotification.cancel();
        }
        if (mSpotifyPlayer != null) {
            mSpotifyPlayer.stopReceivingPlaybackNotificactions();
            Spotify.destroyPlayer(this);
        }
        if (mPlaylistPlayer != null) {
            mPlaylistPlayer.removeAllPlaylistListeners();
            mPlaylistPlayer.disconnect();
        }

        super.onDestroy();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == LoginActivity.QUEUEUP_LOGIN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                toast("Login successful");

                recreate();
            } else if (resultCode == RESULT_CANCELED) {
                toast("Login cancelled");
            } else if (resultCode == LoginActivity.RESULT_LOGIN_FAILURE) {
                Exception e = (Exception) intent.getSerializableExtra(LoginActivity.EXTRA_LOGIN_EXCEPTION);
                String message = "Login unsuccessful";
                if (e != null) {
                    message += ": " + e.getMessage();
                }
                toast(message);
            }
        }
    }

    @Override
    public void onBackStackChanged() {
        displayHomeUp();
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
