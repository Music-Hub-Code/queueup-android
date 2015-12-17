package org.louiswilliams.queueupplayer.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
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
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.gc.materialdesign.views.ProgressBarDeterminate;
import com.squareup.picasso.Picasso;

import org.louiswilliams.queueupplayer.R;
import org.louiswilliams.queueupplayer.activity.InviteContactsActivity;
import org.louiswilliams.queueupplayer.activity.MainActivity;
import org.louiswilliams.queueupplayer.queueup.PlaybackReceiver;
import org.louiswilliams.queueupplayer.queueup.PlaylistClient;
import org.louiswilliams.queueupplayer.queueup.PlaylistListener;
import org.louiswilliams.queueupplayer.queueup.QueueUp;
import org.louiswilliams.queueupplayer.queueup.api.QueueUpClient;
import org.louiswilliams.queueupplayer.queueup.objects.QueueUpPlaylist;
import org.louiswilliams.queueupplayer.queueup.objects.QueueUpStateChange;
import org.louiswilliams.queueupplayer.queueup.objects.QueueUpTrack;
import org.louiswilliams.queueupplayer.queueup.objects.SpotifyArtist;
import org.louiswilliams.queueupplayer.queueup.objects.SpotifyTrack;

import java.util.List;


public class PlaylistFragment extends Fragment implements PlaylistListener {

    private ListView mTrackList;
    private PlaylistClient mPlaylistClient;
    private QueueUpClient mQueueUpClient;
    private QueueUpPlaylist mThisPlaylist;
    private String mPlaylistId;
    private TrackListAdapter mTrackListAdapter;
    private View mView;

    private MainActivity mActivity;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_playlist, container, false);
        mPlaylistId = getArguments().getString("playlist_id");

        Log.d(QueueUp.LOG_TAG, "Loading playlist" + mPlaylistId);

        /* Get the client*/
        mQueueUpClient = mActivity.getQueueUpClient();

        /* Get the playlist data to initially populate the view */
        mQueueUpClient.playlistGet(mPlaylistId, new QueueUp.CallReceiver<QueueUpPlaylist>() {
            @Override
            public void onResult(QueueUpPlaylist result) {
                Log.d(QueueUp.LOG_TAG, result.toString());

                mThisPlaylist = result;

                mActivity.invalidateOptionsMenu();
                populateView(mThisPlaylist);

            }

            @Override
            public void onException(Exception e) {
                mActivity.toast(e.getMessage());
                Log.e(QueueUp.LOG_TAG, "Problem getting playlist: " + e.getMessage());
            }
        });


        return mView;
    }


    @Override
    public void onCreateOptionsMenu (Menu menu, MenuInflater menuInflater) {
        if (mThisPlaylist != null && isUserOwner(mThisPlaylist.adminId)) {
            menuInflater.inflate(R.menu.menu_playlist_admin, menu);
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
            case R.id.action_playlist_relocate:
                showRelocateDialog();
                return true;
            case R.id.action_playlist_invite:
                Intent inviteIntent = new Intent(mActivity.getBaseContext(), InviteContactsActivity.class);

                startActivity(inviteIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroyView() {
        /* Make sure the activity knows that there is no playlist listener anymore  */
        if (mActivity.getPlaybackController() != null) {
            mActivity.getPlaybackController().removePlaylistListener(this);
        }

        unsubscribeAsClient();

        super.onDestroyView();
    }

    private void populateView(final QueueUpPlaylist playlist) {
        List<QueueUpTrack> tracks = playlist.tracks;
        String userId = playlist.adminId;
        final boolean isAdmin = isUserOwner(userId);

        mTrackListAdapter = new TrackListAdapter(mActivity, tracks, R.layout.track_item);
        mTrackList = (ListView) mView.findViewById(R.id.track_list);

        final View playlistHeader, playlistFooter;
        final ImageButton addTrackButton = (ImageButton) mView.findViewById(R.id.add_track_button);

        addTrackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(QueueUp.LOG_TAG, "Add track");
                mActivity.showAddTrackFragment(mPlaylistId);
            }
        });



        playlistHeader = mActivity.getLayoutInflater().inflate(R.layout.playlist_player_header, null);
        playlistFooter = mActivity.getLayoutInflater().inflate(R.layout.playlist_footer, null);

        /* If the current playlist playlist is this one, show the controls */
        if (currentPlaylistIsPlaying()) {
            showPlaylistControls(playlistHeader, playlist.playing);
            setProgressBar(View.GONE);
            mActivity.getPlaybackController().addPlaylistListener(PlaylistFragment.this);

        } else {

            subscribeAsClient();

            final Button playHereButton = (Button) playlistHeader.findViewById(R.id.play_here_button);

            /* If the user is the admin of the current playlist, give the option to play, otherwise say who owns it */
            if (isAdmin) {

                View.OnClickListener playHereListener;

                /* Don't allow playing unless there's a current track */
                if (playlist.current == null) {

                    playHereListener = new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mActivity.toast("Add tracks to play!");
                        }
                    };
                } else {


                    /* Listen to the "play here" button */
                    playHereListener = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            /* Unsubscribe as regular client */
                            unsubscribeAsClient();

                            /* Show the progress bar */
                            setProgressBar(View.VISIBLE);

                            /* Prevent the button from being pressed again*/
                            playHereButton.setOnClickListener(null);

                            /* Tell the activity to subscribe to this playlist and launch the player */
                            mActivity.subscribePlaylistPlayer(mPlaylistId, PlaylistFragment.this);

                            /* Insert the playlist controls */
                            showPlaylistControls(playlistHeader, playlist.playing);


                        }
                    };
                }

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
        final TextView trackArtist = (TextView) playlistHeader.findViewById(R.id.playlist_current_artist);

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                mActivity.setTitle(playlist.name);

                if (playlist.current != null) {

                    if (playlist.current.album != null && playlist.current.album.imageUrls != null && playlist.current.album.imageUrls.size() > 0) {
                        Picasso.with(mActivity).load(playlist.current.album.imageUrls.get(0)).into(albumArt);
                        albumArt.setPadding(0,0,0,0);
                        albumArt.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                showTrackOptionsDialog(null, playlist.current, false);
                            }
                        });
                    }

                    SpotifyArtist artist = playlist.current.artists.get(0);
                    trackName.setText(playlist.current.name);
                    trackArtist.setText(artist.name);
                }

                albumArt.getLayoutParams().height = mTrackList.getWidth() - (mTrackList.getPaddingLeft() + mTrackList.getPaddingRight());
                albumArt.requestLayout();

                mTrackList.addHeaderView(playlistHeader, null, true);
                mTrackList.addFooterView(playlistFooter, null, false);
                mTrackList.setAdapter(mTrackListAdapter);


                if (currentPlaylistIsPlaying()) {
                    onTrackProgress(mActivity.getPlaybackController().getCurrentProgress(), mActivity.getPlaybackController().getCurrentDuration());
                }

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
        ImageButton stopButton = (ImageButton) playlistControls.findViewById(R.id.stop_playback_button);

        View.OnClickListener playButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Just invert the current playing status */
                QueueUpStateChange current =  mActivity.getPlaybackController().getCurrentState();

                /* Unless it's null, which means we should just play anyways */
                boolean updateToPlaying = (current == null || !current.playing);
                updateTrackPlaying(updateToPlaying);
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
            Log.d(QueueUp.LOG_TAG, "NULL!");
        }
    }

    private void updateTrackPlaying(boolean playing) {
        mActivity.getPlaybackController().updateTrackPlaying(playing);

    }

    private void updateCurrentTrack(final SpotifyTrack current) {
        final ImageView albumArt = (ImageView) mView.findViewById(R.id.playlist_image);
        final TextView trackName = (TextView) mView.findViewById(R.id.playlist_current_track);
        final TextView trackArtist = (TextView) mView.findViewById(R.id.playlist_current_artist);

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (albumArt != null) {
                    Picasso.with(mActivity).load(current.album.imageUrls.get(0)).into(albumArt);
                }

                trackName.setText(current.name);
                trackArtist.setText(current.artists.get(0).name);

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
        mQueueUpClient.playlistRename(mPlaylistId, newName, new QueueUp.CallReceiver<QueueUpPlaylist>() {

            @Override
            public void onResult(QueueUpPlaylist result) {
                /* Recreate the fragment without adding it to the back stack */
                mActivity.reloadCurrentFragment();
            }

            @Override
            public void onException(Exception e) {
                Log.e(QueueUp.LOG_TAG, e.getMessage());
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

    public void showRelocateDialog() {
        mActivity.getLocationListener().startUpdates();
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle("Move Playlist Here")
                .setMessage("Do you want to move the playlist to your current location?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mActivity.getLocationListener().stopUpdates();
                        relocatePlaylist();
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mActivity.getLocationListener().stopUpdates();
            }
        }).show();

    }

    public void showTrackDeleteDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        final String trackId = ((QueueUpTrack) mTrackListAdapter.getItem(position)).id;

        builder.setTitle("Remove Track")
                .setMessage("Are you sure you want to remove this track?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeTrack(trackId);
                    }
                }).setNegativeButton("No", null)
                .show();
    }

    public void showTrackOptionsDialog(final String trackId, final SpotifyTrack track, boolean showDelete) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);

        String[] trackOptions;

        if (showDelete) {
            trackOptions = new String[] {"Exit and open in Spotify", "Delete track"};
        } else {
            trackOptions = new String[] {"Exit and open in Spotify"};
        }

        builder.setTitle(track.name + " by " + track.artists.get(0).name)
            .setItems(trackOptions, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (i == 0) {
//                        openLink(track.uri);
                        showOpenInSpotifyDialog(track.uri);
                    } else if (i == 1) {
                        removeTrack(trackId);
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
                        openLink(uri);
                    }
                }).setNegativeButton("No", null)
                .show();
    }

    public void deletePlaylist() {
        mQueueUpClient.playlistDelete(mPlaylistId, new QueueUp.CallReceiver<Boolean>() {

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
                Log.e(QueueUp.LOG_TAG, e.getMessage());
                mActivity.toast(e.getMessage());
            }
        });
    }

    public void relocatePlaylist() {
        Location location = mActivity.getLocationListener().getCurrentBestLocation();
        mActivity.getQueueUpClient().playlistRelocate(mPlaylistId, location, new QueueUp.CallReceiver<QueueUpPlaylist>() {

            @Override
            public void onResult(QueueUpPlaylist result) {
                /* Recreate the fragment without adding it to the back stack */
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.toast("Moved successfully");
                    }
                });
            }

            @Override
            public void onException(Exception e) {
                Log.e(QueueUp.LOG_TAG, e.getMessage());
                mActivity.toast(e.getMessage());
            }
        });
    }

    public void removeTrack(String trackId) {
        mQueueUpClient.playlistDeleteTrack(mPlaylistId, trackId, new QueueUp.CallReceiver<QueueUpPlaylist>() {

            @Override
            public void onResult(QueueUpPlaylist result) {
                mTrackListAdapter.updateTrackList(result.tracks);
            }

            @Override
            public void onException(Exception e) {
                Log.e(QueueUp.LOG_TAG, e.getMessage());
                mActivity.toast(e.getMessage());
            }
        });
    }

    public boolean currentPlaylistIsPlaying() {
        return (mActivity.getPlaybackController() != null && mActivity.getPlaybackController().getPlaylistId().equals(mPlaylistId));
    }

    public void subscribeAsClient() {

        /* Create a temporary playlist client to subscribe to updates, independent of a player */
        mPlaylistClient = mQueueUpClient.getNewPlaylistClient(new QueueUp.CallReceiver<PlaylistClient>() {
            @Override
            public void onResult(PlaylistClient result) {

                result.addPlaylistListener(PlaylistFragment.this);
                result.subscribe(mPlaylistId);
            }

            @Override
            public void onException(Exception e) {
                Log.d(QueueUp.LOG_TAG, e.getMessage());
                mActivity.toast(e.getMessage());
            }
        }, new PlaybackReceiver() {
            @Override
            public void onPlaybackEnd() { }
        });

    }

    public void unsubscribeAsClient() {
        if (mPlaylistClient != null) {
            mPlaylistClient.removeAllPlaylistListeners();
            mPlaylistClient.disconnect();
            mPlaylistClient = null;
        }
    }

    public void openLink(String uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        startActivity(intent);
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
    public void onQueueChanged(final List<QueueUpTrack> queue) {
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

    public boolean isUserOwner(String userId) {
        return (mQueueUpClient.getUserId() != null && mQueueUpClient.getUserId().equals(userId));
    }

    public boolean userCanDeleteTrack(QueueUpTrack track) {
        if (isUserOwner(mThisPlaylist.adminId)) {
            return true;
        } else if (track.addedByUserId != null) {
            return mQueueUpClient.getUserId() != null && mQueueUpClient.getUserId().equals(track.addedByUserId);
        }
        return false;
    }

    @Override
    public String getPlaylistId() {
        return mPlaylistId;
    }


    public class TrackListAdapter extends BaseAdapter {

        private Context mContext;
        private List<QueueUpTrack> mTrackList;
        private int mResource;

        public TrackListAdapter(Context context, List<QueueUpTrack> tracks,  int resource) {
            mContext = context;
            mTrackList = tracks;
            mResource = resource;
        }

        public void updateTrackList(List<QueueUpTrack> list) {
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
        public View getView(final int position, View convertView, ViewGroup parent) {
            View trackView;
            final QueueUpTrack track = mTrackList.get(position);

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

            votes.setBackgroundColor(getResources().getColor(R.color.primary_material_dark));

            final boolean userIsVoter = track.voters.contains(mActivity.getCurrentUserId());

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

            trackView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showTrackOptionsDialog(track.id, track.track, userCanDeleteTrack(track));
                }
            });
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
            final QueueUpClient client = mActivity.getQueueUpClient();

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
                client.playlistVoteOnTrack(mPlaylistId, trackId, !currentVote, new QueueUp.CallReceiver<QueueUpPlaylist>() {
                    @Override
                    public void onResult(final QueueUpPlaylist result) {
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
                view.setBackgroundColor(getResources().getColor(R.color.primary_material_dark));
                imageView.setImageDrawable(mActivity.getResources().getDrawable(R.drawable.ic_action_keyboard_arrow_up_grey_36));
                return true;
            }
            return false;
        }
    }

}
