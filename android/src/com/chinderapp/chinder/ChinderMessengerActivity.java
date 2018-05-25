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
import android.widget.ListView;
import android.widget.TextView;
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

public class ChinderMessengerActivity extends Activity {

    private BroadcastReceiver receiver;

    private List<List<String>> messageHistory;
    private MessengerAdapter messengerAdapter;
    private ListView adapterView;

    private String toId = null;

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
        setContentView(R.layout.activity_chinder_messenger);

        Bundle b = getIntent().getExtras();
        toId = b.getString("to");

        // Clear out any flags on this user
        ((ChinderApplication)getApplication()).deletePreference(toId);

        String name = b.getString("name");
        ActionBar supportActionBar = this.getActionBar();
        supportActionBar.setDisplayHomeAsUpEnabled(true);
        supportActionBar.setTitle(name);

        //add the view via xml or programmatically
        messageHistory = new ArrayList<List<String>>();
        adapterView = (ListView) findViewById(R.id.messengerList);
        messengerAdapter = new MessengerAdapter(this, messageHistory);
        adapterView.setAdapter(messengerAdapter);

        // Register our service receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction("NEW_MESSAGE");
        filter.addAction("NEW_MATCH");
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ( action.matches("NEW_MESSAGE") )
                    refreshHistory();
                else if ( action.matches("NEW_MATCH") )
                    Toast.makeText(context, "New match!", Toast.LENGTH_SHORT);
            }
        };
        registerReceiver(receiver, filter);

        refreshHistory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chinder_messenger, menu);
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

    public void sendMessageOnClick(View v) {
        TextView chatMessage = (TextView)findViewById(R.id.messengerTextField);
        String sendMessage = chatMessage.getText().toString();
        RequestParams params = new RequestParams();
        params.put("session", ((ChinderApplication)getApplication()).getPreference("session"));
        params.put("to", toId);
        params.put("message", sendMessage);
        AsyncHttpClient client = new AsyncHttpClient();
        RequestHandle post = client.post(new UtilityClass().getServer() + "messages/send", params, new AsyncHttpResponseHandler() {
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
                        // lets occupy the listview with our acquired users
                        TextView chatMessage = (TextView)findViewById(R.id.messengerTextField);
                        chatMessage.setText(""); // clear it
                        refreshHistory();
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

    public void refreshHistory() {
        RequestParams params = new RequestParams();
        params.put("session", ((ChinderApplication)getApplication()).getPreference("session"));
        params.put("to", toId);
        AsyncHttpClient client = new AsyncHttpClient();
        RequestHandle post = client.post(new UtilityClass().getServer() + "messages/history", params, new AsyncHttpResponseHandler() {
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
                        // Clear our list
                        messageHistory.clear();

                        // Check to see if this is from or two
                        String photo = reader.getString("photo");

                        JSONArray messages = reader.getJSONArray("messages");
                        for (Integer i = 0; i < messages.length(); i++) {
                            JSONObject message = messages.getJSONObject(i);
                            String text = message.getString("text");
                            String sent = message.getString("sent");
                            String formattedDate;
                            try {
                                SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                                sf.setTimeZone(TimeZone.getTimeZone("UTC"));
                                Date matchedDate = sf.parse(sent);
                                formattedDate = new SimpleDateFormat("hh:mm a").format(matchedDate);
                            } catch ( Exception e ) {
                                formattedDate = "error";
                            }
                            String dateOut = "Sent at " + formattedDate;
                            String origin = message.getString("origin");
                            List<String> messageList = new ArrayList<String>();
                            if ( origin.matches(toId) ) {
                                messageList.add("from");
                            } else {
                                messageList.add("to");
                            }
                            messageList.add(text);
                            messageList.add(dateOut);
                            messageList.add(photo);
                            messageHistory.add(messageList);
                        }
                        messengerAdapter.notifyDataSetChanged();
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
