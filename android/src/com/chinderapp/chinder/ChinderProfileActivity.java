package com.chinderapp.chinder;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DatePickerDialog;
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
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.soundcloud.android.crop.Crop;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class ChinderProfileActivity extends Activity {

    private BroadcastReceiver receiver;

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
        setContentView(R.layout.activity_chinder_profile);

        ActionBar supportActionBar = this.getActionBar();
        supportActionBar.setDisplayHomeAsUpEnabled(true);
        supportActionBar.setTitle("Edit Profile");

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

        String firstname = ((ChinderApplication)getApplication()).getPreference("firstname");
        if ( firstname != null ) {
            EditText firstnameText = (EditText)findViewById(R.id.firstnameProfileText);
            firstnameText.setText(firstname, TextView.BufferType.EDITABLE);
        }
        String birthday = ((ChinderApplication)getApplication()).getPreference("birthday");
        if ( birthday != null ) {
            Button birthdayButton = (Button)findViewById(R.id.birthdayProfileButton);
            birthdayButton.setText(birthday);
        }

        // Load image, decode it to Bitmap and display Bitmap in ImageView (or any other view
        String photo = ((ChinderApplication)getApplication()).getPreference("photo");
        if ( photo != null ) {
            ImageView profileImage = (ImageView) findViewById(R.id.profileImageView);
            DisplayImageOptions options = new DisplayImageOptions.Builder()
                    .showImageOnLoading(R.drawable.default_profile) // resource or drawable
                    .cacheOnDisk(true) // default
                    .build();
            ImageLoader.getInstance().displayImage(photo, profileImage, options, new SimpleImageLoadingListener() {
                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {}
            });
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chinder_profile, menu);
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
        else if ( id == R.id.action_logout )
        {
            ((ChinderApplication)getApplication()).setPreference("session", "");
            Intent chinderStartActivity = new Intent(this, ChinderStartActivity.class);
            chinderStartActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(chinderStartActivity);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    public void saveProfileOnClick(View v ){
        AsyncHttpClient client = new AsyncHttpClient();
        String session = ((ChinderApplication)getApplication()).getPreference("session");
        RequestParams params = new RequestParams();
        params.put("session", ((ChinderApplication)getApplication()).getPreference("session"));
        EditText firstnameEditText = (EditText)findViewById(R.id.firstnameProfileText);
        params.put("firstname", firstnameEditText.getText());
        EditText passwordText = (EditText)findViewById(R.id.profilePasswordText);
        if ( passwordText.getText().toString().matches("") == false)
        {
            params.put("password", passwordText.getText());
        }
        Button birthdayButton = (Button)findViewById(R.id.birthdayProfileButton);
        params.put("birthday", birthdayButton.getText());

        if ( firstnameEditText.getText().length()==0 ||
                birthdayButton.getText().equals("SELECT BIRTHDAY") )
        {
            Context context = getApplicationContext();
            CharSequence text = "Please do not leave fields blank";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return;
        }

        client.post(new UtilityClass().getServer() + "users/update", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                // called when response HTTP status is "200 OK"
                // parse response as JSON
                String responseStr = new String(response);
                try {
                    JSONObject reader = new JSONObject(responseStr);
                    Boolean success = reader.getBoolean("success");
                    if (success == true) {
                        new UtilityClass().savePreferences(reader, (ChinderApplication) getApplication());

                        Context context = getApplicationContext();
                        CharSequence text = "Profile updated successfully";
                        int duration = Toast.LENGTH_SHORT;
                        Toast toast = Toast.makeText(context, text, duration);
                        toast.show();
                        onBackPressed(); // pop our stack
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

    public void profilePictureOnClick(View v){
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
                ImageView mImageView = (ImageView)findViewById(R.id.profileImageView);
                mImageView.setImageBitmap(imageBitmap);

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
            ImageView mImageView = (ImageView)findViewById(R.id.profileImageView);
            mImageView.setImageBitmap(imageBitmap);

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

    String[] monthsArray = {"January", "February", "March", "April"
            , "May", "June", "July", "August", "September"
            , "October", "November", "December"};

    public void selectBirthdayOnClick(View v) {
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

                Button birthdayButton = (Button) findViewById(R.id.birthdayProfileButton);
                birthdayButton.setText(buttonText);
            }
        }, year, month, day);
        dpd.show();
    }
}
