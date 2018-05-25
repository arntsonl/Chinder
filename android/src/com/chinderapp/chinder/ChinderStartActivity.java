package com.chinderapp.chinder;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.parse.ParseAnalytics;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


public class ChinderStartActivity extends Activity {
    private boolean mAppActiveServiceBound = false;
    private EventPollService mAppActiveService = null;
    private ServiceConnection mAppActiveConnection = new ServiceConnection() {
        public void onServiceConnected( ComponentName className, IBinder service ) {
            mAppActiveService = ( (EventPollService.AppActiveBinder) service ).getService();
        }
        public void onServiceDisconnected( ComponentName className ) {
            mAppActiveService = null;
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        mAppActiveServiceBound = bindService( new Intent( this, EventPollService.class ), mAppActiveConnection, Context.BIND_AUTO_CREATE );
    }

    @Override
    public void onStop() {
        super.onStop();
        if( mAppActiveServiceBound ) {
            unbindService( mAppActiveConnection );
            mAppActiveServiceBound = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chinder_start);
        getActionBar().hide(); // hide action bar on this page

        ParseAnalytics.trackAppOpenedInBackground(getIntent());

        checkAuthentication();
    }

    public void signUpOnClick(View v) {
        Intent intent = new Intent(this, ChinderSignUpActivity.class);
        startActivity(intent);
    }

    public void loginOnClick(View v) {
        Intent intent = new Intent(this, ChinderLoginActivity.class);
        startActivity(intent);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    private void checkAuthentication() {
//        // check to see if they are already logged in, if so go straight to the match screen
        String session = ((ChinderApplication)getApplication()).getPreference("session");
        if ( session.equals("") == false ) {
            // do an async request to see if our session is still valid
            AsyncHttpClient client = new AsyncHttpClient();
            RequestParams params = new RequestParams();
            params.put("session", session);
            client.post(new UtilityClass().getServer() + "users/authenticate", params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                    // called when response HTTP status is "200 OK"
                    // parse response as JSON
                    String responseStr = new String(response);
                    try {
                        JSONObject reader = new JSONObject(responseStr);
                        Boolean success = reader.getBoolean("success");
                        if ( success == true ) {
                            new UtilityClass().savePreferences(reader, (ChinderApplication)getApplication());
                            Intent intent = new Intent(ChinderStartActivity.this, ChinderMatchScreenActivity.class);
                            intent.setFlags(intent.getFlags());
                            startActivity(intent);
                            finish();
                        }
                    } catch ( JSONException e ){
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] response, Throwable error) {
                    // Do nothing on home page
                }
            });
        }
    }


}
