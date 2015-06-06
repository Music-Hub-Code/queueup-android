package org.louiswilliams.queueupplayer;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
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
import com.spotify.sdk.android.player.Spotify;

import queueup.Queueup;
import queueup.QueueupClient;


public class MainActivity extends Activity implements FragmentManager.OnBackStackChangedListener, ConnectionStateCallback, PlayerNotificationCallback {

    private static final int REQUEST_CODE = 1234;
    private static final String REDIRECT_URI = "queueup://callback";
    private String[] navigationTitles = {"Hot","Me"};
    private String CLIENT_ID = this.getString(R.string.spotify_client_id);
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private QueueupClient queueupClient;
    private Player mPlayer;
    private String mUserId;
    private String mClientToken;

    private static final String STORE_NAME = "authStore";
    private static final String LOG_TAG = "QUEUEUP";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().addOnBackStackChangedListener(this);

        SharedPreferences prefs = getSharedPreferences(STORE_NAME, 0);
        mClientToken = prefs.getString("clientToken", null);
        mUserId = prefs.getString("userId", null);

        if (mClientToken != null && mUserId != null) {

            queueupClient = new QueueupClient(mClientToken, mUserId);

            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.content_frame, new PlaylistListFragment());
            transaction.commit();

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

    public void spotifyLogin() {
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginInBrowser(this, request);
    }


    public void initPlayer(String accessToken) {
        Config playerConfig = new Config(this, accessToken, CLIENT_ID);
        mPlayer = Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {

            @Override
            public void onInitialized(Player player) {
                Log.d(LOG_TAG, "Init Player");
                player.addConnectionStateCallback(MainActivity.this);
                player.addPlayerNotificationCallback(MainActivity.this);
//                updatePlaying(false);

                player.play("spotify:track:2To3PTOTGJUtRsK3nQemP4");
//                initSocket(playlistId);
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

    public String getCurrentUserId() {
        return mUserId;
    }

    public QueueupClient getQueueupClient() {
        return queueupClient;
    }

    public void displayHomeUp() {
        mDrawerToggle.setDrawerIndicatorEnabled(getFragmentManager().getBackStackEntryCount() ==  0);

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

    }

    @Override
    public void onPlaybackError(ErrorType errorType, String s) {

    }
}
