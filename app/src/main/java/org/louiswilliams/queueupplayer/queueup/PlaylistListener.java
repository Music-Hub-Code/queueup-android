package org.louiswilliams.queueupplayer.queueup;

import org.louiswilliams.queueupplayer.queueup.objects.QueueUpTrack;
import org.louiswilliams.queueupplayer.queueup.objects.SpotifyTrack;

import java.util.List;


public interface PlaylistListener {
    void onPlayingChanged(boolean playing);
    void onTrackChanged(SpotifyTrack track);
    void onQueueChanged(List<QueueUpTrack> tracks);
    void onTrackProgress(int progressMs, int durationMs);
    void onPlayerReady(boolean status);
    String getPlaylistId();
}
