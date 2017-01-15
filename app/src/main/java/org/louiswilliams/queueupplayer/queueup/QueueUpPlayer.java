package org.louiswilliams.queueupplayer.queueup;

import android.util.Log;

import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Metadata;
import com.spotify.sdk.android.player.PlaybackState;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.SpotifyPlayer;


import org.louiswilliams.queueupplayer.queueup.objects.QueueUpStateChange;
import org.louiswilliams.queueupplayer.queueup.objects.QueueUpTrack;
import org.louiswilliams.queueupplayer.queueup.objects.SpotifyTrack;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class QueueUpPlayer implements PlaylistListener, Player.NotificationCallback {

    private static final int PROGRESS_INTERVAL = 1000;

    private SpotifyPlayer mPlayer;
    private PlaybackController mPlaybackController;
    private Timer mProgressTimer;

    public QueueUpPlayer(PlaybackController playlistPlayer, SpotifyPlayer player) {
        mPlayer = player;
        mPlaybackController = playlistPlayer;
    }

    @Override
    public void onPlayingChanged(final boolean playing) {
        PlaybackState playbackState = mPlayer.getPlaybackState();
        if (!playbackState.isPlaying && playing) {
            mPlayer.resume(null);
        } else if (playbackState.isPlaying && !playing){
            mPlayer.pause(null);
        }
    }

    @Override
    public void onTrackChanged(final SpotifyTrack track) {
        Metadata metadata = mPlayer.getMetadata();

        if (metadata != null &&
                metadata.currentTrack != null &&
                !metadata.currentTrack.uri.equals(track.uri))
        {
            mPlayer.playUri(null, track.uri, 0, 0);
        }
    }

    @Override
    public void onQueueChanged(List<QueueUpTrack> tracks) {

    }

    @Override
    public void onTrackProgress(int progressMs, int durationMs) {

    }

    @Override
    public void onPlayerReady(boolean ready) {
        final QueueUpStateChange state = mPlaybackController.getCurrentState();

        if (ready && state != null && state.current != null) {

            Metadata metadata = mPlayer.getMetadata();

            if (metadata.currentTrack == null || !state.current.uri.equals(metadata.currentTrack.uri)) {
                Log.d(QueueUp.LOG_TAG, "Different tracks, so playing");
                mPlayer.playUri(null, state.current.uri, 0, 0);
            }

            if (!state.playing) {
                mPlayer.pause(null);
            }
        }
    }

    @Override
    public String getPlaylistId() {
        return null;
    }

    /* PlayerNotificationCallback */
    public void startProgressUpdater() {
        stopProgressUpdater();
        mProgressTimer = new Timer();

        mProgressTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                PlaybackState playbackState = mPlayer.getPlaybackState();
                Metadata metadata = mPlayer.getMetadata();
                if (metadata != null && metadata.currentTrack != null) {
                    long duration = metadata.currentTrack.durationMs;
                    long progress = playbackState.positionMs;

                    mPlaybackController.updateTrackProgress((int)progress, (int)duration);
                }
            }
        }, 0, PROGRESS_INTERVAL);
    }

    public void stopProgressUpdater() {
        if (mProgressTimer != null) {
            mProgressTimer.cancel();
        }
    }

    public void startReceivingPlaybackNotifications() {
        startProgressUpdater();
        mPlayer.addNotificationCallback(this);
    }

    public void stopReceivingPlaybackNotifications() {
        stopProgressUpdater();
        mPlayer.removeNotificationCallback(this);
    }

    public SpotifyPlayer getPlayer() {
        return mPlayer;
    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        Log.d(QueueUp.LOG_TAG, "EVENT: " + playerEvent.name());
        if (playerEvent == PlayerEvent.kSpPlaybackNotifyAudioDeliveryDone) {
            mPlaybackController.updateTrackDone();
        } else if (playerEvent == PlayerEvent.kSpPlaybackNotifyPause) {
            stopProgressUpdater();
        } else if (playerEvent == PlayerEvent.kSpPlaybackNotifyPlay) {
            startProgressUpdater();
        }
    }

    @Override
    public void onPlaybackError(Error error) {
        Log.e(QueueUp.LOG_TAG, error.toString());
    }
}
