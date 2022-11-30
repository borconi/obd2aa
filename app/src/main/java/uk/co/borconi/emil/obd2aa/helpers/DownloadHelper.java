package uk.co.borconi.emil.obd2aa.helpers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.io.DataInputStream;
import java.io.File;
import java.net.URL;

public class DownloadHelper {
    private static final String mobileURL = "http://data.blitzer.de/output/mobile.php?v=4&key=d83nv9dj38FQ";
    private static final String staticURL = "http://data.blitzer.de/output/static.php?v=4";
    private static String MOBILE_DB_PATH = null;
    private static String STATIC_DB_PATH = null;

    public DownloadHelper(final int i, final Context context, final int i2) {


        Thread r0 = new Thread() {
            public void run() {
                URL url;
                String str;
                CameraDataBaseHelper cameraDataBaseHelper = null;
                try {
                    if (i != 1 || i2 == 0) {
                        if (STATIC_DB_PATH == null)
                            STATIC_DB_PATH = context.getApplicationInfo().dataDir + "/databases/fixedcamera";

                        File file = new File(DownloadHelper.STATIC_DB_PATH);
                        String str2 = "fixedcamera";
                        if (!file.exists() || (System.currentTimeMillis() - file.lastModified()) / 1000 >= 604800 || i2 == 1) {
                            url = new URL(DownloadHelper.staticURL);
                            str = str2;
                        } else {
                            return;
                        }
                    } else {
                        str = "mobilecamera";
                        if (MOBILE_DB_PATH == null)
                            MOBILE_DB_PATH = context.getApplicationInfo().dataDir + "/databases/mobilecamera";
                        File file2 = new File(DownloadHelper.MOBILE_DB_PATH);
                        if (!file2.exists() || (System.currentTimeMillis() - file2.lastModified()) / 1000 >= ((long) i2)) {
                            url = new URL(DownloadHelper.mobileURL);
                        } else {
                            return;
                        }
                    }
                    int contentLength = url.openConnection().getContentLength();
                    DataInputStream dataInputStream = new DataInputStream(url.openStream());
                    byte[] bArr = new byte[contentLength];
                    dataInputStream.readFully(bArr);
                    dataInputStream.close();
                    cameraDataBaseHelper = new CameraDataBaseHelper(context, i, str);
                    try {
                        cameraDataBaseHelper.createDataBase();
                        cameraDataBaseHelper.copyDataBase(bArr);
                       /* if (context instanceof AppSettings) {
                            ((AppSettings) context).showDownload_comp_message(i);
                        }*/
                    } catch (Exception e) {
                        String str3 = "OBD2AA";
                        StringBuilder sb = new StringBuilder();
                        sb.append("Error creating database: ");
                        sb.append(e.getMessage());
                        Log.e(str3, sb.toString());
                    }
                    cameraDataBaseHelper.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                } catch (Throwable th) {
                    if (cameraDataBaseHelper != null)
                        cameraDataBaseHelper.close();
                    throw th;
                }
            }
        };
        NetworkInfo activeNetworkInfo = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        boolean z = false;
        boolean z2 = activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
        if (activeNetworkInfo != null && activeNetworkInfo.getType() == 1) {
            z = true;
        }
        boolean z3 = PreferencesHelper.getPreferences(context).shouldUseMobile();
        if (z2 && z3) {
            r0.start();
        } else if (z2 && z) {
            r0.start();
        }
    }
}
