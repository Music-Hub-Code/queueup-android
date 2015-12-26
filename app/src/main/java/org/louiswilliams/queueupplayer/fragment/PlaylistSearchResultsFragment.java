package org.louiswilliams.queueupplayer.fragment;

import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import org.louiswilliams.queueupplayer.R;
import org.louiswilliams.queueupplayer.queueup.QueueUp;
import org.louiswilliams.queueupplayer.queueup.objects.QueueUpPlaylist;

import java.util.List;

public class PlaylistSearchResultsFragment extends AbstractPlaylistListFragment {

    private String mQuery;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mQuery = getArguments().getString("query");

        return super.onCreateView(inflater, container, savedInstanceState);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);

        /* Recreate the text field */
        SearchView searchView = (SearchView) menu.findItem(R.id.search_playlists).getActionView();
        if (mQuery != null) {
            searchView.setQuery(mQuery, false);
            searchView.setIconified(false);
            searchView.clearFocus();
        }
    }

    @Override
    protected void populate(final boolean refresh) {

        /* Do a search query and pass the results back to the fragment  */
        mActivity.getQueueUpClient().searchPlaylists(mQuery, new QueueUp.CallReceiver<List<QueueUpPlaylist>>() {
            @Override
            public void onResult(List<QueueUpPlaylist> playlists) {
                Log.d(QueueUp.LOG_TAG, "Playlist search success");

                populateDone(playlists, null, refresh);
            }

            @Override
            public void onException(Exception e) {
                Log.e(QueueUp.LOG_TAG, e.getMessage());
                mActivity.toast(e.getMessage());
                populateDone(null, null, refresh);
            }
        });

    }
}
