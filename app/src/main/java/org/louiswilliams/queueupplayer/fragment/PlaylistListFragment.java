package org.louiswilliams.queueupplayer.fragment;

import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import org.louiswilliams.queueupplayer.R;
import org.louiswilliams.queueupplayer.queueup.PlaylistListener;
import org.louiswilliams.queueupplayer.queueup.QueueUp;
import org.louiswilliams.queueupplayer.queueup.objects.QueueUpPlaylist;

import java.util.List;

public class PlaylistListFragment extends AbstractPlaylistListFragment implements PlaylistListener, SwipeRefreshLayout.OnRefreshListener {

    @Override
    protected void populate() {
        Log.d(QueueUp.LOG_TAG, "populating list...");

        mActivity.getQueueupClient().playlistGetList(new QueueUp.CallReceiver<List<QueueUpPlaylist>>() {
            @Override
            public void onResult(List<QueueUpPlaylist> playlists) {
                Log.d(QueueUp.LOG_TAG, "Playlist all success");
                mPlaylists = playlists;

                adapter = new PlaylistGridAdapter(mActivity, mPlaylists, R.layout.playlist_item);
                final ProgressBar progress = (ProgressBar) mView.findViewById(R.id.loading_progress_bar);

                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.setVisibility(View.GONE);
                        playlistGrid.setAdapter(adapter);
                    }
                });

            }

            @Override
            public void onException(Exception e) {
                mActivity.toast(e.getMessage());
                Log.e(QueueUp.LOG_TAG, "Failed to getString playlist list: " + e.getMessage());
            }
        });
    }

}
