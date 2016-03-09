package com.labsgn.githubnotifier.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.labsgn.githubnotifier.R;
import com.labsgn.githubnotifier.utlis.Constant;
import com.labsgn.githubnotifier.utlis.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by rhony on 08/03/16.
 */
public class LoginActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initComponent();

        // Allow connectivity in the main thread
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        if (getIntent().getBooleanExtra("SIGN_IN", false)) {
            attemptLogin();
        }

        if (getIntent().getBooleanExtra("SIGN_OUT", false)){
            SharedPreferences settings = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
            settings.edit().clear().apply();
            gitHubLogout();
        }
    }

    private void initComponent(){
        Button loginButton = (Button) findViewById(R.id.login_sign_in_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = getIntent().getData();
                if (uri == null) {
                    attemptLogin();
                }
            }
        });

        Button publicVersionButton = (Button) findViewById(R.id.login_skip_login_button);
        publicVersionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMain(Constant.GITHUB_PUBLIC_TOKEN);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // the intent filter defined in AndroidManifest will handle the return from ACTION_VIEW intent
        Uri uri = getIntent().getData();
        if (uri != null && uri.toString().startsWith(Constant.GITHUB_REDIRECT_URI)) {
            String code = uri.getQueryParameter("code");
            if (code != null) {
                Logger.log_e("LoginActivity", code);

                try {
                    String result = downloadUrl("https://github.com/login/oauth/access_token?client_id="
                            + Constant.GITHUB_CLIENT_PUBLIC_KEY + "&client_secret=" + Constant.GITHUB_CLIENT_SECRET_KEY + "&code=" + code);
                    Logger.log_i("LoginActivity",result);

                    String accessToken = result.substring(result.indexOf("=")+1, result.indexOf("&"));
                    Logger.log_i("LoginActivity",accessToken);
                    openMain(accessToken);

                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else if (uri.getQueryParameter("error") != null) {
                // show an error message here
                Logger.log_e("LoginActivity", uri.getQueryParameter("error"));
            }
        }
    }


    /**
     * Start Github OAuth Web View
     */
    public void attemptLogin() {
        SharedPreferences settings = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        settings.edit().clear().apply();
        Intent intent = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://github.com/login/oauth/authorize?client_id="
                        + Constant.GITHUB_CLIENT_PUBLIC_KEY + "&redirect_uri=" + Constant.GITHUB_REDIRECT_URI+"&scope=repo")
        );
        startActivity(intent);
    }


    /**
     * Start Github logout Web View
     */
    public void gitHubLogout() {
        Intent intent = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://github.com/logout")
        );
        startActivity(intent);
    }

    /**
     * Start MainActivity. The used GitHub token parameter is copied in SharedPreferences
     * @param accessToken
     */
    private void openMain(String accessToken) {
        SharedPreferences sharedPref = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("gitHubApiToken", accessToken);
        editor.apply();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    // Given a URL, establishes an HttpUrlConnection and retrieves
    // the web page content as a String, which it returns as  a string.
    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;
        try {
            HttpURLConnection conn = (HttpURLConnection) (new URL(myurl)).openConnection();
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            return convertStreamToString(conn.getInputStream());

        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
