package com.chinderapp.chinder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class EventPollAlarm extends BroadcastReceiver
{
    private long alarmLength = 10 * 1000;

    @Override
    public void onReceive(Context context, Intent intent)
    {
        ((ChinderApplication)context.getApplicationContext()).updateEventPoll();

        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, EventPollAlarm.class);
        final PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        am.set(AlarmManager.RTC, System.currentTimeMillis() + alarmLength, pi);
    }

    public void SetAlarm(Context context)
    {
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, EventPollAlarm.class);
        final PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        am.set(AlarmManager.RTC, System.currentTimeMillis() + alarmLength, pi);
    }

    public void CancelAlarm(Context context)
    {
        Intent intent = new Intent(context, EventPollAlarm.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }
}