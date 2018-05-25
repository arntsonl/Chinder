package com.chinderapp.chinder;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseCrashReporting;
import com.parse.ParseUser;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

public class ChinderApplication extends Application {
  private boolean foundLocation = false;
  private Intent mServiceIntent;
  private Activity mCurrentActivity = null;

  @Override
  public void onCreate() {
    super.onCreate();

    // Custom fonts, custom fonts everywhere!
    CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
            .setDefaultFontPath("fonts/roboto-regular.ttf")
            .setFontAttrId(R.attr.fontPath)
            .build());

    // Initialize the Universal Image Loader
    initImageLoader(getApplicationContext());

    // Initialize Crash Reporting.
    ParseCrashReporting.enable(this);

    // Enable Local Datastore.
    Parse.enableLocalDatastore(this);

    // Add your initialization code here
    //Parse.initialize(this);
    Parse.initialize(this, "@string/parse_app_id", "@string/parse_client_key");

    ParseUser.enableAutomaticUser();
    ParseACL defaultACL = new ParseACL();
    // Optionally enable public read access.
    // defaultACL.setPublicReadAccess(true);
    ParseACL.setDefaultACL(defaultACL, true);

    setupLocationListener();

    // debug synchronous
    updateEventPoll();
  }

  public String getPreference(String key) {
    // Restore preferences
    SharedPreferences settings = getSharedPreferences("settings", 0);
    return settings.getString(key, "");
  }

  public void setPreference(String key, String value) {
    // Restore preferences
    SharedPreferences settings = getSharedPreferences("settings", 0);
    SharedPreferences.Editor editor = settings.edit();
    editor.putString(key, value);
    editor.commit();
  }

  public void deletePreference(String key) {
    SharedPreferences settings = getSharedPreferences("settings", 0);
    SharedPreferences.Editor editor = settings.edit();
    editor.remove(key);
    editor.commit();
  }

//  public String getLocalPreference(String key) {
//      return localSettings.get(key);
//  }
//
//  public void setLocalPreference(String key, String value) {
//    localSettings.put(key, value);
//  }

    public static void initImageLoader(Context context) {
        // This configuration tuning is custom. You can tune every option, you may tune some of them,
        // or you can create default configuration by
        //  ImageLoaderConfiguration.createDefault(this);
        // method.
        ImageLoaderConfiguration.Builder config = new ImageLoaderConfiguration.Builder(context);
        config.threadPriority(Thread.NORM_PRIORITY - 2);
        config.denyCacheImageMultipleSizesInMemory();
        config.diskCacheFileNameGenerator(new Md5FileNameGenerator());
        config.diskCacheSize(50 * 1024 * 1024); // 50 MiB
        config.tasksProcessingOrder(QueueProcessingType.LIFO);
        //config.writeDebugLogs(); // Remove for release app

        // Initialize ImageLoader with configuration.
        ImageLoader.getInstance().init(config.build());
    }

    private void setupLocationListener() {
        // Acquire a reference to the system Location Manager
        final LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                if (foundLocation == true) {
                    locationManager.removeUpdates(this);
                }
                else {
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();
                    String geoLocation = String.valueOf(lat) + ", " +
                            String.valueOf(lon);
                    updateLocation(geoLocation);
                }
            }
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(String provider) {}
            public void onProviderDisabled(String provider) {}
        };
        // Register the listener with the Location Manager to receive location updates
        try {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }
        catch (Exception e ) {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            }
            catch (Exception ee) {
                // wow something is wrong
            }
        }
    }

    private void updateLocation(String geolocation) {
        final String session = getPreference("session");
        if ( session.equals("") == false ) {
            // do an async request to see if our session is still valid
            AsyncHttpClient client = new AsyncHttpClient();
            RequestParams params = new RequestParams();
            params.put("session", session);
            params.put("geolocation", geolocation);
            client.post(new UtilityClass().getServer() + "users/location", params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                    // called when response HTTP status is "200 OK"
                    // parse response as JSON
                    String responseStr = new String(response);
                    try {
                        JSONObject reader = new JSONObject(responseStr);
                        Boolean success = reader.getBoolean("success");
                        if ( success == true ) {
                            new UtilityClass().savePreferences(reader, ChinderApplication.this);
                        }
                        else {
                            String message = reader.getString("message");
                            Context context = getApplicationContext();
                            int duration = Toast.LENGTH_SHORT;
                            Toast toast = Toast.makeText(context, message, duration);
                            toast.show();
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

    public void updateEventPoll() {
        final String session = ChinderApplication.this.getPreference("session");
        if ( session.equals("") == false ) {
            // do an async request to see if our session is still valid
            AsyncHttpClient client = new AsyncHttpClient();
            RequestParams params = new RequestParams();
            params.put("session", session);
            client.post(new UtilityClass().getServer() + "events/poll", params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                    // called when response HTTP status is "200 OK"
                    // parse response as JSON
                    String responseStr = new String(response);
                    try {
                        JSONObject reader = new JSONObject(responseStr);
                        Boolean success = reader.getBoolean("success");
                        if ( success == true ) {

                            JSONArray events = reader.getJSONArray("events");
                            int messageCount = 0;
                            int matchCount = 0;
                            for ( int i = 0; i < events.length(); i++ ) {
                                // Do we have a new message?
                                JSONObject event = events.getJSONObject(i);
                                String action = event.getString("action");
                                String from = event.getString("from");
                                if ( action.matches("message") )
                                {
                                    messageCount += 1;
                                    // Mark the history chat (change the border to blue or white?)
                                    setPreference(from, "message");
                                }
                                else if ( action.matches("match") )
                                {
                                    matchCount += 1;
                                    // Mark the history chat (change the border to blue or white?)
                                    setPreference(from, "match");
                                }
                            }
                            if ( messageCount > 0 )
                            {
                                Intent i = new Intent("NEW_MESSAGE");
                                sendBroadcast(i);
                            }
                            if ( matchCount > 0 )
                            {
                                Intent i = new Intent("NEW_MATCH");
                                sendBroadcast(i);
                            }
                        }
                        else {
                            String message = reader.getString("message");
                            Context context = getApplicationContext();
                            int duration = Toast.LENGTH_SHORT;
                            Toast toast = Toast.makeText(context, message, duration);
                            toast.show();
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
