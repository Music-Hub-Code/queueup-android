package org.louiswilliams.queueupplayer.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.gc.materialdesign.views.ButtonFlat;
import com.gc.materialdesign.views.ProgressBarDeterminate;
import com.squareup.picasso.Picasso;

import org.louiswilliams.queueupplayer.R;
import org.louiswilliams.queueupplayer.activity.MainActivity;

import java.util.List;

import org.louiswilliams.queueupplayer.queueup.PlaylistListener;
import org.louiswilliams.queueupplayer.queueup.PlaylistPlayer;
import org.louiswilliams.queueupplayer.queueup.Queueup;
import org.louiswilliams.queueupplayer.queueup.QueueupClient;
import org.louiswilliams.queueupplayer.queueup.objects.QueueupPlaylist;
import org.louiswilliams.queueupplayer.queueup.objects.QueueupStateChange;
import org.louiswilliams.queueupplayer.queueup.objects.QueueupTrack;
import org.louiswilliams.queueupplayer.queueup.objects.SpotifyTrack;

/**
 * Created by Louis on 5/25/2015.
 */
public class PlaylistFragment extends Fragment implements PlaylistListener {

    private String mPlaylistId;
    private QueueupClient mQueueupClient;
    private QueueupPlaylist mThisPlaylist;
    private View mView;
    private TrackListAdapter mTrackListAdapter;
    private ListView mTrackList;

    private MainActivity mActivity;

    @Override
    public void onAttach(Activity activity) {
        mActivity = (MainActivity) activity;
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_playlist, container, false);
        mPlaylistId = getArguments().getString("playlist_id");

        Log.d(Queueup.LOG_TAG, "Loading playlist" + mPlaylistId);

        /* Get the client*/
        mQueueupClient = mActivity.getQueueupClient();

        /* Get the playlist data to initially populate the view */
        mQueueupClient.playlistGet(mPlaylistId, new Queueup.CallReceiver<QueueupPlaylist>() {
            @Override
            public void onResult(QueueupPlaylist result) {
                Log.d(Queueup.LOG_TAG, result.toString());

                mThisPlaylist = result;

                mActivity.invalidateOptionsMenu();
                populateView(mThisPlaylist);

            }

            @Override
            public void onException(Exception e) {
                mActivity.toast(e.getMessage());
                Log.e(Queueup.LOG_TAG, "Problem getting playlist: " + e.getMessage());
            }
        });


        return mView;
    }


    @Override
    public void onCreateOptionsMenu (Menu menu, MenuInflater menuInflater) {
        if (mThisPlaylist != null && isUserAdmin(mThisPlaylist.adminId)) {
            menuInflater.inflate(R.menu.menu_playlist, menu);
        }
        super.onCreateOptionsMenu(menu, menuInflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_playlist_rename:
                showRenameDialog();
                return true;
            case R.id.action_playlist_delete:
                showDeleteDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroyView() {
        /* Make sure the activity knows that there is no playlist listener anymore  */
        if (mActivity.getPlaylistPlayer() != null) {
            mActivity.getPlaylistPlayer().removePlaylistListener(this);
        }

        super.onDestroyView();
    }

    private void populateView(final QueueupPlaylist playlist) {
        List<QueueupTrack> tracks = playlist.tracks;
        String userId = playlist.adminId;
        final boolean isAdmin = isUserAdmin(userId);

        mTrackListAdapter = new TrackListAdapter(mActivity, tracks, R.layout.track_item);
        mTrackList = (ListView) mView.findViewById(R.id.track_list);

        final View playlistHeader, playlistFooter;
        final ImageButton addTrackButton = (ImageButton) mView.findViewById(R.id.add_track_button);

        addTrackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(Queueup.LOG_TAG, "Add track");
                mActivity.showAddTrackFragment(mPlaylistId);
            }
        });



        playlistHeader = mActivity.getLayoutInflater().inflate(R.layout.playlist_player_header, null);
        playlistFooter = mActivity.getLayoutInflater().inflate(R.layout.playlist_footer, null);

        /* If the current playlist playlist is this one, show the controls */
        if (currentPlaylistIsPlaying()) {
            showPlaylistControls(playlistHeader, playlist.playing);
            setProgressBar(View.GONE);
            mActivity.getPlaylistPlayer().addPlaylistListener(PlaylistFragment.this);

        } else {

            final ButtonFlat playHereButton = (ButtonFlat) playlistHeader.findViewById(R.id.play_here_button);

            /* If the user is the admin of the current playlist, give the option to play, otherwise say who owns it */
            if (isAdmin) {

                /* Listen to the "play here" button */
                View.OnClickListener playHereListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        /* Show the progress bar */
                        setProgressBar(View.VISIBLE);

                        /* Prevent the button from being pressed again*/
                        playHereButton.setOnClickListener(null);

                        /* Tell the activity to subscribe to this playlist and launch the player */
                        PlaylistPlayer playlistPlayer = mActivity.subscribeToPlaylist(mPlaylistId);

                        playlistPlayer.addPlaylistListener(PlaylistFragment.this);

                        /* Insert the playlist controls */
                        showPlaylistControls(playlistHeader, playlist.playing);


                    }
                };

                playHereButton.setOnClickListener(playHereListener);

            } else {
                ViewGroup parent = (ViewGroup) playHereButton.getParent();
                parent.removeView(playHereButton);
                TextView replacement = new TextView(getActivity());
                if (!playlist.adminName.isEmpty()) {
                    replacement.setText("Created by " + playlist.adminName);
                } else {
                    replacement.setText("Created by a Queueup user");
                }
                replacement.setGravity(Gravity.CENTER);
                replacement.setTextColor(getResources().getColor(R.color.material_deep_teal_200));
                replacement.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));


                parent.addView(replacement);
            }

        }

        final ImageView albumArt = (ImageView) playlistHeader.findViewById(R.id.playlist_image);
        final TextView trackName = (TextView) playlistHeader.findViewById(R.id.playlist_current_track);
        final TextView trackArist = (TextView) playlistHeader.findViewById(R.id.playlist_current_artist);

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                mActivity.setTitle(playlist.name);

                if (playlist.current != null) {

                    if (playlist.current.album != null && playlist.current.album.imageUrls != null && playlist.current.album.imageUrls.size() > 0) {
                        Picasso.with(mActivity).load(playlist.current.album.imageUrls.get(0)).into(albumArt);

                        albumArt.getLayoutParams().height = mTrackList.getWidth() - (mTrackList.getPaddingLeft() + mTrackList.getPaddingRight());
                        albumArt.requestLayout();

                    }

                    trackName.setText(playlist.current.name);
                    trackArist.setText(playlist.current.artists.get(0).name);
                }

                mTrackList.addHeaderView(playlistHeader, null, false);
                mTrackList.addFooterView(playlistFooter, null, false);
                mTrackList.setAdapter(mTrackListAdapter);
                mTrackList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        view.findViewById(R.id.track_list_item_name).setSelected(true);
                    }
                });

                setProgressBar(View.GONE);

                /* If the fragment was created after a track was added, scroll down to the bottom */
                maybeShowNewTrack();

            }
        });

    }


    public void setProgressBar(final int visibility) {
        final View controlsProgress = mView.findViewById(R.id.loading_progress_bar);

        /* Check to see what thread we're on */
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    controlsProgress.setVisibility(visibility);

                }
            });
        } else {
            controlsProgress.setVisibility(visibility);
        }

    }


    public void showPlaylistControls(View parent, boolean playing) {
        View playlistControls = mActivity.getLayoutInflater().inflate(R.layout.playlist_controls, null);

        /* Replace the contents of the frame with the new control layout */
        FrameLayout controlFrame = (FrameLayout) parent.findViewById(R.id.playlist_control_frame);
        controlFrame.removeAllViews();
        controlFrame.addView(playlistControls);

        ImageButton playButton = (ImageButton) playlistControls.findViewById(R.id.play_button);
        ImageButton skipButton = (ImageButton) playlistControls.findViewById(R.id.skip_button);

        View.OnClickListener playButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Just invert the current playing status */
                QueueupStateChange current =  mActivity.getPlaylistPlayer().getCurrentState();

                /* Unless it's null, which means we should just play anyways */
                boolean updateToPlaying = (current == null || !current.playing);
                updateTrackPlaying(updateToPlaying);
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

        /* Initialize the play button */
        updatePlayButton(playButton, playing);

    }

    private  void updatePlayButton(final ImageButton button, final boolean playing) {
        if (button != null) {
            mActivity.runOnUiThread(new Runnable() {
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

    }

    private void updateCurrentTrack(final SpotifyTrack current) {
        final ImageView albumArt = (ImageView) mView.findViewById(R.id.playlist_image);
        final TextView trackName = (TextView) mView.findViewById(R.id.playlist_current_track);
        final TextView trackArist = (TextView) mView.findViewById(R.id.playlist_current_artist);

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Picasso.with(mActivity).load(current.album.imageUrls.get(0)).into(albumArt);
                trackName.setText(current.name);
                trackArist.setText(current.artists.get(0).name);
            }
        });

    }

    public void showRenameDialog() {
        PlaylistNameFragment playlistNameFragment = new PlaylistNameFragment();

        playlistNameFragment.setDialogTitle("Rename Playlist");
        playlistNameFragment.setTextContent(mThisPlaylist.name);
        playlistNameFragment.setPlaylistNameListener(new PlaylistNameFragment.PlaylistNameListener() {
            @Override
            public void onPlaylistCreate(PlaylistNameFragment dialogFragment) {
                renamePlaylist(dialogFragment.getPlaylistName());
            }

            @Override
            public void onCancel() {
            }
        });

        playlistNameFragment.show(getFragmentManager(), "create_playlist");
    }

    public void renamePlaylist(String newName) {
        mQueueupClient.playlistRename(mPlaylistId, newName, new Queueup.CallReceiver<QueueupPlaylist>() {

            @Override
            public void onResult(QueueupPlaylist result) {
                /* Recreate the fragment without adding it to the back stack */
                mActivity.reloadCurrentFragment();
            }

            @Override
            public void onException(Exception e) {
                Log.e(Queueup.LOG_TAG, e.getMessage());
                mActivity.toast(e.getMessage());
            }
        });
    }

    public void showDeleteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle("Delete Playlist")
                .setMessage("Are you sure you want to delete your playlist?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deletePlaylist();
                    }
                }).setNegativeButton("No", null)
                .show();

    }

    public void deletePlaylist() {
        mQueueupClient.playlistDelete(mPlaylistId, new Queueup.CallReceiver<Boolean>() {

            @Override
            public void onResult(Boolean result) {
                /* Recreate the fragment without adding it to the back stack */
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getFragmentManager().popBackStackImmediate();
                    }
                });
            }

            @Override
            public void onException(Exception e) {
                Log.e(Queueup.LOG_TAG, e.getMessage());
                mActivity.toast(e.getMessage());
            }
        });
    }

    public boolean currentPlaylistIsPlaying() {
        return (mActivity.getPlaylistPlayer() != null && mActivity.getPlaylistPlayer().getPlaylistId().equals(mPlaylistId));
    }

    @Override
    public void onPlayingChanged(boolean playing) {
        ImageButton button = (ImageButton)  mView.findViewById(R.id.play_button);
        updatePlayButton(button, playing);

    }

    @Override
    public void onTrackChanged(SpotifyTrack track) {
        updateCurrentTrack(track);
    }

    @Override
    public void onTrackProgress(int progress, int duration) {
        final ProgressBarDeterminate progressBar = (ProgressBarDeterminate) mView.findViewById(R.id.track_progress);
        final TextView progressLabel = (TextView) mView.findViewById(R.id.track_progress_text);
        String progressText = String.format("%d:%02d", progress / (60 * 1000), (progress / 1000) % 60);
        String durationText = String.format("%d:%02d", duration / (60 * 1000), (duration / 1000) % 60);

        if (progressBar != null && progressLabel != null) {
            progressLabel.setText(progressText+ "/" + durationText);
            progressBar.setMax(duration);
            progressBar.setProgress(progress);
        }

    }

    @Override
    public void onQueueChanged(final List<QueueupTrack> queue) {
        mTrackListAdapter.updateTrackList(queue);
    }

    @Override
    public void onPlayerReady() {
        setProgressBar(View.GONE);
    }


    public void maybeShowNewTrack() {
        if (mActivity.getShowNewTrackAndReset()) {
            mTrackList.setSelection(mTrackList.getCount() - 1);
        }
    }

    public boolean isUserAdmin(String userId) {
        return (mQueueupClient.getUserId() != null && mQueueupClient.getUserId().equals(userId));

    }

    @Override
    public String getPlaylistId() {
        return mPlaylistId;
    }


    public class TrackListAdapter extends BaseAdapter {

        private Context mContext;
        private String mPlaylistId;
        private List<QueueupTrack> mTrackList;
        private int mResource;

        public TrackListAdapter(Context context, List<QueueupTrack> tracks,  int resource) {
            mContext = context;
            mTrackList = tracks;
            mResource = resource;
        }

        public void updateTrackList(List<QueueupTrack> list) {
            mTrackList = list;

            /* Calls are going to be from different asynchronous threads, so to be safe, run on main thread */
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
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
            final QueueupTrack track = mTrackList.get(position);

            if (convertView == null) {
                trackView = LayoutInflater.from(mContext).inflate(mResource, parent, false);

            } else {
                trackView = convertView;

            }

            /* Titles */
            TextView title = (TextView) trackView.findViewById(R.id.track_list_item_name);
            TextView artist = (TextView) trackView.findViewById(R.id.track_list_item_artist);

            title.setText(track.track.name);
            artist.setText(track.track.artists.get(0).name);

            /* Voting views */
            View votes = trackView.findViewById(R.id.track_votes);
            TextView votesCount = (TextView) votes.findViewById(R.id.track_votes_count);
            ImageView votesImage = (ImageView) votes.findViewById(R.id.track_votes_image);

            votesCount.setText(String.valueOf(track.votes));

            votes.setBackgroundResource(R.drawable.background_transparent);

            boolean userIsVoter = track.voters.contains(mActivity.getCurrentUserId());

            if (userIsVoter) {
                votesImage.setImageResource(R.drawable.ic_action_keyboard_arrow_up_white_36);
            } else {
                votesImage.setImageResource(R.drawable.ic_action_keyboard_arrow_up_grey_36);
            }

            /* Call out to vote */
            votes.setOnTouchListener(new OnVoteTouchListener(track.id, userIsVoter));

            List<String> imageUrls = track.track.album.imageUrls;
            ImageView image = (ImageView) trackView.findViewById(R.id.track_list_item_image);

            Picasso.with(mContext).load(imageUrls.get(0)).into(image);

            return trackView;
        }
    }

    private class OnVoteTouchListener implements View.OnTouchListener {

        private String trackId;
        private boolean currentVote;

        public OnVoteTouchListener(String trackId, boolean currentVote){
            this.trackId = trackId;
            this.currentVote = currentVote;
        }

        @Override
        public boolean onTouch(final View view, final MotionEvent event) {
            final QueueupClient client = mActivity.getQueueupClient();

            final ImageView imageView = (ImageView) view.findViewById(R.id.track_votes_image);

            /* Set the background and icon to indicate a press */
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        view.setBackgroundColor(getResources().getColor(R.color.primary_dark_material_light));
                        imageView.setImageDrawable(mActivity.getResources().getDrawable(R.drawable.ic_action_keyboard_arrow_up_36));
                    }
                });

                /* Send request to update vote */
                client.playlistVoteOnTrack(mPlaylistId, trackId, !currentVote, new Queueup.CallReceiver<QueueupPlaylist>() {
                    @Override
                    public void onResult(final QueueupPlaylist result) {
                        mTrackListAdapter.updateTrackList(result.tracks);
                    }

                    @Override
                    public void onException(Exception e) {
                        mActivity.toast(e.getMessage());
                    }
                });
                return true;
            /* Reset the button */
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                view.setBackground(getResources().getDrawable(R.drawable.background_transparent));
                imageView.setImageDrawable(mActivity.getResources().getDrawable(R.drawable.ic_action_keyboard_arrow_up_grey_36));
                return true;
            }
            return false;
        }
    }

}
