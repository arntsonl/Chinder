package com.chinderapp.chinder;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.lorentzos.flingswipe.SwipeFlingAdapterView;
import com.parse.ParseAnalytics;
import com.soundcloud.android.crop.Crop;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class ChinderMatchScreenActivity extends Activity
        implements DialogMatchOverlay.OnFragmentInteractionListener {

    private BroadcastReceiver receiver;

    private List<List<String>> userProfiles;
    private List<String> lastProfile;
    private int i;
    private SwipeAdapter swipeAdapter;
    private SwipeFlingAdapterView adapterView;
    private SwipeFlingAdapterView.onFlingListener listener;
    private DialogMatchOverlay myDiag;

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int CROP_IMAGE_CAPTURE = 2;
    private Uri mImageCaptureUri = null;
    private Uri mImageCropUri = null;

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
        setContentView(R.layout.activity_chinder_match_screen);

        ActionBar supportActionBar = this.getActionBar();
        supportActionBar.setDisplayShowTitleEnabled(false);

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
                }
            }
        };
        registerReceiver(receiver, filter);

        //add the view via xml or programmatically
        userProfiles = new ArrayList<List<String>>();
        lastProfile = new ArrayList<String>();
        adapterView = (SwipeFlingAdapterView) findViewById(R.id.matchFlingView);
        swipeAdapter = new SwipeAdapter(this, userProfiles);
        adapterView.setAdapter(swipeAdapter);
        listener = new SwipeFlingAdapterView.onFlingListener() {
            @Override
            public void removeFirstObjectInAdapter() {
                // this is the simplest way to delete an object from the Adapter (/AdapterView)
                //Log.d("LIST", "removed object!");
                //al.remove(0);
                lastProfile = userProfiles.get(0);
                userProfiles.remove(0);

                // update the current name
                swipeAdapter.notifyDataSetChanged();
            }

            @Override
            public void onLeftCardExit(Object dataObject) {
                //Do something on the left!
                //You also have access to the original object.
                //If you want to use it just cast it (String) dataObject
                RequestParams params = new RequestParams();
                params.put("session", ((ChinderApplication)getApplication()).getPreference("session"));
                params.put("action", "disliked");
                params.put("to", lastProfile.get(2));
                AsyncHttpClient client = new AsyncHttpClient();
                client.post(new UtilityClass().getServer() + "match/swipe", params, new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                        // called when response HTTP status is "200 OK"
                        // parse response as JSON
                        String responseStr = new String(response);
                        try {
                            // Follow me - http://stackoverflow.com/questions/28713376/how-to-set-image-on-swipcard
                            JSONObject reader = new JSONObject(responseStr);
                            Boolean success = reader.getBoolean("success");
                            if ( success == true ) {
                            }
                            else {
                                String message = reader.getString("message");
                                // okay its not this
                                Context context = getApplicationContext();
                                int duration = Toast.LENGTH_SHORT;
                                Toast toast = Toast.makeText(context, message, duration);
                                toast.show();
                            }

                            if ( userProfiles.size() == 0 ) {
                                refreshImages(); // refresh everything
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
                    }
                });

                Toast.makeText(ChinderMatchScreenActivity.this, "Chin Down", Toast.LENGTH_SHORT).show();
                Map<String, String> dimensions = new HashMap<String, String>();
                dimensions.put("swipe", "down");
                ParseAnalytics.trackEventInBackground("MatchScreen", dimensions);
            }

            @Override
            public void onRightCardExit(Object dataObject) {

                RequestParams params = new RequestParams();
                params.put("session", ((ChinderApplication)getApplication()).getPreference("session"));
                params.put("action", "liked");
                params.put("to", lastProfile.get(2));
                AsyncHttpClient client = new AsyncHttpClient();
                client.post("http://chinder.parseapp.com/api/match/swipe", params, new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                        // called when response HTTP status is "200 OK"
                        // parse response as JSON
                        String responseStr = new String(response);
                        try {
                            // Follow me - http://stackoverflow.com/questions/28713376/how-to-set-image-on-swipcard
                            JSONObject reader = new JSONObject(responseStr);
                            Boolean success = reader.getBoolean("success");
                            if ( success == true ) {
                                Boolean match = reader.getBoolean("match");
                                if ( match == true ){
                                    String from = reader.getString("from");
                                    String to = reader.getString("to");
                                    String name = reader.getString("firstname");

                                    // Create an instance of the dialog fragment and show it
                                    myDiag= DialogMatchOverlay.newInstance(from, to, name);
                                    myDiag.show(getFragmentManager(), "DialogMatchFragment");

                                    // trigger that we matched
                                }
                            }
                            else {
                                String message = reader.getString("message");
                                // okay its not this
                                Context context = getApplicationContext();
                                int duration = Toast.LENGTH_SHORT;
                                Toast toast = Toast.makeText(context, message, duration);
                                toast.show();
                            }
                            if ( userProfiles.size() == 0 ) {
                                refreshImages(); // refresh everything
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
                    }
                });

                Toast.makeText(ChinderMatchScreenActivity.this, "Chin Up", Toast.LENGTH_SHORT).show();
                Map<String, String> dimensions = new HashMap<String, String>();
                dimensions.put("swipe", "up");
                ParseAnalytics.trackEventInBackground("MatchScreen", dimensions);
            }

            @Override
            public void onAdapterAboutToEmpty(int itemsInAdapter) {
                swipeAdapter.notifyDataSetChanged();
            }

            @Override
            public void onScroll(float scrollProgressPercent) {
            }
        };
        adapterView.setFlingListener(listener);
        refreshImages();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chinder_match_screen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_chat) {
            Intent intent = new Intent(this, ChinderMatchesActivity.class);
            startActivity(intent);
            return true;
        }
        else if (id == R.id.action_camera) {
            matchScreenCameraClick();
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

    @Override
    public void onFragmentInteraction(Uri uri) {
        // The user selected the headline of an article from the HeadlinesFragment
        // Do something here to display that article
    }

    // turn this into its own class please

    public void matchScreenCameraClick(){
        try{
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            mImageCaptureUri = Uri.fromFile(new File(getApplicationContext().getExternalCacheDir(),
                    "tmp_" + String.valueOf(System.currentTimeMillis()) + ".jpg"));
            takePictureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
                    mImageCaptureUri);
            takePictureIntent.putExtra("return-data", true);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
        // respond to users whose devices do not support the crop action
        catch (ActivityNotFoundException e) {
            Toast toast = Toast.makeText(this, "This device doesn't support a camera capture", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // take care of exceptions
            try {
                try {
                    // call the standard crop action intent (the user device may not
                    // support it)
                    mImageCropUri = Uri.fromFile(new File(getApplicationContext().getExternalCacheDir(),
                            "tmp_" + String.valueOf(System.currentTimeMillis()) + ".jpg"));
                    Crop crop = new Crop(mImageCaptureUri);
                    crop.withAspect(1, 1);
                    crop.output(mImageCropUri).asSquare().start(this);

                    // REQUEST_CODE_CROP_PHOTO is an integer tag you defined to
                    // identify the activity in onActivityResult() when it returns
                    //startActivityForResult(intent, CROP_IMAGE_CAPTURE);
                } catch ( Exception e ) {
                    Toast toast = Toast.makeText(this, "Something went wrong, try again later", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
            // respond to users whose devices do not support the crop action
            catch (ActivityNotFoundException e) {
                File f = new File(mImageCaptureUri.getPath());
                if (f.exists()) f.delete();

                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");

                ByteArrayOutputStream out = null;
                try {
                    out = new ByteArrayOutputStream();
                    imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                    // PNG is a lossless format, the compression factor (100) is ignored
                } catch (Exception exception) {
                    exception.printStackTrace();
                } finally {
                    try {
                        if (out != null) {
                            savePhotoToServer(out.toByteArray());
                            out.close();
                        }
                    } catch (IOException ee) {
                        ee.printStackTrace();
                    }
                }
            }
        }
        else if (requestCode == Crop.REQUEST_CROP && resultCode == RESULT_OK) {
            File f = new File(mImageCaptureUri.getPath());
            if (f.exists()) f.delete();

            Bitmap imageBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(mImageCropUri.getPath()), 256, 256, false);

            f = new File(mImageCropUri.getPath());
            if (f.exists()) f.delete();

            ByteArrayOutputStream out = null;
            try {
                out = new ByteArrayOutputStream();
                imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                // PNG is a lossless format, the compression factor (100) is ignored
            } catch (Exception exception) {
                exception.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        savePhotoToServer(out.toByteArray());
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void savePhotoToServer(byte [] sendPhoto) throws java.io.IOException {
        try {
            AsyncHttpClient client = new AsyncHttpClient();
            String session = ((ChinderApplication)getApplication()).getPreference("session");
            RequestParams params = new RequestParams();

            String photoEncoded = new String(Base64.encode(sendPhoto, Base64.DEFAULT), "UTF-8");
            params.put("photo", photoEncoded);
            params.put("session", session);
            client.post(new UtilityClass().getServer() + "users/uploadphoto", params, new AsyncHttpResponseHandler() {
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
                            Context context = getApplicationContext();
                            CharSequence text = "Profile updated successfully";
                            int duration = Toast.LENGTH_SHORT;
                            Toast toast = Toast.makeText(context, text, duration);
                            toast.show();
                        } else {
                            // okay its not this
                            Context context = getApplicationContext();
                            CharSequence text = "Network error, please try again later";
                            int duration = Toast.LENGTH_SHORT;
                            Toast toast = Toast.makeText(context, text, duration);
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
        } catch (IOException e) {
            // couldn't find the file
        }
    }

    public void leftSwipeOnClick(View V){
        adapterView.getTopCardListener().selectLeft();
    }

    public void rightSwipeOnClick(View V){
        adapterView.getTopCardListener().selectRight();
    }

    public void refreshOnClick(View v){
        refreshImages();
    }

    public void refreshImages() {
        RequestParams params = new RequestParams();
        params.put("session", ((ChinderApplication)getApplication()).getPreference("session"));
        AsyncHttpClient client = new AsyncHttpClient();
        client.post(new UtilityClass().getServer() + "match/latest", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {

                // called when response HTTP status is "200 OK"
                // parse response as JSON
                String responseStr = new String(response);
                try {
                    // Follow me - http://stackoverflow.com/questions/28713376/how-to-set-image-on-swipcard

                    JSONObject reader = new JSONObject(responseStr);
                    Boolean success = reader.getBoolean("success");
                    if ( success == true ) {
                        JSONArray users = reader.getJSONArray("users");
                        for ( Integer i = 0; i < users.length(); i++)
                        {
                            try {
                                JSONObject user = users.getJSONObject(i);
                                String firstname = user.getString("firstname");
                                if ( firstname.length() > 16 ) {
                                    firstname = firstname.substring(0,16) + "...";
                                }
                                Integer age = user.getInt("age");
                                String photo = user.getString("photo");
                                String nameOut = firstname + ", " + age.toString();
                                String id = user.getString("id");
                                List<String> currentUser = new ArrayList<String>();
                                currentUser.add(photo);
                                currentUser.add(nameOut);
                                currentUser.add(id);
                                userProfiles.add(currentUser);
                            } catch ( Exception e ) {
                                // something went wrong with this user
                            }
                        }
                        swipeAdapter.notifyDataSetChanged();
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
            }
        });
    }
}
