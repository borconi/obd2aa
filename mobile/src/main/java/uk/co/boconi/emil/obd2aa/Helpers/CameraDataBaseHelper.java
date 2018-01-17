package uk.co.boconi.emil.obd2aa.Helpers;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import uk.co.boconi.emil.obd2aa.Helpers.NearbyCameras;

/**
 * Created by Emil on 19/10/2017.
 */

public class CameraDataBaseHelper extends SQLiteOpenHelper{

    //The Android's default system path of your application database.
    private static String DB_PATH = "/data/data/uk.co.boconi.emil.obd2aa/databases/";

    private static String DB_NAME_FIXED = "fixedcamera";
    private static String DB_NAME_MOBILE = "mobilecamera";

    private SQLiteDatabase myDataBase;
    private final Context myContext;
    private String DB_NAME;
    private String tablename;
    private int dbtype;

    /**
     * Constructor
     * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
     * @param context
     */
    public CameraDataBaseHelper(Context context,int type,String dbname) {

        super(context, dbname, null, 1);

        this.myContext = context;

        if (type==1)
        {
            DB_NAME=DB_NAME_MOBILE;
            tablename="blitzermob";
            dbtype=type;
        }
        else
        {
            DB_NAME=DB_NAME_FIXED;
            tablename="blitzerstat";
            dbtype=type;
        }

    }

    /**
     * Creates a empty database on the system and rewrites it with your own database.
     * */
    public void createDataBase() throws IOException{

        boolean dbExist = checkDataBase();

        if(dbExist){
            //do nothing - database already exist
        }else{

            //By calling this method and empty database will be created into the default system path
            //of your application so we are gonna be able to overwrite that database with our database.
            this.getReadableDatabase();
       }

    }

    /**
     * Check if the database already exist to avoid re-copying the file each time you open the application.
     * @return true if it exists, false if it doesn't
     */
    private boolean checkDataBase(){

        SQLiteDatabase checkDB = null;

        try{
            String myPath = DB_PATH + DB_NAME;
            checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE);

        }catch(Exception e){

            //database does't exist yet.

        }

        if(checkDB != null){

            checkDB.close();

        }

        return checkDB != null ? true : false;
    }

    /**
     * Copies your database from your local assets-folder to the just created empty database in the
     * system folder, from where it can be accessed and handled.
     * This is done by transfering bytestream.
     * */
    protected void copyDataBase(byte[] myInput) throws IOException {

        // Path to the just created empty db
        String outFileName = DB_PATH + DB_NAME;

        //Open the empty db as the output stream
        OutputStream myOutput = new FileOutputStream(outFileName);

        //transfer bytes from the input to the outputfile
         myOutput.write(myInput, 0, myInput.length);


        //Close the streams
        myOutput.flush();
        myOutput.close();

    }

    public void openDataBase() throws SQLException {

        //Open the database
        String myPath = DB_PATH + DB_NAME;
        myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);

    }

    public List<NearbyCameras> getNearbyCameras(double latfrom, double latto, double longfrom, double longto, double latitude, double longitude, String texfilter)
    {
            List<NearbyCameras> nearbycamera = new ArrayList<NearbyCameras>();
            openDataBase();
            Cursor cursor = myDataBase.rawQuery("SELECT lat,long,id,hdg,spd,type,street from "+tablename+" where lat>='"+latfrom+"' and lat<='"+latto+"' and long>='"+longfrom+"' and long<='"+longto+"' "+texfilter,new String []{});
            if (cursor.getCount() > 0)
            {
                cursor.moveToFirst();
                do {
                    Integer valueOf = (cursor.getString(3) == null || cursor.getString(3).length() <= 0) ? null : Integer.valueOf(cursor.getString(3));
                    //Log.d("OBD2AA","Database values: " + cursor.getString(0) + ", "+cursor.getString(1) + ", heading: " + valueOf);
                    nearbycamera.add(new NearbyCameras(Double.parseDouble(cursor.getString(0)),Double.parseDouble(cursor.getString(1)),cursor.getInt(2),valueOf,cursor.getString(4),cursor.getString(5),cursor.getString(6),dbtype, latitude, longitude));
                } while (cursor.moveToNext());
                cursor.close();
            }
            close();
            return nearbycamera;

    }

    @Override
    public synchronized void close() {

        if(myDataBase != null)
            myDataBase.close();

        super.close();

    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    // Add your public helper methods to access and get content from the database.
    // You could return cursors by doing "return myDataBase.query(....)" so it'd be easy 6
    // to you to create adapters for your views.

}
