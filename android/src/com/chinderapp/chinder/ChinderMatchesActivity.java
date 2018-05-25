package com.chinderapp.chinder;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class ChinderMatchesActivity extends Activity {

    private BroadcastReceiver receiver;

    private List<List<String>> userProfiles;
    private MatchAdapter matchAdapter;
    private ListView adapterView;

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
        refreshHistory();
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
        setContentView(R.layout.activity_chinder_matches);

        ActionBar supportActionBar = this.getActionBar();
        supportActionBar.setDisplayHomeAsUpEnabled(true);
        supportActionBar.setTitle("Matches");

        // Register our service receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction("NEW_MESSAGE");
        filter.addAction("NEW_MATCH");
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.matches("NEW_MESSAGE")) {
                    Toast.makeText(context, "New message!", Toast.LENGTH_SHORT);
                } else if (action.matches("NEW_MATCH")) {
                    Toast.makeText(context, "New match!", Toast.LENGTH_SHORT);
                    refreshHistory();
                }
            }
        };
        registerReceiver(receiver, filter);

        //add the view via xml or programmatically
        userProfiles = new ArrayList<List<String>>();
        adapterView = (ListView) findViewById(R.id.android_chinder_matches);
        matchAdapter = new MatchAdapter(this, userProfiles);
        adapterView.setAdapter(matchAdapter);
        adapterView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // debug
                Intent intent = new Intent(ChinderMatchesActivity.this, ChinderMessengerActivity.class);
                Bundle b = new Bundle();
                b.putString("to", matchAdapter.getItem(position).get(3)); // to id
                b.putString("name", matchAdapter.getItem(position).get(1)); // name of the match
                intent.putExtras(b); //Put your id to your next Intent
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chinder_matches, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home) {
            // API 5+ solution
            onBackPressed();
            return true;
        }
        else if (id == R.id.action_edit_profile){
            Intent intent = new Intent(this, ChinderProfileActivity.class);
            intent.setFlags(intent.getFlags() );
            startActivity(intent);
            return true;
        }
        else if (id == R.id.action_about_chinder){
            // open a new browser to http://chinder.com/
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.chinderapp.com"));
            startActivity(browserIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    public void refreshHistory() {
        RequestParams params = new RequestParams();
        params.put("session", ((ChinderApplication)getApplication()).getPreference("session"));
        AsyncHttpClient client = new AsyncHttpClient();
        RequestHandle post = client.post(new UtilityClass().getServer() + "match/history", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {

                // called when response HTTP status is "200 OK"
                // parse response as JSON
                String responseStr = new String(response);
                try {
                    // Follow me - http://stackoverflow.com/questions/28713376/how-to-set-image-on-swipcard

                    JSONObject reader = new JSONObject(responseStr);
                    Boolean success = reader.getBoolean("success");
                    if (success == true) {
                        JSONArray users = reader.getJSONArray("users");

                        userProfiles.clear(); // clear out our user profiles

                        ActionBar supportActionBar = ChinderMatchesActivity.this.getActionBar();
                        supportActionBar.setTitle(users.length() + " Matches");

                        for (Integer i = 0; i < users.length(); i++) {
                            JSONObject user = users.getJSONObject(i);
                            String firstname = user.getString("firstname");
                            if (firstname.length() > 12) {
                                firstname = firstname.substring(0, 12) + "...";
                            }
                            Integer age = user.getInt("age");
                            String photo = user.getString("photo");
                            String matchedOn = user.getString("updatedAt");
                            String formattedDate;
                            try {
                                SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                                sf.setTimeZone(TimeZone.getTimeZone("UTC"));
                                Date matchedDate = sf.parse(matchedOn);
                                formattedDate = new SimpleDateFormat("MM/dd/yyyy").format(matchedDate);
                            } catch ( Exception e ) {
                                formattedDate = "error";
                            }
                            String nameOut = firstname + ", " + age.toString();
                            String dateOut = "Matched on " + formattedDate;
                            String id = user.getString("id");
                            List<String> currentUser = new ArrayList<String>();
                            currentUser.add(photo);
                            currentUser.add(nameOut);
                            currentUser.add(dateOut);
                            currentUser.add(id);
                            currentUser.add(((ChinderApplication)getApplication()).getPreference(id));
                            userProfiles.add(currentUser);
                        }

                        matchAdapter.notifyDataSetChanged();
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
            }
        });
    }
}
