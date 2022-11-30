package uk.co.borconi.emil.obd2aa;

import android.app.Activity;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Parcelable;
import android.widget.Toast;

import uk.co.borconi.emil.obd2aa.services.HackerService;
import uk.co.borconi.emil.obd2aa.services.OBD2Background;

public class UsbReceiver extends Activity {

    @Override
    protected void onResume() {
        super.onResume();
        if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(getIntent().getAction())) {
            Intent serviceIntent = new Intent(this, HackerService.class);
            serviceIntent.setAction(getIntent().getAction());
            serviceIntent.putExtra("accessory", (Parcelable) getIntent().getParcelableExtra("accessory"));
            Toast.makeText(this, "Starting AA proxy service", Toast.LENGTH_LONG).show();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }
}
