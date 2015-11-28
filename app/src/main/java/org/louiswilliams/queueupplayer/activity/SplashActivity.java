package org.louiswilliams.queueupplayer.activity;

import android.content.Intent;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.louiswilliams.queueupplayer.R;
import org.louiswilliams.queueupplayer.queueup.QueueUp;
import org.louiswilliams.queueupplayer.queueup.QueueUpException;
import org.louiswilliams.queueupplayer.queueup.QueueUpStore;
import org.louiswilliams.queueupplayer.queueup.api.QueueUpClient;
import org.louiswilliams.queueupplayer.queueup.api.SpotifyTokenManager;
import org.louiswilliams.queueupplayer.queueup.objects.QueueUpApiCredential;

public class SplashActivity extends AppCompatActivity {

    QueueUpStore mStore;
    QueueUpClient mQueueupClient;
    final static int SPLASH_TIMEOUT = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStore = QueueUpStore.with(this);
        try {
            mQueueupClient = new QueueUpClient(getApplicationContext(), null, null);
        } catch (QueueUpException e) {
            Log.e(QueueUp.LOG_TAG, e.getMessage());
        }

        final String clientToken = mStore.getString(QueueUpStore.CLIENT_TOKEN);
        final String userId = mStore.getString(QueueUpStore.USER_ID);


        /* This should only occur on the first time launch or after a user fully logs out */
        if (userId == null && clientToken == null) {

            setContentView(R.layout.activity_spash);

            /* Delay for 3 seconds to emulate a splash screen */
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    doFirstTimeInit();
                }
            }, SPLASH_TIMEOUT);
        } else {
            continueToMain();
        }


    }

    public void doFirstTimeInit() {

        mQueueupClient.loginInit(getDeviceId(), new QueueUp.CallReceiver<QueueUpApiCredential>() {
            @Override
            public void onResult(QueueUpApiCredential result) {

                mStore.putString(QueueUpStore.CLIENT_TOKEN, result.clientToken);
                mStore.putString(QueueUpStore.USER_ID, result.userId);

                continueToMain();
            }

            @Override
            public void onException(Exception e) {
                finishWithException(e);
            }
        });
    }

    public void continueToMain() {
        Intent intent = new Intent(getBaseContext(), MainActivity.class);
        startActivity(intent);
    }

    private String getDeviceId() {
        return Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
    }


    private void finishWithException(final Exception exception) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getBaseContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(QueueUp.LOG_TAG, exception.getMessage());
            }
        });
    }


}
