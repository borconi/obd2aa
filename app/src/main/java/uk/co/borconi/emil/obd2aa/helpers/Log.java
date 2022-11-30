package uk.co.borconi.emil.obd2aa.helpers;

import static android.os.Environment.DIRECTORY_DOCUMENTS;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

public class Log {
    private static Context mContext;
    private static boolean mSave = true;
    private static FileOutputStream outputstream;


    public static void openFile(Context context) {
        android.util.Log.d("HU", "openFile called");
        try {
            android.util.Log.d("HU", "Have permission, creating file...");
            String filename = "hur.log";

            File file = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                if (!Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS).isDirectory())
                    Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS).mkdirs();

                file = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS), filename);


            } else
                file = new File(Environment.getExternalStorageState(), filename);


            try {
                if (!file.exists())
                    file.createNewFile();

                FileOutputStream outputStream = new FileOutputStream(file);
                Log.outputstream = outputStream;

            } catch (Exception E) {
                android.util.Log.e("Logger", E.getLocalizedMessage());
            }
        } catch (Exception e) {
            android.util.Log.d("HU", "No permission");
        }
        Log.mContext = context;
        //mSave=PreferenceManager.getDefaultSharedPreferences(context).getBoolean("enabledebug",false);
    }

    public static void close() {
        mContext = null;
        try {
            outputstream.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            outputstream = null;
        }
        mSave = false;
    }

    public static void d(String tag, String text) {
        if (!mSave)
            return;
        save(tag, "D", text);
        android.util.Log.d(tag, text);
    }

    public static void e(String tag, String text) {
        if (!mSave || text == null)
            return;
        save(tag, "E", text);
        android.util.Log.e(tag, text);
    }

    public static void w(String tag, String text) {
        if (!mSave)
            return;
        save(tag, "W", text);
        android.util.Log.w(tag, text);
    }

    public static void i(String tag, String text) {
        if (!mSave)
            return;
        save(tag, "I", text);
        android.util.Log.e(tag, text);
    }

    private static void save(String tag, String type, String text) {
        if (outputstream != null) {
            StringBuilder aux = new StringBuilder();
            aux.append(DateFormat.getDateTimeInstance().format(new Date()));
            aux.append(" ");
            aux.append(type);
            aux.append(" - ");
            aux.append(tag);
            aux.append(": ");
            aux.append(text);
            try {
                outputstream.write(aux.toString().getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
