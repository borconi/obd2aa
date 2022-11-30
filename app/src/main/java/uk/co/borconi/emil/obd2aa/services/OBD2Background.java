package uk.co.borconi.emil.obd2aa.services;


import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Context.UI_MODE_SERVICE;
import static uk.co.borconi.emil.obd2aa.helpers.SunSet.Calculate_Sunset_Sunrise;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import org.greenrobot.eventbus.EventBus;
import org.prowl.torque.remote.ITorqueService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import uk.co.borconi.emil.obd2aa.R;
import uk.co.borconi.emil.obd2aa.androidauto.OBD2AA;
import uk.co.borconi.emil.obd2aa.gauge.GaugeUpdate;
import uk.co.borconi.emil.obd2aa.helpers.CameraDataBaseHelper;
import uk.co.borconi.emil.obd2aa.helpers.DownloadHelper;
import uk.co.borconi.emil.obd2aa.helpers.NearbyCameras;
import uk.co.borconi.emil.obd2aa.helpers.PreferencesHelper;
import uk.co.borconi.emil.obd2aa.helpers.UnitConvertHelper;
import uk.co.borconi.emil.obd2aa.helpers.isCameraInWay;
import uk.co.borconi.emil.obd2aa.helpers.myGeoDecoder;
import uk.co.borconi.emil.obd2aa.pid.PIDToFetch;


/**
 * Created by Emil on 31/08/2017.
 */


public class OBD2Background {

    public static boolean isdebugging;
    static volatile boolean isrunning;
    private final Context context;
    private final OBD2AA mOBD2AA = null;
    private final List<PIDToFetch> pidtofetch = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    public boolean ecuconnected = true;
    NotificationManager mNotifyMgr;
    private ITorqueService torqueService;
    private String[] pids;
    private PreferencesHelper prefs;
    private SharedPreferences.Editor editor;
    private String[] units;
    private boolean alternativepulling;
    private boolean firstfecth = true;
    private boolean mBind = false;
    private int nightMode = 0;
    private long lastcalc = 0;
    private UiModeManager mUimodemanager = null;
    private CameraDataBaseHelper staticDB;
    private CameraDataBaseHelper mobileDB;
    private boolean isSpeedCamShowing;
    private boolean ShowSpeedCamWarrning;
    private boolean playSound;
    private boolean isDemoMode;
    private Paint textpaint;
    private Paint paint;
    private RectF rectF;
    private int audio_1, audio_2, audio_3, visual_display;
    private boolean useImperial;
    private myGeoDecoder mgeodecoder;

    private NearbyCameras prevCamera;
    private boolean streetcard;
    private ExecutorService geodecoderexecutor;
    private int CameraRefreshFreq;
    private String mobile_filter, static_filter;
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
                throw new RuntimeException(e);
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
            Log.d("OBD2AA", "Receiver : " + intent.getAction());

            //Intent localIntent = new Intent(getApplicationContext(), MyOdbService.class);
            if (intent.getAction().equalsIgnoreCase("stop.camera.uk.co.boconi.emil.obd2aa")) {
                if (prevCamera.getid() == torqueAlarm.getInt("camerid", 0)) {
                    prevCamera.setShownot(true);
                }
                return;
            }
            String text;
            if (torqueAlarm.getString("ALARM_TYPE").equalsIgnoreCase("MIN")) {
                text = "Current value " + String.format("%.2f", torqueAlarm.getDouble("CURRENT_VALUE")) + " " + torqueAlarm.getString("UNIT") + " is lower than: " + String.format("%.2f", torqueAlarm.getDouble("TRIGGER_VALUE")) + " " + torqueAlarm.getString("UNIT");
            } else {
                text = "Current value " + String.format("%.2f", torqueAlarm.getDouble("CURRENT_VALUE")) + " " + torqueAlarm.getString("UNIT") + " is over than: " + String.format("%.2f", torqueAlarm.getDouble("TRIGGER_VALUE")) + " " + torqueAlarm.getString("UNIT");
            }

            showNotification(torqueAlarm.getString("ALARM_NAME"), text, R.drawable.ic_category_engine, R.drawable.ic_danger_r);
        }
    };
    private Location lastlocation = null;
    private double accumulated_distance = 999;
    private long lastcardupdate;


    public OBD2Background(ITorqueService torqueService, Context context) {
        this.torqueService = torqueService;
        this.context = context;

        onCreate();
    }

    public void onCreate() {
        staticDB = new CameraDataBaseHelper(context, 2, "fixedcamera");
        mobileDB = new CameraDataBaseHelper(context, 1, "mobilecamera");
        prefs = PreferencesHelper.getPreferences(context);
        mNotifyMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        editor = prefs.edit();
        mgeodecoder = new myGeoDecoder(context);
        textpaint = new Paint();
        textpaint.setARGB(255, 0, 0, 0);
        textpaint.setTextAlign(Paint.Align.CENTER);
        textpaint.setTextSize(110);
        textpaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        rectF = new RectF();
        rectF.set(16, 16, 240, 240);
        geodecoderexecutor = Executors.newSingleThreadExecutor();
        paint = new Paint();
        ShowSpeedCamWarrning = prefs.shouldShowSpeedCamWarning();
        Log.d("OBD2AA", "ShowSpeedCamWarrning: " + ShowSpeedCamWarrning);
        playSound = prefs.shouldPlaySound();
        isDemoMode = prefs.isInDemoMode();
        useImperial = prefs.shouldUseImperial();
        int gaugeNumber = prefs.getNumberOfGauges();
        streetcard = prefs.shouldHaveStreetCard();
        audio_1 = prefs.getAudio1();
        audio_2 = prefs.getAudio2();
        audio_3 = prefs.getAudio3();
        visual_display = prefs.getVisualDisplay();

        if (useImperial) {
            audio_1 = (int) Math.round(audio_1 / 1.09361);
            audio_2 = (int) Math.round(audio_2 / 1.09361);
            audio_3 = (int) Math.round(audio_3 / 1.09361);
            visual_display = (int) Math.round(visual_display / 1.09361);
        }
        Set<String> selections = prefs.getCamTypes();
        if (selections.size() == 4) {
            mobile_filter = "";
            static_filter = "";
        } else {
            mobile_filter = "and type not in (";
            static_filter = "and type not in (";
            if (!selections.contains("1")) {
                mobile_filter = mobile_filter + "1,";
                static_filter = static_filter + "7,";
            }
            if (!selections.contains("2")) {
                mobile_filter = mobile_filter + "2,";
                static_filter = static_filter + "11,10,";
            }
            if (!selections.contains("3")) {
                static_filter = static_filter + "12,13,";
            }
            if (!selections.contains("4")) {
                mobile_filter = mobile_filter + "0,3,4,5,6,";
                static_filter = static_filter + "1,2,3,4,5,6,8,9,14,15,";
            }
            mobile_filter = mobile_filter.substring(0, mobile_filter.length() - 1) + ")";
            static_filter = static_filter.substring(0, static_filter.length() - 1) + ")";

        }

        pids = new String[gaugeNumber];
        units = new String[gaugeNumber];
        CameraRefreshFreq = prefs.getUpdateFrequency();
        isdebugging = prefs.isDebugging();
        alternativepulling = prefs.hasAlternativePulling();
        for (int i = 1; i <= gaugeNumber; i++) {
            pids[i - 1] = prefs.getPidForGauge(i);
            units[i - 1] = prefs.getUnitForGauge(i);
            Log.d("OBD2AA", "Gounde number: " + i + " pid: " + prefs.getPidForGauge(i).split(",")[0] + " Unit: " + prefs.getUnitForGauge(i));
        }

        Log.d("OBD2AA", "OBD2 Background Service Created!");

        IntentFilter filter = new IntentFilter();
        filter.addAction("org.prowl.torque.ALARM_TRIGGERED");
        filter.addAction("stop.camera.uk.co.boconi.emil.obd2aa");
        context.registerReceiver(receiver, filter);

        /* Register Torque Service only if autostart is enabled */
      /*  if (prefs.getBoolean("autostart", false)) {
            startTorque();
            data_fecther();
        }*/

        dataFetcher();
        if ((!prefs.isNight() && (!ShowSpeedCamWarrning)) || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        } else {
            Log.d("OBD2AA", "Registering location listener...");
            FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
            LocationRequest mLocationRequest = new LocationRequest();
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


            if (ShowSpeedCamWarrning) {                  //Polling speed depends on setting, only poll faster if we need to detect speed camera.
                mLocationRequest.setInterval(1000);
                mLocationRequest.setFastestInterval(1000);
                new DownloadHelper(1, context, CameraRefreshFreq);
                refreshMobile();

            } else
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

            mUimodemanager = (UiModeManager) context.getSystemService(UI_MODE_SERVICE);
        }
    }

    public void onDestroy() {
        Log.d("OBD2AA", "OBD2 Background Service on Destroy");
        isrunning = false;
        if (mBind) {
            context.unbindService(connection);
        }
        if (receiver != null) {
            context.unregisterReceiver(receiver);
        }
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(1983);
        notificationManager.cancel(1984);
        notificationManager.cancel(1985);
        notificationManager.cancel(1986);
        Intent sendIntent = new Intent();
        sendIntent.setAction("org.prowl.torque.REQUEST_TORQUE_QUIT"); //Stor torque
        context.sendBroadcast(sendIntent);
        android.os.Process.killProcess(android.os.Process.myPid()); //Do a kill.
    }

    private void dataFetcher() {
        final String[] fuelpid = {prefs.watchFuel(), prefs.coolantPid()};

        isrunning = true;
        Log.d("OBD2AA", "Data fetcher started....");
        Thread thread = new Thread() {
            /* JADX WARNING: No exception handlers in catch block: Catch:{  } */
            @SuppressLint({"WrongConstant"})
            public void run() {
                int i;
                char c;
                double d;
                try {
                    while (OBD2Background.isrunning) {
                        if (!(!prefs.isNight() || mUimodemanager == null || mUimodemanager.getNightMode() == nightMode)) {
                            mUimodemanager.setNightMode(nightMode);
                        }
                        if (torqueService != null) {
                            if (!ecuconnected) {
                                try {
                                    if (torqueService.isConnectedToECU()) {
                                        ecuconnected = true;
                                    }
                                } catch (RemoteException unused) {
                                }
                            } else if (firstfecth) {
                                firstfecth = false;
                                sortPids();
                            } else if (!alternativepulling) {
                                List asList = Arrays.asList(pids);
                                float[] pIDValues = torqueService.getPIDValues(pids);
                                if (OBD2Background.isdebugging) {
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("Pids requested: ");
                                    sb.append(Arrays.toString(pids));
                                    Log.d("OBD2-APP", sb.toString());
                                    StringBuilder sb2 = new StringBuilder();
                                    sb2.append("Float values: ");
                                    sb2.append(Arrays.toString(pIDValues));
                                    Log.d("OBD2-APP", sb2.toString());
                                }
                                long[] pIDUpdateTime = torqueService.getPIDUpdateTime(pids);
                                for (PIDToFetch pIDToFetch : pidtofetch) {
                                    int indexOf = asList.indexOf(pIDToFetch.getSinglePid());
                                    if (indexOf == 0 || pIDValues[indexOf] != pIDValues[indexOf - 1]) {
                                        if (isDemoMode) {
                                            if (pIDToFetch.getLastvalue() == 0.0d) {
                                                d = pIDToFetch.getMaxValue();
                                            } else {
                                                d = pIDToFetch.getLastvalue() * 1.2d;
                                            }
                                            double nextDouble = ThreadLocalRandom.current().nextDouble(Math.max(pIDToFetch.getMinValue(), pIDToFetch.getLastvalue() / 1.2d), Math.min(pIDToFetch.getMaxValue(), d));
                                            pIDToFetch.setLastvalue(nextDouble);
                                            if (EventBus.getDefault().hasSubscriberForEvent(GaugeUpdate.class))
                                                EventBus.getDefault().post(new GaugeUpdate(pIDToFetch.getGaugeNumber(), (float) nextDouble));


                                        } else if (!(pIDUpdateTime[indexOf] == pIDToFetch.getLastFetch() || pIDUpdateTime[indexOf] == 0)) {
                                            pIDToFetch.putLastFetch(pIDUpdateTime[indexOf]);

                                            if (pIDToFetch.getNeedsConversion()) {
                                                if (OBD2Background.isdebugging) {
                                                    StringBuilder sb3 = new StringBuilder();
                                                    sb3.append("PID BEFORE CONVERSION");
                                                    sb3.append(pIDToFetch.getPID()[0]);
                                                    sb3.append(" unit: ");
                                                    sb3.append(pIDToFetch.getUnit());
                                                    sb3.append(" value= ");
                                                    sb3.append(pIDValues[indexOf]);
                                                    sb3.append("last updated at: ");
                                                    sb3.append(pIDUpdateTime[indexOf]);
                                                    Log.d("OBD2-APP", sb3.toString());
                                                }
                                                pIDValues[indexOf] = UnitConvertHelper.ConvertValue(pIDValues[indexOf], pIDToFetch.getUnit());
                                            }
                                            if (EventBus.getDefault().hasSubscriberForEvent(GaugeUpdate.class)) {
                                                GaugeUpdate update = new GaugeUpdate(
                                                        pIDToFetch.getGaugeNumber(),
                                                        Math.max(
                                                                Math.max(pIDValues[indexOf], pIDToFetch.getMinValue()),
                                                                Math.min(pIDValues[indexOf], pIDToFetch.getMaxValue())
                                                        ));
                                                EventBus.getDefault().post(update);
                                            }

                                            if (OBD2Background.isdebugging) {
                                                StringBuilder sb5 = new StringBuilder();
                                                sb5.append("PID   ");
                                                sb5.append(pIDToFetch.getPID()[0]);
                                                sb5.append(" unit: ");
                                                sb5.append(pIDToFetch.getUnit());
                                                sb5.append(" value= ");
                                                sb5.append(pIDValues[indexOf]);
                                                sb5.append("last updated at: ");
                                                sb5.append(pIDUpdateTime[indexOf]);
                                                Log.d("OBD2-APP", sb5.toString());
                                            }
                                        }
                                    }
                                }
                                i = 100;
                                Thread.sleep(i);
                            } else {
                                for (PIDToFetch pIDToFetch2 : pidtofetch) {
                                    float[] fArr = {0.0f};
                                    fArr[0] = torqueService.getValueForPid(Integer.parseInt(pIDToFetch2.getPID()[0].split(",")[0], 16), true);
                                    long[] pIDUpdateTime2 = torqueService.getPIDUpdateTime(pIDToFetch2.getPID());
                                    if (!(pIDUpdateTime2[0] == pIDToFetch2.getLastFetch() || pIDUpdateTime2[0] == 0)) {
                                        pIDToFetch2.putLastFetch(pIDUpdateTime2[0]);
                                        if (mOBD2AA != null) {
                                            if (pIDToFetch2.getNeedsConversion()) {
                                                c = 0;
                                                fArr[0] = UnitConvertHelper.ConvertValue(fArr[0], pIDToFetch2.getUnit());
                                            } else {
                                                c = 0;
                                            }
                                            if (fArr[c] > pIDToFetch2.getMaxValue() && !pIDToFetch2.getPID()[c].equalsIgnoreCase("ff1201") && !pIDToFetch2.getPID()[c].equalsIgnoreCase("ff1203") && !pIDToFetch2.getPID()[c].equalsIgnoreCase("ff1207")) {
                                                pIDToFetch2.setMaxValue((float) Math.round(fArr[c]));

                                                if (EventBus.getDefault().hasSubscriberForEvent(GaugeUpdate.class))
                                                    EventBus.getDefault().post(new GaugeUpdate(pIDToFetch2.getGaugeNumber(), pIDToFetch2.getMaxValue(), true, false));


                                                StringBuilder sb6 = new StringBuilder();
                                                sb6.append("Need to update Gauge_");
                                                sb6.append(pIDToFetch2.getGaugeNumber() + 1);
                                                sb6.append("Max val: ");
                                                sb6.append(Math.round(fArr[0]));
                                                sb6.append("Stored: ");
                                                sb6.append(pIDToFetch2.getMaxValue());
                                                Log.d("OBD2AA", sb6.toString());
                                            }
                                            if (EventBus.getDefault().hasSubscriberForEvent(GaugeUpdate.class))
                                                EventBus.getDefault().post(new GaugeUpdate(pIDToFetch2.getGaugeNumber(), fArr[0]));


                                        }
                                        if (OBD2Background.isdebugging) {
                                            StringBuilder sb7 = new StringBuilder();
                                            sb7.append("PID   ");
                                            sb7.append(pIDToFetch2.getPID()[0]);
                                            sb7.append(" unit: ");
                                            sb7.append(pIDToFetch2.getUnit());
                                            sb7.append(" value= ");
                                            sb7.append(fArr[0]);
                                            sb7.append("last updated at: ");
                                            sb7.append(pIDUpdateTime2[0]);
                                            Log.d("OBD2-APP", sb7.toString());
                                        }
                                    }
                                }
                            }
                        } else
                            try {
                                Thread.sleep(250);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                    }
                    Log.d("OBS2AA", "Running StopSelf...");
                    OBD2Background.isrunning = false;
                    if (mBind) {
                        mBind = false;
                    }
                    connection = null;
                    if (receiver != null) {
//                        unregisterReceiver(receiver);
                    }
                    receiver = null;
                    Intent intent = new Intent();
                    intent.setAction("org.prowl.torque.REQUEST_TORQUE_QUIT");
//                    sendBroadcast(intent);
//                    stopSelf();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        };

        //Second Thread for monitoring fuel, this can pull much slower, once every 30 seconds for example.
        Thread fuelwatcher = new Thread() {
            @Override
            public void run() {
                int lastwarningvalue = 0;
                boolean miles = false;
                boolean celsius = true;
                boolean first = true;
                boolean warm_engine = false;
                int warm_engine_degree = prefs.getCoolantWarningValue();
                while (isrunning) {
                    if (torqueService != null) {
                        if (first) {
                            first = false;
                            try {
                                miles = !torqueService.getPreferredUnit("km").equalsIgnoreCase("km");
                                celsius = torqueService.getPreferredUnit("°C").equalsIgnoreCase("°C");
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }

                        try {

                            if (prefs.shouldMonitorFuel()) {  //If we should monitor fuel
                                int fuelval = Math.round(torqueService.getPIDValues(fuelpid)[0]);
                                if ((fuelval < 80) && (fuelpid[0].equalsIgnoreCase("ff126a")) || fuelval < 5) {
                                    if (lastwarningvalue != fuelval) {
                                        String warrning = "";
                                        if (fuelpid[0].equalsIgnoreCase("ff126a")) {
                                            if (miles)
                                                // warrning = "Estimated fuel range is only: " + Math.round(fuelval / 1.60) + " miles.";
                                                warrning = context.getString(R.string.est_range, Math.round(fuelval / 1.60), " miles");
                                            else
                                                warrning = context.getString(R.string.est_range, fuelval, " km");
                                        } else
                                            warrning = context.getString(R.string.rem_fuel, fuelval, " %");
                                        lastwarningvalue = fuelval;
                                        showNotification(context.getResources().getString(R.string.low_fuel_tit), warrning, R.drawable.ic_danger_r, R.drawable.fuel);
                                    }
                                }
                            }
                            if (prefs.shouldMonitorCoolant()) { //If we should monitor coolant
                                float coolantval = torqueService.getPIDValues(fuelpid)[1];
                                if (!celsius)
                                    coolantval = UnitConvertHelper.ConvertValue(coolantval, "°C");

                                if (!warm_engine && coolantval >= warm_engine_degree) {
                                    Log.d("OBD2AA", "Should show the engine temp warning.");
                                    warm_engine = true;
                                    showNotification(context.getResources().getString(R.string.coolant_ok), context.getResources().getString(R.string.engine_warm), R.drawable.ic_danger_green, R.drawable.ic_coolant);

                                    Thread.sleep(10000);
                                    Log.d("OBD2AA", "Should clear warm engine temp");
                                    NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
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
        if (prefs.shouldMonitorFuel() || prefs.shouldMonitorCoolant()) {
            fuelwatcher.start();
        }
    }

    public String getUnit(String unit) {
        try {
            return torqueService.getPreferredUnit(unit);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return unit;
    }

    private void sortPids() {

        Log.d("OBD2AA", "PIDS to string....");
        try {
            String[] pidsdesc = torqueService.getPIDInformation(pids);

            for (int i = 0; i < pids.length; i++) {
                //Build the pidstofetch object with correct data
                boolean needsconversion = !(torqueService.getPreferredUnit(units[i]).equalsIgnoreCase(units[i]));
                // Log.d("OBD2AA","Pid "+pids[i] + "Status: " +pidMap.get(pids[i]));

                String[] info = pidsdesc[i].split(",");
                // Log.d("OBD2AA"," Max val stored for pid (" + pids[i]+"): "+prefs.getFloat("maxval_" + (i+1), 0) +" Reported from Torque: " + parseInt(info[3]) + "units: " +units[i] + ",Locked: " + prefs.getBoolean("locked_"+(i+1),false) +"needconversion: "+needsconversion);

                if (!prefs.isLockedForGauge(i + 1) && prefs.getMaxValueForGauge(i + 1) != Float.parseFloat(info[3])) {
                    if (EventBus.getDefault().hasSubscriberForEvent(GaugeUpdate.class)) {
                        EventBus.getDefault().post(new GaugeUpdate(i, Float.parseFloat(info[3]), true, false));
                    }
                    editor.putString("maxval_" + (i + 1), info[3]);
                    editor.apply();

                }
                if (needsconversion) {
                    Float max = UnitConvertHelper.ConvertValue(Float.parseFloat(info[3]), units[i]);
                    if (!prefs.isLockedForGauge(i + 1) && !prefs.getMaxValueForGauge(i + 1).equals(max)) {
                        if (EventBus.getDefault().hasSubscriberForEvent(GaugeUpdate.class)) {
                            EventBus.getDefault().post(new GaugeUpdate(i, max, true, false));
                        }
                        Log.d("OBD2AA", "Need to update Gauge_" + (i + 1) + "Max val: " + max);
                        editor.putString("maxval_" + (i + 1), max.toString());
                        editor.apply();
                    }

                    Float min = UnitConvertHelper.ConvertValue(Float.parseFloat(info[4]), units[i]);
                    if (!prefs.isLockedForGauge(i + 1) && !prefs.getMinValueForGauge(i + 1).equals(min)) {
                        if (EventBus.getDefault().hasSubscriberForEvent(GaugeUpdate.class)) {
                            EventBus.getDefault().post(new GaugeUpdate(i, min, false, true));
                        }
                        Log.d("OBD2AA", "Need to update Gauge_" + (i + 1) + "Min val: " + min);
                        editor.putString("minval_" + (i + 1), min.toString());
                        editor.apply();
                    }
                }
                pidtofetch.add(new PIDToFetch(pids[i], true, 0, i, units[i], needsconversion, prefs.getMaxValueForGauge(i + 1), prefs.getMinValueForGauge(i + 1)));
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private void startTorque() {
        Intent intent = new Intent();
        intent.setClassName("org.prowl.torque", "org.prowl.torque.remote.TorqueService");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }

        boolean successfulBind = context.bindService(intent, connection, 0);
        if (successfulBind) {
            mBind = true;
            Log.d("HU", "Connected to torque service!");
        } else {
            mBind = false;
            Log.e("HU", "Unable to connect to Torque plugin service");
        }
    }

    protected void showNotification(String Title, String Subtitle, int actionicon, int thumbnail) {
/*
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID = "torque_not_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notifications", NotificationManager.IMPORTANCE_DEFAULT);

            // Configure the notification channel.
            notificationChannel.setDescription("Channel used for Torque notifications");
            mNotifyMgr.createNotificationChannel(notificationChannel);
        }

        CarNotificationExtender paramString2 = new CarNotificationExtender.Builder()
                .setTitle(Title)
                .setSubtitle(Subtitle)
                .setShouldShowAsHeadsUp(true)
                .setActionIconResId(actionicon)
                .setBackgroundColor(Color.WHITE)
                .setNightBackgroundColor(Color.DKGRAY)
                .setThumbnail(BitmapFactory.decodeResource(getResources(), thumbnail))
                .build();

        NotificationCompat.Builder mynot = new NotificationCompat.Builder(getApplicationContext(),NOTIFICATION_CHANNEL_ID)
                .setContentTitle(Title)
                .setContentText(Subtitle)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), thumbnail))
                .setSmallIcon(actionicon)
                .extend(paramString2);


        mNotifyMgr.notify(1984,mynot.build());

*/
    }

    protected void showNotification(String Title, String Subtitle, int actionicon, Bitmap thumbnail, int cameraid) {
/*
        CarToast.makeText(getBaseContext(), Title+" "+Subtitle, Toast.LENGTH_LONG).show();

        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID = "speedcam_not_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notifications", NotificationManager.IMPORTANCE_DEFAULT);

            // Configure the notification channel.
            notificationChannel.setDescription("Channel used for Speed cameras");
            mNotifyMgr.createNotificationChannel(notificationChannel);
        }

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

        NotificationCompat.Builder mynot = new NotificationCompat.Builder(this,NOTIFICATION_CHANNEL_ID)
                .setContentTitle(Title)
                .setContentText(Subtitle)
                .setLargeIcon(thumbnail)
                .setSmallIcon(actionicon)
                .extend(paramString2);

        mynot.setContentIntent(pendingIntent).setDeleteIntent(pendingIntent);

        mNotifyMgr.notify(1985,mynot.build());
*/

    }

    public void onLocationChanged(final Location location) {
        if (location.getAccuracy() > 35) {  //Ignore any location whith accuracy less than 35 meters.
            return;
        }
        accumulated_distance += SphericalUtil.computeDistanceBetween(new LatLng(location.getLatitude(), location.getLongitude()), new LatLng(lastlocation.getLatitude(), lastlocation.getLongitude()));
        lastlocation = location;
        Log.d("HU-Location", "Curr location: " + location);
        if (!isSpeedCamShowing && streetcard && ((lastcardupdate < System.currentTimeMillis() - 5000 && accumulated_distance >= 200))) { // || bearingdiff>60))
            geodecoderexecutor.submit(() -> {
                try {
                    mgeodecoder.dedoceaddress(location);
                    synchronized (this) {
                        lastcardupdate = System.currentTimeMillis();
                        accumulated_distance = 0;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }


        if (ShowSpeedCamWarrning) {
            NearbyCameras camera = null;

            if (prevCamera != null && isCameraInWay.stillinRange(location, prevCamera, visual_display))
                camera = prevCamera;
            else {
                isCameraInWay iscamera = new isCameraInWay(location, mobileDB, staticDB, visual_display, mobile_filter, static_filter);
                if (iscamera.getCamera() != null) {
                    camera = iscamera.getCamera();
                }
            }
            //Log.d("OBD2AA","Camera is: " + camera);
            if (camera != null) {
                prevCamera = camera;
                int[] resources = camera.geticon();
                Bitmap bmp;
                if (resources[2] == 1) {
                    bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.traffic_light);
                } else {
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
                    if (useImperial) {
                        showNotification(context.getString(resources[1]) + " " + Math.round(camera.getDistaceToCam() * 1.0936) + " yd", camera.getstreet(), resources[0], bmp, camera.getid());
                    } else {
                        showNotification(context.getString(resources[1]) + " " + camera.getDistaceToCam() + " m", camera.getstreet(), resources[0], bmp, camera.getid());
                    }
                }

                int sound_to_play = 0;
                if (camera.getDistaceToCam() > audio_2 && camera.getDistaceToCam() <= audio_1 && !camera.getShow_warrning(0)) {
                    sound_to_play = R.raw.beep;
                    camera.setShow_warrning(0, true);
                }
                if (camera.getDistaceToCam() > audio_3 && camera.getDistaceToCam() <= audio_2 && !camera.getShow_warrning(1)) {
                    sound_to_play = R.raw.beepbeep;
                    camera.setShow_warrning(1, true);
                }
                if (camera.getDistaceToCam() <= audio_3 && !camera.getShow_warrning(2)) {
                    sound_to_play = R.raw.beepbeepbeep;
                    camera.setShow_warrning(2, true);
                }
                if (playSound) {
                    MediaPlayer mp = MediaPlayer.create(context, sound_to_play);
                    mp.start();
                }

                isSpeedCamShowing = true;
            } else if (isSpeedCamShowing) {
                Log.d("OBD2AA", "Removing speed camera notification...");
                mNotifyMgr.cancel(1985);
                isSpeedCamShowing = false;
                prevCamera = null;
            }
        }

        if ((System.currentTimeMillis() - lastcalc) > 600000) {
            lastcalc = System.currentTimeMillis();
            nightMode = Calculate_Sunset_Sunrise(location);
        }
    }

    private void refreshMobile() {
        handler.postDelayed(() -> {
            if (isdebugging)
                Log.d("OBD2AA", "Refreshing the mobile database");
            new DownloadHelper(1, context, CameraRefreshFreq);
            refreshMobile();
        }, 60000);
    }
}
