package uk.co.boconi.emil.obd2aa.Helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.File;
import java.net.URL;
import java.net.URLConnection;

import uk.co.boconi.emil.obd2aa.AppSettings;
import uk.co.boconi.emil.obd2aa.R;

/**
 * Created by Emil on 19/10/2017.
 */

public class DownloadHelper {

    private static String mobileURL = "http://data.blitzer.de/output/mobile.php?v=4&key=d83nv9dj38FQ";
    private static String staticURL = "http://data.blitzer.de/output/static.php?v=4";
    private static String MOBILE_DB_PATH = "/data/data/uk.co.boconi.emil.obd2aa/databases/mobilecamera";
    private static String STATIC_DB_PATH = "/data/data/uk.co.boconi.emil.obd2aa/databases/fixedcamera";


        public DownloadHelper(final int type, final Context context, final int freq){
            Thread mydownloadthread=new Thread() {
                @Override
                public void run() {
                    try {
                        URL u=null;
                        String dbname;
                        if (type==1 && freq!=0)
                        {
                            dbname="mobilecamera";
                            File file = new File(MOBILE_DB_PATH);
                            if (file.exists() && ((System.currentTimeMillis()-file.lastModified())/1000)<freq) //Only download a new file every 5 minutes;
                                return;
                            u = new URL(mobileURL);
                        }
                        else {
                            File file = new File(STATIC_DB_PATH);
                            dbname="fixedcamera";
                            if (file.exists() && ((System.currentTimeMillis()-file.lastModified())/1000)<604800 && freq!=1)  //Only download a new static database file every 7 days.
                                return;
                            u = new URL(staticURL);
                        }
                        URLConnection conn = u.openConnection();
                        int contentLength = conn.getContentLength();
                        DataInputStream stream = new DataInputStream(u.openStream());

                        byte[] buffer = new byte[contentLength];
                        stream.readFully(buffer);
                        stream.close();
                        CameraDataBaseHelper xxx = new CameraDataBaseHelper(context,type,dbname);
                        xxx.createDataBase();
                        xxx.copyDataBase(buffer);
                        Log.d("OBD2AA","Download has been completed!");
                        if (context instanceof AppSettings)
                        {


                              ((AppSettings) context).showDownload_comp_message(type);


                        }
                    }
                    catch (Exception E)
                    {
                        E.printStackTrace();
                    }
                }
            };

            ConnectivityManager cm =  (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            boolean isWiFi = activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean use_mobile=prefs.getBoolean("use_mobile",true);
            if (isConnected && use_mobile)
                mydownloadthread.start();
            else if (isConnected && isWiFi)
                mydownloadthread.start();
        }
}
