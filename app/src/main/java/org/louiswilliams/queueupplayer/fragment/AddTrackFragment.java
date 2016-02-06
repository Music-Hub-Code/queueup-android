package org.louiswilliams.queueupplayer.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONObject;
import org.louiswilliams.queueupplayer.R;
import org.louiswilliams.queueupplayer.activity.MainActivity;
import org.louiswilliams.queueupplayer.queueup.QueueUp;
import org.louiswilliams.queueupplayer.queueup.objects.SpotifyTrack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class AddTrackFragment extends Fragment implements BackButtonListener {

    private MainActivity mActivity;
    private View mView;
    private EditText mSearchBox;
    private TrackSearchListAdapter mTrackListAdapter;
    private ListView mTrackListView;
    private Timer searchBackoffTimer;
    private boolean loadingMoreItems;
    private String mPlaylistId;

    @Override
    public void onAttach(Context activity) {
        mActivity = (MainActivity) activity;
        mActivity.setTitle("Add Track");

        super.onAttach(activity);
    }

    @Override
    public void onAttach(Activity activity) {
        mActivity = (MainActivity) activity;
        mActivity.setTitle("Add Track");

        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_add_track, container, false);
        mPlaylistId = getArguments().getString("playlist_id");

        mTrackListView = (ListView) mView.findViewById(R.id.track_search_list);


        mTrackListAdapter = new TrackSearchListAdapter(mActivity, new ArrayList<SpotifyTrack>(), R.layout.track_item);
        View searchFooter = mActivity.getLayoutInflater().inflate(R.layout.track_search_footer, null);

        mTrackListView.addFooterView(searchFooter, null, false);
        mTrackListView.setAdapter(mTrackListAdapter);

        mTrackListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

                if (scrollState != 0) {
                    mActivity.hideKeyboard();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                /* Adjust the numbers because the footer throws off the counts by 1 */
                int visibleAdj = visibleItemCount - 1;
                int totalAdj = totalItemCount - 1;

                if (totalAdj != 0 && firstVisibleItem + visibleAdj == totalAdj) {
                    loadMoreItems();
                }

            }
        });

        mTrackListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SpotifyTrack track = (SpotifyTrack) mTrackListAdapter.getItem(position);
                showTrackOptionsDialog(track);
            }
        });

        mSearchBox = (EditText) mView.findViewById(R.id.track_search_box);
        mSearchBox.requestFocus();
        mActivity.showKeyboard();

        final ImageButton searchClear = (ImageButton) mView.findViewById(R.id.track_search_clear);
        searchClear.setVisibility(View.GONE);
        final ImageButton importButton = (ImageButton) mView.findViewById(R.id.import_from_spotify_button);
        final LinearLayout importParent = (LinearLayout) mView.findViewById(R.id.import_from_spotify_wrap);

        mSearchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchTracks(s.toString());

                /* Toggle display of clear and import buttons */
                if (s.length() == 0) {
                    searchClear.setVisibility(View.GONE);
                    importParent.setVisibility(View.VISIBLE);
                    mTrackListAdapter.updateTrackList(new ArrayList<SpotifyTrack>());
                } else {
                    searchClear.setVisibility(View.VISIBLE);
                    importParent.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        searchClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSearchBox.setText(null);
            }
        });

        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.getFragmentManager().popBackStack(PlaylistFragment.class.getName(), 0);
                mActivity.showSpotifyPlaylistListFragment(mPlaylistId);
            }
        });

        return mView;
    }

    @Override
    public void onDestroy() {
        mActivity.hideKeyboard();

        super.onDestroy();
    }

    public void showTrackOptionsDialog(final SpotifyTrack track) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);

        String[] trackOptions = new String[] {"Add Track", "Exit and open in Spotify"};

        builder.setTitle(track.name + " by " + track.artists.get(0).name);

        builder.setItems(trackOptions, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (i == 0) {
                    mTrackListAdapter.mTrackList.remove(track);
                    mTrackListAdapter.notifyDataSetChanged();
                    addTrackToPlaylist(track);
                } else if (i == 1) {
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

    public void addTrackToPlaylist(SpotifyTrack track) {

        mActivity.toastTop("Track added");

        mActivity.getQueueUpClient().playlistAddTrack(mPlaylistId, track.id, new QueueUp.CallReceiver<JSONObject>() {
            @Override
            public void onResult(JSONObject result) {
                // Instead of exiting the fragment, just show a confirmation
            }

            @Override
            public void onException(Exception e) {
                mActivity.toast("Couldn't add track: " + e.getMessage());
            }
        });
    }

    public void loadMoreItems() {

        Log.d(QueueUp.LOG_TAG, "Loading more items");

        /* Essentially place a lock so this doesn't getString called when already loading*/
        if (!loadingMoreItems) {
            loadingMoreItems = true;

            TextView footerText = (TextView) mTrackListView.findViewById(R.id.track_list_footer_text);
            footerText.setText("Loading...");

            String query = mSearchBox.getText().toString();

            doSearch(query, mTrackListAdapter.getCount(), new QueueUp.CallReceiver<List<SpotifyTrack>>() {
                @Override
                public void onResult(final List<SpotifyTrack> results) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTrackListAdapter.addItems(results);

                            /* Remove lock on loading more tracks */
                            loadingMoreItems = false;
                        }
                    });
                }

                @Override
                public void onException(Exception e) {
                    Log.e(QueueUp.LOG_TAG, "Problem loading more tracks: " + e.getMessage());
                }
            });
        }
    }

    public void searchTracks(final String query) {

        /* The backoff timer reduces the amount of unnecessary querying that will just be aborted
         * soon after when the user types another character. This forces a delay for each
         * keypress and executes it only after a short delay */


        if (searchBackoffTimer != null) {
            Log.d(QueueUp.LOG_TAG, "Backing off last query");
            searchBackoffTimer.cancel();
        }

        final TextView footerText = (TextView) mTrackListView.findViewById(R.id.track_list_footer_text);

        if (query.length() == 0) {
            footerText.setText("");
            return;
        } else {
            footerText.setText("Searching...");
        }


        searchBackoffTimer = new Timer();

        searchBackoffTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                doSearch(query, 0, new QueueUp.CallReceiver<List<SpotifyTrack>>() {
                    @Override
                    public void onResult(final List<SpotifyTrack> results) {

                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mTrackListAdapter.updateTrackList(results);
                                Log.d(QueueUp.LOG_TAG, results.size() + " search results");
                            }
                        });
                    }

                    @Override
                    public void onException(Exception e) {
                        Log.e(QueueUp.LOG_TAG, "Problem loading search: " + e.getMessage());
                    }
                });
            }
        }, 250);

    }


    public void doSearch(String query, int offset, QueueUp.CallReceiver<List<SpotifyTrack>> receiver) {
        Log.d(QueueUp.LOG_TAG, "Searching for " + query + " at offset " + offset);

        mActivity.getQueueUpClient().searchTracks(query, offset, receiver);

    }

    @Override
    public boolean onBackButtonPressed() {
        /* If the back button is pressed and there is a query in the box, clear it */
        if (mSearchBox.getText().length() > 0) {
            mSearchBox.setText(null);
            return true;
        }
        return false;
    }


    public static class TrackSearchListAdapter extends BaseAdapter {

        private Context mContext;
        private List<SpotifyTrack> mTrackList;
        private int mResource;

        public TrackSearchListAdapter(Context context, List<SpotifyTrack> tracks,  int resource) {
            mContext = context;
            mTrackList = tracks;
            mResource = resource;
        }

        public void updateTrackList(List<SpotifyTrack> list) {
            mTrackList = list;
            notifyDataSetChanged();
        }

        public void addItems(List<SpotifyTrack> list) {
            mTrackList.addAll(list);
            notifyDataSetChanged();
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
            SpotifyTrack track = mTrackList.get(position);

            if (convertView == null) {
                trackView = LayoutInflater.from(mContext).inflate(mResource, parent, false);

            } else {
                trackView = convertView;

            }

            TextView title = (TextView) trackView.findViewById(R.id.track_list_item_name);
            TextView artist = (TextView) trackView.findViewById(R.id.track_list_item_artist);
            ImageView image = (ImageView) trackView.findViewById(R.id.track_list_item_image);

            title.setText(track.name);
            artist.setText(track.artists.get(0).name);

            List<String> imageUrls = track.album.imageUrls;

            if (imageUrls.size() > 0) {
                Picasso.with(mContext).load(imageUrls.get(0)).into(image);
            }

            /* Hide votes */
            trackView.findViewById(R.id.track_votes).setVisibility(View.GONE);

            return trackView;
        }
    }

}