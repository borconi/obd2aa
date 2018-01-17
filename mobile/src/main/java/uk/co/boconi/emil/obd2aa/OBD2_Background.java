package uk.co.boconi.emil.obd2aa;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.car.hardware.CarSensorEvent;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.apps.auto.sdk.CarActivity;

import com.google.android.apps.auto.sdk.notification.CarNotificationExtender;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;


import org.prowl.torque.remote.ITorqueService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import uk.co.boconi.emil.obd2aa.Helpers.CameraDataBaseHelper;
import uk.co.boconi.emil.obd2aa.Helpers.DownloadHelper;
import uk.co.boconi.emil.obd2aa.Helpers.NearbyCameras;
import uk.co.boconi.emil.obd2aa.Helpers.isCameraInWay;
import uk.co.boconi.emil.obd2aa.Helpers.myGeoDecoder;

import static java.lang.Integer.parseInt;
import static uk.co.boconi.emil.obd2aa.SunSet.Calculate_Sunset_Sunrise;



/**
 * Created by Emil on 31/08/2017.
 */


public class OBD2_Background extends Service  {

    protected ITorqueService torqueService;
    private OBD2AA mOBD2AA = null;
    private CarActivity savedcaractivity=null;

    public boolean ecuconnected = true;
    private String[] pids;
    private SharedPreferences prefs;
    SharedPreferences.Editor editor;
    private int gauge_number;
    private String[] units;

    public static boolean isdebugging;
    private boolean alternativepulling;
    private boolean firstfecth = true;
    private List<PIDToFetch> pidtofetch = new ArrayList<PIDToFetch>();
    static volatile boolean isrunning;
    private boolean mBind = false;
    private int nightMode=0;
    private long lastcalc=0;
    private UiModeManager mUimodemanager=null;
    private FusedLocationProviderClient mFusedLocationClient=null;
    private LocationRequest mLocationRequest;
    private CameraDataBaseHelper staticDB=new CameraDataBaseHelper(this,2,"fixedcamera");
    private CameraDataBaseHelper mobileDB=new CameraDataBaseHelper(this,1,"mobilecamera");
    private boolean isSpeedCamShowing;
    NotificationManager mNotifyMgr;
    private boolean ShowSpeedCamWarrning;
    private boolean playSound;
    private boolean isDemoMode;
    private Paint textpaint;
    private Paint paint;
    private RectF rectF;
    private int audio_1,audio_2,audio_3,visual_display;
    private boolean useImperial;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private myGeoDecoder mgeodecoder;
   
    private NearbyCameras prevCamera,camera;
    private boolean streetcard;
    private ExecutorService geodecoderexecutor;
    private int CameraRefreshFreq;
    private String mobile_filter,static_filter;


    public class LocalBinder extends Binder {
        OBD2_Background getService() {
            // Return this instance of LocalService so clients can call public methods
            return OBD2_Background.this;
        }
    }

    protected void OBD2AA_update(OBD2AA obd2) {

        mOBD2AA = obd2;
        if (savedcaractivity==null && obd2!=null)
            savedcaractivity=obd2;
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mNotifyMgr =  (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        editor = prefs.edit();
        mgeodecoder=new myGeoDecoder(this);
        textpaint = new Paint();
        textpaint.setARGB(255, 0, 0, 0);
        textpaint.setTextAlign(Paint.Align.CENTER);
        textpaint.setTextSize(110);
        textpaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        rectF = new RectF();
        rectF.set(16, 16 , 240 ,240);
        geodecoderexecutor = Executors.newSingleThreadExecutor();
        paint=new Paint();
        ShowSpeedCamWarrning=prefs.getBoolean("ShowSpeedCamWarrning",true);
        Log.d("OBD2AA","ShowSpeedCamWarrning: " + ShowSpeedCamWarrning);
        playSound=prefs.getBoolean("play_sound",true);
        isDemoMode=prefs.getBoolean("demomode",false);
        useImperial=prefs.getBoolean("useimperials",true);
        gauge_number = prefs.getInt("gauge_number", 0);
        streetcard=prefs.getBoolean("streetcard",true);
        audio_1=parseInt(prefs.getString("audio_1","800"));
        audio_2=parseInt(prefs.getString("audio_2","400"));
        audio_3=parseInt(prefs.getString("audio_3","100"));
        visual_display=parseInt(prefs.getString("visual_display","1000"));
        if (useImperial)
        {
            audio_1= (int) Math.round(audio_1/1.09361);
            audio_2= (int) Math.round(audio_2/1.09361);
            audio_3= (int) Math.round(audio_3/1.09361);
            visual_display= (int) Math.round(visual_display/1.09361);
        }
        Set<String> selections = prefs.getStringSet("cam_types", new HashSet<String>(Arrays.asList("1,2,3,4".split(","))));
        if (selections.size()==4)
        {
            mobile_filter="";
            static_filter="";
        }
        else
        {
            mobile_filter="and type not in (";
            static_filter="and type not in (";
            if (!selections.contains("1"))
            {
                mobile_filter=mobile_filter + "1,";
                static_filter=static_filter + "7,";
            }
            if (!selections.contains("2"))
            {
                mobile_filter=mobile_filter + "2,";
                static_filter=static_filter + "11,10,";
            }
            if (!selections.contains("3"))
            {
                static_filter=static_filter + "12,13,";
            }
            if (!selections.contains("4"))
            {
                mobile_filter=mobile_filter + "0,3,4,5,6,";
                static_filter=static_filter + "1,2,3,4,5,6,8,9,14,15,";
            }
            mobile_filter=mobile_filter.substring(0,mobile_filter.length()-1)+")";
            static_filter=static_filter.substring(0,static_filter.length()-1)+")";

        }

        pids = new String[gauge_number];
        units = new String[gauge_number];
        CameraRefreshFreq=parseInt(prefs.getString("mobile_update_freq","3000"));
        isdebugging = prefs.getBoolean("debugging", false);
        alternativepulling = prefs.getBoolean("alternativepulling", false);
        for (int i = 1; i <= gauge_number; i++) {
            pids[i - 1] = prefs.getString("gaugepid_" + i, "");
            units[i - 1] = prefs.getString("gaugeunit_" + i, "");
            Log.d("OBD2AA", "Gounde number: " + i + " pid: " + prefs.getString("gaugepid_" + i, "").split(",")[0] + " Unit: " + prefs.getString("gaugeunit_" + i, ""));
        }

        Log.d("OBD2AA", "OBD2 Background Service Created!");

        IntentFilter filter = new IntentFilter();
        filter.addAction("org.prowl.torque.ALARM_TRIGGERED");
        filter.addAction("stop.camera.uk.co.boconi.emil.obd2aa");
        registerReceiver(receiver, filter);

        /* Register Torque Service only if autostart is enabled */
        if (prefs.getBoolean("autostart", false)) {
            startTorque();
            data_fecther();
        }

        if ((!prefs.getBoolean("daynight",false) && (!ShowSpeedCamWarrning)) || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        else
        {
            Log.d("OBD2AA","Registering location listener...");
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            mLocationRequest = new LocationRequest();
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


            if (ShowSpeedCamWarrning){                  //Polling speed depends on setting, only poll faster if we need to detect speed camera.
                    mLocationRequest.setInterval(1000);
                    mLocationRequest.setFastestInterval(1000);
                    new DownloadHelper(1,this,CameraRefreshFreq);
                    refreshmoible();

            }
            else
                mLocationRequest.setInterval(60 * 1000);



            // Create LocationSettingsRequest object using location request
            LocationSettingsRequest.Builder LocationRequestBuilder = new LocationSettingsRequest.Builder();
            LocationRequestBuilder.addLocationRequest(mLocationRequest);



            // new Google API SDK v11 uses getFusedLocationProviderClient(this)
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, new LocationCallback() {
                        @Override
                        public void onLocationResult(LocationResult locationResult) {
                            // do work here
                            try {
                                onLocationChanged(locationResult.getLastLocation());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    },
                    Looper.myLooper());

            mUimodemanager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        }





    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("OBD2AA","OBD2 Background Service on Start");
        if (intent!=null)
            if (intent.hasExtra("muststartTorque"))
                startTorque();
        if (!isrunning)
            data_fecther();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("OBD2AA","OBD2 Background Service on Destroy");
        isrunning=false;
        if (mBind)
        try {
            unbindService(connection);
        }
        catch (Exception E)
        {
            throw E;
        }

        if (receiver!=null)
        try {
            unregisterReceiver(receiver);
        }
        catch (Exception E)
        {
            throw  E;
        }

        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(1983);
        notificationManager.cancel(1984);
        notificationManager.cancel(1985);
        notificationManager.cancel(1986);
        Intent sendIntent = new Intent();
        sendIntent.setAction("org.prowl.torque.REQUEST_TORQUE_QUIT"); //Stor torque
        sendBroadcast(sendIntent);
        android.os.Process.killProcess(android.os.Process.myPid()); //Do a kill.
    }

    private void data_fecther() {

        final String[] fuelpid={prefs.getString("watch_fuel","0"),prefs.getString("coolant_pid","05,0")};

        isrunning=true;

        Log.d("OBD2AA","Data fetcher started....");
        Thread thread = new Thread() {
         @SuppressLint("WrongConstant")
         @Override
            public void run() {
                while (isrunning) {
                    int sleeptime=100;

                    try {

                        //Context mContext = createPackageContext("com.google.android.projection.gearhead", 3);
                        //SharedPreferences myprefs = mContext.getSharedPreferences("engineer_preferences", Context.MODE_PRIVATE);
                        //SharedPreferences.Editor medit = myprefs.edit();
                       // Log.d("OBD2AA","Max speed: " + myprefs.toString());
                        //medit.putString("parking_card_max_speed_mps","200");
                       // medit.apply();
                       // medit.commit();
                        //Log.d("OBD2AA","Driver position: " + localCarInfo.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                    if (prefs.getBoolean("daynight",false) && mUimodemanager!=null && mUimodemanager.getNightMode()!=nightMode)
                        mUimodemanager.setNightMode(nightMode);
                    if (torqueService != null) {


                        if (!ecuconnected)
                            try {

                                //showNotification();
                                if (torqueService.isConnectedToECU()  ) {
                                    ecuconnected = true;
                                }
                            } catch (RemoteException e) {
                               // e.printStackTrace();
                            }
                        else {

                            if (firstfecth)
                            {
                                firstfecth=false;
                                do_pids_sorting();
                            }
                            else
                                try {
                                    if (!alternativepulling)
                                    {
                                        List<String> pidsarry = Arrays.asList(pids);
                                        float myvals[]=torqueService.getPIDValues(pids);
                                        long[] myupdates = torqueService.getPIDUpdateTime(pids);
                                        for (PIDToFetch currpid : pidtofetch) {

                                            int index=pidsarry.indexOf(currpid.getSinglePid());
                                            if (isDemoMode)
                                            {
                                                double newmax=0;
                                                if (currpid.getLastvalue()==0)
                                                    newmax=currpid.getMaxValue();
                                                else
                                                    newmax=currpid.getLastvalue()*1.2;

                                                double random = ThreadLocalRandom.current().nextDouble(Math.max(currpid.getMinValue(),currpid.getLastvalue()/1.2), Math.min(currpid.getMaxValue(),newmax));
                                                currpid.setLastvalue(random);
                                                Message msg = new Message(); //handler.obtainMessage();
                                                msg.obj = Float.parseFloat(String.valueOf(random)); //Make sure you never show more than the MAX.
                                                msg.arg1 = currpid.getGaugeNumber();
                                                mOBD2AA.handler.sendMessage(msg);
                                            }

                                             else if (!isDemoMode && myupdates[index]!=currpid.getLastFetch() && myupdates[index]!=0 && (index==0 || myupdates[index]!=myupdates[Math.max((index-1),0)]))
                                             {
                                                 currpid.putLastFetch(myupdates[index]);
                                                 if (mOBD2AA != null) {
                                                     if (currpid.getNeedsConversion()) {
                                                         if (isdebugging)
                                                             Log.d("OBD2-APP", "PID BEFORE CONVERSION" + currpid.getPID()[0] + " unit: " + currpid.getUnit() + " value= " + myvals[index] + "last updated at: " + myupdates[index]);

                                                         myvals[index] = UnitConvertHelper.ConvertValue(myvals[index], currpid.getUnit());
                                                     }

                                                    /* - Don't mess around with it, let user adjust min/max from preferences!
                                                     if (myvals[index]>currpid.getMaxValue() && !currpid.getSinglePid().equalsIgnoreCase("ff1201,0") && !currpid.getSinglePid().equalsIgnoreCase("ff1203,0"))
                                                     {
                                                         currpid.setMaxValue(Math.round(myvals[index]));
                                                         mOBD2AA.update_gauge_max(currpid.getGaugeNumber()+1,currpid.getMaxValue());
                                                             Log.d("OBD2AA","Need to update Gauge_"+(currpid.getGaugeNumber()+1)+ "Max val: "+Math.round(myvals[index]) + "Stored: " +currpid.getMaxValue());

                                                     }
                                                   */

                                                     Message msg = new Message(); //handler.obtainMessage();
                                                     //Log.d("OBD2AA","Current value: " + myvals[index] + ", min: " + currpid.getMinValue()+ ", max: " + currpid.getMaxValue() + ", logic: " +Math.max(Math.max(myvals[index],currpid.getMinValue()),Math.min(myvals[index],currpid.getMaxValue())));
                                                     msg.obj = Math.max(Math.max(myvals[index],currpid.getMinValue()),Math.min(myvals[index],currpid.getMaxValue())); //Make sure you never show more than the MAX.
                                                     msg.arg1 = currpid.getGaugeNumber();
                                                     mOBD2AA.handler.sendMessage(msg);

                                                 }
                                                 if (isdebugging)
                                                     Log.d("OBD2-APP", "PID   " + currpid.getPID()[0] + " unit: " + currpid.getUnit() + " value= " + myvals[index] + "last updated at: " + myupdates[index]);

                                             }

                                        }
                                        sleeptime=200;
                                    }
                                    else {
                                        for (PIDToFetch currpid : pidtofetch) {
                                            //if (currpid.getActive()) {
                                            //float[] myvals = torqueService.getPIDValues(currpid.getPID());
                                            //float[] myvals = torqueService.getPIDValues(currpid.getPID());
                                            float[] myvals = {0};
                                            myvals[0] = torqueService.getValueForPid(Integer.parseInt(currpid.getPID()[0].split(",")[0], 16), true);
                                            long[] myupdates = torqueService.getPIDUpdateTime(currpid.getPID());
                                            if (myupdates[0] != currpid.getLastFetch() && myupdates[0] != 0) {
                                                currpid.putLastFetch(myupdates[0]);
                                                if (mOBD2AA != null) {
                                                    if (currpid.getNeedsConversion())
                                                        myvals[0]=UnitConvertHelper.ConvertValue(myvals[0],currpid.getUnit());


                                                    if (myvals[0] > currpid.getMaxValue() && !currpid.getPID()[0].equalsIgnoreCase("ff1201") && !currpid.getPID()[0].equalsIgnoreCase("ff1203") && !currpid.getPID()[0].equalsIgnoreCase("ff1207")) {
                                                        currpid.setMaxValue(Math.round(myvals[0]));
                                                        mOBD2AA.update_gauge_max(currpid.getGaugeNumber() + 1, currpid.getMaxValue());
                                                        Log.d("OBD2AA", "Need to update Gauge_" + (currpid.getGaugeNumber() + 1) + "Max val: " + Math.round(myvals[0]) + "Stored: " + currpid.getMaxValue());

                                                    }
                                                   /* {
                                                        if (mOBD2AA!=null) {
                                                            String[] pidinfo = torqueService.getPIDInformation(currpid.getPID());
                                                            if (parseInt(pidinfo[3]) > currpid.getMaxValue())
                                                                currpid.setMaxValue(parseInt(pidinfo[3]));
                                                            mOBD2AA.update_gauge_max(currpid.getGaugeNumber(),currpid.getMaxValue());
                                                        }
                                                    }*/

                                                    Message msg = new Message(); //handler.obtainMessage();
                                                    msg.obj = myvals[0];
                                                    msg.arg1 = currpid.getGaugeNumber();
                                                    mOBD2AA.handler.sendMessage(msg);

                                                }
                                                if (isdebugging)
                                                    Log.d("OBD2-APP", "PID   " + currpid.getPID()[0] + " unit: " + currpid.getUnit() + " value= " + myvals[0] + "last updated at: " + myupdates[0]);

                                            }
                                            // }
                                        }
                                        sleeptime=200;
                                    }
                                } catch (Exception E) {

                                }
                        }
                    }
                    else
                        Log.d("OBS2AA","Not connected to Torque Service yet...");

                    try {

                        Thread.sleep(sleeptime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }


                }
             Log.d("OBS2AA","Running StopSelf...");
             isrunning=false;
             if (mBind)
                 try {
                     unbindService(connection);
                     mBind=false;
                 }
                 catch (Exception E)
                 {
                     throw E;
                 }
             connection=null;
             if (receiver!=null)
             try {
                 unregisterReceiver(receiver);
             }
             catch (Exception E)
             {
                 throw  E;
             }
             receiver=null;
             Intent sendIntent = new Intent();
             sendIntent.setAction("org.prowl.torque.REQUEST_TORQUE_QUIT");
             sendBroadcast(sendIntent);
             stopSelf();
            }
        };
        //Second Thread for monitoring fuel, this can pull much slower, once every 30 seconds for example.
        Thread fuelwatcher=new Thread() {
            @Override
            public void run() {
                int lastwarningvalue=0;
                boolean miles=false;
                boolean celsius=true;
                boolean first=true;
                boolean warm_engine=false;
                int warm_engine_degree=parseInt(prefs.getString("coolanttemp",""));
                while (isrunning) {
                if (torqueService!=null)
                        {
                            if (first)
                            {
                                first=false;
                                try {
                                    miles=!torqueService.getPreferredUnit("km").equalsIgnoreCase("km");
                                    celsius=torqueService.getPreferredUnit("°C").equalsIgnoreCase("°C");
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            }

                            try {

                                if (prefs.getBoolean("monitorfuel",false)) {  //If we should monitor fuel
                                    int fuelval = Math.round(torqueService.getPIDValues(fuelpid)[0]);
                                    if ((fuelval < 80) && (fuelpid[0].equalsIgnoreCase("ff126a")) || fuelval < 5) {
                                        if (lastwarningvalue != fuelval) {
                                            String warrning = "";
                                            if (fuelpid[0].equalsIgnoreCase("ff126a")) {
                                                if (miles)
                                                   // warrning = "Estimated fuel range is only: " + Math.round(fuelval / 1.60) + " miles.";
                                                    warrning=getString(R.string.est_range, Math.round(fuelval / 1.60)," miles");
                                                else
                                                    warrning=getString(R.string.est_range, fuelval," km");
                                            } else
                                                warrning = getString(R.string.rem_fuel, fuelval," %");
                                            lastwarningvalue = fuelval;
                                            showNotification(getResources().getString(R.string.low_fuel_tit), warrning, R.drawable.ic_danger_r, R.drawable.fuel);
                                        }
                                    }
                                }
                                if (prefs.getBoolean("monitorcoolant",false)) { //If we should monitor coolant
                                    float coolantval=torqueService.getPIDValues(fuelpid)[1];
                                    if (!celsius)
                                        coolantval=UnitConvertHelper.ConvertValue(coolantval,"°C");

                                    if (!warm_engine && coolantval>=warm_engine_degree)
                                    {
                                        Log.d("OBD2AA","Should show the engine temp warning.");
                                        warm_engine=true;
                                        showNotification(getResources().getString(R.string.coolant_ok), getResources().getString(R.string.engine_warm), R.drawable.ic_danger_green, R.drawable.ic_coolant);

                                        Thread.sleep(10000);
                                        Log.d("OBD2AA","Should clear warm engine temp");
                                        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                                        notificationManager.cancel(1984);

                                    }
                                }

                            } catch (RemoteException e) {
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        };
        thread.start();
        if (prefs.getBoolean("monitorfuel",false) || prefs.getBoolean("monitorcoolant",false))
          fuelwatcher.start();

    }

    protected String getUnit(String unit)
    {
        if (torqueService==null)
            return "";

        try {
            return torqueService.getPreferredUnit(unit);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return unit;
    }


    private void do_pids_sorting() {

        Log.d("OBD2AA","PIDS to string....");
        try {
            String[] pidsdesc = torqueService.getPIDInformation(pids);

            for (int i = 0; i < pids.length; i++) {                                 //Build the pidstofetch object with correct data
                boolean needsconversion=!(torqueService.getPreferredUnit(units[i]).equalsIgnoreCase(units[i]));
               // Log.d("OBD2AA","Pid "+pids[i] + "Status: " +pidMap.get(pids[i]));

                String [] info=pidsdesc[i].split(",");
               // Log.d("OBD2AA"," Max val stored for pid (" + pids[i]+"): "+prefs.getFloat("maxval_" + (i+1), 0) +" Reported from Torque: " + parseInt(info[3]) + "units: " +units[i] + ",Locked: " + prefs.getBoolean("locked_"+(i+1),false) +"needconversion: "+needsconversion);

                if (!prefs.getBoolean("locked_"+(i+1),false) && prefs.getFloat("maxval_" + (i+1), 0)!=Float.parseFloat(info[3]))
                {
                    if (mOBD2AA!=null)
                        mOBD2AA.update_gauge_max((i+1),Float.parseFloat(info[3]));
                    editor.putFloat("maxval_"+(i+1),Float.parseFloat(info[3]));
                    editor.apply();

                }
                if (needsconversion)
                {
                    int max=Math.round(UnitConvertHelper.ConvertValue(Float.parseFloat(info[3]),units[i]));

                    if (!prefs.getBoolean("locked_"+(i+1),false) && prefs.getFloat("maxval_" + (i+1), 0)!=max)
                    {
                        if (mOBD2AA!=null)
                            mOBD2AA.update_gauge_max((i+1),max);
                        Log.d("OBD2AA","Need to update Gauge_"+(i+1)+ "Max val: "+max);
                        editor.putFloat("maxval_"+(i+1),max);
                        editor.apply();
                    }
                    int min=Math.round(UnitConvertHelper.ConvertValue(Float.parseFloat(info[4]),units[i]));
                    if (!prefs.getBoolean("locked_"+(i+1),false) && prefs.getFloat("minval_" + (i+1), 0)!=min)
                    {
                        if (mOBD2AA!=null)
                            mOBD2AA.update_gauge_min((i+1),min);
                        Log.d("OBD2AA","Need to update Gauge_"+(i+1)+ "Min val: "+min);
                        editor.putFloat("minval_"+(i+1),min);
                        editor.apply();
                    }

                }
                try {
                    pidtofetch.add(new PIDToFetch(pids[i], true, 0, i, units[i], needsconversion, prefs.getFloat("maxval_" + (i + 1), 0), prefs.getFloat("minval_" + (i + 1), 0)));
                }
                catch (Exception E)
                {
                    //Bad luck again with previousely stored min/max as integer let's convert it to float and try again
                    editor.putFloat("maxval_"+(i+1),prefs.getInt("maxval_"+(i+1),0));
                    editor.putFloat("minval_"+(i+1),prefs.getInt("minval_"+(i+1),0));
                    editor.apply();
                    pidtofetch.add(new PIDToFetch(pids[i], true, 0, i, units[i], needsconversion, prefs.getFloat("maxval_" + (i + 1), 0), prefs.getFloat("minval_" + (i + 1), 0)));
                }
            }



        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void startTorque ()
    {
        Intent intent = new Intent();
        intent.setClassName("org.prowl.torque", "org.prowl.torque.remote.TorqueService");
        startService(intent);
        boolean successfulBind = bindService(intent, connection, 0);
        if (successfulBind) {
            mBind=true;
            Log.d("HU", "Connected to torque service!");
        } else {
            mBind=false;
            Log.e("HU", "Unable to connect to Torque plugin service");
        }
    }

    protected void showNotification(String Title, String Subtitle, int actionicon, int thumbnail){



        CarNotificationExtender paramString2 = new CarNotificationExtender.Builder()
                .setTitle(Title)
                .setSubtitle(Subtitle)
                .setShouldShowAsHeadsUp(true)
                .setActionIconResId(actionicon)
                .setBackgroundColor(Color.WHITE)
                .setNightBackgroundColor(Color.DKGRAY)
                .setThumbnail(BitmapFactory.decodeResource(OBD2_Background.this.getResources(), thumbnail))
                .build();

        NotificationCompat.Builder mynot = new NotificationCompat.Builder(getApplicationContext())
                .setContentTitle(Title)
                .setContentText(Subtitle)
                .setLargeIcon(BitmapFactory.decodeResource(OBD2_Background.this.getResources(), thumbnail))
                .setSmallIcon(actionicon)
                .extend(paramString2);


        mNotifyMgr.notify(1984,mynot.build());


    }

    protected void showNotification(String Title, String Subtitle, int actionicon, Bitmap thumbnail, int cameraid){



        Intent intent = new Intent(this, OBD2_Background.class);
        intent.setAction("stop.camera.uk.co.boconi.emil.obd2aa");
        intent.putExtra("camerid",cameraid);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), 0, intent, 0);

        CarNotificationExtender paramString2 = new CarNotificationExtender.Builder()
                .setTitle(Title)
                .setSubtitle(Subtitle)
                .setShouldShowAsHeadsUp(true)
                .setActionIconResId(actionicon)
                .setBackgroundColor(Color.WHITE)
                .setNightBackgroundColor(Color.DKGRAY)
                .setThumbnail(thumbnail)

                .build();

        NotificationCompat.Builder mynot = new NotificationCompat.Builder(this)
                .setContentTitle(Title)
                .setContentText(Subtitle)
                .setLargeIcon(thumbnail)
                .setSmallIcon(actionicon)
                .extend(paramString2);

        mynot.setContentIntent(pendingIntent).setDeleteIntent(pendingIntent);

        mNotifyMgr.notify(1985,mynot.build());

/*

        NotificationCompat.Builder xxx = new NotificationCompat.Builder(this);
        xxx.setSmallIcon(R.drawable.ic_danger_r);
        xxx.setSubText("Sub Text test")
                .setContentTitle("TITLE")
                .setContentText("CONTENT TEXT")
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.road))
                .setContentIntent(pendingIntent)


               new CarNavExtender()
                        .setContentId(2)
                        .setContentTextDay("Text Day")
                        .setContentTextNight("Text night")
                        .setContentTitleDay("Title Day")
                        .setContentTitleNight("Title Night")
                        .setSubTextDay("Subtext Day")
                        .setSubTextNight("Subtext Night")
                        .setColorDay(Color.BLUE)
                        .setColorNight(-15850947)
                        .setShowInStream(true)
                        .setShowAsHeadsUp(false)
                        .setIgnoreInStream(false)
                        .setContentIntent(new Intent())
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.road))
                        .setActionIconDay(R.drawable.ic_info_outline_black_24dp)
                        .setActionIconNight(R.drawable.ic_info_outline_black_24dp)
                        .setType(0)
                       .extend(xxx);



        mNotifyMgr.notify(31002, xxx.build());
        */
    }

    private ServiceConnection connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName arg0, IBinder service) {
            Log.d("HU", "SERVICE CONNECTED!");
            torqueService = ITorqueService.Stub.asInterface(service);

            try {
                if (torqueService.getVersion() < 19) {
                    Log.d("OBD2-APP", "Incorrect version. You are using an old version of Torque with this plugin.\n\nThe plugin needs the latest version of Torque to run correctly.\n\nPlease upgrade to the latest version of Torque from Google Play");
                    return;
                }
            } catch (RemoteException e) {

            }
            Log.d("HU", "Have Torque service connection, starting fetching");
         }
       public void onServiceDisconnected(ComponentName name) {
            torqueService = null;
        }
    };


    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle torqueAlarm = intent.getExtras();
            Log.d("OBD2AA","Receiver : " + intent.getAction());
            /*
            if (torqueAlarm != null) {
                for (String key : torqueAlarm.keySet()) {
                    Object value = torqueAlarm.get(key);
                    Log.d("OBD2AA", String.format("%s %s (%s)", key,
                            value.toString(), value.getClass().getName()));
                }
            }
            */

            //Intent localIntent = new Intent(getApplicationContext(), MyOdbService.class);
            if (intent.getAction().equalsIgnoreCase("stop.camera.uk.co.boconi.emil.obd2aa"))
            {
                if (prevCamera.getid()==torqueAlarm.getInt("camerid",0))
                        prevCamera.setShownot(true);
                return;
            }
            String text;
            if (torqueAlarm.getString("ALARM_TYPE").equalsIgnoreCase("MIN"))
                text= "Current value " + String.format("%.2f",torqueAlarm.getDouble("CURRENT_VALUE")) + " " + torqueAlarm.getString("UNIT") + " is lower than: " + String.format("%.2f",torqueAlarm.getDouble("TRIGGER_VALUE")) + " " + torqueAlarm.getString("UNIT");
            else
                text= "Current value " + String.format("%.2f",torqueAlarm.getDouble("CURRENT_VALUE")) + " " + torqueAlarm.getString("UNIT") + " is over than: " + String.format("%.2f",torqueAlarm.getDouble("TRIGGER_VALUE")) + " " + torqueAlarm.getString("UNIT");

            showNotification(torqueAlarm.getString("ALARM_NAME"),text,R.drawable.ic_category_engine,R.drawable.ic_danger_r);


        }
    };

    private Location lastlocation=null;
    private double accumulated_distance=999;
    private long lastcardupdate;


    public void onLocationChanged(final Location location) throws Exception {

    if (location.getAccuracy()>35)  //Ignore any location whith accuracy less than 35 meters.
        return;
        float bearingdiff=0;
       try{
           bearingdiff = Math.abs(location.getBearing() - lastlocation.getBearing());
           accumulated_distance+=SphericalUtil.computeDistanceBetween(new LatLng(location.getLatitude(),location.getLongitude()), new LatLng(lastlocation.getLatitude(),lastlocation.getLongitude()));

       }
       catch (Exception e)
       {
           Log.e("OBD2AA",e.getMessage());

       }


            lastlocation=location;

      // Log.d("OBD2AA","Speedcam: " + isSpeedCamShowing + "Streetcard: " + streetcard + "bearingdiff: " + bearingdiff);
        if (!isSpeedCamShowing && streetcard && ((lastcardupdate<System.currentTimeMillis()-5000 && accumulated_distance>=200)))// || bearingdiff>60))
        {
            geodecoderexecutor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        mgeodecoder.dedoceaddress(location);
                        synchronized (this)
                        {
                            lastcardupdate=System.currentTimeMillis();
                            accumulated_distance=0;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });


        }


        if (ShowSpeedCamWarrning) {
            camera=null;

            if (prevCamera!=null && isCameraInWay.stillinRange(location,prevCamera,visual_display))
                camera=prevCamera;
            else {
                isCameraInWay iscamera = new isCameraInWay(location, mobileDB, staticDB,visual_display,mobile_filter,static_filter);
                if (iscamera.getCamera() != null)
                    camera=iscamera.getCamera();
            }

            //Log.d("OBD2AA","Camera is: " + camera);
            if (camera != null) {
                prevCamera=camera;
                int [] resources=camera.geticon();
                Bitmap bmp;
                if (resources[2]==1)
                {
                    bmp = BitmapFactory.decodeResource(getResources(),R.drawable.traffic_light);
                }
                else {
                    bmp = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bmp);
                    paint.setColor(Color.WHITE);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawArc(rectF, 0, 360, false, paint);
                    paint.setStrokeWidth(24);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setColor(Color.RED);
                    canvas.drawArc(rectF, 0, 360, false, paint);
                    canvas.drawText(camera.getspeed(), 128, (128 - ((textpaint.descent() + textpaint.ascent()) / 2)), textpaint);
                }
                if (camera.getShownot()) {
                    if (useImperial)
                        showNotification(getString(resources[1]) + " " + Math.round(camera.getDistaceToCam() * 1.0936) + " yd", camera.getstreet(), resources[0], bmp, camera.getid());
                     else
                        showNotification(getString(resources[1]) + " " + camera.getDistaceToCam() + " m", camera.getstreet(), resources[0], bmp, camera.getid());

                }

                     int sound_to_play=0;
                    if (camera.getDistaceToCam() > audio_2 && camera.getDistaceToCam() <= audio_1 && !camera.getShow_warrning(0)) {
                        sound_to_play = R.raw.beep;
                        camera.setShow_warrning(0,true);
                    }
                    if (camera.getDistaceToCam() > audio_3 && camera.getDistaceToCam() <= audio_2 && !camera.getShow_warrning(1)) {
                        sound_to_play = R.raw.beepbeep;
                        camera.setShow_warrning(1,true);
                    }
                    if (camera.getDistaceToCam() <= audio_3 && !camera.getShow_warrning(2)) {
                        sound_to_play = R.raw.beepbeepbeep;
                        camera.setShow_warrning(2,true);
                    }

                    if (!playSound)
                        {
                            CarNotificationSoundPlayer soundPlayer = new CarNotificationSoundPlayer(OBD2_Background.this, sound_to_play);
                            soundPlayer.play();
                        }
                    else
                        {
                             MediaPlayer mp = MediaPlayer.create(this, sound_to_play);
                             mp.start();
                        }

                isSpeedCamShowing = true;
            } else if (isSpeedCamShowing)
            {   Log.d("OBD2AA","Removing speed camera notification...");
                mNotifyMgr.cancel(1985);
                isSpeedCamShowing=false;
                prevCamera=null;
            }
        }

        if ((System.currentTimeMillis()-lastcalc)>600000) {
            lastcalc=System.currentTimeMillis();
            nightMode = Calculate_Sunset_Sunrise(location);
        }
    }




    private void populateLocationCarSensorEvent(CarSensorEvent paramCarSensorEvent, Location paramLocation)
    {

        paramCarSensorEvent.intValues[1] = ((int)(0 * 1.0E7D));
        int j = 0x0 | 0x2 | 0x1;
        paramCarSensorEvent.intValues[1] = ((int)(0 * 1.0E7D));
        int i = j;

            i = j | 0x4;
            paramCarSensorEvent.floatValues[2] = 99999f;



            i = j | 0x10;
            paramCarSensorEvent.floatValues[4] = 0;


        paramCarSensorEvent.intValues[0] = j;
    }

    private void refreshmoible(){
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isdebugging)
                    Log.d("OBD2AA","Refreshing the mobile database");
                new DownloadHelper(1,getBaseContext(),CameraRefreshFreq);
                refreshmoible();
            }
        },60000);
    }
}
