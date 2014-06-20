package gps;

import android.content.Intent;
import android.util.Log;

import com.example.routemap.app.MainActivity;

public class GpsService extends WakeIntentService {

    public GpsService() {
        super("ReminderService");
    }

    @Override
    void performWork(Intent intent)
    {
        Log.d("ReminderService", "Starting work.");
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }//end of gps work

}//end of gps service
