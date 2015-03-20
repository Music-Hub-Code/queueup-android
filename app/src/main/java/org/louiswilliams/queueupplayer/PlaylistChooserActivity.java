package org.louiswilliams.queueupplayer;

import android.app.ListActivity;
import android.content.CursorLoader;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaylistChooserActivity extends ActionBarActivity {

    private static final String urlRoot = "http://q.louiswilliams.org";
    private static final String LOG_TAG = "QUEUEUP";
    private ListView playlistList;
    private List<Map<String, String>> names;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_chooser);

        playlistList = (ListView) findViewById(R.id.playlist_list);

        playlistList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), PlayerActivity.class);
                Map<String, String> playlist = names.get(position);
                Log.d(LOG_TAG, "Using playlist ID: " + playlist.get("id"));
                intent.putExtra("playlist_id", playlist.get("id"));

                startActivity(intent);
            }
        });


        new AsyncTask<String, Void, String>() {

            @Override
            protected String doInBackground(String... params) {

                try {
                    URL url = new URL(params[0]);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        BufferedReader input = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()), 8);
                        String line = null;
                        StringBuilder result = new StringBuilder();
                        while ((line = input.readLine()) != null) {
                            result.append(line);
                        }
                        return result.toString();
                    } else {
                        Log.d(LOG_TAG, "Response code: " + urlConnection.getResponseCode());
                        return null;
                    }
                } catch (MalformedURLException e) {
                    Log.d(LOG_TAG, e.getMessage());
                    return null;
                } catch (IOException e) {
                    Log.d(LOG_TAG, e.getMessage());
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String result) {
                try {
                    JSONObject json = new JSONObject(result);
                    listFromJson(json);
                } catch (JSONException e) {
                    Log.d(LOG_TAG, "JSON Problem: " + e.getMessage());
                }

            }
        }.execute(urlRoot + "/api/playlists");

    }

    private void listFromJson(JSONObject json) {
        String[] from = {"name", "track", "id"};
        int[] to = {R.id.playlist_list_item_title, R.id.playlist_list_item_track, R.id.playlist_list_item_id};

        try {
            JSONArray playlists = json.getJSONArray("playlists");
            names = new ArrayList<Map<String, String>>();
            CursorLoader loader = new CursorLoader(this);
            for (int i =0; i < playlists.length(); i++) {
                JSONObject playlist = playlists.getJSONObject(i);
                HashMap<String,String> name = new HashMap<String,String>();
                name.put("name", playlist.getString("name"));
                name.put("id", playlist.getString("_id"));
                name.put("track", playlist.getJSONObject("current").getString("name") + " by " + playlist.getJSONObject("current").getJSONArray("artists").getJSONObject(0).getString("name"));
                names.add(name);
            }
            SimpleAdapter adapter = new SimpleAdapter(this, names, R.layout.playlist_item, from, to);
            playlistList.setAdapter(adapter);
        } catch (JSONException e) {
            Log.d(LOG_TAG, e.getMessage());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_playlist_chooser, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
