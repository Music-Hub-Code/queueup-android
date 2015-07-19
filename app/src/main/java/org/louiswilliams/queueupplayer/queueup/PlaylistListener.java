package org.louiswilliams.queueupplayer.queueup;

import java.util.List;

import org.louiswilliams.queueupplayer.queueup.objects.QueueupTrack;
import org.louiswilliams.queueupplayer.queueup.objects.SpotifyTrack;

/**
 * Created by Louis on 6/6/2015.
 */
public interface PlaylistListener {
    void onPlayingChanged(boolean playing);
    void onTrackChanged(SpotifyTrack track);
    void onQueueChanged(List<QueueupTrack> tracks);
    void onTrackProgress(int progressMs, int durationMs);
    void onPlayerReady();
    String getPlaylistId();
}
