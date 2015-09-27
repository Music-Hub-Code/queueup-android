package org.louiswilliams.queueupplayer.activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.google.android.gms.analytics.ExceptionReporter;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.Spotify;
import com.squareup.picasso.Picasso;

import org.louiswilliams.queueupplayer.QueueUpApplication;
import org.louiswilliams.queueupplayer.R;
import org.louiswilliams.queueupplayer.fragment.AddTrackFragment;
import org.louiswilliams.queueupplayer.fragment.PlaylistFragment;
import org.louiswilliams.queueupplayer.fragment.PlaylistListFragment;
import org.louiswilliams.queueupplayer.fragment.PlaylistSearchResultsFragment;
import org.louiswilliams.queueupplayer.queueup.PlaybackReceiver;
import org.louiswilliams.queueupplayer.queueup.PlaylistClient;
import org.louiswilliams.queueupplayer.queueup.PlaylistPlayer;
import org.louiswilliams.queueupplayer.queueup.QueueUp;
import org.louiswilliams.queueupplayer.queueup.QueueUpStore;
import org.louiswilliams.queueupplayer.queueup.SpotifyPlayer;
import org.louiswilliams.queueupplayer.queueup.api.QueueUpClient;
import org.louiswilliams.queueupplayer.queueup.api.SpotifyTokenManager;
import org.louiswilliams.queueupplayer.queueup.objects.QueueUpPlaylist;
import org.louiswilliams.queueupplayer.queueup.objects.QueueUpUser;
import org.louiswilliams.queueupplayer.widget.PlayerNotification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity
        extends AppCompatActivity
        implements
            FragmentManager.OnBackStackChangedListener {

    public static final String PLAYLISTS_ALL = "all";
    public static final String PLAYLISTS_FRIENDS = "friends";
    public static final String PLAYLISTS_MINE = "mine";
    public static final int[] NAVIGATION_TITLES = {R.string.top_playlists, R.string.friends_playlists, R.string.my_playlists};
    public static final String[] NAVIGATION_ACTIONS = {PLAYLISTS_ALL, PLAYLISTS_FRIENDS, PLAYLISTS_MINE};
    public int currentNavigationAction;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private DrawerListAdapter mDrawerAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private QueueUpClient mQueueUpClient;
    private QueueUpStore mStore;
    private SpotifyTokenManager mSpotifyTokenManager;
    private String mUserId;
    private String mClientToken;
    private String mFacebookId;
    private String mEmailAddress;
    private boolean showNewTrackOnPlaylistLoad;
    private PlaylistPlayer mPlaylistPlayer;
    private SpotifyPlayer mSpotifyPlayer;
    private String CLIENT_ID;
    private PlayerNotification mPlayerNotification;

    private static final String REDIRECT_URI = "queueup://callback";
    private static final String LOG_TAG = QueueUp.LOG_TAG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CLIENT_ID = getString(R.string.spotify_client_id);

        /* Set up Google analytics for uncaught exceptions */
        Tracker tracker = ((QueueUpApplication)getApplication()).getDefaultTracker();
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = new ExceptionReporter(
                tracker,
                Thread.getDefaultUncaughtExceptionHandler(),
                getApplicationContext()
        );
        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);

        getFragmentManager().addOnBackStackChangedListener(this);

        mStore = QueueUpStore.with(this);
        mSpotifyTokenManager = SpotifyTokenManager.with(mStore);

        mClientToken = mStore.getString(QueueUpStore.CLIENT_TOKEN);
        mUserId = mStore.getString(QueueUpStore.USER_ID);
        mFacebookId = mStore.getString(QueueUpStore.FACEBOOK_ID);
        mEmailAddress = mStore.getString(QueueUpStore.EMAIL_ADDRESS);

        /* If we are registered non-anonymously, proceed, otherwise login anonymously */
        if (isLoggedIn()) {
            mQueueUpClient = new QueueUpClient(mClientToken, mUserId);
        } else {
            mQueueUpClient = new QueueUpClient(null, null);
            goToSplash();
            return;
        }

        /* Set up out layout */
        doSetup(savedInstanceState);

    }

    /* Setup UI elements */
    public void doSetup(Bundle savedInstanceState) {

        currentNavigationAction = 0;

        if (savedInstanceState == null) {
            showPlaylistListFragment();
        } else {
            Log.d(QueueUp.LOG_TAG, "savedInstanceState != null");
        }

        setContentView(R.layout.drawer_main);

        /* Make sure FB SDK is initialized before doing FB stuff later on */
        ensureSdkInitialized();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_main);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        List<String> titles = new ArrayList<>();
        for (int id : NAVIGATION_TITLES) {
            titles.add(getString(id));
        }
        mDrawerAdapter = new DrawerListAdapter(this, R.layout.drawer_list_item, titles);
        mDrawerAdapter.setSelection(currentNavigationAction);

        mDrawerList.setOnItemClickListener(new ListView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                currentNavigationAction = position - 1;
                mDrawerAdapter.setSelection(currentNavigationAction);
                handleDrawerClickAction(currentNavigationAction);

            }
        });

        LinearLayout headerView;

        /* Display different navigation headers if a user is logged in or not */
        if (isClientRegistered()) {
            headerView = (LinearLayout) getLayoutInflater().inflate(R.layout.drawer_header, null, false);

            Button logoutButton = (Button) headerView.findViewById(R.id.drawer_logout_button);

            logoutButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doLogout();
                }
            });

            populateHeaderWithProfileInfo(headerView);


        } else {
            headerView = (LinearLayout) getLayoutInflater().inflate(R.layout.drawer_header_default, null, false);

            Button loginButton = (Button) headerView.findViewById(R.id.drawer_login_button);
            loginButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doLogin();
                }
            });
        }


        mDrawerList.addHeaderView(headerView, null, false);
        mDrawerList.setAdapter(mDrawerAdapter);

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

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        displayHomeUp();

        handleIntent(getIntent());

    }

    public void goToSplash() {
        Intent intent = new Intent(getBaseContext(), SplashActivity.class);
        startActivity(intent);
        finish();
    }

    public  void goToLogin() {
        Intent intent = new Intent(getBaseContext(), LoginActivity.class);
        startActivity(intent);
        finish();
    }

    public PlaylistFragment showPlaylistFragment(String playlistId) {

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Bundle bundle = new Bundle();
        bundle.putString("playlist_id", playlistId);

        PlaylistFragment playlistFragment = new PlaylistFragment();
        playlistFragment.setArguments(bundle);

        transaction.replace(R.id.content_frame, playlistFragment);
        transaction.addToBackStack(playlistFragment.getClass().getName());

        transaction.commit();
        return playlistFragment;
    }

    public PlaylistListFragment showPlaylistListFragment() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        Bundle bundle = new Bundle();
        bundle.putString("action", NAVIGATION_ACTIONS[currentNavigationAction]);
        PlaylistListFragment playlistListFragment = new PlaylistListFragment();
        playlistListFragment.setArguments(bundle);

        transaction.replace(R.id.content_frame, playlistListFragment);
        transaction.commit();
        return playlistListFragment;
    }

    public PlaylistSearchResultsFragment showPlaylistSearchResultsFragment(String query) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        Bundle bundle = new Bundle();
        bundle.putString("query", query);
        PlaylistSearchResultsFragment playlistListFragment = new PlaylistSearchResultsFragment();
        playlistListFragment.setArguments(bundle);

        transaction.replace(R.id.content_frame, playlistListFragment);

        /* This fragment is special, we don't want more than one on the back stack */
        Fragment current = getCurrentFragment();
        if (current instanceof PlaylistSearchResultsFragment) {
            getFragmentManager().popBackStackImmediate();
        }
        transaction.addToBackStack(playlistListFragment.getClass().getName());
        transaction.commit();
        return playlistListFragment;
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
        transaction.detach(current);
        transaction.attach(current);
        transaction.commit();
    }

    /* Not used at the moment */
    public void onAddTrackFinshed(final boolean trackAdded) {
        showNewTrackOnPlaylistLoad = trackAdded;
        FragmentManager fm = getFragmentManager();
        fm.popBackStackImmediate();
    }


    public String getCurrentUserId() {
        return mUserId;
    }

    public Fragment getCurrentFragment() {
        return getFragmentManager().findFragmentById(R.id.content_frame);
    }

    public PlaylistPlayer getPlaylistPlayer() {
        return mPlaylistPlayer;
    }

    public boolean getShowNewTrackAndReset() {
        boolean tmp = showNewTrackOnPlaylistLoad;
        showNewTrackOnPlaylistLoad = false;
        return tmp;
    }

    public PlaylistPlayer subscribePlaylistPlayer(final String playlistId) {

        unsubscribeFromCurrentPlaylist();

        /* Initial authentication to getString the player */
        mPlaylistPlayer = mQueueUpClient.getPlaylistPlayer(new QueueUp.CallReceiver<PlaylistClient>() {
            @Override
            public void onResult(PlaylistClient result) {
                Log.d(QueueUp.LOG_TAG, "AUTH SUCCESS");
                PlaylistPlayer player = (PlaylistPlayer) result;

                /* Attach the notification listener... */
                mPlaylistPlayer.addPlaylistListener(getPlayerNotification());


                /* Start log into Spotify sequence */
                spotifyLogin();

                /* Perform the subscription */
                player.subscribe(playlistId, true);

            }

            @Override
            public void onException(Exception e) {
                toast(e.getMessage());
                Log.e(QueueUp.LOG_TAG, "AUTH PROBLEM: " + e.getMessage());
            }
        }, new PlaybackReceiver() {
            @Override
            public void onPlaybackEnd() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stopPlayback();

                    }
                });
            }
        });

        return mPlaylistPlayer;
    }

    public void unsubscribeFromCurrentPlaylist() {
        if (mPlaylistPlayer != null) {
            Log.d(QueueUp.LOG_TAG, "Unsubscribing from previous player");
            mPlaylistPlayer.removeAllPlaylistListeners();
            if (mSpotifyPlayer != null) {
                mSpotifyPlayer.stopReceivingPlaybackNotifications();
            }
            mPlaylistPlayer.disconnect();
            mPlaylistPlayer = null;
        }
    }


    public QueueUpClient getQueueupClient() {
        return mQueueUpClient;
    }

    public void spotifyLogin() {

        /* If we want to skip logging in again... */
        if (mSpotifyPlayer != null && mSpotifyPlayer.getPlayer().isLoggedIn()) {
            Log.d(QueueUp.LOG_TAG, "Not re-logging in, no need");

            /* Make sure we create the player on the main thread */
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    beginPlayback();
                }
            });

        } else {

            /* If we have an access token*/
            if (mSpotifyTokenManager.haveAccessToken()) {

                /* And it hasn't expired */
                if (mSpotifyTokenManager.haveValidAccessToken()) {

                    /* Initialize with the valid access token... */
                    initPlayer(mSpotifyTokenManager.getAccessToken());
                } else {

                    /* Refresh an expired token */
                    mSpotifyTokenManager.refreshToken(new QueueUp.CallReceiver<String>() {
                        @Override
                        public void onResult(String result) {
                            initPlayer(result);
                        }

                        @Override
                        public void onException(Exception e) {
                            e.printStackTrace();
                            Log.e(QueueUp.LOG_TAG, "Error refreshing token: " + e.getMessage());
                            toast("Error refreshing token");
                        }
                    });
                }
            } else {

                /* Build a request to log in through the browser...*/
                AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(
                        CLIENT_ID,
                        AuthenticationResponse.Type.CODE,
                        REDIRECT_URI);
                builder.setScopes(new String[]{"user-read-private", "streaming"});
                AuthenticationRequest request = builder.build();

                AuthenticationClient.openLoginInBrowser(this, request);

                /* To be seen again... in onNewIntent */
            }
        }

    }

    private void handleDrawerClickAction(int index) {
        String action = NAVIGATION_ACTIONS[index];
        Log.d(LOG_TAG, "Clicked " + action);
        showPlaylistListFragment();
        mDrawerLayout.closeDrawers();
    }

    private void handleIntent(Intent intent) {

        /* Search intent  */
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            final String query = intent.getStringExtra(SearchManager.QUERY);

            showPlaylistSearchResultsFragment(query);


        /* Spotify auth response intent */
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();
            if (uri != null) {
                AuthenticationResponse response = AuthenticationResponse.fromUri(uri);
                switch (response.getType()) {
                    case CODE:

                        /* Swap out the token for a code */
                        mSpotifyTokenManager.swapCodeForToken(response.getCode(), new QueueUp.CallReceiver<String>() {

                            @Override
                            public void onResult(String result) {
                                initPlayer(result);
                            }

                            @Override
                            public void onException(Exception e) {
                                e.printStackTrace();
                                Log.e(QueueUp.LOG_TAG, "Error swapping code for token:" + e.getMessage());
                                toast("Error swapping code for token");
                            }
                        });
                    case ERROR:
                        if (response.getError() != null) {
                            Log.e(QueueUp.LOG_TAG, "Login Error: " + response.getError());
                        }
                        break;
                }

                /* Clear the data so reloading the activity doesn't try to swap a token again */
                intent.setData(null);
            }
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
        super.onNewIntent(intent);
    }

    public void initPlayer(String accessToken) {
        Config playerConfig = new Config(this, accessToken, CLIENT_ID);


        /* Don't getString another player if there's already one initialized */
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
        mPlaylistPlayer.updateTrackPlaying(true);

    }

    /* Just recreate the activity, which resets everything to a clean slate */
    public void stopPlayback() {

        Log.d(QueueUp.LOG_TAG, "Stopping playback!");
        recreate();
    }

    public void displayHomeUp() {
        mDrawerToggle.setDrawerIndicatorEnabled(getFragmentManager().getBackStackEntryCount() == 0);

    }

    public boolean isLoggedIn() {
        return mUserId != null && mClientToken != null;
    }

    public boolean isClientRegistered() {
        return mEmailAddress != null || mFacebookId != null;
    }

    public AccessToken getAccessToken() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        Log.d(QueueUp.LOG_TAG, "Access token: " + accessToken);
        return accessToken;
    }

    public boolean isLoggedInWithFacebook() {
        return mFacebookId != null;
    }


    public void populateHeaderWithProfileInfo(View headerView) {
        ImageView userImage = (ImageView) headerView.findViewById(R.id.drawer_user_image);
        final TextView userName = (TextView) headerView.findViewById(R.id.drawer_user_name);

        Profile profile = Profile.getCurrentProfile();

        /* If logged in with Facebook*/
        if (getAccessToken() != null && profile != null) {
            Uri proPicUri = profile.getProfilePictureUri(userImage.getLayoutParams().width, userImage.getLayoutParams().height);
            Picasso.with(this).load(proPicUri).into(userImage);

            userName.setText(profile.getFirstName() + " " + profile.getLastName());
        } else {

            mQueueUpClient.userGet(mUserId, new QueueUp.CallReceiver<QueueUpUser>() {
                @Override
                public void onResult(final QueueUpUser result) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            userName.setText(result.name);
                        }
                    });
                }

                @Override
                public void onException(Exception e) {
                    toast(e.getMessage());
                }
            });
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
        mStore.clear();

        LoginManager.getInstance().logOut();

        recreate();
    }

    /* Initialize the FB SDK if it isn't already */
    public void ensureSdkInitialized() {
        if (!FacebookSdk.isInitialized()) {
            FacebookSdk.sdkInitialize(getApplicationContext(), new FacebookSdk.InitializeCallback() {
                @Override
                public void onInitialized() {
                    maybeDoFacebookLogin();
                }
            });
        }
    }

    /* Only redo the FB login if the access token is null and the user is logged in */
    public void maybeDoFacebookLogin() {
        if (mStore.getString(QueueUpStore.FACEBOOK_ID) != null && AccessToken.getCurrentAccessToken() == null) {
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            intent.putExtra(LoginActivity.EXTRA_DO_LOGIN, true);
            startActivity(intent);
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
            Log.e(QueueUp.LOG_TAG, "Cant get focus to hide keyboard!");
        }

    }

    /* Show keyboard */
    public void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

    }

    /* Create a new playlist */
    public void doCreatePlaylist(String name) {
        mQueueUpClient.playlistCreate(name, new QueueUp.CallReceiver<QueueUpPlaylist>() {
            @Override
            public void onResult(QueueUpPlaylist result) {
                toast("Created " + result.name);
                showPlaylistFragment(result.id);
            }

            @Override
            public void onException(Exception e) {
                Log.e(QueueUp.LOG_TAG, "Problem creating playlist: " + e.getMessage());
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
            mSpotifyPlayer.stopReceivingPlaybackNotifications();
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

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() == 0) {

//            MenuItem searchItem = mMenu.findItem(R.id.search_playlists);
//            SearchView search = (SearchView) searchItem.getActionView();
//            if (search != null && !search.isIconified()) {
//                searchItem.collapseActionView();
//                search.setIconified(true);
//            } else {
//            }
            super.onBackPressed();

        } else {
            getFragmentManager().popBackStack();
        }
    }

    public void toast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void toastTop(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 10);
                toast.show();
            }
        });
    }

}
