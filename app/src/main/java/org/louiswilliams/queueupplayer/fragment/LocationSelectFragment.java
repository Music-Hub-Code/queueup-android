package org.louiswilliams.queueupplayer.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import org.louiswilliams.queueupplayer.R;
import org.louiswilliams.queueupplayer.activity.MainActivity;
import org.louiswilliams.queueupplayer.queueup.LocationUpdateListener;
import org.louiswilliams.queueupplayer.queueup.QueueUp;
import org.louiswilliams.queueupplayer.queueup.objects.QueueUpPlaylist;

public class LocationSelectFragment extends Fragment implements OnMapReadyCallback, LocationUpdateListener {

    public static final String ARG_PLAYLIST_ID = "playlist_id";
    public static final String ARG_PLAYLIST_NAME = "playlist_name";
    public static final String ARG_ACTION = "action";
    public static final String ACTION_MOVE = "move";
    public static final String ACTION_CREATE = "create";

    MainActivity mActivity;
    View mView;
    GoogleMap googleMap;
    String mAction;
    String mPlaylistId;
    String mPlaylistName;

    @Override
    public void onAttach(Context activity) {
        mActivity = (MainActivity) activity;
        mActivity.setTitle("Current Location");
        super.onAttach(activity);
    }

    @Override
    public void onAttach(Activity activity) {
        mActivity = (MainActivity) activity;
        mActivity.setTitle("Current Location");
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mPlaylistId = args.getString(ARG_PLAYLIST_ID);
        mPlaylistName = args.getString(ARG_PLAYLIST_NAME);
        mAction = args.getString(ARG_ACTION);
        if (mAction == null) {
            throw new IllegalStateException("No action sent!");
        }
        if (mAction.equals(ACTION_CREATE) && mPlaylistName  == null) {
            throw  new IllegalStateException("create requires playlist_name");
        }
        if (mAction.equals(ACTION_MOVE) && mPlaylistId == null) {
            throw  new IllegalStateException("move requires playlist_id");
        }

        if (!isGooglePlayServicesAvailable()) {
            mActivity.toast("Google Play Services not Installed");
            mActivity.getFragmentManager().popBackStack();
        } else if (!mActivity.locationPermissionGranted(true) || !mActivity.isLocationEnabled(true)) {
            mActivity.getFragmentManager().popBackStack();
        }
        mActivity.getLocationListener().startUpdates();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_location_select, container, false);

        Button button = (Button) mView.findViewById(R.id.set_location_button);
        TextView message = (TextView) mView.findViewById(R.id.set_location_message);
        if (mAction.equals(ACTION_MOVE)) {
            message.setText(R.string.current_location_move);
        } else if (mAction.equals(ACTION_CREATE)){
            message.setText(R.string.current_location_create);
        }

        FragmentManager fragmentManager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            fragmentManager = getChildFragmentManager();
        } else {
            fragmentManager = getFragmentManager();
        }

        MapFragment mapFragment = (MapFragment) fragmentManager.findFragmentById(R.id.google_map);
        mapFragment.getMapAsync(this);

        if (mAction.equals(ACTION_MOVE)) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    relocatePlaylist();
                }
            });
        } else if (mAction.equals(ACTION_CREATE)) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mActivity.getFragmentManager().popBackStack();
                    mActivity.doCreatePlaylist(mPlaylistName, mActivity.getLocationListener().getCurrentBestLocation());
                }
            });
        }

        return mView;
    }


    public void relocatePlaylist() {
        mActivity.getLocationListener().stopUpdates();

        Location location = mActivity.getLocationListener().getCurrentBestLocation();
        mActivity.getQueueUpClient().playlistRelocate(mPlaylistId, location, new QueueUp.CallReceiver<QueueUpPlaylist>() {

            @Override
            public void onResult(QueueUpPlaylist result) {
                /* Recreate the fragment without adding it to the back stack */
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.toast("Moved successfully");
                        mActivity.getFragmentManager().popBackStack();
                    }
                });
            }

            @Override
            public void onException(Exception e) {
                Log.e(QueueUp.LOG_TAG, e.getMessage());
                mActivity.toast(e.getMessage());
                mActivity.getFragmentManager().popBackStack();
            }
        });
    }


    @Override
    public void onLocation(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(
                CameraPosition.fromLatLngZoom(latLng, 15));
        googleMap.moveCamera(cameraUpdate);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.setMyLocationEnabled(true);
        mActivity.getLocationListener().getLocationUpdates(this);
    }

    @Override
    public void onDestroy() {
        mActivity.getLocationListener().stopLocationUpdates(this);
        mActivity.getLocationListener().stopUpdates();

        super.onDestroy();
    }

    private boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(mActivity);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(status, mActivity, 0).show();
            return false;
        }
    }

}
