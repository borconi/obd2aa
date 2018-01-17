package uk.co.boconi.emil.obd2aa;


import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;


import com.google.android.apps.auto.sdk.CarToast;

import com.google.android.apps.auto.sdk.notification.CarNotificationExtender;



/**
 * Created by Emil on 04/09/2017.
 * Notification listener. This class will show CamSam and Blitzer.De notifications inside Android Auto
 * Can be future extended to show notifications from other apps as
 */

public class CarnotificationListener extends NotificationListenerService {
    Context context;

    @Override

    public void onCreate() {

        super.onCreate();
        context = getApplicationContext();

    }
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn){

       //Nothing to do here
    }


    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
       // if (!OBD2_Background.isrunning)
          //  return;
        int icon=0;
        String pack = sbn.getPackageName();
        if  (pack.equalsIgnoreCase("nu.fartkontrol.app"))
        {

            icon=R.drawable.speed_camera_sam;
        }

        else
            return;


        Bundle extras = sbn.getNotification().extras;


        if (extras.getString("android.title")==null ||  extras.getString("android.title").equalsIgnoreCase("Fartkontrol.nu"))
        {
            //Log.d("ODB2AA","Title null or Only CamSam");
            return;
        }
        final NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        String title = "Speed Camera ahead";
        String text = extras.getString("android.title");
        Bitmap bmp = BitmapFactory.decodeResource(getResources(),icon);
        if (pack.equalsIgnoreCase("nu.fartkontrol.app"))
        {
            title=extras.getString("android.title");
            text=extras.getString("android.text");
            bmp =  extras.getParcelable(Notification.EXTRA_LARGE_ICON);



        }
        //Log.d("OBD2AA","About to send a notification to the car...");



        CarNotificationExtender paramString2 = new CarNotificationExtender.Builder()
                .setTitle(title)
                .setSubtitle(text)
                .setShouldShowAsHeadsUp(true)
                .setActionIconResId(R.drawable.ic_danger_r)
                .setBackgroundColor(Color.WHITE)
                .setNightBackgroundColor(Color.DKGRAY)
                .setThumbnail(bmp)
                .build();

        NotificationCompat.Builder mynot = new NotificationCompat.Builder(context)
                .setContentTitle(title)
                .setContentText(text)
                .setLargeIcon(bmp)

                .setSmallIcon(R.drawable.ic_danger_r)
                .setColor(Color.GRAY)
                .extend(paramString2);


        mNotifyMgr.notify(1983,mynot.build());



    }
}
