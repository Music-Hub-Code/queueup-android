package org.louiswilliams.queueupplayer.service;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.Spotify;

import org.louiswilliams.queueupplayer.R;
import org.louiswilliams.queueupplayer.activity.MainActivity;
import org.louiswilliams.queueupplayer.queueup.PlaybackController;
import org.louiswilliams.queueupplayer.queueup.PlaybackReceiver;
import org.louiswilliams.queueupplayer.queueup.PlaylistClient;
import org.louiswilliams.queueupplayer.queueup.PlaylistListener;
import org.louiswilliams.queueupplayer.queueup.PlaylistPlayer;
import org.louiswilliams.queueupplayer.queueup.QueueUp;
import org.louiswilliams.queueupplayer.queueup.QueueUpStore;
import org.louiswilliams.queueupplayer.queueup.SpotifyPlayer;
import org.louiswilliams.queueupplayer.queueup.api.QueueUpClient;
import org.louiswilliams.queueupplayer.queueup.api.SpotifyTokenManager;
import org.louiswilliams.queueupplayer.queueup.objects.QueueUpStateChange;
import org.louiswilliams.queueupplayer.widget.PlayerNotification;

public class PlayerService extends Service implements PlaybackController {

    public static final String EXTRA_PLAYLIST_ID = "PLAYLIST_ID";

    private IBinder mBinder;
    private PlayerNotification mNotification;
    private PlaylistPlayer mPlaylistPlayer;
    private PlaybackReceiver mPlaybackReceiver;
    private QueueUpClient queueUpClient;
    private QueueUpStore mStore;
    private SpotifyPlayer mSpotifyPlayer;
    private String mSpotifyClientId;
    private String mClientToken;
    private String mUserId;

    @Override
    public void onCreate() {
        mBinder = new LocalBinder();
        mNotification = new PlayerNotification(this);

        mSpotifyClientId = getString(R.string.spotify_client_id);

        mStore = QueueUpStore.with(this);
        mClientToken = mStore.getString(QueueUpStore.CLIENT_TOKEN);
        mUserId = mStore.getString(QueueUpStore.USER_ID);

        queueUpClient = new QueueUpClient(mClientToken, mUserId);

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
                    Log.e(QueueUp.LOG_TAG, "Could not initialize: " + throwable.getMessage());
                }
            });

            /* Construct our player wrapper */
            mSpotifyPlayer = new SpotifyPlayer(PlayerService.this, player);
        }
    }

    public void beginPlayback() {
        Log.d(QueueUp.LOG_TAG, "Init Player");

        /* Attach the player listener */
        mSpotifyPlayer.startReceivingPlaybackNotifications();
        addPlaylistListener(mSpotifyPlayer);
        updatePlaybackReady();
        updateTrackPlaying(true);

    }

    public void unsubscribeFromCurrentPlaylist() {
        if (mPlaylistPlayer != null) {
            Log.d(QueueUp.LOG_TAG, "Unsubscribing from previous player");
            removeAllPlaylistListeners();
            if (mSpotifyPlayer != null) {
                mSpotifyPlayer.stopReceivingPlaybackNotifications();
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
        mPlaylistPlayer.disconnect();
        mNotification.cancel();
        if (mSpotifyPlayer != null) {
            mSpotifyPlayer.stopReceivingPlaybackNotifications();
            Spotify.destroyPlayer(this);
        }
        super.onDestroy();
    }


    public void addPlaylistListener(PlaylistListener listener) {
        mPlaylistPlayer.addPlaylistListener(listener);
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
    public void updatePlaybackReady() {
        mPlaylistPlayer.updatePlaybackReady();
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

    public class LocalBinder extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
        }
    }
}