package uk.co.boconi.emil.obd2aa;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by Emil on 31/08/2017.
 */
public class CarModeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("OBD2AA", "receiver fired");
        if (intent.getAction().equalsIgnoreCase("android.app.action.ENTER_CAR_MODE")) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            Log.d("OBD2AA", "Should start the service now");
            Intent starts = new Intent(context, uk.co.boconi.emil.obd2aa.OBD2_Background.class);
            context.startService(starts);


            if (prefs.getBoolean("fartkontrol", false)) {
                Intent LaunchIntent = context.getPackageManager().getLaunchIntentForPackage("nu.fartkontrol.app");
                if (LaunchIntent != null)
                    context.startActivity(LaunchIntent);
            }


        } else {
            Log.d("OBD2AA", "Should stop the service now");
            OBD2_Background.isrunning = false;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

            if (prefs.getBoolean("fartkontrol", false)) {
                Intent paramIntent = context.getPackageManager().getLaunchIntentForPackage("nu.fartkontrol.app");
                if (paramIntent != null) {
                    paramIntent.putExtra("BLUETOOTH_END", true);
                    context.startActivity(paramIntent);
                }
            }

        }

    }
}
