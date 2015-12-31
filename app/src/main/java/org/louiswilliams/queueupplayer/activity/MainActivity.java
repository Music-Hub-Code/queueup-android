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
import android.net.http.HttpResponseCache;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
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

import org.json.JSONObject;
import org.louiswilliams.queueupplayer.QueueUpApplication;
import org.louiswilliams.queueupplayer.R;
import org.louiswilliams.queueupplayer.fragment.AddTrackFragment;
import org.louiswilliams.queueupplayer.fragment.BackButtonListener;
import org.louiswilliams.queueupplayer.fragment.LocationSelectFragment;
import org.louiswilliams.queueupplayer.fragment.PlaylistFragment;
import org.louiswilliams.queueupplayer.fragment.PlaylistListFragment;
import org.louiswilliams.queueupplayer.fragment.PlaylistSearchResultsFragment;
import org.louiswilliams.queueupplayer.fragment.SpotifyPlaylistFragment;
import org.louiswilliams.queueupplayer.fragment.SpotifyPlaylistListFragment;
import org.louiswilliams.queueupplayer.queueup.PlaybackController;
import org.louiswilliams.queueupplayer.queueup.PlaybackReceiver;
import org.louiswilliams.queueupplayer.queueup.PlaylistListener;
import org.louiswilliams.queueupplayer.queueup.QueueUp;
import org.louiswilliams.queueupplayer.queueup.QueueUpException;
import org.louiswilliams.queueupplayer.queueup.QueueUpLocationListener;
import org.louiswilliams.queueupplayer.queueup.QueueUpStore;
import org.louiswilliams.queueupplayer.queueup.api.QueueUpClient;
import org.louiswilliams.queueupplayer.queueup.api.SpotifyClient;
import org.louiswilliams.queueupplayer.queueup.api.SpotifyTokenManager;
import org.louiswilliams.queueupplayer.queueup.objects.QueueUpPlaylist;
import org.louiswilliams.queueupplayer.queueup.objects.QueueUpUser;
import org.louiswilliams.queueupplayer.queueup.objects.SpotifyUser;
import org.louiswilliams.queueupplayer.service.PlayerService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity
        extends AppCompatActivity
        implements PlaybackReceiver, FragmentManager.OnBackStackChangedListener {

    public static final String PLAYLISTS_ALL = "all";
    public static final String PLAYLISTS_NEARBY = "nearby";
    public static final String PLAYLISTS_FRIENDS = "friends";
    public static final String PLAYLISTS_MINE = "mine";
    public static final int[] NAVIGATION_TITLES = {R.string.nearby_playlists, R.string.top_playlists, R.string.friends_playlists, R.string.my_playlists};
    public static final String[] NAVIGATION_ACTIONS = {PLAYLISTS_NEARBY, PLAYLISTS_ALL, PLAYLISTS_FRIENDS, PLAYLISTS_MINE};
    private static final String LOG_TAG = QueueUp.LOG_TAG;
    private static final int LOCATION_SETTINGS_CODE = 4444;
    private static final int SPOTIFY_LOGIN_CODE = 1234;
    private static final int REQUEST_PERMISSION = 1;


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
    private QueueUp.CallReceiver<String> spotifyAuthListener;
    private String mClientToken;
    private String mEmailAddress;
    private String mFacebookId;
    private String mSpotifyClientId;
    private String mUserId;
    private String mUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Set up Google analytics for uncaught exceptions */
        Tracker tracker = ((QueueUpApplication)getApplication()).getDefaultTracker();
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = new ExceptionReporter(
                tracker,
                Thread.getDefaultUncaughtExceptionHandler(),
                getApplicationContext()
        );
        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);

        /* Install a small cache */
        try {
            File httpCacheDir = new File(getCacheDir(), "queueUpHttp");
            long httpCacheSize = 5 * 1024 * 1024; // 5MiB
            HttpResponseCache.install(httpCacheDir, httpCacheSize);
        } catch (IOException e) {
            Log.e(QueueUp.LOG_TAG, "Could not install HTTP cache: " + e);
        }

        /* Handle certain navigation actions with the action bar back button*/
        getFragmentManager().addOnBackStackChangedListener(this);

        /* Start listening for location updates until we get a decent and fresh and decently accurate location */
        initLocationListener(false);

        mStore = QueueUpStore.with(this);

        mClientToken = mStore.getString(QueueUpStore.CLIENT_TOKEN);
        mUserId = mStore.getString(QueueUpStore.USER_ID);
        mUserName = mStore.getString(QueueUpStore.USER_NAME);
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

    public void showSpotifyPlaylistListFragment(String playlistId) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Bundle bundle = new Bundle();
        bundle.putString("playlist_id", playlistId);

        SpotifyPlaylistListFragment fragment = new SpotifyPlaylistListFragment();
        fragment.setArguments(bundle);

        transaction.replace(R.id.content_frame, fragment);
        transaction.addToBackStack(fragment.getClass().getName());
        transaction.commit();
    }

    public void showSpotifyPlaylistFragment(String playlistId, String spotifyUserId, String spotifyPlaylistId) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Bundle bundle = new Bundle();
        bundle.putString("playlist_id", playlistId);
        bundle.putString("spotify_user_id", spotifyUserId);
        bundle.putString("spotify_playlist_id", spotifyPlaylistId);

        SpotifyPlaylistFragment fragment = new SpotifyPlaylistFragment();
        fragment.setArguments(bundle);

        transaction.replace(R.id.content_frame, fragment);
        transaction.addToBackStack(fragment.getClass().getName());
        transaction.commit();
    }

    public void showLocationSelectCreateFragment(String playlistName) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Bundle bundle = new Bundle();
        bundle.putString(LocationSelectFragment.ARG_PLAYLIST_NAME, playlistName);
        bundle.putString(LocationSelectFragment.ARG_ACTION, LocationSelectFragment.ACTION_CREATE);

        LocationSelectFragment fragment = new LocationSelectFragment();
        fragment.setArguments(bundle);

        transaction.replace(R.id.content_frame, fragment);
        transaction.addToBackStack(fragment.getClass().getName());
        transaction.commit();
    }

    public void showLocationSelectMoveFragment(String playlistId) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Bundle bundle = new Bundle();
        bundle.putString(LocationSelectFragment.ARG_PLAYLIST_ID, playlistId);
        bundle.putString(LocationSelectFragment.ARG_ACTION, LocationSelectFragment.ACTION_MOVE);

        LocationSelectFragment fragment = new LocationSelectFragment();
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

    public void setClientName(String name, QueueUp.CallReceiver<JSONObject> receiver) {
        if (name != null && name.length() > 0) {
            mStore.putString(QueueUpStore.USER_NAME, name);
            mQueueUpClient.setUserName(getCurrentUserId(), name, receiver);
        } else {
            receiver.onException(new QueueUpException("Name cannot be empty"));
        }
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

    public QueueUpStore getStore() {
        return mStore;
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

    public void spotifyLogin(final QueueUp.CallReceiver<String> listener) {

        /* If we have an access token*/
        if (mSpotifyTokenManager.haveAccessToken()) {

            /* And it hasn't expired */
            if (mSpotifyTokenManager.haveValidAccessToken()) {

                /* Initialize with the valid access token... */
                listener.onResult(mSpotifyTokenManager.getAccessToken());
            } else {

                /* Refresh an expired token */
                mSpotifyTokenManager.refreshToken(new QueueUp.CallReceiver<String>() {
                    @Override
                    public void onResult(String result) {
                        listener.onResult(result);
                    }

                    @Override
                    public void onException(Exception e) {
                        listener.onException(new Exception("Error refreshing token: ", e));
                    }
                });
            }
        } else {

            /* Listen for the intent response */
            spotifyAuthIntentListen(listener);

            /* Build a request to log in through the browser...*/
            AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(
                    mSpotifyClientId,
                    AuthenticationResponse.Type.CODE,
                    QueueUp.SPOTIFY_LOGIN_REDIRECT_URI);
            builder.setScopes(new String[]{"user-read-private", "playlist-read-private", "streaming"});
            AuthenticationRequest request = builder.build();

            AuthenticationClient.openLoginActivity(this, SPOTIFY_LOGIN_CODE, request);
//            AuthenticationClient.openLoginInBrowser(this, request);
            /* To be seen again... in onNewIntent */
        }
    }

    private void handleSpotifyLoginResponse(AuthenticationResponse response) {
        final QueueUp.CallReceiver<String> receiver = spotifyAuthListener;

        /* Consume the listener by invalidating it */
        spotifyAuthListener = null;

        switch (response.getType()) {
            case CODE:

                /* Swap out the token for a code */
                mSpotifyTokenManager.swapCodeForToken(response.getCode(), new QueueUp.CallReceiver<String>() {

                    @Override
                    public void onResult(String result) {
                        if (receiver != null) {
                            receiver.onResult(result);
                        }
                    }

                    @Override
                    public void onException(Exception e) {
                        if (receiver != null) {
                            receiver.onException(new Exception("Error swapping code for token", e));
                        }
                    }
                });
            case ERROR:
                if (response.getError() != null && receiver != null) {
                    receiver.onException(new QueueUpException(response.getError()));
                }
                break;
            default:
                receiver.onException(new QueueUpException("Received response from Spotify: " + response.getType().name()));
                break;
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

            /* Get the reference to the listener */
            final QueueUp.CallReceiver<String> receiver = spotifyAuthListener;

            if (uri != null) {
                AuthenticationResponse response = AuthenticationResponse.fromUri(uri);

                handleSpotifyLoginResponse(response);
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recreate();

            }
        });
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
                    spotifyLogin(new QueueUp.CallReceiver<String>() {
                        @Override
                        public void onResult(final String accessToken) {
                            SpotifyClient.with(accessToken).getMe(new QueueUp.CallReceiver<SpotifyUser>() {

                                @Override
                                public void onResult(SpotifyUser user) {
                                    if (SpotifyUser.PRODUCT_PREMIUM.equalsIgnoreCase(user.product)) {
                                        mPlayerService.initPlayer(accessToken);
                                    } else {
                                        toast("You need Spotify Premium");
                                        mPlayerService.stopPlayback();
                                    }
                                }

                                @Override
                                public void onException(Exception e) {
                                    Log.e(QueueUp.LOG_TAG, e.getMessage());
                                    toast(e.getMessage());
                                }
                            });
                        }

                        @Override
                        public void onException(Exception e) {
                            Log.e(QueueUp.LOG_TAG, e.getMessage());
                            toast(e.getMessage());
                        }
                    });
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

    public void initLocationListener(boolean requestIfNotEnabled) {
        if (locationListener == null) {
            LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

            if (locationPermissionGranted(requestIfNotEnabled)) {
                locationListener = new QueueUpLocationListener(locationManager);
                locationListener.startUpdatesUntilBest();
            }
        } else {
            locationListener.stopUpdates();
            locationListener.startUpdatesUntilBest();
        }
    }

    public boolean isLocationEnabled(boolean requestIfDisabled) {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean enabled =  (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
        if (requestIfDisabled && !enabled) {
            alertLocationEnable();
        }
        return enabled;
    }

    /** Provide rationale for location permission request if previously denied */
    public void alertLocationRequestRationale(final String permission) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.location_not_found_title);  // GPS not found
        builder.setMessage(R.string.location_not_found_message); // Want to enable?
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, REQUEST_PERMISSION);
            }
        });
        builder.setNegativeButton(R.string.no, null);
        builder.create().show();
    }

    /** Provide user with option to enable location services in system */
    public void alertLocationEnable() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);

        builder.setTitle(R.string.location_not_found_title);  // GPS not found
        builder.setMessage(R.string.location_not_found_message); // Want to enable?
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                startActivityForResult(intent, LOCATION_SETTINGS_CODE);
            }
        });
        builder.setNegativeButton(R.string.no, null);
        builder.create().show();
    }

    public boolean locationPermissionGranted(boolean requestIfDisabled) {
        final String permission = Manifest.permission.ACCESS_FINE_LOCATION;
        boolean disabled = false;

        /* Only in marshmallow do we need to do this */
        if (Build.VERSION.SDK_INT >= 23) {
            disabled = (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED);
            if (disabled && requestIfDisabled) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    alertLocationRequestRationale(permission);
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{permission}, REQUEST_PERMISSION);
                }
            }
        }
        return !disabled;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initLocationListener(true);
                } else {
                    toast("Location permission not granted");
                }
                break;
            default:
                break;
        }
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

    public boolean clientHasName() {
        return mUserName != null;
    }

    public AccessToken getAccessToken() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        Log.d(QueueUp.LOG_TAG, "Access token: " + accessToken);
        return accessToken;
    }

    public boolean isLoggedInWithFacebook() {
        return mFacebookId != null;
    }


    public void spotifyAuthIntentListen(QueueUp.CallReceiver<String> receiver) {
        spotifyAuthListener = receiver;
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

            if (mUserName == null) {
                mQueueUpClient.userGet(mUserId, new QueueUp.CallReceiver<QueueUpUser>() {
                    @Override
                    public void onResult(final QueueUpUser result) {
                        mStore.putString(QueueUpStore.USER_NAME, result.name);
                        mUserName = result.name;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                userName.setText(mUserName);
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


    }

    public void doLogin() {
        Intent loginIntent = new Intent(getBaseContext(), LoginActivity.class);
        startActivityForResult(loginIntent, LoginActivity.QUEUEUP_LOGIN_REQUEST_CODE);

    }

    public void doLogout() {
        mStore.clear();

        AuthenticationClient.clearCookies(this);
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

        /* Write out cache to FS before exiting */
        HttpResponseCache cache = HttpResponseCache.getInstalled();
        if (cache != null) {
            cache.flush();
        }

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == LoginActivity.QUEUEUP_LOGIN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
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
        } else if (requestCode == LOCATION_SETTINGS_CODE) {
            if (isLocationEnabled(false)) {
                navigateDrawer(0);
            }
        } else if (requestCode == SPOTIFY_LOGIN_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            handleSpotifyLoginResponse(response);
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
            Fragment current = getCurrentFragment();
            if (current != null && current instanceof BackButtonListener) {

                /* If the fragment doesn't consume the event, do the default action */
                if (!((BackButtonListener) current).onBackButtonPressed()) {
                    getFragmentManager().popBackStack();
                }
            } else {
                getFragmentManager().popBackStack();
            }
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
