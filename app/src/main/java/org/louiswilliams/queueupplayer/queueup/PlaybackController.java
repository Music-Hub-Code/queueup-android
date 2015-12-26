package org.louiswilliams.queueupplayer.queueup;

import org.louiswilliams.queueupplayer.queueup.objects.QueueUpStateChange;

public interface PlaybackController {

    void updatePlaybackReady(boolean ready);

    void updateTrackPlaying(boolean playing);

    void updateTrackDone();

    void updateTrackProgress(int progress, int duration);

    void addPlaylistListener(PlaylistListener listener);

    void removePlaylistListener(PlaylistListener listener);

    void removeAllPlaylistListeners();

    String getPlaylistId();

    int getCurrentProgress();

    int getCurrentDuration();

    QueueUpStateChange getCurrentState();

    void stopPlayback();
}
