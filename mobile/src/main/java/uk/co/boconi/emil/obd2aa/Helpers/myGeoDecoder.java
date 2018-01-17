package uk.co.boconi.emil.obd2aa.Helpers;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.apps.auto.sdk.notification.CarNotificationExtender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import uk.co.boconi.emil.obd2aa.OBD2_Background;
import uk.co.boconi.emil.obd2aa.R;

/**
 * Created by Emil on 23/10/2017.
 */

public class myGeoDecoder {
    Context mcontext;
    String LastStreet;
    Bitmap road;
    NotificationCompat.Builder mynot;
    public myGeoDecoder( final Context context){
        mcontext=context;
        road=BitmapFactory.decodeResource(context.getResources(), R.drawable.road);


    }

    public void dedoceaddress(Location location) throws Exception {
        Log.d("OBD2AA","Getting street card info...");
        Geocoder geodecoder = new Geocoder(mcontext);
        List<Address> myaddress = geodecoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
        if (myaddress !=null )
        {
            String street = myaddress.get(0).getThoroughfare();
            if (street!=null) //Updated notification
            {

                String speedlimit="";
                try {

                    URL url = new URL("http://94.23.203.111/zone/o2/osm.php?l=" + location.getLatitude() + "&x=" + location.getLongitude());
                    URLConnection con = url.openConnection();
                    con.setConnectTimeout(2000);
                    con.setReadTimeout(2000);
                    InputStream is = con.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    speedlimit = br.readLine();
                    if (speedlimit!=null && !speedlimit.contains("mph") && !speedlimit.contains("km/h") && !speedlimit.isEmpty())
                        speedlimit=speedlimit+" km/h";
                }
                catch (Exception e)
                {
                    Log.e("OBD2AA","Timeout getting current speed limit...");
                }

                CarNotificationExtender paramString2 = new CarNotificationExtender.Builder()
                        .setTitle(street)
                        .setSubtitle(speedlimit)

                        .setShouldShowAsHeadsUp(false)
                        .setActionIconResId(R.drawable.ic_info_outline_black_24dp)
                        .setBackgroundColor(Color.WHITE)
                        .setNightBackgroundColor(Color.DKGRAY)
                        .setThumbnail(road)
                        .build();

               mynot = new NotificationCompat.Builder(mcontext)
                        .setContentTitle(street)

                        .setSubText(speedlimit)
                        .setLargeIcon(road)
                        .setSmallIcon(R.drawable.ic_info_outline_black_24dp)
                        .setOngoing(false)
                        .setPriority(Notification.PRIORITY_DEFAULT)
                        .extend(paramString2);


                NotificationManager mNotifyMgr = (NotificationManager) mcontext.getSystemService(mcontext.NOTIFICATION_SERVICE);


                mNotifyMgr.notify(1986,mynot.build());

            }
        }

    }
}
