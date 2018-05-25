package com.chinderapp.chinder;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

public class EventPollService extends Service {
    EventPollAlarm alarm = new EventPollAlarm();
    public void onCreate(){
        super.onCreate();

        alarm.SetAlarm(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        //alarm.SetAlarm(EventPollService.this);
        //alarm.CancelAlarm(EventPollService.this);
        return START_STICKY;
    }

    public void onStart(Context context,Intent intent, int startId)
    {
        //alarm.SetAlarm(context);
       //alarm.CancelAlarm(context);
    }

    /**
     * Provides a handle to the bound service.
     */
    public class  AppActiveBinder extends Binder {
        EventPollService getService() {
            return EventPollService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onDestroy(){
        // TODO: Here is presumably "application level" pause
        super.onDestroy();

        alarm.CancelAlarm(this);
    }
}