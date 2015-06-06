package org.louiswilliams.queueupplayer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.github.nkzawa.socketio.client.Socket;

import java.util.logging.Level;

import queueup.Queueup;
import queueup.QueueupClient;
import queueup.objects.QueueupCredential;

public class LoginActivity extends Activity {

    private static final String STORE_NAME = "authStore";
    private CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidLoggingHandler.reset(new AndroidLoggingHandler());
        java.util.logging.Logger.getLogger(Socket.class.getName()).setLevel(Level.FINEST);

        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();


        SharedPreferences prefs =  getSharedPreferences(STORE_NAME, 0);

        if (prefs.getString("clientToken", null) != null && prefs.getString("userId", null) != null) {
            Log.d(Queueup.LOG_TAG, "Client already logged in...");
            goToMain();
        } else if (AccessToken.getCurrentAccessToken() != null) {
            Log.d(Queueup.LOG_TAG, "Client already had access token...");
            doLogin(AccessToken.getCurrentAccessToken());
        }

        setContentView(R.layout.activity_login);

        LoginButton loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.setReadPermissions("email");

        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(final LoginResult loginResult) {
                Log.d(Queueup.LOG_TAG, "ACCESS_TOKEN:" + loginResult.getAccessToken().getToken());

                doLogin(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Log.d(Queueup.LOG_TAG, "Cancelled FB Login");
            }

            @Override
            public void onError(FacebookException exception) {
                Log.e(Queueup.LOG_TAG, exception.getMessage());
            }
        });
    }

    private void goToMain() {
        Intent mainIntent = new Intent(getBaseContext(), MainActivity.class);
        startActivity(mainIntent);
        finish();
    }

    private void doLogin(final AccessToken accessToken) {
        QueueupClient.loginFacebook(accessToken.getToken(), new Queueup.CallReceiver<QueueupCredential>() {

            @Override
            public void onResult(QueueupCredential result) {
                final SharedPreferences prefs = getSharedPreferences(STORE_NAME, 0);

                prefs.edit().putString("clientToken", result.clientToken).commit();
                prefs.edit().putString("userId", result.userId).commit();

                goToMain();
            }

            @Override
            public void onException(Exception e) {
                Log.e(Queueup.LOG_TAG, "Problem logging in with Facebook: " + e.getMessage());
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        callbackManager.onActivityResult(requestCode, resultCode, intent);
    }

}
