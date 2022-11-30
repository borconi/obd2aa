package uk.co.borconi.emil.obd2aa.preference;


import static uk.co.borconi.emil.obd2aa.MainActivity.pidlist;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.InflateException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import org.xmlpull.v1.XmlPullParser;

import java.lang.reflect.Constructor;
import java.util.HashMap;

import uk.co.borconi.emil.obd2aa.R;
import uk.co.borconi.emil.obd2aa.pid.PidList;


public class GaugePreference {

    private static final Class<?>[] CONSTRUCTOR_SIGNATURE = new Class<?>[]{
            Context.class, AttributeSet.class};

    private static final HashMap<String, Constructor<?>> CONSTRUCTOR_MAP = new HashMap<>();
    private final Context context;
    private final String[] mDefaultPackages = new String[]{
            // Preference was originally in android.support.v7.preference.
            Preference.class.getPackage().getName() + ".",
            // SwitchPreference was originally in android.support.v14.preference.
            SwitchPreference.class.getPackage().getName() + "."
    };

    public GaugePreference(Context context) {
        this.context = context;
    }

    public void buildPrefs(PreferenceScreen prefscreen, int gaugecount, boolean visible) {
        Log.d("OBD2", "settting up gauge: " + gaugecount);

        Preference parent = new Preference(context);
        parent.setTitle(context.getString(R.string.gauge_name) + " " + gaugecount);
        parent.setKey("gauge_group_" + gaugecount);
        parent.setIcon(R.drawable.ic_baseline_expand_more_24);
        parent.setVisible(visible);
        prefscreen.addPreference(parent);

        PreferenceCategory group = new PreferenceCategory(context);
        group.setKey("collapser_" + gaugecount);
        group.setVisible(false);

        prefscreen.addPreference(group);
        XmlPullParser parser = context.getResources().getXml(R.xml.gauge_setting);
        try {
            parser.next();
            parser.next();
            parser.getDepth();
            do {
                parser.next();
                AttributeSet attr = Xml.asAttributeSet(parser);
                if (attr.getAttributeCount() > 0) {
                    Preference pref = createItemFromTag(parser.getName(), attr);
                    pref.setKey(pref.getKey() + "_" + gaugecount);
                    group.addPreference(pref);
                }
            } while (parser.getName() != null);

            int i = 0;
            String[] entries = new String[pidlist.size()];
            String[] entriesValues = new String[pidlist.size()];
            for (PidList pid : pidlist) {
                entries[i] = pid.getPidName();
                entriesValues[i] = pid.getPid() + "__" + pid.getShortPidName() + "__" + pid.getUnit(); // TODO This is ugly
                i++;
            }
            ListPreference pid = group.findPreference("gaugepid_" + gaugecount);
            pid.setEntries(entries);
            pid.setEntryValues(entriesValues);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        parent.setOnPreferenceClickListener(preference -> {
            group.setVisible(!group.isVisible());
            if (group.isVisible()) {
                parent.setIcon(R.drawable.ic_baseline_expand_less_24);
            } else {
                parent.setIcon(R.drawable.ic_baseline_expand_more_24);
            }
            return false;
        });

        Preference customNeedlePath = group.findPreference("custom_needle_path_" + gaugecount);
        if (customNeedlePath != null) {
            customNeedlePath.setDependency("use_custom_needle_" + gaugecount);
        }
        Preference customBackgroundPath = group.findPreference("custom_bg_path_" + gaugecount);
        if (customBackgroundPath != null) {
            customBackgroundPath.setDependency("use_custom_bg_" + gaugecount);
        }
    }

    private Preference createItemFromTag(String name, AttributeSet attrs) {
        try {
            final Preference item;
            if (-1 == name.indexOf('.')) {
                item = onCreateItem(name, attrs);
            } else {
                item = createItem(name, null, attrs);
            }
            return item;

        } catch (InflateException e) {
            throw e;

        } catch (ClassNotFoundException e) {
            final InflateException ie = new InflateException(attrs
                    .getPositionDescription()
                    + ": Error inflating class (not found)" + name, e);
            throw ie;

        } catch (Exception e) {
            final InflateException ie = new InflateException(attrs
                    .getPositionDescription()
                    + ": Error inflating class " + name, e);
            throw ie;
        }
    }

    protected Preference onCreateItem(String name, AttributeSet attrs)
            throws ClassNotFoundException {
        return createItem(name, mDefaultPackages, attrs);
    }

    private Preference createItem(@NonNull String name, @Nullable String[] prefixes, AttributeSet attrs) throws ClassNotFoundException, InflateException {
        Constructor<?> constructor = CONSTRUCTOR_MAP.get(name);

        try {
            if (constructor == null) {
                // Class not found in the cache, see if it's real,
                // and try to add it
                final ClassLoader classLoader = context.getClassLoader();
                Class<?> clazz = null;
                if (prefixes == null || prefixes.length == 0) {
                    clazz = Class.forName(name, false, classLoader);
                } else {
                    ClassNotFoundException notFoundException = null;
                    for (final String prefix : prefixes) {
                        try {
                            clazz = Class.forName(prefix + name, false, classLoader);
                            break;
                        } catch (final ClassNotFoundException e) {
                            notFoundException = e;
                        }
                    }
                    if (clazz == null) {
                        if (notFoundException == null) {
                            throw new InflateException(attrs
                                    .getPositionDescription()
                                    + ": Error inflating class " + name);
                        } else {
                            throw notFoundException;
                        }
                    }
                }
                constructor = clazz.getConstructor(CONSTRUCTOR_SIGNATURE);
                constructor.setAccessible(true);
                CONSTRUCTOR_MAP.put(name, constructor);
            }

            Object[] args = new Object[2];
            args[0] = context;
            args[1] = attrs;
            return (Preference) constructor.newInstance(args);

        } catch (ClassNotFoundException e) {
            // If loadClass fails, we should propagate the exception.
            throw e;
        } catch (Exception e) {
            final InflateException ie = new InflateException(attrs
                    .getPositionDescription() + ": Error inflating class " + name, e);
            throw ie;
        }
    }

}
