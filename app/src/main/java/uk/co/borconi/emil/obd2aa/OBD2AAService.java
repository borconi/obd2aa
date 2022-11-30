package uk.co.borconi.emil.obd2aa;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarAppService;
import androidx.car.app.Screen;
import androidx.car.app.Session;
import androidx.car.app.validation.HostValidator;

import org.prowl.torque.remote.ITorqueService;

import uk.co.borconi.emil.obd2aa.androidauto.OBD2AA;
import uk.co.borconi.emil.obd2aa.services.OBD2Background;

public class OBD2AAService extends CarAppService {

    private OBD2Background background;

    private ServiceConnection connection;

    public OBD2AAService() {
    }

    @Override
    public void onCreate() {
        connection = new ServiceConnection() {

            public void onServiceConnected(ComponentName arg0, IBinder service) {
                Log.d("HU", "SERVICE CONNECTED!");
                ITorqueService torqueService = ITorqueService.Stub.asInterface(service);
                background = new OBD2Background(torqueService, getApplicationContext());
            }

            public void onServiceDisconnected(ComponentName name) {
            }
        };
        Intent intent = new Intent();
        intent.setClassName("org.prowl.torque", "org.prowl.torque.remote.TorqueService");

        bindService(intent, connection, BIND_AUTO_CREATE);

        super.onCreate();
    }

    @Override
    public void onDestroy() {
        background.onDestroy();

        super.onDestroy();
    }

    @NonNull
    @Override
    public Session onCreateSession() {
        return new Session() {
            @Override
            @NonNull
            public Screen onCreateScreen(@Nullable Intent intent) {
                return new OBD2AA(getCarContext(), getApplicationContext());
                //return null;

            }
        };
    }

    @NonNull
    @Override
    public HostValidator createHostValidator() {
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
        } else {
            return new HostValidator.Builder(getApplicationContext())
                    .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                    .build();
        }
    }
}