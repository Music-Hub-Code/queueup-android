package org.louiswilliams.queueupplayer.activity;

import android.app.Activity;
import android.content.Intent;
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

import org.louiswilliams.queueupplayer.R;

import org.louiswilliams.queueupplayer.queueup.Queueup;
import org.louiswilliams.queueupplayer.queueup.api.QueueupClient;
import org.louiswilliams.queueupplayer.queueup.QueueupStore;
import org.louiswilliams.queueupplayer.queueup.objects.QueueupApiCredential;

public class LoginActivity extends Activity {

    public static final int QUEUEUP_LOGIN_REQUEST_CODE = 2222;
    public static final int RESULT_LOGIN_FAILURE = 2;
    public static final String EXTRA_LOGIN_EXCEPTION = "EXTRA_LOGIN_EXCEPTION";
    public static final String EXTRA_DO_LOGIN = "EXTRA_DO_LOGIN";

    private CallbackManager callbackManager;
    private QueueupStore mStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStore = QueueupStore.with(this);

        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();

        Intent intent = getIntent();

        /* If we are told to go ahead and do a login, bypassing displaying anything. */
        if (!intent.getBooleanExtra(EXTRA_DO_LOGIN, false)) {

            setContentView(R.layout.activity_login);

            LoginButton loginButton = (LoginButton) findViewById(R.id.login_button);
            loginButton.setReadPermissions("email");
            loginButton.setReadPermissions("user_friends");
        }



        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(final LoginResult loginResult) {
                Log.d(Queueup.LOG_TAG, "ACCESS_TOKEN:" + loginResult.getAccessToken().getToken());

                doLogin(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Log.d(Queueup.LOG_TAG, "Cancelled FB Login");

                finishAsCancelled();
            }

            @Override
            public void onError(FacebookException exception) {
                Log.e(Queueup.LOG_TAG, exception.getMessage());

                finishWithException(exception);
            }
        });
    }


    private void doLogin(final AccessToken accessToken) {
        QueueupClient.loginFacebook(accessToken.getToken(), new Queueup.CallReceiver<QueueupApiCredential>() {

            @Override
            public void onResult(QueueupApiCredential result) {

                mStore.putString(QueueupStore.CLIENT_TOKEN, result.clientToken);
                mStore.putString(QueueupStore.USER_ID, result.userId);
                mStore.putString(QueueupStore.FACEBOOK_ID, accessToken.getUserId());

                finishAsOk();
            }

            @Override
            public void onException(Exception e) {
                Log.e(Queueup.LOG_TAG, "Problem logging in with Facebook: " + e.getMessage());

                finishWithException(e);
            }
        });
    }

    private void finishWithException(Exception exception) {
        Intent i = new Intent();
        i.putExtra(EXTRA_LOGIN_EXCEPTION, exception);

        setResult(RESULT_LOGIN_FAILURE, i);
        finish();
    }

    private void finishAsCancelled() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void finishAsOk() {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        callbackManager.onActivityResult(requestCode, resultCode, intent);
    }

}
