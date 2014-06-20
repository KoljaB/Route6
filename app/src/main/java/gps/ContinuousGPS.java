package gps;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class ContinuousGPS {

    private Context mContext;
    private AlarmManager mAlarmManager;
    private PendingIntent pi;

    public ContinuousGPS(Context context) {
        mContext = context;
        mAlarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(mContext, AlarmReceiver.class);
        pi = PendingIntent.getBroadcast(mContext, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
    }
    public void cancelContinuous(){ mAlarmManager.cancel(pi); }

    public void setContinuous(long alarmtime) {
        mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 10000, alarmtime, pi);
    }
}
