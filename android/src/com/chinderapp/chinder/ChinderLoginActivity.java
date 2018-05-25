package com.chinderapp.chinder;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class ChinderLoginActivity extends Activity {
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
        setContentView(R.layout.activity_chinder_login);

        ActionBar supportActionBar = this.getActionBar();
        supportActionBar.setDisplayHomeAsUpEnabled(true);
        supportActionBar.setTitle("Login");

//        // Set our privacy policy and terms of use HTML
//        TextView forgotText = (TextView)findViewById(R.id.forgotPasswordText);
//        forgotText.setText(Html.fromHtml(
//         "<a href='http://www.chinder.com/forgot' target='_blank' " +
//         "style='color:#72efd2'>Forgot Password?</a>"));
//        forgotText.setMovementMethod(LinkMovementMethod.getInstance());

        // Set our email if we have it saved in storage
        String email = ((ChinderApplication)getApplication()).getPreference("email");
        if ( email != null )
        {
            EditText emailEditText = (EditText)findViewById(R.id.usernameLoginEditText);
            emailEditText.setText(email);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chinder_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == android.R.id.home) {
            // API 5+ solution
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void loginSubmitOnClick(View v) {
        EditText emailEditText = (EditText)findViewById(R.id.usernameLoginEditText);
        EditText passwordText = (EditText)findViewById(R.id.passwordLoginEditText);

        if ( emailEditText.getText().length()==0 ||
                passwordText.getText().length()==0 )
        {
            Context context = getApplicationContext();
            CharSequence text = "Please complete all fields";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return;
        }

        RequestParams params = new RequestParams();
        params.put("email",emailEditText.getText());
        params.put("password", passwordText.getText());

        AsyncHttpClient client = new AsyncHttpClient();
        client.post(new UtilityClass().getServer() + "users/login", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                // called when response HTTP status is "200 OK"
                // parse response as JSON
                String responseStr = new String(response);
                try {
                    JSONObject reader = new JSONObject(responseStr);
                    Boolean success = reader.getBoolean("success");
                    if (success == true) {
                        new UtilityClass().savePreferences(reader, (ChinderApplication)getApplication());
                        Intent intent = new Intent(ChinderLoginActivity.this, ChinderMatchScreenActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        String message = reader.getString("message");
                        // okay its not this
                        Context context = getApplicationContext();
                        int duration = Toast.LENGTH_SHORT;
                        Toast toast = Toast.makeText(context, message, duration);
                        toast.show();
                    }
                } catch (JSONException e) {
                    // okay its not this
                    Context context = getApplicationContext();
                    CharSequence text = "Network error, please try again later";
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                Context context = getApplicationContext();
                CharSequence text = "Network error, please try again later";
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        });
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }
}
