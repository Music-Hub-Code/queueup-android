package org.louiswilliams.queueupplayer;

import java.util.List;

import queueup.objects.QueueupStateChange;
import queueup.objects.QueueupTrack;
import queueup.objects.SpotifyTrack;

/**
 * Created by Louis on 6/6/2015.
 */
public interface PlaylistListener {
    void onPlayingChanged(boolean playing);
    void onTrackChanged(SpotifyTrack track);
    void onQueueChanged(List<QueueupTrack> tracks);
    void onTrackProgress(int progressMs, int durationMs);
    String getPlaylistId();
}
