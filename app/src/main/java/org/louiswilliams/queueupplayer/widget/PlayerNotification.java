package org.louiswilliams.queueupplayer.widget;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.gc.materialdesign.views.ProgressBarDeterminate;
import com.squareup.picasso.Picasso;

import org.louiswilliams.queueupplayer.R;
import org.louiswilliams.queueupplayer.activity.MainActivity;

import queueup.PlaylistPlayer;
import queueup.Queueup;
import queueup.objects.QueueupPlaylist;
import queueup.objects.QueueupStateChange;
import queueup.objects.SpotifyTrack;

public class PlayerNotification extends Notification {

    private static final int NOTIF_ID = 1;
    private static final String PLAY_BUTTON_INTENT = "QUEUEUP_PLAY_BUTTON";
    private static final String SKIP_BUTTON_INTENT = "QUEUEUP_SKIP_BUTTON";
    private static final String STOP_BUTTON_INTENT = "QUEUEUP_STOP_BUTTON";

    private Context mContext;
    private NotificationManager mNotificationManager;
    private Builder mBuilder;
    private RemoteViews mContentView;

    public PlayerNotification(Context context) {
        super();
        mContext = context;
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        mContext.registerReceiver(new PlayButtonHandler(), new IntentFilter(PLAY_BUTTON_INTENT));
        mContext.registerReceiver(new SkipButtonHandler(), new IntentFilter(SKIP_BUTTON_INTENT));
        mContext.registerReceiver(new StopButtonHandler(), new IntentFilter(STOP_BUTTON_INTENT));

        mBuilder = new Notification.Builder(mContext);
        mBuilder.setOngoing(true);

    }

    public void show(boolean playing, SpotifyTrack current) {

        mContentView = new RemoteViews(mContext.getPackageName(), R.layout.notification_player);
        mBuilder.setContent(mContentView);

        if (playing) {
            mContentView.setImageViewResource(R.id.play_button, R.drawable.ic_action_pause_36);
            mBuilder.setSmallIcon(R.drawable.ic_action_play_circle_fill);
        } else {
            mContentView.setImageViewResource(R.id.play_button, R.drawable.ic_action_play_arrow_36);
            mBuilder.setSmallIcon(R.drawable.ic_action_pause_circle_fill);
        }

        mBuilder.setProgress(0,0, true);

        Intent playButtonIntent = new Intent(PLAY_BUTTON_INTENT);
        Intent skipButtonIntent = new Intent(SKIP_BUTTON_INTENT);
        Intent stopButtonIntent = new Intent(STOP_BUTTON_INTENT);


        mContentView.setOnClickPendingIntent(R.id.play_button, PendingIntent.getBroadcast(mContext, 0, playButtonIntent, 0));
        mContentView.setOnClickPendingIntent(R.id.skip_button, PendingIntent.getBroadcast(mContext, 0, skipButtonIntent, 0));
        mContentView.setOnClickPendingIntent(R.id.stop_playback_button, PendingIntent.getBroadcast(mContext, 0, stopButtonIntent, 0));

        PendingIntent openAppIntent = PendingIntent.getActivity(mContext, 0, new Intent(mContext, MainActivity.class), 0);
        mBuilder.setContentIntent(openAppIntent);

        Notification notification = mBuilder.build();

        if (current != null) {
            mContentView.setTextViewText(R.id.playlist_current_track, current.name);
            mContentView.setTextViewText(R.id.playlist_current_artist, current.artists.get(0).name);
            if (current.album.imageUrls != null && current.album.imageUrls.size() > 0) {
                Picasso.with(mContext).load(current.album.imageUrls.get(0)).into(mContentView, R.id.playlist_image, NOTIF_ID, notification);
            }
        }


        mNotificationManager.notify(NOTIF_ID, notification);
    }

    public void updateProgress(int progress, int duration) {

        String progressText = String.format("%d:%02d", progress / (60 * 1000), (progress / 1000) % 60);
        String durationText = String.format("%d:%02d", duration / (60 * 1000), (duration / 1000) % 60);

        mContentView.setTextViewText(R.id.track_progress_text, progressText + "/" + durationText);
//        mContentView.setProgressBar(R.id.track_progress, duration, progress, false);

        mNotificationManager.notify(NOTIF_ID, mBuilder.build());
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
            Log.d(Queueup.LOG_TAG, "Stop pressed");
        }
    }
}
