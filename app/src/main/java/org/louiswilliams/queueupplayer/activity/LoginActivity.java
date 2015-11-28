package org.louiswilliams.queueupplayer.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.louiswilliams.queueupplayer.R;
import org.louiswilliams.queueupplayer.queueup.QueueUp;
import org.louiswilliams.queueupplayer.queueup.QueueUpException;
import org.louiswilliams.queueupplayer.queueup.QueueUpStore;
import org.louiswilliams.queueupplayer.queueup.api.QueueUpClient;
import org.louiswilliams.queueupplayer.queueup.objects.QueueUpApiCredential;

import java.util.Arrays;

public class LoginActivity extends Activity {

    public static final int QUEUEUP_LOGIN_REQUEST_CODE = 2222;
    public static final int RESULT_LOGIN_FAILURE = 2;
    public static final String EXTRA_LOGIN_EXCEPTION = "EXTRA_LOGIN_EXCEPTION";
    public static final String EXTRA_DO_LOGIN = "EXTRA_DO_LOGIN";

    private CallbackManager callbackManager;
    private QueueUpStore mStore;
    private QueueUpClient mQueueupClient;
    private boolean isStateLogIn = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStore = QueueUpStore.with(this);
        String userId = mStore.getString(QueueUpStore.CLIENT_TOKEN);
        String clientToken = mStore.getString(QueueUpStore.USER_ID);

        try {
            mQueueupClient = new QueueUpClient(getApplicationContext(), userId, clientToken);
        } catch (QueueUpException e){
            Log.e(QueueUp.LOG_TAG, e.getMessage());
        }


        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();

        Intent intent = getIntent();

        /* If we want to show the login activity, and not bypass the display */
        if (!intent.getBooleanExtra(EXTRA_DO_LOGIN, false)) {

            setContentView(R.layout.activity_login);

            LoginButton loginButton = (LoginButton) findViewById(R.id.login_button);
            loginButton.setReadPermissions("email");
            loginButton.setReadPermissions("user_friends");

            final TextView nameText = (TextView) findViewById(R.id.user_name);
            final TextView emailText = (TextView) findViewById(R.id.email);
            final TextView passwordText = (TextView) findViewById(R.id.password);
            final TextView passwordConfText = (TextView) findViewById(R.id.password_conf);

            Button emailMultiButton = (Button) findViewById(R.id.login_multi_button);
            final Button emailLoginButton = (Button) findViewById(R.id.login_email_button);
            final Button emailRegisterButton = (Button) findViewById(R.id.register_email_button);


            emailLoginButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    nameText.setVisibility(View.GONE);
                    passwordConfText.setVisibility(View.GONE);

                    isStateLogIn = true;
                    view.setEnabled(false);
                    emailRegisterButton.setEnabled(true);
                }
            });

            emailRegisterButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    nameText.setVisibility(View.VISIBLE);
                    passwordConfText.setVisibility(View.VISIBLE);

                    isStateLogIn = false;
                    view.setEnabled(false);
                    emailLoginButton.setEnabled(true);
                }
            });

            emailMultiButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    String name = nameText.getText().toString();
                    String email = emailText.getText().toString();
                    String password = passwordText.getText().toString();
                    String passwordConf = passwordConfText.getText().toString();

                    if (isStateLogIn) {

                        if (email.isEmpty() || password.isEmpty()) {
                            Toast.makeText(LoginActivity.this, "Email and password cannot be empty", Toast.LENGTH_SHORT).show();
                        } else {
                            doEmailLogin(email, password);
                        }
                    } else {
                        if (email.isEmpty() || password.isEmpty() || name.isEmpty() || passwordConf.isEmpty()) {
                            Toast.makeText(LoginActivity.this, "No fields can be empty", Toast.LENGTH_SHORT).show();
                        } else {
                            if (!password.equals(passwordConf)) {
                                Toast.makeText(LoginActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                            } else {
                                doEmailRegister(nameText.getText().toString(), emailText.getText().toString(), passwordText.getText().toString());
                            }

                        }
                    }
                }
            });

        } else {
            LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "user_friends"));
        }


        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(final LoginResult loginResult) {
                Log.d(QueueUp.LOG_TAG, "ACCESS_TOKEN:" + loginResult.getAccessToken().getToken());

                doFacebookLogin(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Log.d(QueueUp.LOG_TAG, "Cancelled FB Login");

                finishAsCancelled();
            }

            @Override
            public void onError(FacebookException exception) {
                Log.e(QueueUp.LOG_TAG, exception.getMessage());

                finishWithException(exception);
            }
        });
    }


    private void doFacebookLogin(final AccessToken accessToken) {

        mQueueupClient.loginFacebook(accessToken.getToken(), new QueueUp.CallReceiver<QueueUpApiCredential>() {

            @Override
            public void onResult(QueueUpApiCredential result) {

                mStore.putString(QueueUpStore.USER_ID, result.userId);
                mStore.putString(QueueUpStore.CLIENT_TOKEN, result.clientToken);
                mStore.putString(QueueUpStore.FACEBOOK_ID, accessToken.getUserId());

                finishAsOk();
            }

            @Override
            public void onException(Exception e) {
                Log.e(QueueUp.LOG_TAG, "Problem logging in with Facebook: " + e.getMessage());

                finishWithException(e);
            }
        });

    }

    private void doEmailLogin(final String email, String password) {

        mQueueupClient.loginEmail(email, password, new QueueUp.CallReceiver<QueueUpApiCredential>() {
            @Override
            public void onResult(QueueUpApiCredential result) {

                mStore.putString(QueueUpStore.EMAIL_ADDRESS, email);
                mStore.putString(QueueUpStore.CLIENT_TOKEN, result.clientToken);
                mStore.putString(QueueUpStore.USER_ID, result.userId);

                finishAsOk();
            }

            @Override
            public void onException(Exception e) {
                finishWithException(e);
            }
        });

    }

    private void doEmailRegister(String name, final String email, String password) {
        mQueueupClient.register(name, email, password, new QueueUp.CallReceiver<QueueUpApiCredential>() {
            @Override
            public void onResult(QueueUpApiCredential result) {

                mStore.putString(QueueUpStore.EMAIL_ADDRESS, email);
                mStore.putString(QueueUpStore.CLIENT_TOKEN, result.clientToken);
                mStore.putString(QueueUpStore.USER_ID, result.userId);

                finishAsOk();

            }

            @Override
            public void onException(Exception e) {
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

    private String getDeviceId() {
        return Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        callbackManager.onActivityResult(requestCode, resultCode, intent);
    }

}
