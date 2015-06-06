package org.louiswilliams.queueupplayer;

import android.app.Fragment;
import android.content.Context;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.squareup.picasso.Picasso;

import java.util.List;

import queueup.PlaylistClient;
import queueup.PlaylistPlayer;
import queueup.Queueup;
import queueup.QueueupClient;
import queueup.objects.QueueupPlaylist;
import queueup.objects.QueueupStateChange;
import queueup.objects.QueueupTrack;
import queueup.objects.SpotifyTrack;

/**
 * Created by Louis on 5/25/2015.
 */
public class PlaylistFragment extends Fragment implements View.OnClickListener {

    private String mPlaylistId;
    private PlaylistPlayer mPlaylistPlayer;
    private QueueupClient mQueueupClient;
    private View mView;

    private QueueupPlaylist currentPlaylist;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_playlist, container, false);
        mPlaylistId = getArguments().getString("playlist_id");

        Log.d(Queueup.LOG_TAG, "Loading playlist" + mPlaylistId);

        mQueueupClient = ((MainActivity) getActivity()).getQueueupClient();

        mQueueupClient.playlistGet(mPlaylistId, new Queueup.CallReceiver<QueueupPlaylist>() {
            @Override
            public void onResult(QueueupPlaylist result) {
                Log.d(Queueup.LOG_TAG, result.toString());

                currentPlaylist = result;
                populateView(result);

            }

            @Override
            public void onException(Exception e) {
                toast(e.getMessage());
                Log.e(Queueup.LOG_TAG, "Problem getting playlist: " + e.getMessage());
            }
        });

        final PlaylistClient.StateChangeListener stateChangeListener = new PlaylistClient.StateChangeListener() {
            @Override
            public void onStateChange(QueueupStateChange state) {
                Log.d(Queueup.LOG_TAG, "State change: " + state);

                handleStateChange(state);
            }

            @Override
            public void onError(String message) {
                toast(message);
                Log.e(Queueup.LOG_TAG, message);
            }
        };

        mPlaylistPlayer = mQueueupClient.getPlaylistPlayer(new Queueup.CallReceiver<PlaylistClient>() {
            @Override
            public void onResult(PlaylistClient result) {
                Log.d(Queueup.LOG_TAG, "AUTH SUCCESS");
                PlaylistPlayer player  = (PlaylistPlayer) result;
                player.subscribe(mPlaylistId, true, stateChangeListener);

            }

            @Override
            public void onException(Exception e) {
                toast(e.getMessage());
                Log.e(Queueup.LOG_TAG, "AUTH PROBLEM: " + e.getMessage());
            }
        });


        return mView;
    }

    private void populateView(final QueueupPlaylist playlist) {
        List<QueueupTrack> tracks = playlist.tracks;
        String userId = playlist.adminId;
        final boolean isAdmin = userId.equals(mQueueupClient.getUserId());

        final TrackListAdapter trackListAdapter = new TrackListAdapter(getActivity(), tracks, R.layout.track_item);
        final ListView trackList = (ListView) mView.findViewById(R.id.track_list);

        final View playlistHeader;

        if (isAdmin) {
            playlistHeader = getActivity().getLayoutInflater().inflate(R.layout.playlist_player_header, null);
        } else {
            playlistHeader = getActivity().getLayoutInflater().inflate(R.layout.playlist_header, null);
        }

        final ImageView albumArt = (ImageView) playlistHeader.findViewById(R.id.playlist_image);
        final TextView trackName = (TextView) playlistHeader.findViewById(R.id.playlist_current_track);
        final TextView trackArist = (TextView) playlistHeader.findViewById(R.id.playlist_current_artist);

//        final ImageButton playButton = (ImageButton) mView.findViewById(R.id.play_button);

//        if (userId.equals(mQueueupClient.getUserId())) {
//            playButton.setOnClickListener(this);
//        } else {
//            playButton.setVisibility(View.GONE);
//        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                if (currentPlaylist.playing) {
//                    playButton.setImageResource(R.drawable.ic_action_pause);
//                } else {
//                    playButton.setImageResource(R.drawable.ic_action_play_arrow);
//                }

                Picasso.with(getActivity()).load(playlist.current.album.imageUrls.get(0)).into(albumArt);
                if (isAdmin) {
                    albumArt.getLayoutParams().height = trackList.getWidth() - (trackList.getPaddingLeft() + trackList.getPaddingRight());
                    albumArt.requestLayout();
                }
                trackName.setText(playlist.current.name);
                trackArist.setText(playlist.current.artists.get(0).name);

                trackList.addHeaderView(playlistHeader, null, false);
                trackList.setAdapter(trackListAdapter);

                getActivity().setTitle(playlist.name);
            }
        });

    }

    @Override
    public void onDestroy() {
        if (mPlaylistPlayer != null) {
            mPlaylistPlayer.disconnect();
        }

        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
    //        ((MainActivity) getActivity()).spotifyLogin();
        updateTrackPlaying(!currentPlaylist.playing);
    }

    private  void updatePlayButton(final boolean playing) {
//        final ImageButton button =  (ImageButton)  mView.findViewById(R.id.play_button);
//        if (button != null) {
//            getActivity().runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    if (playing) {
//                        button.setImageResource(R.drawable.ic_action_pause);
//                    } else {
//                        button.setImageResource(R.drawable.ic_action_play_arrow);
//                    }
//                }
//            });
//        } else {
//            Log.d(Queueup.LOG_TAG, "NULL!");
//        }
    }

    private void updateTrackPlaying(boolean playing) {
        currentPlaylist.playing = playing;

        mPlaylistPlayer.updateTrackPlaying(playing);

        updatePlayButton(playing);
    }

    private void updateCurrentTrack(final SpotifyTrack current) {
        final ImageView albumArt = (ImageView) mView.findViewById(R.id.playlist_image);
        final TextView trackName = (TextView) mView.findViewById(R.id.playlist_current_track);
        final TextView trackArist = (TextView) mView.findViewById(R.id.playlist_current_artist);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Picasso.with(getActivity()).load(current.album.imageUrls.get(0)).into(albumArt);
                trackName.setText(current.name);
                trackArist.setText(current.artists.get(0).name);
            }
        });

    }

    public void handleStateChange(QueueupStateChange state) {
        updatePlayButton(state.playing);

        if (state.current != null) {
            updateCurrentTrack(state.current);
        }

    }

    public static class TrackListAdapter extends BaseAdapter {

        private Context mContext;
        private List<QueueupTrack> mTrackList;
        private int mResource;

        public TrackListAdapter(Context context, List<QueueupTrack> tracks,  int resource) {
            mContext = context;
            mTrackList = tracks;
            mResource = resource;
        }

        @Override
        public int getCount() {
            return mTrackList.size();
        }

        @Override
        public Object getItem(int position) {
            return mTrackList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View trackView;
            QueueupTrack track = mTrackList.get(position);

            if (convertView == null) {
                trackView = LayoutInflater.from(mContext).inflate(mResource, parent, false);

            } else {
                trackView = convertView;

            }

            TextView title = (TextView) trackView.findViewById(R.id.track_list_item_name);
            TextView artist = (TextView) trackView.findViewById(R.id.track_list_item_artist);

            title.setText(track.track.name);
            artist.setText(track.track.artists.get(0).name);

            List<String> imageUrls = track.track.album.imageUrls;
            ImageView image = (ImageView) trackView.findViewById(R.id.track_list_item_image);

            Picasso.with(mContext).load(imageUrls.get(0)).into(image);

            return trackView;
        }
    }

    public void toast(final String message) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
