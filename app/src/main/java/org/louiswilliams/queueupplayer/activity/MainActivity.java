package org.louiswilliams.queueupplayer.activity;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
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
import com.google.android.gms.analytics.Tracker;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.squareup.picasso.Picasso;

import org.louiswilliams.queueupplayer.QueueUpApplication;
import org.louiswilliams.queueupplayer.R;
import org.louiswilliams.queueupplayer.fragment.AddTrackFragment;
import org.louiswilliams.queueupplayer.fragment.PlaylistFragment;
import org.louiswilliams.queueupplayer.fragment.PlaylistListFragment;
import org.louiswilliams.queueupplayer.fragment.PlaylistSearchResultsFragment;
import org.louiswilliams.queueupplayer.queueup.PlaybackController;
import org.louiswilliams.queueupplayer.queueup.PlaybackReceiver;
import org.louiswilliams.queueupplayer.queueup.PlaylistListener;
import org.louiswilliams.queueupplayer.queueup.QueueUp;
import org.louiswilliams.queueupplayer.queueup.QueueUpException;
import org.louiswilliams.queueupplayer.queueup.QueueUpLocationListener;
import org.louiswilliams.queueupplayer.queueup.QueueUpStore;
import org.louiswilliams.queueupplayer.queueup.api.QueueUpClient;
import org.louiswilliams.queueupplayer.queueup.api.SpotifyTokenManager;
import org.louiswilliams.queueupplayer.queueup.objects.QueueUpPlaylist;
import org.louiswilliams.queueupplayer.queueup.objects.QueueUpUser;
import org.louiswilliams.queueupplayer.service.PlayerService;

import java.util.ArrayList;
import java.util.List;

public class MainActivity
        extends AppCompatActivity
        implements
        PlaybackReceiver,
        FragmentManager.OnBackStackChangedListener {

    public static final String PLAYLISTS_ALL = "all";
    public static final String PLAYLISTS_NEARBY = "nearby";
    public static final String PLAYLISTS_FRIENDS = "friends";
    public static final String PLAYLISTS_MINE = "mine";
    public static final int[] NAVIGATION_TITLES = {R.string.nearby_playlists, R.string.friends_playlists, R.string.my_playlists};
    public static final String[] NAVIGATION_ACTIONS = {PLAYLISTS_NEARBY, PLAYLISTS_FRIENDS, PLAYLISTS_MINE};
    private static final String REDIRECT_URI = "queueup://callback";
    private static final String LOG_TAG = QueueUp.LOG_TAG;


    private boolean showNewTrackOnPlaylistLoad;
    public int currentNavigationAction;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private DrawerListAdapter mDrawerAdapter;
    private Intent mPlayerServiceIntent;
    private ListView mDrawerList;
    private QueueUpLocationListener locationListener;
    private PlayerService mPlayerService;
    private QueueUpClient mQueueUpClient;
    private QueueUpStore mStore;
    private ServiceConnection mServiceConnection;
    private SpotifyTokenManager mSpotifyTokenManager;
    private String mClientToken;
    private String mEmailAddress;
    private String mFacebookId;
    private String mSpotifyClientId;
    private String mUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Set up Google analytics for uncaught exceptions */
//        Tracker tracker = ((QueueUpApplication)getApplication()).getDefaultTracker();
//        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = new ExceptionReporter(
//                tracker,
//                Thread.getDefaultUncaughtExceptionHandler(),
//                getApplicationContext()
//        );
//        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);

        /* Handle certain navigation actions with the action bar back button*/
        getFragmentManager().addOnBackStackChangedListener(this);

        /* Start listening for location updates until we get a decent and fresh and decently accurate location */
        if (locationListener == null) {
            LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

            if (ensureAndRequestPermission(Manifest.permission.ACCESS_COARSE_LOCATION) && ensureAndRequestPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                locationListener = new QueueUpLocationListener(locationManager);
                locationListener.startUpdatesUntilBest();
            }
        } else {
            locationListener.stopUpdates();
            locationListener.startUpdatesUntilBest();
        }

        mStore = QueueUpStore.with(this);

        mClientToken = mStore.getString(QueueUpStore.CLIENT_TOKEN);
        mUserId = mStore.getString(QueueUpStore.USER_ID);
        mFacebookId = mStore.getString(QueueUpStore.FACEBOOK_ID);
        mEmailAddress = mStore.getString(QueueUpStore.EMAIL_ADDRESS);
        mSpotifyClientId = getString(R.string.spotify_client_id);

        /* If we are registered non-anonymously, proceed, otherwise login anonymously */
        try {
            if (isLoggedIn()) {
                mQueueUpClient = new QueueUpClient(getApplicationContext(), mClientToken, mUserId);
            } else {
                mQueueUpClient = new QueueUpClient(getApplicationContext(), null, null);
                goToSplash();
                return;
            }
        } catch (QueueUpException e) {
            Log.e(QueueUp.LOG_TAG, e.getMessage());
            toast(e.getMessage());
        }

        mSpotifyTokenManager = SpotifyTokenManager.with(mQueueUpClient, mStore);

        /* Set up out layout */
        doSetup(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    /* Setup UI elements */
    public void doSetup(Bundle savedInstanceState) {

        currentNavigationAction = 0;

        if (savedInstanceState == null) {
            PlaylistListFragment playlistListFragment = showPlaylistListFragment();

            /* Bind to player if it is running */
            if (isPlayerServiceRunning()) {
                bindPlayerService(playlistListFragment, false);
            }
        } else {
            Log.d(QueueUp.LOG_TAG, "savedInstanceState != null");

            if (isPlayerServiceRunning()) {
                bindPlayerService(null, false);
            }
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
                navigateDrawer(position - 1);
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

        LinearLayout footerView = (LinearLayout) getLayoutInflater().inflate(R.layout.drawer_footer, null, false);

        mDrawerList.addHeaderView(headerView, null, false);
        mDrawerList.addFooterView(footerView, null, false);
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

    public void goToLogin() {
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

    public String getCurrentUserId() {
        return mUserId;
    }

    public Fragment getCurrentFragment() {
        return getFragmentManager().findFragmentById(R.id.content_frame);
    }

    public PlaybackController getPlaybackController() {
        return mPlayerService;
    }

    public boolean getShowNewTrackAndReset() {
        boolean tmp = showNewTrackOnPlaylistLoad;
        showNewTrackOnPlaylistLoad = false;
        return tmp;
    }

    public void subscribePlaylistPlayer(String playlistId, final PlaylistListener listener) {

//        unsubscribeFromCurrentPlaylist();

        mPlayerServiceIntent = new Intent(MainActivity.this, PlayerService.class);
        mPlayerServiceIntent.putExtra(PlayerService.EXTRA_PLAYLIST_ID, playlistId);
        startService(mPlayerServiceIntent);

        bindPlayerService(listener, true);
    }


    public QueueUpClient getQueueUpClient() {
        return mQueueUpClient;
    }

    public void navigateDrawer(int index) {
        currentNavigationAction = index;
        mDrawerAdapter.setSelection(currentNavigationAction);
        handleDrawerClickAction(currentNavigationAction);
    }

    private void handleDrawerClickAction(int index) {
        String action = NAVIGATION_ACTIONS[index];
        Log.d(LOG_TAG, "Clicked " + action);
        showPlaylistListFragment();
        mDrawerLayout.closeDrawers();
    }

    public void spotifyLogin() {

        /* If we have an access token*/
        if (mSpotifyTokenManager.haveAccessToken()) {

            /* And it hasn't expired */
            if (mSpotifyTokenManager.haveValidAccessToken()) {

                /* Initialize with the valid access token... */
                mPlayerService.initPlayer(mSpotifyTokenManager.getAccessToken());
            } else {

                /* Refresh an expired token */
                mSpotifyTokenManager.refreshToken(new QueueUp.CallReceiver<String>() {
                    @Override
                    public void onResult(String result) {
                        mPlayerService.initPlayer(result);
                    }

                    @Override
                    public void onException(Exception e) {
                        e.printStackTrace();
                        Log.e(QueueUp.LOG_TAG, "Error refreshing token: " + e.getMessage());
                    }
                });
            }
        } else {

            /* Build a request to log in through the browser...*/
            AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(
                    mSpotifyClientId,
                    AuthenticationResponse.Type.CODE,
                    REDIRECT_URI);
            builder.setScopes(new String[]{"user-read-private", "streaming"});
            AuthenticationRequest request = builder.build();

            AuthenticationClient.openLoginInBrowser(this, request);

            /* To be seen again... in onNewIntent */
        }
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
                                mPlayerService.initPlayer(result);
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


    /* Just recreate the activity, which resets everything to a clean slate */
    public void stopPlayback() {

        Log.d(QueueUp.LOG_TAG, "Stopping playback!");

        if (mPlayerService != null) {
            mPlayerService.removeAllPlaylistListeners();

            unBindPlayerService();
            stopService(mPlayerServiceIntent);
            mPlayerService = null;
            mPlayerServiceIntent = null;
            mServiceConnection = null;
        }

        recreate();
    }

    public boolean isPlayerServiceRunning() {
        if (mPlayerService != null) {
            return true;
        } else {
            ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningServiceInfo serviceInfo : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (PlayerService.class.getName().equals(serviceInfo.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public void bindPlayerService(final PlaylistListener listener, final boolean doLogin) {

        unBindPlayerService();

        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                Log.d(QueueUp.LOG_TAG, "Service connected");
                mPlayerService = ((PlayerService.LocalBinder) iBinder).getService();

                if (doLogin) {
                    spotifyLogin();
                }

                if (listener != null) {
                    mPlayerService.addPlaylistListener(listener);
                }
                mPlayerService.setPlaybackReceiver(MainActivity.this);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                Log.d(QueueUp.LOG_TAG, "Service disconnected");
            }
        };

        if (mPlayerServiceIntent == null) {
            mPlayerServiceIntent = new Intent(this, PlayerService.class);
        }

        bindService(mPlayerServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public void unBindPlayerService() {
        if (mPlayerService != null && mServiceConnection != null) {
            mPlayerService.setPlaybackReceiver(null);
            unbindService(mServiceConnection);
            mServiceConnection = null;
        }
    }

    @Override
    public void onPlaybackEnd() {
        stopPlayback();
    }

    public boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    public void alertLocationEnable() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.location_not_found_title);  // GPS not found
        builder.setMessage(R.string.location_not_found_message); // Want to enable?
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        });
        builder.setNegativeButton(R.string.no, null);
        builder.create().show();
    }

    public boolean ensureAndRequestPermission(String permission) {
        boolean disabled = false;

        /* Only in marshmallow do we need to do this */
        if (Build.VERSION.SDK_INT >= 23) {
            disabled= (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED);
            if (disabled) {
                ActivityCompat.requestPermissions(this, new String[]{permission}, 1);
            }
        }
        return !disabled;
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
    public void doCreatePlaylist(String name, Location location) {
        Log.d(QueueUp.LOG_TAG, "Created playlist at " + location);
        mQueueUpClient.playlistCreate(name, location, new QueueUp.CallReceiver<QueueUpPlaylist>() {
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

    public QueueUpLocationListener getLocationListener() {
        return locationListener;
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
    protected void onPause() {
        if (locationListener != null) {
            locationListener.startPassiveUpdates();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {

        unBindPlayerService();

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == LoginActivity.QUEUEUP_LOGIN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
//                toast("Login successful");
                recreate();
            } else if (resultCode == RESULT_CANCELED) {
//                toast("Login cancelled");
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
