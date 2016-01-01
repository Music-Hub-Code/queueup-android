package org.louiswilliams.queueupplayer.fragment;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.json.JSONArray;
import org.louiswilliams.queueupplayer.QueueUpApplication;
import org.louiswilliams.queueupplayer.R;
import org.louiswilliams.queueupplayer.activity.MainActivity;
import org.louiswilliams.queueupplayer.queueup.LocationUpdateListener;
import org.louiswilliams.queueupplayer.queueup.PlaylistListener;
import org.louiswilliams.queueupplayer.queueup.QueueUp;
import org.louiswilliams.queueupplayer.queueup.objects.QueueUpPlaylist;

import java.util.ArrayList;
import java.util.List;

public class PlaylistListFragment extends AbstractPlaylistListFragment implements PlaylistListener, SwipeRefreshLayout.OnRefreshListener, LocationUpdateListener {

    private String mAction;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mAction = getArguments().getString("action");
        return super.onCreateView(inflater, container, savedInstanceState);
    }


    @Override
    protected void populate(boolean refresh) {
        Log.d(QueueUp.LOG_TAG, "populating list...");

        /* Handle which action to take */
        if (mAction == null) {
            throw new IllegalStateException("action should not be null!");
        }
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.setTitle(getString(MainActivity.NAVIGATION_TITLES[mActivity.currentNavigationAction]));
            }
        });

        mTracker.send(new HitBuilders.EventBuilder().setAction("playlist_list").setLabel(mAction).build());

        switch (mAction) {
            case MainActivity.PLAYLISTS_ALL:
                populateAll(refresh);
                break;
            case MainActivity.PLAYLISTS_NEARBY:
                populateNearby(refresh);
                break;
            case MainActivity.PLAYLISTS_FRIENDS:
                populateFriends(refresh);
                break;
            case MainActivity.PLAYLISTS_MINE:
                populateMine(refresh);
                break;
            default:
                throw new IllegalStateException("No valid action given");
        }

    }

    public void populateAll(final boolean refresh) {

        mActivity.getQueueUpClient().playlistGetList(new QueueUp.CallReceiver<List<QueueUpPlaylist>>() {
            @Override
            public void onResult(List<QueueUpPlaylist> playlists) {
                Log.d(QueueUp.LOG_TAG, "Playlist all success");
                populateDone(playlists, null, refresh);
            }

            @Override
            public void onException(Exception e) {
                populateDone(null, e.getMessage(), refresh);
                Log.e(QueueUp.LOG_TAG, "Failed to getString playlist list: " + e.getMessage());
            }
        });
    }

    public void populateNearby(final boolean refresh) {
         /* Find nearby playlists */
        /* If the user hasn't enabled location services... */
        if (!mActivity.locationPermissionGranted(true) || !mActivity.isLocationEnabled(true)) {
            populateDone(null, null, refresh);
            mActivity.navigateDrawer(1); // Display "All" instead of nearby
            return;
        }
        Location location = mActivity.getLocationListener().getCurrentBestLocation();

        /* If we don't have a location yet, let the user know*/
        if (location == null) {
            mActivity.getLocationListener().getSingleLocationUpdate(this);

            String m = null;
            if (isAdded())  {
                m = getString(R.string.waiting_for_location);
            }
            populateDone(null, m, refresh);
            return;
        }

        mActivity.getQueueUpClient().getNearbyPlaylists(location, new QueueUp.CallReceiver<List<QueueUpPlaylist>>() {
            @Override
            public void onResult(List<QueueUpPlaylist> playlists) {
                if (playlists.size() > 0) {
                    populateDone(playlists, null, refresh);
                } else {
                    String m = null;
                    if (isAdded())  {
                        m = getString(R.string.no_playlists_nearby);
                    }
                    populateDone(null, m, refresh);
                }
            }

            @Override
            public void onException(Exception e) {
                e.printStackTrace();
                populateDone(null, e.getMessage(), refresh);
            }
        });
    }

    public void populateFriends(final boolean refresh) {
        /* If the user is logged in with Facebook, do a graph request to get their FB friends*/
        AccessToken accessToken = mActivity.getAccessToken();
        if (accessToken != null) {
            GraphRequest graphRequest = GraphRequest.newMyFriendsRequest(accessToken, new GraphRequest.GraphJSONArrayCallback() {
                @Override
                public void onCompleted(JSONArray jsonArray, GraphResponse graphResponse) {
                    List<String> ids = new ArrayList<String>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        ids.add(jsonArray.optJSONObject(i).optString("id"));
                    }

                    /* Now do a request to QueueUp to show us our friend's playlists*/
                    mActivity.getQueueUpClient().getFriendsPlaylists(ids, new QueueUp.CallReceiver<List<QueueUpPlaylist>>() {
                        @Override
                        public void onResult(List<QueueUpPlaylist> playlists) {

                            if (playlists.size() > 0) {
                                populateDone(playlists, null, refresh);
                            } else {
                                String m = null;
                                if (isAdded())  {
                                    m = getString(R.string.no_friends_playlists);
                                }
                                populateDone(null, m, refresh);
                            }
                        }

                        @Override
                        public void onException(Exception e) {
                            populateDone(null, e.getMessage(), refresh);
                        }
                    });
                }
            });

            graphRequest.executeAsync();

        } else {
            String m = null;
            if (isAdded())  {
                m = getString(R.string.facebook_notification);
            }
            populateDone(null, m, refresh);
        }
    }

    public void populateMine(final boolean refresh) {
        mActivity.getQueueUpClient().getUserPlaylists(mActivity.getCurrentUserId(), new QueueUp.CallReceiver<List<QueueUpPlaylist>>() {
            @Override
            public void onResult(List<QueueUpPlaylist> playlists) {

                if (playlists.size() > 0) {
                    populateDone(playlists, null, refresh);
                } else {
                    populateDone(null, getString(R.string.create_playlist_notification), refresh);
                }
            }

            @Override
            public void onException(Exception e) {
                Log.e(QueueUp.LOG_TAG, "Failed to getString playlist list: " + e.getMessage());
                populateDone(null, e.getMessage(), refresh);
            }
        });
    }

    @Override
    public void onLocation(Location location) {
        if (mAction.equals(MainActivity.PLAYLISTS_NEARBY)) {
            populateNearby(true);
        }
    }
}
