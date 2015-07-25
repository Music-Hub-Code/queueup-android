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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gc.materialdesign.views.ProgressBarDeterminate;
import com.squareup.picasso.Picasso;

import org.louiswilliams.queueupplayer.R;
import org.louiswilliams.queueupplayer.activity.MainActivity;

import java.util.List;

import org.louiswilliams.queueupplayer.queueup.PlaylistListener;
import org.louiswilliams.queueupplayer.queueup.PlaylistPlayer;
import org.louiswilliams.queueupplayer.queueup.Queueup;
import org.louiswilliams.queueupplayer.queueup.objects.QueueupPlaylist;
import org.louiswilliams.queueupplayer.queueup.objects.QueueupStateChange;
import org.louiswilliams.queueupplayer.queueup.objects.QueueupTrack;
import org.louiswilliams.queueupplayer.queueup.objects.SpotifyTrack;

public class PlaylistListFragment extends Fragment implements PlaylistListener, SwipeRefreshLayout.OnRefreshListener {

    private GridView playlistGrid;
    private List<QueueupPlaylist> mPlaylists;
    private MainActivity mActivity;
    private SwipeRefreshLayout mView;

    @Override
    public void onAttach(Activity activity) {
        mActivity = (MainActivity) activity;

        super.onAttach(activity);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        populateList();

        mView = (SwipeRefreshLayout) inflater.inflate(R.layout.fragment_playlist_list, container, false);
        mView.setOnRefreshListener(this);

        playlistGrid = (GridView) mView.findViewById(R.id.playlist_grid);
        playlistGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                QueueupPlaylist playlist = mPlaylists.get(position);
                Log.d(Queueup.LOG_TAG, "Using playlist ID: " + playlist.id);

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
        if (mActivity.getPlaylistPlayer() != null) {
            setupPlayerBar(mView);
        } else {
            playerBar.setVisibility(View.GONE);
        }

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.setTitle("Hot Playlists");
            }
        });

        return mView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        if (mActivity.getPlaylistPlayer() != null) {
            mActivity.getPlaylistPlayer().removePlaylistListener(PlaylistListFragment.this);
        }

        super.onDestroyView();
    }

    private void populateList() {
        Log.d(Queueup.LOG_TAG, "populating list...");

        mActivity.getQueueupClient().playlistGetList(new Queueup.CallReceiver<List<QueueupPlaylist>>() {
            @Override
            public void onResult(List<QueueupPlaylist> playlists) {
                Log.d(Queueup.LOG_TAG, "Playlist all success");
                mPlaylists = playlists;

                final PlaylistGridAdapter adapter = new PlaylistGridAdapter(mActivity, mPlaylists, R.layout.playlist_item);
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
                Log.e(Queueup.LOG_TAG, "Failed to getString playlist list: " + e.getMessage());
            }
        });
    }

    public void showCreatePlaylistDialog() {
        if (mActivity.isLoggedIn()) {
            PlaylistNameFragment playlistNameFragment = new PlaylistNameFragment();

            playlistNameFragment.setDialogTitle("New Playlist");
            playlistNameFragment.setPlaylistNameListener(new PlaylistNameFragment.PlaylistNameListener() {
                @Override
                public void onPlaylistCreate(PlaylistNameFragment dialogFragment) {
                    mActivity.doCreatePlaylist(dialogFragment.getPlaylistName());
                }

                @Override
                public void onCancel() {
                }
            });

            playlistNameFragment.show(getFragmentManager(), "create_playlist");
        } else {
            mActivity.toast("You need to log in first");
            mActivity.doLogin();
        }

    }

    public void setupPlayerBar(View bar) {

        PlaylistPlayer playlistPlayer = mActivity.getPlaylistPlayer();
        final QueueupStateChange currentState = playlistPlayer.getCurrentState();

        /* Set up buttons and listeners */
        ImageButton playButton = (ImageButton) bar.findViewById(R.id.play_button);
        ImageButton skipButton = (ImageButton) bar.findViewById(R.id.skip_button);

        View.OnClickListener playButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Just invert the current playing status */
                updateTrackPlaying(!mActivity.getPlaylistPlayer().getCurrentState().playing);

            }
        };

        View.OnClickListener skipButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Send the update signal */
                mActivity.getPlaylistPlayer().updateTrackDone();
            }
        };

        playButton.setOnClickListener(playButtonListener);
        skipButton.setOnClickListener(skipButtonListener);

        /* Populate visual content */

        if (currentState != null) {
            updatePlayButton(currentState.playing);

            updateTrackViews(currentState.current);

            onTrackProgress(playlistPlayer.getCurrentProgress(), playlistPlayer.getCurrentDuration());
        }

        /* Tell the activity we are now the active listener */
        mActivity.getPlaylistPlayer().addPlaylistListener(PlaylistListFragment.this);

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
        final TextView trackArist = (TextView) mView.findViewById(R.id.playlist_current_artist);

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                List<String> imageUrls = track.album.imageUrls;
                Picasso.with(mActivity).load(imageUrls.get(imageUrls.size() - 1)).into(albumArt);

                trackName.setText(track.name);
                trackArist.setText(track.artists.get(0).name);
                trackName.setSelected(true);
                trackArist.setSelected(true);
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
            Log.d(Queueup.LOG_TAG, "NULL!");
        }
    }

    private void updateTrackPlaying(boolean playing) {
        mActivity.getPlaylistPlayer().updateTrackPlaying(playing);

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
    public void onQueueChanged(List<QueueupTrack> queue) {
        Log.d(Queueup.LOG_TAG, "Queueup changed, not necessary to update views right now");

    }

    @Override
    public void onPlayerReady() {
        /* TODO: Implement */
    }

    @Override
    public String getPlaylistId() {
        return mActivity.getPlaylistPlayer().getPlaylistId();
    }

    @Override
    public void onRefresh() {
        populateList();
        mView.setRefreshing(false);
    }

    class PlaylistGridAdapter extends BaseAdapter {

        private Context mContext;
        private List<QueueupPlaylist> mPlaylists;
        private int mResource;

        public PlaylistGridAdapter(Context c, List<QueueupPlaylist> playlists, int resource) {
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
            QueueupPlaylist playlist = mPlaylists.get(position);

            if (convertView == null) {
                playlistItem = LayoutInflater.from(mContext).inflate(mResource, parent, false);
            } else {
                playlistItem = convertView;
            }

            TextView title = (TextView) playlistItem.findViewById(R.id.playlist_list_item_title);
            TextView adminName = (TextView) playlistItem.findViewById(R.id.playlist_list_item_admin);
            ImageView adminIcon = (ImageView) playlistItem.findViewById(R.id.playlist_list_item_admin_icon);

            title.setText(playlist.name.toUpperCase());
            if (!playlist.adminName.isEmpty()) {
                adminName.setText(playlist.adminName);
                adminIcon.setImageDrawable(getResources().getDrawable(R.mipmap.ic_facebook));
            } else {
                adminName.setText("Spotify User");
                adminIcon.setImageDrawable(getResources().getDrawable(R.mipmap.ic_spotify));
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
                Log.d(Queueup.LOG_TAG, "Current playlist is null...");
            }

            return playlistItem;
        }
    }
}
