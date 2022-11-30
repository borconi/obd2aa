package uk.co.borconi.emil.obd2aa;

import static uk.co.borconi.emil.obd2aa.MainActivity.pidlist;

import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import uk.co.borconi.emil.obd2aa.pid.PidList;


public class TpmsSettings extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.tpms_settings);
        CharSequence[] entries, entryValues;
        if (pidlist.size() == 0 || pidlist == null) {
            entries = new String[1];
            entryValues = new String[1];
            entries[0] = getString(R.string.notorque).split("\n")[0];
            entryValues[0] = null;
        } else {
            entries = new String[pidlist.size()];
            entryValues = new String[pidlist.size()];
            int i = 0;
            for (PidList pid : pidlist) {
                entries[i] = pid.getPidName();
                entryValues[i] = pid.getPid();
                i++;
            }
        }
        ((ListPreference) findPreference("front_left_pressure")).setEntries(entries);
        ((ListPreference) findPreference("front_left_pressure")).setEntryValues(entryValues);

        ((ListPreference) findPreference("front_right_pressure")).setEntries(entries);
        ((ListPreference) findPreference("front_right_pressure")).setEntryValues(entryValues);

        ((ListPreference) findPreference("rear_left_pressure")).setEntries(entries);
        ((ListPreference) findPreference("rear_left_pressure")).setEntryValues(entryValues);

        ((ListPreference) findPreference("rear_right_pressure")).setEntries(entries);
        ((ListPreference) findPreference("rear_right_pressure")).setEntryValues(entryValues);
    }
}
