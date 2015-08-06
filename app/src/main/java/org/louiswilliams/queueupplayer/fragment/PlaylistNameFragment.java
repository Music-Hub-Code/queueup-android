package org.louiswilliams.queueupplayer.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import org.louiswilliams.queueupplayer.R;
import org.louiswilliams.queueupplayer.activity.MainActivity;

import org.louiswilliams.queueupplayer.queueup.QueueUp;

public class PlaylistNameFragment extends DialogFragment {

    private String newPlaylistName;
    private String dialogTitle = "Playlist name";
    private String textContent = null;
    private MainActivity mainActivity;
    private EditText nameBox;
    private PlaylistNameListener playlistNameListener;


    public void setPlaylistNameListener(PlaylistNameListener playlistNameListener) {
        this.playlistNameListener = playlistNameListener;
    }

    public void setDialogTitle(String name) {
        dialogTitle = name;
    }

    public void setTextContent(String content) {
        textContent = content;
    }

    @Override
    public void onAttach(Activity activity) {
        if (activity instanceof MainActivity) {
            mainActivity = (MainActivity) activity;
        } else {
            throw new RuntimeException("Activity must be MainActivity, is " + activity.getClass().getName());
        }
        super.onAttach(activity);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        View dialogView = getActivity().getLayoutInflater().inflate(R.layout.fragment_create_playlist, null);

        nameBox = (EditText) dialogView.findViewById(R.id.playlist_name_box);

        if (textContent != null) {
            nameBox.append(textContent);
        }

        builder.setView(dialogView);
        builder.setMessage(dialogTitle);
        builder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                newPlaylistName = nameBox.getText().toString();

                if (playlistNameListener != null) {
                    playlistNameListener.onPlaylistCreate(PlaylistNameFragment.this);
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (playlistNameListener != null) {
                    playlistNameListener.onCancel();
                }
            }
        });


        mainActivity.showKeyboard();
        return builder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        Log.d(QueueUp.LOG_TAG, "Dismissing...");
        mainActivity.hideKeyboard();
        super.onDismiss(dialogInterface);
    }

    public String getPlaylistName() {
        return  newPlaylistName;
    }

    public interface PlaylistNameListener {
        void onPlaylistCreate(PlaylistNameFragment dialogFragment);
        void onCancel();
    }
}
