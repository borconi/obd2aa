package uk.co.borconi.emil.obd2aa.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PreferencesHelper {

    private final SharedPreferences preferences;

    private PreferencesHelper(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public static PreferencesHelper getPreferences(Context mcontext) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mcontext);
        return new PreferencesHelper(preferences);
    }

    // Hack Service
    public boolean hasSpeedHack() {
        return preferences.getBoolean("speed_hack", false);
    }

    public int getScreen() {
        return preferences.getInt("def_screen", 3);
    }

    public int getDpi() {
        return preferences.getInt("dpi", 140);
    }

    public boolean shouldAutoplayMusic() {
        return preferences.getBoolean("autoplay_music", false);
    }

    public boolean hasChangedDpi() {
        return preferences.getBoolean("changedpi", true);
    }

    public boolean hasBluetoothHack() {
        return preferences.getBoolean("bt_hack", false);
    }

    // OBD2Background
    public boolean isInDemoMode() {
        return preferences.getBoolean("demomode", false);
    }

    public boolean hasAlternativePulling() {
        return preferences.getBoolean("alternativepulling", false);
    }

    public boolean shouldShowSpeedCamWarning() {
        return preferences.getBoolean("ShowSpeedCamWarrning", true);
    }

    public boolean shouldPlaySound() {
        return preferences.getBoolean("play_sound", true);
    }

    public boolean shouldUseImperial() {
        return preferences.getBoolean("useimperials", true);
    }

    public boolean shouldHaveStreetCard() {
        return preferences.getBoolean("streetcard", true);
    }

    public int getAudio1() {
        String output = preferences.getString("audio_1", "800");
        return Integer.parseInt(output);
    }

    public int getAudio2() {
        String output = preferences.getString("audio_2", "400");
        return Integer.parseInt(output);
    }

    public int getAudio3() {
        String output = preferences.getString("audio_3", "100");
        return Integer.parseInt(output);
    }

    public int getVisualDisplay() {
        String output = preferences.getString("visual_display", "1000");
        return Integer.parseInt(output);
    }

    public int getUpdateFrequency() {
        String output = preferences.getString("mobile_update_freq", "3000");
        return Integer.parseInt(output);
    }

    public Set<String> getCamTypes() {
        return preferences.getStringSet("cam_types", new HashSet<>(Arrays.asList("1,2,3,4".split(","))));
    }

    public boolean isNight() {
        return preferences.getBoolean("daynight", false);
    }

    public boolean shouldMonitorFuel() {
        return preferences.getBoolean("monitorfuel", false);
    }

    public boolean shouldMonitorCoolant() {
        return preferences.getBoolean("monitorcoolant", false);
    }

    public String watchFuel() {
        return preferences.getString("watch_fuel", "0");
    }

    public String coolantPid() {
        return preferences.getString("coolant_pid", "05,0");
    }

    public int getCoolantWarningValue() {
        String output = preferences.getString("coolanttemp", "0");
        return Integer.parseInt(output);
    }

    public int getColor(String key, int defaultColor) {
        return preferences.getInt(key, defaultColor);
    }

    public int getNumberOfGauges() {
        String output = preferences.getString("gauge_counter", "0");
        return Integer.parseInt(output);
    }

    public boolean shouldUseMobile() {
        return preferences.getBoolean("use_mobile", true);
    }

    public boolean hasCustomBackground() {
        return preferences.getBoolean("custombg", false);
    }

    public String getCustomBackgroundPath() {
        return preferences.getString("custom_bg_path", "");
    }

    public String getLayoutStyle() {
        return preferences.getString("layout_style_spinner", "AUTO");
    }

    public boolean shouldUseDigital() {
        return preferences.getBoolean("usedigital", false);
    }

    public boolean isDebugging() {
        return preferences.getBoolean("debugging", true);
    }

    public int getArchColor() {
        return preferences.getInt("def_color_selector", 0);
    }

    public int getWarn1Color() {
        return preferences.getInt("warn1_color_selector", 0);
    }

    public int getWarn2Color() {
        return preferences.getInt("warn2_color_selector", 0);
    }

    public int getTextColor() {
        return preferences.getInt("text_color_selector", 0);
    }

    public int getArchWidth() {
        String output = preferences.getString("arch_width", "3");
        return Integer.parseInt(output);
    }

    public int getNeedleColor() {
        return preferences.getInt("needle_color_selector", -1);
    }

    public Float getMaxValueForGauge(int gauge) {
        String output = preferences.getString(String.format("maxval_%s", gauge), "0");
        return Float.parseFloat(output);
    }

    public Float getMinValueForGauge(int gauge) {
        String output = preferences.getString(String.format("minval_%s", gauge), "0");
        return Float.parseFloat(output);
    }

    public float getWarn1LevelForGauge(int gauge) {
        float defaultValue = (100 * getMaxValueForGauge(gauge) / 100);
        String output = preferences.getString(String.format("warn1level_%s", gauge), Float.toString(defaultValue));
        return Float.parseFloat(output);
    }

    public float getWarn2LevelForGauge(int gauge) {
        float defaultValue = (100 * getMaxValueForGauge(gauge) / 100);
        String output = preferences.getString(String.format("warn2level_%s", gauge), Float.toString(defaultValue));
        return Float.parseFloat(output);
    }

    public boolean isReversedForGauge(int gauge) {
        return preferences.getBoolean(String.format("isreversed_%s", gauge), false);
    }

    public int getStyleForGauge(int gauge) {
        String output = preferences.getString(String.format("gauges_style_spinner_%s", gauge), "0");
        return Integer.parseInt(output);
    }

    public int getArchIndentForGauge(int gauge) {
        return preferences.getInt(String.format("arch_indent_%s", gauge), 0);
    }

    public int getArchLengthForGauge(int gauge) {
        return preferences.getInt(String.format("arch_length_%s", gauge), 288);
    }

    public int getArchStartPositionForGauge(int gauge) {
        return preferences.getInt(String.format("arch_startpos_%s", gauge), 270);
    }

    public boolean shouldShowNeedleForGauge(int gauge) {
        return preferences.getBoolean(String.format("showneedle_%s", gauge), true);
    }

    public boolean shouldShowScaleForGauge(int gauge) {
        return preferences.getBoolean(String.format("showscale_%s", gauge), true);
    }

    public boolean shouldShowTextForGauge(int gauge) {
        return preferences.getBoolean(String.format("showtext_%s", gauge), true);
    }

    public boolean shouldUseGradientTextForGauge(int gauge) {
        return preferences.getBoolean(String.format("usegradienttext_%s", gauge), false);
    }

    public boolean shouldShowDecimalForGauge(int gauge) {
        return preferences.getBoolean(String.format("showdecimal_%s", gauge), false);
    }

    public boolean shouldShowUnitForGauge(int gauge) {
        return preferences.getBoolean(String.format("showunit_%s", gauge), false);
    }

    public boolean isLockedForGauge(int gauge) {
        return preferences.getBoolean(String.format("locked_%s", gauge), false);
    }

    public String getPidForGauge(int gauge) {
        String output = preferences.getString(String.format("gaugepid_%s", gauge), "");
        String[] items = output.split("__"); // TODO This is ugly
        if (items.length < 1) {
            return "";
        }
        return items[0];
    }

    public String getNameForGauge(int gauge) {
        String output = preferences.getString(String.format("gaugepid_%s", gauge), "");
        String[] items = output.split("__"); // TODO This is ugly
        if (items.length < 2) {
            return "";
        }
        return items[1];
    }

    public String getUnitForGauge(int gauge) {
        String output = preferences.getString(String.format("gaugepid_%s", gauge), "");
        String[] items = output.split("__"); // TODO This is ugly
        if (items.length < 3) {
            return "";
        }
        return items[2];
    }

    public SharedPreferences.Editor edit() {
        return preferences.edit();
    }

    public Map<String, ?> getAll() {
        return preferences.getAll();
    }
}
