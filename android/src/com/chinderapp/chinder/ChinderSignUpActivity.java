package com.chinderapp.chinder;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class ChinderSignUpActivity extends Activity {

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
        setContentView(R.layout.activity_chinder_sign_up);

        ActionBar supportActionBar = this.getActionBar();
        supportActionBar.setDisplayHomeAsUpEnabled(true);
        supportActionBar.setTitle("Sign Up");

        // Set our privacy policy and terms of use HTML
        TextView legalText = (TextView)findViewById(R.id.signUpLegalText);
        legalText.setText(Html.fromHtml(
                "By creating an account you acknowledge to the " +
                "<a href='http://www.chinderapp.com/terms' target='_blank'" +
                "style='color:#72efd2'>Terms of use</a> and you acknowledge " +
                "that you have read the <a href='http://www.chinderapp.com/privacy'"+
                " target='_blank' style='color:#72efd2'>Privacy Policy </a>."));
        legalText.setMovementMethod(LinkMovementMethod.getInstance());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chinder_sign_up, menu);
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

    public void signupSubmitOnClick(View v) {
        // Get our values to send
        EditText firstnameEditText = (EditText) findViewById(R.id.firstnameEditText);
        EditText emailEditText = (EditText) findViewById(R.id.emailAddressEditText);
        EditText passwordText = (EditText) findViewById(R.id.passwordEditText);
        Button birthdayButton = (Button) findViewById(R.id.signupBirthdayButton);

        if ( firstnameEditText.getText().length()==0 ||
                emailEditText.getText().length()==0 ||
                passwordText.getText().length()==0 ||
                birthdayButton.getText().equals("SELECT BIRTHDAY") )
        {
            Context context = getApplicationContext();
            CharSequence text = "Please complete all fields";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return;
        }

        RequestParams params = new RequestParams();
        params.put("firstname",firstnameEditText.getText());
        params.put("birthday",birthdayButton.getText());
        params.put("email",emailEditText.getText());
        params.put("password", passwordText.getText());
        params.put("geolocation", ((ChinderApplication)getApplication()).getPreference("geolocation"));

        AsyncHttpClient client = new AsyncHttpClient();
        client.post(new UtilityClass().getServer() + "users/signup", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                // called when response HTTP status is "200 OK"
                // parse response as JSON
                String responseStr = new String(response);
                try {
                    JSONObject reader = new JSONObject(responseStr);
                    Boolean success = reader.getBoolean("success");
                    if ( success == true ) {
                        String session = reader.getString("session");
                        ((ChinderApplication)getApplication()).setPreference("session", session);
                        String firstname = reader.getString("firstname");
                        ((ChinderApplication)getApplication()).setPreference("firstname", firstname);
                        String birthday = reader.getString("birthday");
                        ((ChinderApplication)getApplication()).setPreference("birthday", birthday);
                        String email = reader.getString("email");
                        ((ChinderApplication)getApplication()).setPreference("email", email);
                        String photo = reader.getString("photo");
                        ((ChinderApplication)getApplication()).setPreference("photo", photo);
                        String geolocation = reader.getString("geolocation");
                        ((ChinderApplication)getApplication()).setPreference("geolocation", geolocation);
                        Intent intent = new Intent(ChinderSignUpActivity.this, ChinderMatchScreenActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                    else {
                        String message = reader.getString("message");
                        // okay its not this
                        Context context = getApplicationContext();
                        int duration = Toast.LENGTH_SHORT;
                        Toast toast = Toast.makeText(context, message, duration);
                        toast.show();
                    }
                } catch ( JSONException e ){
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

    String[] monthsArray = {"January", "February", "March", "April"
            , "May", "June", "July", "August", "September"
            , "October", "November", "December"};


    public void selectBirthdayOnClick(View v){
        // Process to get Current Date
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        // Launch Date Picker Dialog
        DatePickerDialog dpd = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year,
                                  int monthOfYear, int dayOfMonth) {
                String buttonText = monthsArray[monthOfYear] + " " +
                        String.valueOf(dayOfMonth) + ", " + String.valueOf(year);

                Button birthdayButton = (Button) findViewById(R.id.signupBirthdayButton);
                birthdayButton.setText(buttonText);
            }
        }, year, month, day);
        dpd.show();
    }
}
