package uk.co.borconi.emil.obd2aa.helpers;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CameraDataBaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME_FIXED = "fixedcamera";
    private static final String DB_NAME_MOBILE = "mobilecamera";
    private static String DB_PATH = null;
    private final Context myContext;
    private final String DB_NAME;
    private final int dbtype;
    private SQLiteDatabase myDataBase;
    private final String tablename;

    public CameraDataBaseHelper(Context context, int i, String str) {
        super(context, str, null, 1);
        if (DB_PATH == null)
            DB_PATH = context.getApplicationInfo().dataDir + "/databases/";
        this.myContext = context;
        if (i == 1) {
            this.DB_NAME = DB_NAME_MOBILE;
            this.tablename = "blitzermob";
            this.dbtype = i;
            return;
        }
        this.DB_NAME = DB_NAME_FIXED;
        this.tablename = "blitzerstat";
        this.dbtype = i;
    }

    public void onCreate(SQLiteDatabase sQLiteDatabase) {
    }

    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
    }

    public void createDataBase() throws IOException {
        if (!checkDataBase()) {
            getReadableDatabase().close();
        }
    }

    private boolean checkDataBase() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(DB_PATH);
            sb.append(this.DB_NAME);
            SQLiteDatabase openDatabase = SQLiteDatabase.openDatabase(sb.toString(), null, 0);
            if (openDatabase == null) {
                return false;
            }
            openDatabase.close();
            return true;
        } catch (Throwable unused) {
            Log.d("CameraDatabaseHelper", "Database does not exist yet, should create it");
            return false;
        }
    }

    /* access modifiers changed from: protected */
    public void copyDataBase(byte[] bArr) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(DB_PATH);
        sb.append(this.DB_NAME);
        FileOutputStream fileOutputStream = new FileOutputStream(sb.toString());
        fileOutputStream.write(bArr, 0, bArr.length);
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    public void openDataBase() throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append(DB_PATH);
        sb.append(this.DB_NAME);
        this.myDataBase = SQLiteDatabase.openDatabase(sb.toString(), null, 1);
    }

    public List<NearbyCameras> getNearbyCameras(double d, double d2, double d3, double d4, double d5, double d6, String str) {
        Integer num;
        ArrayList arrayList = new ArrayList();
        openDataBase();
        try {
            SQLiteDatabase sQLiteDatabase = this.myDataBase;
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT lat,long,id,hdg,spd,type,street from ");
            sb.append(this.tablename);
            sb.append(" where lat>='");
            sb.append(d);
            sb.append("' and lat<='");
            sb.append(d2);
            sb.append("' and long>='");
            sb.append(d3);
            sb.append("' and long<='");
            sb.append(d4);
            sb.append("' ");
            sb.append(str);
            Cursor rawQuery = sQLiteDatabase.rawQuery(sb.toString(), new String[0]);
            if (rawQuery.getCount() > 0) {
                rawQuery.moveToFirst();
                do {
                    if (rawQuery.getString(3) != null) {
                        if (rawQuery.getString(3).length() > 0) {
                            num = Integer.valueOf(rawQuery.getString(3));
                            NearbyCameras nearbyCameras = new NearbyCameras(Double.parseDouble(rawQuery.getString(0)), Double.parseDouble(rawQuery.getString(1)), rawQuery.getInt(2), num, rawQuery.getString(4), rawQuery.getString(5), rawQuery.getString(6), this.dbtype, d5, d6);
                            arrayList.add(nearbyCameras);
                        }
                    }
                    num = null;
                    NearbyCameras nearbyCameras2 = new NearbyCameras(Double.parseDouble(rawQuery.getString(0)), Double.parseDouble(rawQuery.getString(1)), rawQuery.getInt(2), num, rawQuery.getString(4), rawQuery.getString(5), rawQuery.getString(6), this.dbtype, d5, d6);
                    arrayList.add(nearbyCameras2);
                } while (rawQuery.moveToNext());
                rawQuery.close();
            }
            close();
            return arrayList;
        } catch (Exception unused) {
            return null;
        }
    }

    public synchronized void close() {
        if (this.myDataBase != null) {
            this.myDataBase.close();
        }
        super.close();
    }
}
