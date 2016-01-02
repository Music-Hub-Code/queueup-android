package org.louiswilliams.queueupplayer.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
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

    private static final int OFFSET = 100;

    private MainActivity mActivity;
    private View mView;
    private SpotifyPlaylist mPlaylist;
    private String mPlaylistId;
    private String mSpotifyPlaylist;
    private String mSpotifyUser;
    private SpotifyClient spotifyClient;
    private TrackListAdapter mTrackListAdapter;
    private ListView mTrackList;
    private boolean loadingMoreItems;

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
        mSpotifyUser = getArguments().getString("spotify_user_id");

        mActivity.spotifyLogin(new QueueUp.CallReceiver<String>() {
            @Override
            public void onResult(String accessToken) {
                spotifyClient = new SpotifyClient(accessToken);
                spotifyClient.getUserPlaylist(mSpotifyUser, mSpotifyPlaylist, new QueueUp.CallReceiver<SpotifyPlaylist>() {
                    @Override
                    public void onResult(final SpotifyPlaylist playlist) {
                        mPlaylist = playlist;
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
        final View searchFooter = mActivity.getLayoutInflater().inflate(R.layout.track_search_footer, null);

        Button addSelectedButton = (Button) mView.findViewById(R.id.add_selected_tracks);

        mTrackList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                boolean checked = mTrackListAdapter.isItemChecked(position);
                mTrackListAdapter.setItemChecked(position, !checked);
            }
        });

        mTrackList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showTrackOptionsDialog(mTrackListAdapter.getTrackList().get(position));
                return true;
            }
        });

        mTrackList.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                if (totalItemCount != 0 && firstVisibleItem + visibleItemCount == totalItemCount) {
                    loadMoreItems();
                }
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
                mTrackList.addFooterView(searchFooter, null, false);
                mTrackList.setAdapter(mTrackListAdapter);

            }
        });
    }

    public void showTrackOptionsDialog(final SpotifyTrack track) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);

        String[] trackOptions = new String[] {"Exit and open in Spotify"};

        builder.setTitle(track.name + " by " + track.artists.get(0).name);

        builder.setItems(trackOptions, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (i == 0) {
                    showOpenInSpotifyDialog(track.uri);
                }
            }
        }).show();
    }

    public void showOpenInSpotifyDialog(final String uri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);

        builder.setTitle("Are you sure you want to exit?")
            .setMessage("This will leave the app and open Spotify")
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mActivity.openSpotifyUri(uri);
                }
            }).setNegativeButton("No", null)
            .show();
    }

    private void loadMoreItems() {
        if (!loadingMoreItems) {

            final TextView footerText = (TextView) mTrackList.findViewById(R.id.track_list_footer_text);
            if (mTrackListAdapter.getTrackList().size() < mPlaylist.totalTracks) {
                loadingMoreItems = true;
                footerText.setText("Loading...");

                spotifyClient.getUserPlaylistTracks(mSpotifyUser, mSpotifyPlaylist, mTrackListAdapter.getCount(), OFFSET, new QueueUp.CallReceiver<List<SpotifyTrack>>() {
                    @Override
                    public void onResult(List<SpotifyTrack> result) {
                        mTrackListAdapter.addItems(result);

                    /* Remove lock on loading more tracks */
                        loadingMoreItems = false;
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                footerText.setText("");
                            }
                        });
                    }

                    @Override
                    public void onException(Exception e) {
                        Log.e(QueueUp.LOG_TAG, "Problem loading more tracks: " + e.getMessage());
                    }
                });
            } else {
                footerText.setText("End of list");
            }
        }
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

        public List<SpotifyTrack> getTrackList() {
            return mTrackList;
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

        public void addItems(List<SpotifyTrack> list) {
            mTrackList.addAll(list);
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
