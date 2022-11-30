package uk.co.borconi.emil.obd2aa;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import uk.co.borconi.emil.obd2aa.helpers.PreferencesHelper;

public class AppPreference extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preference);
        PreferencesHelper preferences = PreferencesHelper.getPreferences(getContext());
        findPreference("custom_bg_path").setSummary(preferences.getCustomBackgroundPath());
    }
}
