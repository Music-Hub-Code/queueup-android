package org.louiswilliams.queueupplayer.widget;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.RemoteViews;

import com.squareup.picasso.Picasso;

import org.louiswilliams.queueupplayer.R;
import org.louiswilliams.queueupplayer.activity.MainActivity;

import java.util.List;

import org.louiswilliams.queueupplayer.queueup.PlaylistListener;
import org.louiswilliams.queueupplayer.queueup.PlaylistPlayer;
import org.louiswilliams.queueupplayer.queueup.Queueup;
import org.louiswilliams.queueupplayer.queueup.objects.QueueupTrack;
import org.louiswilliams.queueupplayer.queueup.objects.SpotifyTrack;

public class PlayerNotification extends Notification implements PlaylistListener {

    private static final int NOTIFICATION_ID = 1;
    private static final String PLAY_BUTTON_INTENT = "QUEUEUP_PLAY_BUTTON";
    private static final String SKIP_BUTTON_INTENT = "QUEUEUP_SKIP_BUTTON";
    private static final String STOP_BUTTON_INTENT = "QUEUEUP_STOP_BUTTON";

    private Activity mActivity;
    private NotificationManager mNotificationManager;
    private Builder mBuilder;
    private RemoteViews mContentView;

    private BroadcastReceiver playReceiver;
    private BroadcastReceiver skipReceiver;
    private BroadcastReceiver stopReceiver;

    public PlayerNotification(Activity context) {
        super();

        Log.d(Queueup.LOG_TAG, "Creating notification for the first time...");

        mActivity = context;
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        playReceiver = new PlayButtonHandler();
        skipReceiver = new SkipButtonHandler();
        stopReceiver = new StopButtonHandler();

        mActivity.registerReceiver(playReceiver, new IntentFilter(PLAY_BUTTON_INTENT));
        mActivity.registerReceiver(skipReceiver, new IntentFilter(SKIP_BUTTON_INTENT));
        mActivity.registerReceiver(stopReceiver, new IntentFilter(STOP_BUTTON_INTENT));


        mBuilder = new Notification.Builder(mActivity);
        mBuilder.setOngoing(true);

        mContentView = new RemoteViews(mActivity.getPackageName(), R.layout.notification_player);
        mBuilder.setContent(mContentView);

        Intent playButtonIntent = new Intent(PLAY_BUTTON_INTENT);
        Intent skipButtonIntent = new Intent(SKIP_BUTTON_INTENT);
        Intent stopButtonIntent = new Intent(STOP_BUTTON_INTENT);


        mContentView.setOnClickPendingIntent(R.id.play_button, PendingIntent.getBroadcast(mActivity, 0, playButtonIntent, 0));
        mContentView.setOnClickPendingIntent(R.id.skip_button, PendingIntent.getBroadcast(mActivity, 0, skipButtonIntent, 0));
        mContentView.setOnClickPendingIntent(R.id.stop_playback_button, PendingIntent.getBroadcast(mActivity, 0, stopButtonIntent, 0));

        PendingIntent openAppIntent = PendingIntent.getActivity(mActivity, 0, new Intent(mActivity, MainActivity.class), 0);
        mBuilder.setContentIntent(openAppIntent);


    }

    public void cancel() {
        try {
            mActivity.unregisterReceiver(playReceiver);
            mActivity.unregisterReceiver(skipReceiver);
            mActivity.unregisterReceiver(stopReceiver);
        } catch (IllegalArgumentException e) {
            Log.w(Queueup.LOG_TAG, e.getMessage());
        }

        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    @Override
    public void onPlayingChanged(boolean playing) {

        if (playing) {
            mContentView.setImageViewResource(R.id.play_button, R.drawable.ic_action_pause_36);
            mBuilder.setSmallIcon(R.drawable.ic_action_play_circle_fill);
        } else {
            mContentView.setImageViewResource(R.id.play_button, R.drawable.ic_action_play_arrow_36);
            mBuilder.setSmallIcon(R.drawable.ic_action_pause_circle_fill);
        }

        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    @Override
    public void onTrackChanged(final SpotifyTrack current) {
        final Notification notification = mBuilder.build();

        if (current != null) {
            mContentView.setTextViewText(R.id.playlist_current_track, current.name);
            mContentView.setTextViewText(R.id.playlist_current_artist, current.artists.get(0).name);
            if (current.album.imageUrls != null && current.album.imageUrls.size() > 0) {

                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Picasso.with(mActivity).load(current.album.imageUrls.get(0)).into(mContentView, R.id.playlist_image, NOTIFICATION_ID, notification);
                    }
                });
            }
        }


        mNotificationManager.notify(NOTIFICATION_ID, notification);

    }

    @Override
    public void onTrackProgress(int progress, int duration) {
        String progressText = String.format("%d:%02d", progress / (60 * 1000), (progress / 1000) % 60);
        String durationText = String.format("%d:%02d", duration / (60 * 1000), (duration / 1000) % 60);

        mContentView.setTextViewText(R.id.track_progress_text, progressText + "/" + durationText);

        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    @Override
    public void onQueueChanged(List<QueueupTrack> tracks) {   }

    @Override
    public void onPlayerReady() {}

    @Override
    public String getPlaylistId() {
        return null;
    }

    public class PlayButtonHandler extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(Queueup.LOG_TAG, "Play button pressed");
            if (context instanceof MainActivity) {
                PlaylistPlayer player = ((MainActivity) context).getPlaylistPlayer();

                boolean playing = player.getCurrentState().playing;
                player.updateTrackPlaying(!playing);
            } else {
                Log.e(Queueup.LOG_TAG, "Received context isn't an instance of Main activity...");
            }
        }
    }

    public class SkipButtonHandler extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(Queueup.LOG_TAG, "Skip button pressed");
            if (context instanceof MainActivity) {

                PlaylistPlayer player = ((MainActivity) context).getPlaylistPlayer();

                player.updateTrackDone();
            } else {
                Log.e(Queueup.LOG_TAG, "Received context isn't an instance of Main activity...");
            }
        }
    }

    public class StopButtonHandler extends  BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            cancel();
            Log.d(Queueup.LOG_TAG, "Stop pressed");
            if (context instanceof  MainActivity) {
                ((MainActivity) context).stopPlayback();
            }
        }
    }
}
