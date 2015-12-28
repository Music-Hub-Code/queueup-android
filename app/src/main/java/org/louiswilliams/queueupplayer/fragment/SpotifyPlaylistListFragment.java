package org.louiswilliams.queueupplayer.fragment;


import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.louiswilliams.queueupplayer.R;
import org.louiswilliams.queueupplayer.activity.MainActivity;
import org.louiswilliams.queueupplayer.queueup.QueueUp;
import org.louiswilliams.queueupplayer.queueup.api.SpotifyClient;
import org.louiswilliams.queueupplayer.queueup.objects.SpotifyPlaylist;

import java.util.List;

public class SpotifyPlaylistListFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private MainActivity mActivity;
    private SwipeRefreshLayout mView;
    private GridView playlistGrid;
    private List<SpotifyPlaylist> mPlaylists;
    private PlaylistGridAdapter mAdapter;
    private SpotifyClient spotifyClient;
    private String playlistId;


    @Override
    public void onAttach(Context activity) {
        mActivity = (MainActivity) activity;
        super.onAttach(activity);
    }

    @Override
    public void onAttach(Activity activity) {
        mActivity = (MainActivity) activity;
        super.onAttach(activity);
    }

    private void populate(final boolean refresh) {
        /* Get the user's following playlists and update the adapter on success */
        if (spotifyClient == null) {
            return;
        }
        spotifyClient.getMyPlaylists(new QueueUp.CallReceiver<List<SpotifyPlaylist>>() {
            @Override
            public void onResult(List<SpotifyPlaylist> result) {

                mPlaylists = result;

                mAdapter = new PlaylistGridAdapter(mActivity, mPlaylists, R.layout.playlist_item);

                final ProgressBar progress = (ProgressBar) mView.findViewById(R.id.loading_progress_bar);
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.setVisibility(View.GONE);
                        playlistGrid.setAdapter(mAdapter);
                        if (refresh) {
                            mView.setRefreshing(false);
                        }
                        mActivity.setTitle("My Playlists");
                    }
                });
            }

            @Override
            public void onException(Exception e) {
                Log.e(QueueUp.LOG_TAG, e.getMessage());
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = (SwipeRefreshLayout) inflater.inflate(R.layout.fragment_playlist_list, container, false);
        mView.setOnRefreshListener(this);

        playlistId = getArguments().getString("playlist_id");

        mActivity.spotifyLogin(new QueueUp.CallReceiver<String>() {

            @Override
            public void onResult(String accessToken) {
                spotifyClient = SpotifyClient.with(accessToken);
            }

            @Override
            public void onException(Exception e) {
                Log.e(QueueUp.LOG_TAG, e.getMessage());
            }
        });


        playlistGrid = (GridView) mView.findViewById(R.id.playlist_grid);
        playlistGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SpotifyPlaylist playlist = mPlaylists.get(position);
                Log.d(QueueUp.LOG_TAG, "Using playlist ID: " + playlist.id);

                mActivity.showSpotifyPlaylistFragment(playlistId, playlist.owner.id, playlist.id);
            }
        });

        playlistGrid.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisible, int visibleItems, int totalItems) {
                int topPosition = (playlistGrid.getChildCount() == 0) ? 0 : playlistGrid.getChildAt(0).getTop();
                mView.setEnabled(firstVisible == 0 && topPosition >= 0);
            }
        });

        View addButton = mView.findViewById(R.id.add_playlist_button);
        addButton.setVisibility(View.GONE);

        View playerBar = mView.findViewById(R.id.player_bar);
        playerBar.setVisibility(View.GONE);

        populate(false);

        return mView;
    }

    @Override
    public void onRefresh() {
        populate(true);
    }

    class PlaylistGridAdapter extends BaseAdapter {

        private Context mContext;
        private List<SpotifyPlaylist> mPlaylists;
        private int mResource;

        public PlaylistGridAdapter(Context c, List<SpotifyPlaylist> playlists, int resource) {
            mContext = c;
            mPlaylists = playlists;
            mResource = resource;
        }

        @Override
        public int getCount() {
            return mPlaylists.size();
        }

        @Override
        public Object getItem(int position) {
            return mPlaylists.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View playlistItem;
            SpotifyPlaylist playlist = mPlaylists.get(position);

            if (convertView == null) {
                playlistItem = LayoutInflater.from(mContext).inflate(mResource, parent, false);
            } else {
                playlistItem = convertView;
            }

            TextView title = (TextView) playlistItem.findViewById(R.id.playlist_list_item_title);
            TextView adminName = (TextView) playlistItem.findViewById(R.id.playlist_list_item_admin);
            ImageView adminIcon = (ImageView) playlistItem.findViewById(R.id.playlist_list_item_admin_icon);

            title.setText(playlist.name);

            adminName.setText(playlist.totalTracks + " tracks");
            adminIcon.setImageResource(R.mipmap.ic_spotify);
            adminIcon.setBackgroundResource(R.drawable.background_transparent);

            if (playlist.imageUrls != null) {
                List<String> imageUrls = playlist.imageUrls;
                if (imageUrls.size() > 0) {
                    ImageView image = (ImageView) playlistItem.findViewById(R.id.playlist_list_item_image);
                    Picasso.with(mContext).load(imageUrls.get(0)).into(image);
                }
            } else {
                ImageView image = (ImageView) playlistItem.findViewById(R.id.playlist_list_item_image);
                image.setImageResource(R.drawable.background_opaque_gray);
            }

            return playlistItem;
        }

        public void updateList(List<SpotifyPlaylist> list) {
            mPlaylists = list;

            /* Calls are going to be from different asynchronous threads, so to be safe, run on main thread */
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }
    }
}
