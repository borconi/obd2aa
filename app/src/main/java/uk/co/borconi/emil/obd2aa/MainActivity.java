package uk.co.borconi.emil.obd2aa;

import static java.lang.Integer.parseInt;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import org.json.JSONArray;
import org.json.JSONException;
import org.prowl.torque.remote.ITorqueService;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import uk.co.borconi.emil.obd2aa.databinding.ActivityMainBinding;
import uk.co.borconi.emil.obd2aa.helpers.PreferencesHelper;
import uk.co.borconi.emil.obd2aa.pid.PidList;
import uk.co.borconi.emil.obd2aa.services.OBD2Background;


public class MainActivity extends AppCompatActivity {

    public static List<PidList> pidlist = null;
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private SharedPreferences.Editor editor;
    private PreferencesHelper preferences;
    private boolean isdebugging;
    private NavController navController;
    ActivityResultLauncher<String> mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    // Handle the returned Uri
                    try {
                        InputStream is = getContentResolver().openInputStream(uri);
                        byte[] bytes = new byte[is.available()];
                        is.read(bytes);
                        String contents = new String(bytes);
                        JSONArray jsonArr = new JSONArray(contents);

                        for (int i = 0; i < jsonArr.length(); i++) {

                            JSONArray temp = jsonArr.getJSONArray(i);

                            Log.d("OBD2", "Processing: " + temp);
                            if (temp.getString(2).equalsIgnoreCase("S"))
                                editor.putString(temp.getString(0), temp.getString(1));
                            else if (temp.getString(2).equalsIgnoreCase("I"))
                                editor.putInt(temp.getString(0), parseInt(temp.getString(1)));
                            else if (temp.getString(2).equalsIgnoreCase("F"))
                                editor.putFloat(temp.getString(0), Float.parseFloat(temp.getString(1)));
                            else
                                editor.putBoolean(temp.getString(0), Boolean.parseBoolean(temp.getString(1)));

                            editor.commit();
                        }

                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle(getResources().getString(R.string.imp_comp_tit));
                        builder.setMessage(getResources().getString(R.string.imp_comp_text));
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //reload the fragment!

                                int navid = navController.getCurrentDestination().getId();
                                navController.popBackStack(navid, true);
                                navController.navigate(navid);
                            }
                        });

                        builder.show();

                    } catch (IOException | JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
    private final ServiceConnection connection = new ServiceConnection() {

        public void onServiceConnected(ComponentName arg0, IBinder service) {
            Log.d("HU", "SERVICE CONNECTED!");
            ITorqueService torqueService = ITorqueService.Stub.asInterface(service);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(new Intent(getApplicationContext(), OBD2Background.class));
            } else {
                startService(new Intent(getApplicationContext(), OBD2Background.class));
            }

            try {
                if (torqueService.getVersion() < 19) {
                    Toast.makeText(MainActivity.this, "Incorrect version. You are using an old version of Torque with this plugin.\n\nThe plugin needs the latest version of Torque to run correctly.\n\nPlease upgrade to the latest version of Torque from Google Play", Toast.LENGTH_LONG);
                    return;
                }
            } catch (RemoteException e) {

            }
            Log.d("HU", "Have Torque service connection!");
            pidlist = new ArrayList<PidList>();

            String[] pidsdesc = null;
            String[] pids = null;
            try {
                pids = torqueService.listAllPIDs();
                pidsdesc = torqueService.getPIDInformation(pids);
                for (int i = 0; i < pids.length; i++) {
                    pidsdesc[i] = pidsdesc[i] + "," + pids[i];
                }
                Arrays.sort(pidsdesc, String.CASE_INSENSITIVE_ORDER);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            if (pids == null)
                return;

            for (int i = 0; i < pids.length; i++) {
                String[] info = pidsdesc[i].split(",");
                if (isdebugging)
                    try {
                        Log.d("OBD2AA", "Pid name: " + info[0] + " Units: " + info[2] + " Unit conversion: " + torqueService.getPreferredUnit(info[2]));
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                pidlist.add(new PidList(pidsdesc[i]));
            }
            navController.popBackStack();
            navController.navigate(R.id.FirstFragment);
        }

        public void onServiceDisconnected(ComponentName name) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Intent intent = new Intent();
        intent.setClassName("org.prowl.torque", "org.prowl.torque.remote.TorqueService");

        bindService(intent, connection, BIND_AUTO_CREATE);
        try {
            Context packagecontext = createPackageContext("org.prowl.torque", 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                packagecontext.startForegroundService(intent);
            } else {
                startService(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.onCreate(savedInstanceState);

        preferences = PreferencesHelper.getPreferences(this);
        editor = preferences.edit();
        isdebugging = preferences.isDebugging();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        navController = navHostFragment.getNavController();
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
    }

    @Override
    protected void onPause() {
        Log.d("OBD2AA", "onPause");
        super.onPause();
        try {
            unbindService(connection);
            Intent sendIntent = new Intent();

            final Intent intent = new Intent();
            intent.setAction("com.pkg.perform.Ruby");
            intent.putExtra("KeyName", "code1id");
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            intent.setComponent(
                    new ComponentName("org.prowl.torque", "org.prowl.torque.remote.TorqueService"));
            sendBroadcast(intent);

            sendIntent.setAction("org.prowl.torque.REQUEST_TORQUE_QUIT");
            sendIntent.setClassName("org.prowl.torque", "org.prowl.torque.remote.TorqueService");
            Context packagecontext = createPackageContext("org.prowl.torque", 0);
            packagecontext.sendBroadcast(sendIntent);
            sendBroadcast(sendIntent);
            packagecontext.stopService(sendIntent);
        } catch (Exception E) {
            Log.d("OBD2AA", "No service to unbind from");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pidlist != null) {
            navController.popBackStack();
            navController.navigate(R.id.FirstFragment);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            SpannableString spanString = new SpannableString(menu.getItem(i).getTitle().toString());
            spanString.setSpan(new ForegroundColorSpan(getColor(R.color.teal_200)), 0, spanString.length(), 0); //fix the color to white
            item.setTitle(spanString);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Log.d("Option", "option selected: " + item);
        int id = item.getItemId();
        NavController navController = Navigation.findNavController(MainActivity.this, R.id.nav_host_fragment_content_main);
        //ToDo Implement menu handling.

        if (id == R.id.speedcam_menu) {
            navController.navigate(R.id.AppCameraSettings);
        }
        if (id == R.id.pref_menu) {
            navController.navigate(R.id.PreferenceFragment);
        }
        if (id == R.id.tpms_menu) {
            navController.navigate(R.id.TpmsSettings);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}