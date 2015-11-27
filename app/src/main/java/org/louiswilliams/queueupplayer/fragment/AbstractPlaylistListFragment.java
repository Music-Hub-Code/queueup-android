package org.louiswilliams.queueupplayer.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.app.SearchManager;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.gc.materialdesign.views.ProgressBarDeterminate;
import com.squareup.picasso.Picasso;

import org.louiswilliams.queueupplayer.R;
import org.louiswilliams.queueupplayer.activity.MainActivity;
import org.louiswilliams.queueupplayer.queueup.PlaybackController;
import org.louiswilliams.queueupplayer.queueup.PlaylistListener;
import org.louiswilliams.queueupplayer.queueup.PlaylistPlayer;
import org.louiswilliams.queueupplayer.queueup.QueueUp;
import org.louiswilliams.queueupplayer.queueup.QueueUpLocationListener;
import org.louiswilliams.queueupplayer.queueup.objects.QueueUpPlaylist;
import org.louiswilliams.queueupplayer.queueup.objects.QueueUpStateChange;
import org.louiswilliams.queueupplayer.queueup.objects.QueueUpTrack;
import org.louiswilliams.queueupplayer.queueup.objects.SpotifyTrack;

import java.util.List;


public abstract class AbstractPlaylistListFragment extends Fragment implements PlaylistListener, SwipeRefreshLayout.OnRefreshListener {

    protected abstract void populate();

    protected GridView playlistGrid;
    protected List<QueueUpPlaylist> mPlaylists;
    protected PlaylistGridAdapter adapter;
    protected MainActivity mActivity;
    protected SwipeRefreshLayout mView;

    @Override
    public void onAttach(Activity activity) {
        mActivity = (MainActivity) activity;

        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        mView = (SwipeRefreshLayout) inflater.inflate(R.layout.fragment_playlist_list, container, false);
        mView.setOnRefreshListener(this);

        populate();

        playlistGrid = (GridView) mView.findViewById(R.id.playlist_grid);
        playlistGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                QueueUpPlaylist playlist = mPlaylists.get(position);
                Log.d(QueueUp.LOG_TAG, "Using playlist ID: " + playlist.id);

                mActivity.showPlaylistFragment(playlist.id);
            }
        });

        /* Enable the swipe to refresh only if the first item is showing and at the top  */
        playlistGrid.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {}

            @Override
            public void onScroll(AbsListView absListView, int firstVisible, int visibleItems, int totalItems) {
                int topPosition = (playlistGrid.getChildCount() == 0) ? 0 : playlistGrid.getChildAt(0).getTop();
                mView.setEnabled(firstVisible == 0 && topPosition >= 0);
            }
        });

        ImageButton addPlaylistButton = (ImageButton) mView.findViewById(R.id.add_playlist_button);
        addPlaylistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCreatePlaylistDialog();
            }
        });

        /* Show or hide the player bar if something is playing */
        View playerBar = mView.findViewById(R.id.player_bar);
        if (mActivity.getPlaybackController() != null) {
            setupPlayerBar(playerBar);
        } else {
            playerBar.setVisibility(View.GONE);
        }

        return mView;
    }

    @Override
    public void onCreateOptionsMenu (Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_playlist_list, menu);

        SearchManager searchManager = (SearchManager) mActivity.getSystemService(Context.SEARCH_SERVICE);
        MenuItem menuItem = menu.findItem(R.id.search_playlists);
        SearchView searchView = (SearchView) menuItem.getActionView();

        searchView.setSearchableInfo(searchManager.getSearchableInfo(mActivity.getComponentName()));

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                mActivity.getFragmentManager().popBackStack();
                return false;
            }
        });


        super.onCreateOptionsMenu(menu, menuInflater);
    }


    @Override
    public void onDestroyView() {
        if (mActivity.getPlaybackController() != null) {
            mActivity.getPlaybackController().removePlaylistListener(AbstractPlaylistListFragment.this);
        }

        super.onDestroyView();
    }

    public void showCreatePlaylistDialog() {
        if (mActivity.isClientRegistered()) {
            if (!mActivity.isLocationEnabled()) {
                mActivity.alertLocationEnable();
                return;
            }
            final QueueUpLocationListener locationListener = mActivity.getLocationListener();
            locationListener.startUpdates();

            PlaylistNameFragment playlistNameFragment = new PlaylistNameFragment();

            playlistNameFragment.setDialogTitle("New Playlist");
            playlistNameFragment.setPlaylistNameListener(new PlaylistNameFragment.PlaylistNameListener() {
                @Override
                public void onPlaylistCreate(PlaylistNameFragment dialogFragment) {
                    Location location = locationListener.getCurrentBestLocation();
                    locationListener.stopUpdates();
                    mActivity.doCreatePlaylist(dialogFragment.getPlaylistName(), location);
                }

                @Override
                public void onCancel() {
                    locationListener.stopUpdates();
                }
            });

            playlistNameFragment.show(getFragmentManager(), "create_playlist");
        } else {
            mActivity.toast("You need to log in first");
            mActivity.doLogin();
        }

    }

    public void setupPlayerBar(View bar) {

        PlaybackController playbackController = mActivity.getPlaybackController();
        final QueueUpStateChange currentState = playbackController.getCurrentState();

        /* Set up buttons and listeners */
        ImageButton playButton = (ImageButton) bar.findViewById(R.id.play_button);
        ImageButton skipButton = (ImageButton) bar.findViewById(R.id.skip_button);
        ImageButton stopButton = (ImageButton) bar.findViewById(R.id.stop_playback_button);

        bar.setClickable(true);

        View.OnClickListener playButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Just invert the current playing status */
                updateTrackPlaying(!mActivity.getPlaybackController().getCurrentState().playing);

            }
        };

        View.OnClickListener skipButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Send the update signal */
                mActivity.getPlaybackController().updateTrackDone();
            }
        };

        View.OnClickListener stopButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Send the update signal */
                mActivity.stopPlayback();
            }
        };

        playButton.setOnClickListener(playButtonListener);
        skipButton.setOnClickListener(skipButtonListener);
        stopButton.setOnClickListener(stopButtonListener);

        /* Populate visual content */

        if (currentState != null) {
            updatePlayButton(currentState.playing);

            updateTrackViews(currentState.current);

            onTrackProgress(playbackController.getCurrentProgress(), playbackController.getCurrentDuration());
        }

        /* Tell the activity we are now the active listener */
        mActivity.getPlaybackController().addPlaylistListener(AbstractPlaylistListFragment.this);

        bar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.showPlaylistFragment(getPlaylistId());
            }
        });
    }

    public void updateTrackViews(final SpotifyTrack track) {
        final ImageView albumArt = (ImageView) mView.findViewById(R.id.playlist_image);
        final TextView trackName = (TextView) mView.findViewById(R.id.playlist_current_track);
        final TextView trackArtist = (TextView) mView.findViewById(R.id.playlist_current_artist);

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                List<String> imageUrls = track.album.imageUrls;
                Picasso.with(mActivity).load(imageUrls.get(imageUrls.size() - 1)).into(albumArt);

                trackName.setText(track.name);
                trackArtist.setText(track.artists.get(0).name);
                trackName.setSelected(true);
                trackArtist.setSelected(true);
            }
        });


    }

    private void updatePlayButton(final boolean playing) {
        final ImageButton button =  (ImageButton)  mView.findViewById(R.id.play_button);
        if (button != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (playing) {
                        button.setImageResource(R.drawable.ic_action_pause_36);
                    } else {
                        button.setImageResource(R.drawable.ic_action_play_arrow_36);
                    }
                }
            });
        } else {
            Log.d(QueueUp.LOG_TAG, "NULL!");
        }
    }

    private void updateTrackPlaying(boolean playing) {
        mActivity.getPlaybackController().updateTrackPlaying(playing);

//        updatePlayButton(playing);
    }

    @Override
    public void onPlayingChanged(boolean playing) {
        updatePlayButton(playing);
    }

    @Override
    public void onTrackChanged(SpotifyTrack track) {
        updateTrackViews(track);
    }

    @Override
    public void onTrackProgress(int progress, int duration) {
        final ProgressBarDeterminate progressBar = (ProgressBarDeterminate) mView.findViewById(R.id.track_progress);
        final TextView progressLabel = (TextView) mView.findViewById(R.id.track_progress_text);
        String progressText = String.format("%d:%02d", progress / (60 * 1000), (progress / 1000) % 60);
        String durationText = String.format("%d:%02d", duration / (60 * 1000), (duration / 1000) % 60);

        progressLabel.setText(progressText+ "/" + durationText);
        progressBar.setMax(duration);
        progressBar.setProgress(progress);

    }

    @Override
    public void onQueueChanged(List<QueueUpTrack> queue) {
        Log.d(QueueUp.LOG_TAG, "Queueup changed, not necessary to update views right now");

    }

    @Override
    public void onPlayerReady() {
        /* TODO: Implement */
    }

    @Override
    public String getPlaylistId() {
        return mActivity.getPlaybackController().getPlaylistId();
    }

    @Override
    public void onRefresh() {
        populate();
        mView.setRefreshing(false);
    }

    class PlaylistGridAdapter extends BaseAdapter {

        private Context mContext;
        private List<QueueUpPlaylist> mPlaylists;
        private int mResource;

        public PlaylistGridAdapter(Context c, List<QueueUpPlaylist> playlists, int resource) {
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
            QueueUpPlaylist playlist = mPlaylists.get(position);

            if (convertView == null) {
                playlistItem = LayoutInflater.from(mContext).inflate(mResource, parent, false);
            } else {
                playlistItem = convertView;
            }

            TextView title = (TextView) playlistItem.findViewById(R.id.playlist_list_item_title);
            TextView adminName = (TextView) playlistItem.findViewById(R.id.playlist_list_item_admin);
            ImageView adminIcon = (ImageView) playlistItem.findViewById(R.id.playlist_list_item_admin_icon);

            title.setText(playlist.name.toUpperCase());
            adminIcon.setImageDrawable(getResources().getDrawable(R.mipmap.ic_queueup));
            if (!playlist.adminName.isEmpty()) {
                adminName.setText(playlist.adminName);
            } else {
                adminName.setText("QueueUp User");
            }

            if (playlist.current != null && playlist.current.album != null && playlist.current.album.imageUrls != null) {
                List<String> imageUrls = playlist.current.album.imageUrls;
                if (imageUrls.size() > 0) {
                    ImageView image = (ImageView) playlistItem.findViewById(R.id.playlist_list_item_image);
                    Picasso.with(mContext).load(imageUrls.get(0)).into(image);
                }
            } else {
                ImageView image = (ImageView) playlistItem.findViewById(R.id.playlist_list_item_image);
                image.setImageDrawable(mContext.getResources().getDrawable(R.drawable.background_opaque_gray));
            }

            return playlistItem;
        }

        public void updateList(List<QueueUpPlaylist> list) {
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
