package com.chinderapp.chinder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Base64;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by Stealth7 on 4/2/2015.
 */
public class UtilityClass {
    public String getServer() {
        // if debug
        if (BuildConfig.DEBUG) {
            // do something for a debug build
            return "http://chinder.parseapp.com/experimental/";
        }
        else {
            return "http://chinder.parseapp.com/api/";
        }
    }


    public void savePhotoToServer(Bitmap imageBitmap, ChinderApplication application, Context context, AsyncHttpResponseHandler asyncHandler) throws java.io.IOException {
        ByteArrayOutputStream out = null;
        try {
            out = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    byte [] sendPhoto = out.toByteArray();
                    out.close();

                    AsyncHttpClient client = new AsyncHttpClient();
                    String session = application.getPreference("session");
                    RequestParams params = new RequestParams();

                    String photoEncoded = new String(Base64.encode(sendPhoto, Base64.DEFAULT), "UTF-8");
                    params.put("photo", photoEncoded);
                    params.put("session", session);
                    client.post(getServer() + "users/uploadphoto", params, asyncHandler);
                }
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    public void savePreferences(JSONObject reader, ChinderApplication application) {
        try {
            String session = reader.getString("session");
            application.setPreference("session", session);
            String firstname = reader.getString("firstname");
            application.setPreference("firstname", firstname);
            String birthday = reader.getString("birthday");
            application.setPreference("birthday", birthday);
            String email = reader.getString("email");
            application.setPreference("email", email);
            String photo = reader.getString("photo");
            application.setPreference("photo", photo);
            String geolocation = reader.getString("geolocation");
            application.setPreference("geolocation", geolocation);
        } catch ( Exception e ){
        }
    }

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, float roundPx) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }
}
