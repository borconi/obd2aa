package uk.co.borconi.emil.obd2aa;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.rarepebble.colorpicker.ColorPreference;
import com.takisoft.preferencex.PreferenceFragmentCompat;

import java.util.HashMap;

import uk.co.borconi.emil.obd2aa.preference.GaugePreference;


public class AppSettings extends PreferenceFragmentCompat {

    private final static int MAX_GAUGE = 15;

    @Override
    public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.gauge_preferences);
        findPreferenceByKey("layout_style_spinner").setOnPreferenceChangeListener((Preference preference, Object newValue) -> {
            updateGaugeCounterDisabledStatus();
            return true;
        });
        findPreferenceByKey("gauge_counter").setOnPreferenceChangeListener((Preference preference, Object newValue) -> {
            updateGaugeCount(Integer.parseInt(newValue.toString()));
            return true;
        });
        initGaugeCount();
        updateGaugeCounterDisabledStatus();
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference instanceof ColorPreference) {
            ((ColorPreference) preference).showDialog(this, 0);
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    private void initGaugeCount() {
        int gaugeCount = Integer.parseInt(((ListPreference) findPreferenceByKey("gauge_counter")).getValue());
        GaugePreference g = new GaugePreference(getContext());
        for (int i = 1; i <= MAX_GAUGE; i++) {
            boolean visible = gaugeCount >= i;
            g.buildPrefs(this.getPreferenceScreen(), i, visible);
        }
    }

    private void updateGaugeCount(int gaugeCount) {
        for (int i = 1; i < MAX_GAUGE; i++) {
            Preference pref = findPreferenceByKey("gauge_group_" + i);
            if (i <= gaugeCount) {
                pref.setVisible(true);
                pref.setIcon(R.drawable.ic_baseline_expand_more_24);
            } else {
                pref.setVisible(false);
            }
            findPreferenceByKey("collapser_" + i).setVisible(false);
        }
    }

    private void updateGaugeCounterDisabledStatus() {
        String layoutStyle = ((ListPreference) findPreferenceByKey("layout_style_spinner")).getValue();

        if ("AUTO".equalsIgnoreCase(layoutStyle)) {
            findPreferenceByKey("gauge_counter").setEnabled(true);
            return;
        }

        findPreferenceByKey("gauge_counter").setEnabled(false);
        String newGaugeCount = new HashMap<String, String>() {{
            put("1", "5");
            put("2", "3");
            put("3", "4");
        }}.get(layoutStyle);

        if (newGaugeCount == null) {
            ((ListPreference) findPreferenceByKey("layout_style_spinner")).setValue("AUTO");
            ((ListPreference) findPreferenceByKey("gauge_counter")).setValue("1");
            findPreferenceByKey("gauge_counter").setEnabled(true);
            newGaugeCount = "1";
        }
        Preference preference = findPreferenceByKey("gauge_counter");
        ((ListPreference) preference).setValue(newGaugeCount);
        updateGaugeCount(Integer.parseInt(newGaugeCount));
    }

    public <T extends Preference> T findPreferenceByKey(@NonNull CharSequence key) {
        T preference = super.findPreference(key);
        if (preference == null) {
            throw new RuntimeException(String.format("Preference with key [%s] not found", key));
        }
        return preference;
    }
}