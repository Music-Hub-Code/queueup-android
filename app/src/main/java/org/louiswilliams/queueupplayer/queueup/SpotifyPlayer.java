package org.louiswilliams.queueupplayer.queueup;

import android.util.Log;

import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.PlayerStateCallback;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.louiswilliams.queueupplayer.queueup.objects.QueueUpStateChange;
import org.louiswilliams.queueupplayer.queueup.objects.QueueUpTrack;
import org.louiswilliams.queueupplayer.queueup.objects.SpotifyTrack;

public class SpotifyPlayer implements PlaylistListener, PlayerNotificationCallback {

    private static final int PROGRESS_INTERVAL = 1000;

    private Player mPlayer;
    private PlaylistPlayer mPlaylistPlayer;
    private Timer mProgressTimer;

    public SpotifyPlayer(PlaylistPlayer playlistPlayer, Player player) {
        mPlayer = player;
        mPlaylistPlayer = playlistPlayer;
    }

    @Override
    public void onPlayingChanged(final boolean playing) {
        mPlayer.getPlayerState(new PlayerStateCallback() {
            @Override
            public void onPlayerState(PlayerState playerState) {

                if (!playerState.playing && playing) {
                    mPlayer.resume();
                } else if (playerState.playing && !playing){
                    mPlayer.pause();
                }
            }
        });
    }

    @Override
    public void onTrackChanged(final SpotifyTrack track) {
        mPlayer.getPlayerState(new PlayerStateCallback() {
            @Override
            public void onPlayerState(PlayerState playerState) {
                if (!playerState.trackUri.equals(track.uri)) {
                    mPlayer.play(track.uri);
                }
            }
        });
    }

    @Override
    public void onQueueChanged(List<QueueUpTrack> tracks) {

    }

    @Override
    public void onTrackProgress(int progressMs, int durationMs) {

    }

    @Override
    public void onPlayerReady() {
        final QueueUpStateChange state = mPlaylistPlayer.getCurrentState();

        if (state != null && state.current != null) {

            mPlayer.getPlayerState(new PlayerStateCallback() {
                @Override
                public void onPlayerState(PlayerState playerState) {

                    if (!playerState.trackUri.equals(state.current.uri)) {
                        Log.d(QueueUp.LOG_TAG, "Different tracks, so playing");
                        mPlayer.play(state.current.uri);
                    }

                    if (!state.playing) {
                        mPlayer.pause();
                    }
                }
            });

        }
    }

    @Override
    public String getPlaylistId() {
        return null;
    }

        /* PlayerNotificationCallback */

    @Override
    public void onPlaybackEvent(PlayerNotificationCallback.EventType eventType, PlayerState playerState) {
        Log.d(QueueUp.LOG_TAG, "EVENT: " + eventType);
        if (eventType == PlayerNotificationCallback.EventType.END_OF_CONTEXT) {
            mPlaylistPlayer.updateTrackDone();
        } else if (eventType == PlayerNotificationCallback.EventType.PAUSE) {
            stopProgressUpdater();
        } else if (eventType == PlayerNotificationCallback.EventType.PLAY) {
            startProgressUpdater();
        }
    }

    @Override
    public void onPlaybackError(PlayerNotificationCallback.ErrorType errorType, String s) {
        Log.e(QueueUp.LOG_TAG, s);
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

                        mPlaylistPlayer.updateTrackProgress(progress, duration);

                    }
                });
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
        mPlayer.addPlayerNotificationCallback(this);
    }

    public void stopReceivingPlaybackNotifications() {
        stopProgressUpdater();
        mPlayer.removePlayerNotificationCallback(this);
    }

    public Player getPlayer() {
        return mPlayer;
    }
}
