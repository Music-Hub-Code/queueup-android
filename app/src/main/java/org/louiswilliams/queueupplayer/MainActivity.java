package org.louiswilliams.queueupplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void onPlayButton(View view) {
        Intent playerIntent = new Intent(getApplicationContext(), PlaylistChooserActivity.class);
        startActivity(playerIntent);
    }

    public void onQueueButton(View view) {
        Intent intent = new Intent(getApplicationContext(), QueueActivity.class);
        startActivity(intent);
    }

}
