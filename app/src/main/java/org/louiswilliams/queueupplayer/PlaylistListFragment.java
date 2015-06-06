package org.louiswilliams.queueupplayer;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import queueup.Queueup;
import queueup.QueueupClient;
import queueup.objects.QueueupPlaylist;
import queueup.objects.SpotifyAlbum;

public class PlaylistListFragment extends Fragment {

    private GridView playlistGrid;
    private List<QueueupPlaylist> mPlaylists;
    private QueueupClient client;

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);

        populateList();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist_list, container, false);

        client = ((MainActivity) getActivity()).getQueueupClient();

        playlistGrid = (GridView) view.findViewById(R.id.playlist_grid);

        playlistGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                QueueupPlaylist playlist = mPlaylists.get(position);
                Log.d(Queueup.LOG_TAG, "Using playlist ID: " + playlist.id);

                MainActivity main = (MainActivity) getActivity();
                main.showPlaylistFragment(playlist.id);
            }
        });

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().setTitle("Hot Playlists");
            }
        });

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private void populateList() {
        Log.d(Queueup.LOG_TAG, "populating list...");
        client.playlistGetList(new Queueup.CallReceiver<List<QueueupPlaylist>>() {
            @Override
            public void onResult(List<QueueupPlaylist> playlists) {
                Log.d(Queueup.LOG_TAG, "Playlist all success");
                mPlaylists = playlists;

                final PlaylistGridAdapter adapter = new PlaylistGridAdapter(getActivity(), mPlaylists, R.layout.playlist_item);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        playlistGrid.setAdapter(adapter);
                    }
                });

            }

            @Override
            public void onException(Exception e) {
                Log.e(Queueup.LOG_TAG, "Failed to get playlist list: " + e.getMessage());
            }
        });
    }

    class PlaylistGridAdapter extends BaseAdapter {

        private Context mContext;
        private List<QueueupPlaylist> mPlaylists;
        private int mResource;

        public PlaylistGridAdapter(Context c, List<QueueupPlaylist> playlists, int resource) {
            mContext = c;
            mPlaylists = playlists;
            mResource = resource;
        }

        @Override
        public int getCount() {
            return mPlaylists.size();
        }

        @Override
        public Object getItem(int position) {
            return mPlaylists.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View playlistItem;
            QueueupPlaylist playlist = mPlaylists.get(position);

            if (convertView == null) {
                View v = LayoutInflater.from(mContext).inflate(mResource, parent, false);
                playlistItem = v;

                TextView title = (TextView) playlistItem.findViewById(R.id.playlist_list_item_title);
                title.setText(playlist.name);

                if (playlist.current != null && playlist.current.album != null && playlist.current.album.imageUrls != null) {
                    List<String> imageUrls = playlist.current.album.imageUrls;
                    if (imageUrls.size() > 0) {
                        ImageView image = (ImageView) v.findViewById(R.id.playlist_list_item_image);
                        Picasso.with(mContext).load(imageUrls.get(0)).into(image);
                    }
                }
            } else {
                playlistItem = convertView;
            }


            return playlistItem;
        }
    }
}
