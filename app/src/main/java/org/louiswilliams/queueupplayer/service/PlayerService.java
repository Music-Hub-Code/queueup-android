package org.louiswilliams.queueupplayer.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.analytics.ExceptionReporter;
import com.google.android.gms.analytics.Tracker;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;

import org.louiswilliams.queueupplayer.QueueUpApplication;
import org.louiswilliams.queueupplayer.R;
import org.louiswilliams.queueupplayer.queueup.PlaybackController;
import org.louiswilliams.queueupplayer.queueup.PlaybackReceiver;
import org.louiswilliams.queueupplayer.queueup.PlaylistClient;
import org.louiswilliams.queueupplayer.queueup.PlaylistListener;
import org.louiswilliams.queueupplayer.queueup.PlaylistPlayer;
import org.louiswilliams.queueupplayer.queueup.QueueUp;
import org.louiswilliams.queueupplayer.queueup.QueueUpException;
import org.louiswilliams.queueupplayer.queueup.QueueUpStore;
import org.louiswilliams.queueupplayer.queueup.QueueUpPlayer;
import org.louiswilliams.queueupplayer.queueup.api.QueueUpClient;
import org.louiswilliams.queueupplayer.queueup.objects.QueueUpStateChange;
import org.louiswilliams.queueupplayer.widget.PlayerNotification;

public class PlayerService extends Service implements PlaybackController, ConnectionStateCallback {

    public static final String EXTRA_PLAYLIST_ID = "PLAYLIST_ID";

    private IBinder mBinder;
    private PlayerNotification mNotification;
    private PlaylistPlayer mPlaylistPlayer;
    private PlaybackReceiver mPlaybackReceiver;
    private QueueUpClient queueUpClient;
    private QueueUpPlayer mQueueUpPlayer;
    private String mSpotifyClientId;
    private Tracker mTracker;

    @Override
    public void onCreate() {
        mBinder = new LocalBinder();
        mNotification = new PlayerNotification(this);

        mTracker = ((QueueUpApplication)getApplication()).getDefaultTracker();
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = new ExceptionReporter(
                mTracker,
                Thread.getDefaultUncaughtExceptionHandler(),
                getApplicationContext()
        );
        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);

        mSpotifyClientId = getString(R.string.spotify_client_id);

        QueueUpStore mStore = QueueUpStore.with(this);
        String mClientToken = mStore.getString(QueueUpStore.CLIENT_TOKEN);
        String mUserId = mStore.getString(QueueUpStore.USER_ID);

        try {
            queueUpClient = new QueueUpClient(getApplicationContext(), mClientToken, mUserId);
        } catch (QueueUpException e) {
            Log.e(QueueUp.LOG_TAG, e.getMessage());

        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        final String playlistId = intent.getStringExtra(EXTRA_PLAYLIST_ID);

        unsubscribeFromCurrentPlaylist();

        /* Initial authentication to getString the player */
        mPlaylistPlayer = queueUpClient.getPlaylistPlayer(new QueueUp.CallReceiver<PlaylistClient>() {
            @Override
            public void onResult(PlaylistClient result) {
                Log.d(QueueUp.LOG_TAG, "AUTH SUCCESS");
                PlaylistPlayer playlistPlayer = (PlaylistPlayer) result;

                /* Attach the notification listener... */
                playlistPlayer.addPlaylistListener(mNotification);

                /* Perform the subscription */
                playlistPlayer.subscribe(playlistId, true);

            }

            @Override
            public void onException(Exception e) {
                Log.e(QueueUp.LOG_TAG, "AUTH PROBLEM: " + e.getMessage());
            }
        }, new PlaybackReceiver() {
            @Override
            public void onPlaybackEnd() {
                stopSelf();
            }
        });

        startForeground(PlayerNotification.NOTIFICATION_ID, mNotification);

        return START_NOT_STICKY;
    }

    public void initPlayer(String accessToken) {
        Config playerConfig = new Config(this, accessToken, mSpotifyClientId);
        playerConfig.useCache(true);

        /* Don't getString another player if there's already one initialized */
        if (mQueueUpPlayer != null &&  mQueueUpPlayer.getPlayer().isInitialized()) {
            beginPlayback();
        } else {


            SpotifyPlayer player = Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {

                @Override
                public void onInitialized(SpotifyPlayer spotifyPlayer) {
                    // Construct our player wrapper
                    mQueueUpPlayer = new QueueUpPlayer(PlayerService.this, spotifyPlayer);
                    beginPlayback();
                }

                @Override
                public void onError(Throwable throwable) {
                    Log.e(QueueUp.LOG_TAG, "Could not initialize: " + throwable.getMessage());
                    updatePlaybackReady(false);
                }
            });

        }
    }

    public void beginPlayback() {
        Log.d(QueueUp.LOG_TAG, "Init Player");

        /* Attach the player listener */
        mQueueUpPlayer.startReceivingPlaybackNotifications();
        mQueueUpPlayer.getPlayer().addConnectionStateCallback(this);
        addPlaylistListener(mQueueUpPlayer);
    }

    public void unsubscribeFromCurrentPlaylist() {
        if (mPlaylistPlayer != null) {
            Log.d(QueueUp.LOG_TAG, "Unsubscribing from previous player");
            removeAllPlaylistListeners();
            if (mQueueUpPlayer != null) {
                mQueueUpPlayer.stopReceivingPlaybackNotifications();
            }
            mPlaylistPlayer = null;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        removeAllPlaylistListeners();
        updatePlaybackReady(false);
        updateTrackPlaying(false);
        mPlaylistPlayer.disconnect();
        mNotification.cancel();
        if (mQueueUpPlayer != null) {
            mQueueUpPlayer.stopReceivingPlaybackNotifications();
            Spotify.destroyPlayer(this);
        }
        super.onDestroy();
    }


    public void addPlaylistListener(PlaylistListener listener) {
        if (mPlaylistPlayer != null) {
            mPlaylistPlayer.addPlaylistListener(listener);
        }
    }

    public void removePlaylistListener(PlaylistListener listener) {
        mPlaylistPlayer.removePlaylistListener(listener);
    }

    public void removeAllPlaylistListeners() {
        mPlaylistPlayer.removeAllPlaylistListeners();
    }

    /* This service is a wrapper for the PlaylistPlayer, so we just pass control to it */

    @Override
    public String getPlaylistId() {
        return mPlaylistPlayer.getPlaylistId();
    }

    @Override
    public int getCurrentProgress() {
        return mPlaylistPlayer.getCurrentProgress();
    }

    @Override
    public int getCurrentDuration() {
        return mPlaylistPlayer.getCurrentDuration();
    }

    @Override
    public QueueUpStateChange getCurrentState() {
        return mPlaylistPlayer.getCurrentState();
    }

    @Override
    public void stopPlayback() {
        mPlaylistPlayer.stopPlayback();

        if (mPlaybackReceiver != null) {
            mPlaybackReceiver.onPlaybackEnd();
        }
        stopSelf();
    }

    @Override
    public void updatePlaybackReady(boolean ready) {
        mPlaylistPlayer.updatePlaybackReady(ready);
    }

    @Override
    public void updateTrackPlaying(boolean playing) {
        mPlaylistPlayer.updateTrackPlaying(playing);
    }

    @Override
    public void updateTrackDone() {
        mPlaylistPlayer.updateTrackDone();
    }

    @Override
    public void updateTrackProgress(int progress, int duration) {
        mPlaylistPlayer.updateTrackProgress(progress, duration);
    }

    public void setPlaybackReceiver(PlaybackReceiver receiver) {
        mPlaybackReceiver = receiver;
    }


    /* Spotify ConnectionStateCallback */
    @Override
    public void onLoggedIn() {
        Log.i(QueueUp.LOG_TAG, "Spotify Logged in");
        updatePlaybackReady(true);
        updateTrackPlaying(true);
    }

    @Override
    public void onLoggedOut() {
        Log.w(QueueUp.LOG_TAG, "Spotify logged out. Stopping player service");
        stopSelf();
    }

    @Override
    public void onLoginFailed(Error error) {
        Log.e(QueueUp.LOG_TAG, "Login error: " + error.toString());
    }

    @Override
    public void onTemporaryError() {

    }

    @Override
    public void onConnectionMessage(String s) {

    }

    public class LocalBinder extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
        }
    }
}