package uk.co.borconi.emil.obd2aa;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;


public class AppCameraSettings extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        //super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.speedcamprefs);
    }
}
