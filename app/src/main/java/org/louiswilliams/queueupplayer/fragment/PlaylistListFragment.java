package org.louiswilliams.queueupplayer.fragment;

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

import org.json.JSONArray;
import org.louiswilliams.queueupplayer.R;
import org.louiswilliams.queueupplayer.activity.MainActivity;
import org.louiswilliams.queueupplayer.queueup.PlaylistListener;
import org.louiswilliams.queueupplayer.queueup.QueueUp;
import org.louiswilliams.queueupplayer.queueup.objects.QueueUpPlaylist;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class PlaylistListFragment extends AbstractPlaylistListFragment implements PlaylistListener, SwipeRefreshLayout.OnRefreshListener {

    private String mAction;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mAction = getArguments().getString("action");

        return super.onCreateView(inflater, container, savedInstanceState);
    }


    @Override
    protected void populate() {
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

        switch (mAction) {
            case MainActivity.PLAYLISTS_ALL:
                populateAll();
                break;
            case MainActivity.PLAYLISTS_FRIENDS:
                populateFriends();
                break;
            case MainActivity.PLAYLISTS_MINE:
                populateMine();
                break;
            default:
                throw new IllegalStateException("No valid action given");
        }

    }

    public void populateAll() {

        mActivity.getQueueupClient().playlistGetList(new QueueUp.CallReceiver<List<QueueUpPlaylist>>() {
            @Override
            public void onResult(List<QueueUpPlaylist> playlists) {
                Log.d(QueueUp.LOG_TAG, "Playlist all success");
                mPlaylists = playlists;

                adapter = new PlaylistGridAdapter(mActivity, mPlaylists, R.layout.playlist_item);
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
                Log.e(QueueUp.LOG_TAG, "Failed to getString playlist list: " + e.getMessage());
            }
        });
    }

    public void populateFriends() {
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
                    mActivity.getQueueupClient().getFriendsPlaylists(ids, new QueueUp.CallReceiver<List<QueueUpPlaylist>>() {
                        @Override
                        public void onResult(List<QueueUpPlaylist> result) {
                            mPlaylists  = result;

                            adapter = new PlaylistGridAdapter(mActivity, mPlaylists, R.layout.playlist_item);
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
                            e.printStackTrace();
                        }
                    });

                }
            });

            graphRequest.executeAsync();

        } else {
            final ProgressBar progress = (ProgressBar) mView.findViewById(R.id.loading_progress_bar);
            final TextView playlistNotif = (TextView) mView.findViewById(R.id.playlist_notification);

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progress.setVisibility(View.GONE);
                    playlistNotif.setText(getString(R.string.facebook_notification));
                    playlistNotif.setVisibility(View.VISIBLE);
                }
            });


        }
    }

    public void populateMine() {
            mActivity.getQueueupClient().getUserPlaylists(mActivity.getCurrentUserId(), new QueueUp.CallReceiver<List<QueueUpPlaylist>>() {
            @Override
            public void onResult(List<QueueUpPlaylist> playlists) {
                Log.d(QueueUp.LOG_TAG, "Playlist all success");

                final ProgressBar progress = (ProgressBar) mView.findViewById(R.id.loading_progress_bar);
                final TextView notification = (TextView) mView.findViewById(R.id.playlist_notification);

                if (playlists.size() > 0) {
                    mPlaylists = playlists;
                    adapter = new PlaylistGridAdapter(mActivity, mPlaylists, R.layout.playlist_item);

                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progress.setVisibility(View.GONE);
                            playlistGrid.setAdapter(adapter);
                        }
                    });
                } else {


                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progress.setVisibility(View.GONE);
                            notification.setText(getString(R.string.create_playlist_notification));
                            notification.setVisibility(View.VISIBLE);
                        }
                    });
                }





            }

            @Override
            public void onException(Exception e) {
                mActivity.toast(e.getMessage());
                Log.e(QueueUp.LOG_TAG, "Failed to getString playlist list: " + e.getMessage());
            }
        });
    }
}
