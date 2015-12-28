package org.louiswilliams.queueupplayer.fragment;

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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONObject;
import org.louiswilliams.queueupplayer.R;
import org.louiswilliams.queueupplayer.activity.MainActivity;
import org.louiswilliams.queueupplayer.queueup.QueueUp;
import org.louiswilliams.queueupplayer.queueup.api.SpotifyClient;
import org.louiswilliams.queueupplayer.queueup.objects.SpotifyPlaylist;
import org.louiswilliams.queueupplayer.queueup.objects.SpotifyTrack;

import java.util.ArrayList;
import java.util.List;

public class SpotifyPlaylistFragment extends Fragment {

    private MainActivity mActivity;
    private View mView;
    private String mPlaylistId;
    private String mSpotifyPlaylist;
    private String mSpoitfyUser;
    private SpotifyClient spotifyClient;
    private TrackListAdapter mTrackListAdapter;
    private ListView mTrackList;

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


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_spotify_playlist, container, false);
        mPlaylistId = getArguments().getString("playlist_id");
        mSpotifyPlaylist = getArguments().getString("spotify_playlist_id");
        mSpoitfyUser = getArguments().getString("spotify_user_id");

        mActivity.spotifyLogin(new QueueUp.CallReceiver<String>() {
            @Override
            public void onResult(String accessToken) {
                spotifyClient = new SpotifyClient(accessToken);
                spotifyClient.getUserPlaylist(mSpoitfyUser, mSpotifyPlaylist, new QueueUp.CallReceiver<SpotifyPlaylist>() {
                    @Override
                    public void onResult(final SpotifyPlaylist playlist) {
                        populate(playlist);
                    }

                    @Override
                    public void onException(Exception e) {
                        e.printStackTrace();
                        Log.e(QueueUp.LOG_TAG, "Error getting User playlist:" + e.getMessage());
                    }
                });
            }

            @Override
            public void onException(Exception e) {
                e.printStackTrace();
                Log.e(QueueUp.LOG_TAG, "Error getting Spotify access token" + e.getMessage());
            }
        });


        return mView;
    }

    public void populate(final SpotifyPlaylist playlist) {
        mTrackListAdapter = new TrackListAdapter(mActivity, playlist.tracks, R.layout.track_item);
        mTrackList = (ListView) mView.findViewById(R.id.track_list);
        final View controlsProgress = mView.findViewById(R.id.loading_progress_bar);
        Button addSelectedButton = (Button) mView.findViewById(R.id.add_selected_tracks);

        mTrackList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                boolean checked = mTrackListAdapter.isItemChecked(position);
                mTrackListAdapter.setItemChecked(position, !checked);
            }
        });

        addSelectedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controlsProgress.setVisibility(View.VISIBLE);

                List<SpotifyTrack> tracks = mTrackListAdapter.getCheckedTracks();
                List<String> trackIds = new ArrayList<String>();
                for (SpotifyTrack track : tracks) {
                    trackIds.add(track.id);
                }

                mActivity.getQueueUpClient().playlistAddTracks(mPlaylistId, trackIds, new QueueUp.CallReceiver<JSONObject>() {
                    @Override
                    public void onResult(JSONObject result) {
                        mActivity.getFragmentManager().popBackStack(PlaylistFragment.class.getName(), 0);
                    }

                    @Override
                    public void onException(Exception e) {
                        mActivity.toast(e.getMessage());
                        mActivity.getFragmentManager().popBackStack(PlaylistFragment.class.getName(), 0);
                    }
                });
            }
        });


        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                controlsProgress.setVisibility(View.GONE);
                mActivity.setTitle(playlist.name);
                mTrackList.setAdapter(mTrackListAdapter);
            }
        });
    }



    public class TrackListAdapter extends BaseAdapter {

        private Context mContext;
        private List<SpotifyTrack> mTrackList;
        private List<SpotifyTrack> checkedList;
        private int mResource;

        public TrackListAdapter(Context context, List<SpotifyTrack> tracks,  int resource) {
            mContext = context;
            mTrackList = tracks;
            mResource = resource;
            checkedList = new ArrayList<>();
        }

        public void updateTrackList(List<SpotifyTrack> list) {
            mTrackList = list;

            /* Calls are going to be from different asynchronous threads, so to be safe, run on main thread */
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }

        public void setItemChecked(int position, boolean checked) {
            SpotifyTrack track = mTrackList.get(position);
            if (!checked) {
                checkedList.remove(track);
            } else {
                checkedList.add(mTrackList.get(position));
            }
            notifyDataSetChanged();
        }

        public boolean isItemChecked(int position) {
            return checkedList.contains(mTrackList.get(position));
        }

        public List<SpotifyTrack> getCheckedTracks() {
            return checkedList;
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
        public View getView(final int position, View convertView, ViewGroup parent) {
            View trackView;
            final SpotifyTrack track = mTrackList.get(position);

            if (convertView == null) {
                trackView = LayoutInflater.from(mContext).inflate(mResource, parent, false);
            } else {
                trackView = convertView;
            }

            if (checkedList.contains(mTrackList.get(position))) {
                trackView.setBackgroundColor(getResources().getColor(R.color.tertiary_light));
            } else {
                trackView.setBackgroundColor(getResources().getColor(R.color.primary_dark));
            }

            /* Titles */
            TextView title = (TextView) trackView.findViewById(R.id.track_list_item_name);
            TextView artist = (TextView) trackView.findViewById(R.id.track_list_item_artist);

            title.setText(track.name);
            if (track.artists.size()  > 0)
                artist.setText(track.artists.get(0).name);

            List<String> imageUrls = track.album.imageUrls;
            ImageView image = (ImageView) trackView.findViewById(R.id.track_list_item_image);

            if (imageUrls.size() > 0)
                Picasso.with(mContext).load(imageUrls.get(0)).into(image);

            trackView.findViewById(R.id.track_votes).setVisibility(View.GONE);

            return trackView;
        }
    }
}
