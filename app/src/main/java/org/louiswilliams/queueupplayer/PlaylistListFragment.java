package org.louiswilliams.queueupplayer;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.gc.materialdesign.views.ProgressBarDeterminate;
import com.spotify.sdk.android.player.Player;
import com.squareup.picasso.Picasso;

import java.util.List;

import queueup.Queueup;
import queueup.QueueupClient;
import queueup.objects.QueueupPlaylist;
import queueup.objects.QueueupStateChange;
import queueup.objects.QueueupTrack;
import queueup.objects.SpotifyTrack;

public class PlaylistListFragment extends Fragment implements PlaylistListener{

    private GridView playlistGrid;
    private List<QueueupPlaylist> mPlaylists;
    private QueueupClient client;
    private MainActivity mActivity;
    private View mView;

    @Override
    public void onAttach(Activity activity) {
        mActivity = (MainActivity) activity;

        super.onAttach(activity);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        populateList();

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_playlist_list, container, false);

        client = mActivity.getQueueupClient();
        playlistGrid = (GridView) mView.findViewById(R.id.playlist_grid);

        playlistGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                QueueupPlaylist playlist = mPlaylists.get(position);
                Log.d(Queueup.LOG_TAG, "Using playlist ID: " + playlist.id);

                mActivity.showPlaylistFragment(playlist.id);
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
        mActivity.setPlaylistListener(null);

        super.onDestroyView();
    }

    private void populateList() {
        Log.d(Queueup.LOG_TAG, "populating list...");
        client.playlistGetList(new Queueup.CallReceiver<List<QueueupPlaylist>>() {
            @Override
            public void onResult(List<QueueupPlaylist> playlists) {
                Log.d(Queueup.LOG_TAG, "Playlist all success");
                mPlaylists = playlists;

                final PlaylistGridAdapter adapter = new PlaylistGridAdapter(mActivity, mPlaylists, R.layout.playlist_item);
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        playlistGrid.setAdapter(adapter);
                    }
                });

            }

            @Override
            public void onException(Exception e) {
                Log.e(Queueup.LOG_TAG, "Failed to get playlist list: " + e.getMessage());
            }
        });
    }

    public void setupPlayerBar(View bar) {

        final QueueupStateChange currentState = mActivity.getPlaylistPlayer().getCurrentState();

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

            final ImageView albumArt = (ImageView) bar.findViewById(R.id.playlist_image);
            final TextView trackName = (TextView) bar.findViewById(R.id.playlist_current_track);
            final TextView trackArist = (TextView) bar.findViewById(R.id.playlist_current_artist);

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    List<String> imageUrls = currentState.current.album.imageUrls;
                    Picasso.with(mActivity).load(imageUrls.get(imageUrls.size() - 1)).into(albumArt);

                    trackName.setText(currentState.current.name);
                    trackArist.setText(currentState.current.artists.get(0).name);
                    trackName.setSelected(true);
                    trackArist.setSelected(true);
                }
            });
        }

        /* Tell the activity we are now the active listener */
        mActivity.setPlaylistListener(this);

        bar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.showPlaylistFragment(getPlaylistId());
            }
        });
        Log.d(Queueup.LOG_TAG, "PLAYLIST LIST is now listener");
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

        Player player = mActivity.getSpotifyPlayer();
        if (player != null) {
            if (playing) {
                player.resume();
            } else {
                player.pause();
            }
        }
        updatePlayButton(playing);
    }

    @Override
    public void onPlayingChanged(boolean playing) {
        updatePlayButton(playing);
    }

    @Override
    public void onTrackChanged(SpotifyTrack track) {

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
    public String getPlaylistId() {
        return mActivity.getPlaylistPlayer().getPlaylistId();
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
                View v = LayoutInflater.from(mContext).inflate(mResource, parent, false);
                playlistItem = v;

                TextView title = (TextView) playlistItem.findViewById(R.id.playlist_list_item_title);
                title.setText(playlist.name.toUpperCase());

                if (playlist.current != null && playlist.current.album != null && playlist.current.album.imageUrls != null) {
                    List<String> imageUrls = playlist.current.album.imageUrls;
                    if (imageUrls.size() > 0) {
                        ImageView image = (ImageView) v.findViewById(R.id.playlist_list_item_image);
                        Picasso.with(mContext).load(imageUrls.get(0)).into(image);
                    }
                }
            } else {
                playlistItem = convertView;
            }


            return playlistItem;
        }
    }
}
