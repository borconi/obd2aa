package uk.co.borconi.emil.obd2aa.services;


import static uk.co.borconi.emil.obd2aa.AAMessage.FLAG_ALL;
import static uk.co.borconi.emil.obd2aa.SensorMessageId.SENSOR_MESSAGE_BATCH_VALUE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;

import com.google.protobuf.InvalidProtocolBufferException;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ssl.SSLEngine;

import uk.co.borconi.emil.obd2aa.AAMessage;
import uk.co.borconi.emil.obd2aa.Common;
import uk.co.borconi.emil.obd2aa.ConnectionConfiguration;
import uk.co.borconi.emil.obd2aa.ControlMessageType;
import uk.co.borconi.emil.obd2aa.DrivingStatus;
import uk.co.borconi.emil.obd2aa.DrivingStatusData;
import uk.co.borconi.emil.obd2aa.Gear;
import uk.co.borconi.emil.obd2aa.GearData;
import uk.co.borconi.emil.obd2aa.LocationData;
import uk.co.borconi.emil.obd2aa.MainActivity;
import uk.co.borconi.emil.obd2aa.ParkingBrakeData;
import uk.co.borconi.emil.obd2aa.PingConfiguration;
import uk.co.borconi.emil.obd2aa.PingRequest;
import uk.co.borconi.emil.obd2aa.PingResponse;
import uk.co.borconi.emil.obd2aa.R;
import uk.co.borconi.emil.obd2aa.SensorBatch;
import uk.co.borconi.emil.obd2aa.ServiceDiscoveryResponse;
import uk.co.borconi.emil.obd2aa.SpeedData;
import uk.co.borconi.emil.obd2aa.WirelessTcpConfiguration;
import uk.co.borconi.emil.obd2aa.helpers.HeadunitServerToggle;
import uk.co.borconi.emil.obd2aa.helpers.Log;
import uk.co.borconi.emil.obd2aa.helpers.PreferencesHelper;
import uk.co.borconi.emil.obd2aa.sslhelpers.NioSSL;
import uk.co.borconi.emil.obd2aa.sslhelpers.sslEngineBuilder;
import uk.co.borconi.emil.obd2aa.streams.InOutStream;

/**
 * Created by Emil on 25/03/2018.
 */

public class HackerService extends Service {

    private static final String PACKAGE_NAME_ANDROID_AUTO_WIRELESS = "com.google.android.projection.gearhead";
    private static final String CLASS_NAME_ANDROID_AUTO_WIRELESS = "com.google.android.apps.auto.wireless.setup.service.impl.WirelessStartupActivity";

    private static final String PARAM_HOST_ADDRESS_EXTRA_NAME = "PARAM_HOST_ADDRESS";
    private static final String PARAM_SERVICE_PORT_EXTRA_NAME = "PARAM_SERVICE_PORT";


    private static final String TAG = "AAGateWay";
    private final static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private static boolean RUNNING = false;
    private final IBinder mBinder = new LocalBinder();
    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            UiModeManager mUimodemanager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
            mUimodemanager.disableCarMode(0);
            stop();
        }
    };
    private ParcelFileDescriptor mFileDescriptor;
    private NioSSL localAAServer;
    private NioSSL carAAClient;
    private boolean localCompleted, usbCompleted;
    private Integer sensorChannel = null;
    private Thread tcpreader;
    private boolean disableTapLimit = true;
    private InOutStream phoneStream;
    private InOutStream carStream;

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        String CHANNEL_ONE_ID = "uk.co.borconi.emil.aagateway";
        String CHANNEL_ONE_NAME = "Channel One";
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder mynotification = new Notification.Builder(this)
                .setContentTitle("Android Auto Hacker")
                .setContentText("Running....")
                .setSmallIcon(R.drawable.hu_icon_256)
                .setTicker("");
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel mChannel = new NotificationChannel(
                    CHANNEL_ONE_ID, CHANNEL_ONE_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(mChannel);
            mynotification.setChannelId(CHANNEL_ONE_ID);
        }
        startForeground(1, mynotification.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("AAGateWay", "Service Started");
        if (RUNNING) {
            return START_STICKY;
        }
        RUNNING = true;
        super.onStartCommand(intent, flags, startId);
        UsbAccessory mAccessory = intent.getParcelableExtra("accessory");
        Log.d("AAGateWay", "Accessory: " + mAccessory);
        UsbManager mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        mFileDescriptor = mUsbManager.openAccessory(mAccessory);
        if (mFileDescriptor != null) {
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();
            FileInputStream instream = new FileInputStream(fd);
            FileOutputStream outstrem = new FileOutputStream(fd);
            carStream = new InOutStream(instream, outstrem);
        }
        PreferencesHelper prefs = PreferencesHelper.getPreferences(this);
        disableTapLimit = prefs.hasSpeedHack();
        Thread usbreader = new Thread(new usbpollthread());
        tcpreader = new Thread(new tcppollthread());
        usbreader.start();

        return START_STICKY;
    }

    private void processCarMessage(byte[] buf) throws IOException {
        AAMessage carmessage = new AAMessage(buf, carAAClient);
        if (carmessage.channel == 0 && carmessage.messageID == ControlMessageType.MESSAGE_SERVICE_DISCOVERY_RESPONSE_VALUE) {
            ServiceDiscoveryResponse.Builder newdata = ServiceDiscoveryResponse.parseFrom(carmessage.data).toBuilder();
            for (uk.co.borconi.emil.obd2aa.Service.Builder current : newdata.getServicesBuilderList()) {
                if (current.hasSensorSourceService()) {
                    sensorChannel = current.getId();
                }
            }
            newdata.setConnectionConfiguration(
                    ConnectionConfiguration.newBuilder()
                            .setPingConfiguration(PingConfiguration.newBuilder().setHighLatencyThresholdMs(200).setIntervalMs(1000).setTimeoutMs(3000).setTrackedPingCount(999).build())
                            .setWirelessTcpConfiguration(WirelessTcpConfiguration.newBuilder().setSocketReceiveBufferSizeKb(16384).setSocketSendBufferSizeKb(16384).setSocketReadTimeoutMs(500).build())
                            .build()
            );
            newdata.setHeadunitInfo(Common.HeadUnitInfo.newBuilder()
                    .setHeadUnitMake("Google")
                    .setHeadUnitModel("Desktop Head Unit")
                    .setMake("Google")
                    .setModel("Desktop Head Unit")
                    .setHeadUnitSoftwareBuild("HUR")
                    .setHeadUnitSoftwareVersion("1.1")
                    .build());

            Log.d("AAGW", "Service discovery response: " + newdata);
            carmessage.setData(newdata.build().toByteArray());
        } else if (carmessage.channel == 0 && carmessage.messageID == ControlMessageType.MESSAGE_PING_REQUEST_VALUE) {
            handlePing(carmessage, true);
            return;
        } else if (sensorChannel != null && carmessage.channel == sensorChannel && disableTapLimit) {
            if (carmessage.getMessageID() == SENSOR_MESSAGE_BATCH_VALUE) {
                SensorBatch.Builder sensordata = SensorBatch.parseFrom(carmessage.data).toBuilder();
                if (sensordata.getLocationDataCount() > 0) {
                    for (LocationData.Builder gps : sensordata.getLocationDataBuilderList()) {
                        gps.setSpeedE3(500);
                    }
                }
                if (sensordata.getSpeedDataCount() > 0) {
                    for (SpeedData.Builder gps : sensordata.getSpeedDataBuilderList()) {
                        gps.setSpeedE3(500);
                    }
                }
                if (sensordata.getDrivingStatusDataCount() > 0) {
                    for (DrivingStatusData.Builder driving : sensordata.getDrivingStatusDataBuilderList()) {
                        driving.setStatus(DrivingStatus.DRIVE_STATUS_UNRESTRICTED_VALUE);
                    }
                }
                if (sensordata.getGearDataCount() > 0) {
                    for (GearData.Builder gear : sensordata.getGearDataBuilderList()) {
                        gear.setGear(Gear.GEAR_PARK);
                    }
                }
                if (sensordata.getParkingBrakeDataCount() > 0) {
                    for (ParkingBrakeData.Builder parking : sensordata.getParkingBrakeDataBuilderList()) {
                        parking.setParkingBrake(true);
                    }
                }
                carmessage.setData(sensordata.build().toByteArray());
            }
        }
        phoneStream.write(carmessage.getFormated(localAAServer));
    }

    private void handlePing(AAMessage msg, boolean hu) throws InvalidProtocolBufferException {
        PingRequest pingRequest = PingRequest.parseFrom(msg.getData());
        PingResponse.Builder pingResponse = PingResponse.newBuilder();
        pingResponse.setTimestamp(pingRequest.getTimestamp());
        AAMessage resp = new AAMessage(AAMessage.CONTROLCHANNEL, FLAG_ALL, ControlMessageType.MESSAGE_PING_RESPONSE_VALUE);
        resp.setEncrypted(msg.getEncrypted());
        resp.setData(pingResponse.build().toByteArray());
        if (hu) {
            carStream.write(resp.getFormated(carAAClient));
        } else {
            phoneStream.write(resp.getFormated(localAAServer));
        }
    }

    @Override
    public void onDestroy() {
        RUNNING = false;
        unregisterReceiver(mUsbReceiver);
        try {
            mFileDescriptor.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        OBD2Background.isrunning = false;
    }

    private void stop() {
        RUNNING = false;
        UiModeManager m_uim_mgr = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        m_uim_mgr.disableCarMode(UiModeManager.DISABLE_CAR_MODE_GO_HOME);

        stopSelf();
    }

    public class LocalBinder extends Binder {
        HackerService getService() {
            return HackerService.this;
        }
    }

    class tcppollthread implements Runnable {
        public void run() {
            Log.d("HU", "TCP Loop started");
            HeadunitServerToggle.StartStop(false);

            try {
                Looper.prepare();
                Log.d("HU", "About to connect to the headunit server");
                ServerSocket server = new ServerSocket(5288);
                server.setReuseAddress(true);


                Intent androidAutoWirelessIntent = new Intent();

                androidAutoWirelessIntent.setClassName(PACKAGE_NAME_ANDROID_AUTO_WIRELESS, CLASS_NAME_ANDROID_AUTO_WIRELESS);
                androidAutoWirelessIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                androidAutoWirelessIntent
                        .putExtra(PARAM_HOST_ADDRESS_EXTRA_NAME, "127.0.0.1")
                        .putExtra(PARAM_SERVICE_PORT_EXTRA_NAME, 5288);

                try {
                    startActivity(androidAutoWirelessIntent);
                } catch (Exception e) {
                    stopSelf();
                    return;
                }
                System.out.println("Start intent sent");

                Socket socket = server.accept();
                phoneStream = new InOutStream(socket.getInputStream(), socket.getOutputStream());

                Log.d("HU", "Connected to local.");

                phoneStream.write(new byte[]{0, 3, 0, 6, 0, 1, 0, 1, 0, 2});
                phoneStream.read(12);
                SSLEngine engine = sslEngineBuilder.Builder(getBaseContext());
                Log.d("AAGateWay", "engine setup ok");
                localAAServer = new NioSSL(engine, phoneStream, true);
                localCompleted = true;
                while (!usbCompleted) {
                    Thread.sleep(100);
                }
                while (RUNNING) {
                    AAMessage inmessage = new AAMessage(phoneStream.read(), localAAServer);

                    if (inmessage.channel == 0 && inmessage.getMessageID() == ControlMessageType.MESSAGE_PING_REQUEST_VALUE) {
                        handlePing(inmessage, false);
                    } else {
                        carStream.write(inmessage.getFormated(carAAClient));
                    }
                }
            } catch (Exception e) {
                Log.e("AAGateWay", e.getMessage());
                Intent i = new Intent(HackerService.this, MainActivity.class);
                i.setAction("localerror");
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (!localCompleted) {
                    startActivity(i);
                } else {
                    stop();
                }
            }
        }
    }

    class usbpollthread implements Runnable {
        public void run() {
            Log.d("AAGateWay", "Runnable run");
            Looper.prepare();

            Log.d("AAGateWay", "After Native call, connected to the localhost...");
            try {
                Log.d("AAGateWay", "Try to read from car");
                carStream.read();
                Log.d("AAGateWay", "Send hello to car");
                carStream.write(new byte[]{0, 3, 0, 8, 0, 2, 0, 1, 0, 4, 0, 0});
                Log.d("AAGateWay", "Buildin SSL Engine");
                //tcpreader.join();
                SSLEngine engine = sslEngineBuilder.Builder(getBaseContext());
                Log.d("AAGateWay", "engine setup ok");
                carAAClient = new NioSSL(engine, carStream, false);
                usbCompleted = true;
                tcpreader.start();
                while (!localCompleted) {
                    Thread.sleep(100);
                }
            } catch (Exception e) {
                Log.e("AaGateWay", "Error communicating with the car: " + e.getMessage());
            }

            Log.d(TAG, "Entering reading loop");
            while (RUNNING) {
                try {
                    processCarMessage(carStream.read());
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "Error reading: " + e.getMessage());
                    RUNNING = false;
                    stop();
                }
            }
        }
    }
}