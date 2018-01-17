package uk.co.boconi.emil.obd2aa;


import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;
import com.google.android.apps.auto.sdk.CarToast;
import com.google.android.apps.auto.sdk.nav.suggestion.NavigationSuggestion;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;
import org.prowl.torque.remote.ITorqueService;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import me.priyesh.chroma.ChromaDialog;
import me.priyesh.chroma.ColorMode;
import me.priyesh.chroma.ColorSelectListener;
import uk.co.boconi.emil.obd2aa.Helpers.CameraDataBaseHelper;
import uk.co.boconi.emil.obd2aa.Helpers.DownloadHelper;
import uk.co.boconi.emil.obd2aa.Helpers.NearbyCameras;

import static android.R.attr.data;
import static android.R.attr.layout;
import static java.lang.Integer.parseInt;

import com.google.gson.GsonBuilder;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;


/**
 * Created by Emil on 14/08/2017.
 */


public class AppSettings extends AppCompatActivity {
    private int mColor;
    private int def_color_selector;
    private int warn1_color_selector;
    private int warn2_color_selector;
    SharedPreferences prefs;
    SharedPreferences.Editor editor;
    int gauge_number = 0;
    private ITorqueService torqueService;
    private String pids[];
    private String pidsdesc[];


    private Intent intent;
    private int text_color,needle_color;
    private String arch_width;
    private boolean isdebugging;
    private boolean alternativepulling;
    private AlertDialog.Builder b;
    List<PidList> pidlist = new ArrayList<PidList>();
    private String watch_fule;
    ArrayList<ItemData> list = new ArrayList<>();
    ArrayList<ItemData> style_list = new ArrayList<>();
    private boolean canclose;
    private FilePickerDialog filedialog;
    private boolean should_Restart = false;
    private int layout_style;
    private boolean dont_do_loop_update = false;
    private CameraDataBaseHelper staticDB=new CameraDataBaseHelper(this,2,"fixedcamera");
    private CameraDataBaseHelper mobileDB=new CameraDataBaseHelper(this,1,"mobilecamera");
    private AlertDialog notification_dialog;
    private AlertDialog location_dialog;
    private AlertDialog storage_dialog;


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (should_Restart) {
            Intent i = new Intent(getBaseContext(), AppSettings.class);
            startActivity(i);
        }
    }

    @Override
    protected void onPause() {
        Log.d("OBD2AA", "onPause");
        super.onPause();
        try {
            unbindService(connection);
        } catch (Exception E) {
            Log.d("OBD2AA", "No service to unbind from");
        }
        if (canclose) {
            Intent sendIntent = new Intent();
            sendIntent.setAction("org.prowl.torque.REQUEST_TORQUE_QUIT");
            sendBroadcast(sendIntent);
        }
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            location_dialog.dismiss();
            notification_dialog.dismiss();
            storage_dialog.dismiss();
        }
        catch (Exception E)
        {

        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        canclose = true;
        boolean successfulBind = bindService(intent, connection, 0);

        if (successfulBind) {

            // Not really anything to do here.  Once you have bound to the service, you can start calling
            // methods on torqueService.someMethod()  - look at the aidl file for more info on the calls
            Log.d("HU", "Connected to torque service!");

        } else {
            findViewById(R.id.mainappsetting).setVisibility(View.GONE);
            findViewById(R.id.notorque).setVisibility(View.VISIBLE);
            Log.e("HU", "Unable to connect to Torque plugin service");
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> enabledapps = NotificationManagerCompat.getEnabledListenerPackages(this);
        boolean havepermission = false;
        for (String currapp : enabledapps) {
            Log.d("OBD2AA", "package:" + currapp);
            if (currapp.equalsIgnoreCase("uk.co.boconi.emil.obd2aa"))
                havepermission = true;
        }

        if (!havepermission && prefs.getBoolean("fartkontrol", false)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(AppSettings.this);
            builder.setTitle(getResources().getString(R.string.perm_req));
            builder.setMessage(getResources().getString(R.string.perm_desc));
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                }
            });
            builder.setNegativeButton(getString(R.string.ignore), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            notification_dialog = builder.show();
        }


        if (((ContextCompat.checkSelfPermission(AppSettings.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(AppSettings.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) && prefs.getBoolean("daynight", false)) {

            AlertDialog.Builder builder = new AlertDialog.Builder(AppSettings.this);
            builder.setTitle(getResources().getString(R.string.loc_perm_tit));
            builder.setMessage(getResources().getString(R.string.loc_perm_desc));
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                    ActivityCompat.requestPermissions(AppSettings.this,
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 0);
                }
            });
            builder.setNegativeButton(getString(R.string.ignore), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            location_dialog = builder.show();

        }

        if (((ContextCompat.checkSelfPermission(AppSettings.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(AppSettings.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) && prefs.getBoolean("ShowSpeedCamWarrning",false)) {

            AlertDialog.Builder builder = new AlertDialog.Builder(AppSettings.this);
            builder.setTitle(getResources().getString(R.string.storage_perm));
            builder.setMessage(getResources().getString(R.string.storage_perm_desc));
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                    ActivityCompat.requestPermissions(AppSettings.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1983);
                }
            });
            builder.setNegativeButton(getString(R.string.ignore), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            storage_dialog = builder.show();

        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Resources res = getResources();
        // Change locale settings in the app.
        /*
        android.content.res.Configuration conf = res.getConfiguration();
        conf.setLocale(new Locale("fi")); // API 17+ only.
        getResources().updateConfiguration(conf, null);
        */
        should_Restart = false;
        setContentView(R.layout.mysettings);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        editor = prefs.edit();
        PreferenceManager.setDefaultValues(this, R.xml.preference, false);

        list.add(new ItemData(getResources().getString(R.string.clear), R.drawable.clear, 0));
        list.add(new ItemData(getResources().getString(R.string.a_nobg_only_compl_feel), R.drawable.a_nobg_only_compl_feel, 0));
        list.add(new ItemData(getResources().getString(R.string.b_nobg_fullfill), R.drawable.b_nobg_fullfill, 0));
        list.add(new ItemData(getResources().getString(R.string.c_bg_nofill), R.drawable.c_bg_nofill, 0));
        list.add(new ItemData(getResources().getString(R.string.d_bg_fill_compl), R.drawable.d_bg_fill_compl, 0));
        list.add(new ItemData(getResources().getString(R.string.e_bg_ffull_fill), R.drawable.e_bg_ffull_fill, 0));
        list.add(new ItemData(getResources().getString(R.string.d_bg_fill_compl), R.drawable.partfill_bg1, 0));
        list.add(new ItemData(getResources().getString(R.string.d_bg_fill_compl), R.drawable.partfill_bg2, 0));


        style_list.add(new ItemData("Auto", R.drawable.auto, 0));
        style_list.add(new ItemData(getString(R.string.style) + " 1", R.drawable.style_1, 5));
        style_list.add(new ItemData(getString(R.string.style) + " 2", R.drawable.style_2, 3));
        style_list.add(new ItemData(getString(R.string.style) + " 3", R.drawable.style_3, 4));


        //Load default colors:

        if (prefs.contains("def_color_selector")) {
            def_color_selector = prefs.getInt("def_color_selector", 0);
            warn1_color_selector = prefs.getInt("def_warn1_selector", 0);
            warn2_color_selector = prefs.getInt("def_warn2_selector", 0);
            text_color = prefs.getInt("text_color", 0);
            needle_color = prefs.getInt("needle_color", 0);
            arch_width = prefs.getString("arch_width", "3");
            gauge_number = prefs.getInt("gauge_number", 0);
            isdebugging = prefs.getBoolean("debugging", false);
            alternativepulling = prefs.getBoolean("alternativepulling", false);
            watch_fule = prefs.getString("watch_fuel", "0");
            layout_style = prefs.getInt("layout", 0);

        } else {
            def_color_selector = Color.rgb(0, 255, 0);
            warn1_color_selector = Color.rgb(255, 106, 0);
            warn2_color_selector = Color.rgb(255, 0, 0);
            text_color = Color.WHITE;
            needle_color = Color.WHITE;
            arch_width = "8";
            addgauge(6);
            gauge_number = 6;
            isdebugging = false;
            alternativepulling = false;
            watch_fule = "0";
            layout_style = 0;

            editor.putInt("def_color_selector", def_color_selector);
            editor.putInt("def_warn1_selector", warn1_color_selector);
            editor.putInt("def_warn2_selector", warn2_color_selector);
            editor.putInt("gauge_number", gauge_number);
            editor.putInt("text_color", text_color);
            editor.putInt("needle_color", needle_color);
            editor.putInt("layout", layout_style);
            editor.putString("arch_width", arch_width);
            editor.putBoolean("debugging", isdebugging);
            editor.putBoolean("alternativepulling", alternativepulling);
            editor.putString("watch_fuel", watch_fule);
            editor.putString("watch_fuel", watch_fule);
            editor.apply();
            // editor.commit();

        }

        final Spinner sp = (Spinner) findViewById(R.id.layout_style_spinner);
        SpinnerAdapter adapter = new SpinnerAdapter(this,
                R.layout.spinner_layout, R.id.txt, style_list);
        sp.setAdapter(adapter);
        sp.setSelection(layout_style);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                Log.d("HU", "BG Spinner callback  pos: " + position + "Guages needed: " + style_list.get(position).getGaugenumber());

                editor.putInt("layout", position);
                editor.apply();
                editor.commit();
                Spinner tmpspinner = (Spinner) findViewById(R.id.gaugeselector);
                dont_do_loop_update = true;
                tmpspinner.setSelection(style_list.get(position).getGaugenumber() - 1);

                //gaugechanger(findViewById(R.id.gaugeselector));

            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });


        EditText et = (EditText) findViewById(R.id.arch_width);
        et.setTag("arch_width");
        et.setText(arch_width);
        et.addTextChangedListener(getTextWatcher(et));

        findViewById(R.id.def_color_selector).setBackgroundColor(def_color_selector);
        findViewById(R.id.warn1_color_selector).setBackgroundColor(warn1_color_selector);
        findViewById(R.id.warn2_color_selector).setBackgroundColor(warn2_color_selector);
        findViewById(R.id.text_color_selector).setBackgroundColor(text_color);
        findViewById(R.id.needle_color_selector).setBackgroundColor(needle_color);


        Spinner myspinner = (Spinner) findViewById(R.id.gaugeselector);
        dont_do_loop_update = true;
        myspinner.setSelection(gauge_number - 1);

        myspinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                Log.d("HU", "Spinner onSelected callback");
                gaugechanger(parentView);
                if (!dont_do_loop_update)
                    sp.setSelection(0);
                else
                    dont_do_loop_update = false;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });

        ColorMode colorMode = ColorMode.RGB;

        intent = new Intent();
        intent.setClassName("org.prowl.torque", "org.prowl.torque.remote.TorqueService");
        startService(intent);


        if (prefs.getBoolean("ShowSpeedCamWarrning",true) && (ContextCompat.checkSelfPermission(AppSettings.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(AppSettings.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) ) { //only download if enabled!
            new DownloadHelper(1, this, 3000);
            new DownloadHelper(2, this, 0);
        }

    }
    public void showcolorpicker(final View V)
    {


        if(V.getId()==R.id.def_color_selector)
            mColor=def_color_selector;
        else if (V.getId()==R.id.warn1_color_selector)
            mColor=warn1_color_selector;
        else if (V.getId()==R.id.warn2_color_selector)
            mColor=warn2_color_selector;
        else if (V.getId()==R.id.text_color_selector)
            mColor=text_color;
        else
            mColor=needle_color;

        ArrayAdapter<ColorMode> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, ColorMode.values());

        Spinner mColorModeSpinner = new Spinner(this);
        mColorModeSpinner.setAdapter(adapter);
        mColorModeSpinner.setSelection(adapter.getPosition(ColorMode.RGB));

        new ChromaDialog.Builder()
                .initialColor(mColor)
                .colorMode((ColorMode) mColorModeSpinner.getSelectedItem())
                .onColorSelected(new ColorSelectListener() {
                    @Override public void onColorSelected(int color) {
                        updatecolor(V, color);
                        mColor = color;
                    }
                })
                .create()
                .show(getSupportFragmentManager(), "dialog");
    }
    private void updatecolor(View V,int newcolor)
    {
        if(V.getId()==R.id.def_color_selector)
            def_color_selector=newcolor;
        else if(V.getId()==R.id.warn1_color_selector)
            warn1_color_selector=newcolor;
        else if(V.getId()==R.id.warn2_color_selector)
            warn2_color_selector=newcolor;
        else if(V.getId()==R.id.text_color_selector)
            text_color=newcolor;
        else
            needle_color=newcolor;
        applyandshowcolor();
    }

    private void applyandshowcolor(){

        editor.putInt("def_color_selector",def_color_selector);
        editor.putInt("def_warn1_selector",warn1_color_selector);
        editor.putInt("def_warn2_selector",warn2_color_selector);
        editor.putInt("text_color",text_color);
        editor.putInt("needle_color",needle_color);
        editor.apply();
        //editor.commit();
        findViewById(R.id.def_color_selector).setBackgroundColor(def_color_selector);
        findViewById(R.id.warn1_color_selector).setBackgroundColor(warn1_color_selector);
        findViewById(R.id.warn2_color_selector).setBackgroundColor(warn2_color_selector);
        findViewById(R.id.text_color_selector).setBackgroundColor(text_color);
        findViewById(R.id.needle_color_selector).setBackgroundColor(needle_color);
    }

    public void gaugechanger(View v)
    {
        Log.d("HU","Gaugchanger called");

        Spinner myspinner=(Spinner) v;
        int currgauge=0;
       try {
            currgauge = parseInt(myspinner.getSelectedItem().toString());
       }
       catch (Exception E)
       {
           Log.e("OBD2AA",E.getMessage());
           return;
       }

        if (currgauge==gauge_number)
        {
            showgaugesettings(1,currgauge);
            return;
        }
        else if (currgauge<gauge_number)
            removegauge(currgauge);
        else if (currgauge>gauge_number)
        {
            addgauge(currgauge);
            showgaugesettings(gauge_number+1,currgauge);
        }
        gauge_number=currgauge;
        editor.putInt("gauge_number",gauge_number);
        editor.apply();

    }

    public void addgauge(int newgauge) {
        for (int i = gauge_number + 1; i <= newgauge; i++) {
           editor.putString("gaugepid_"+i,"");
           editor.putString("gaugename_"+i,"");
           editor.putString("gauge_orig_name_"+i,"");
           editor.putString("gaugeunit_"+i,"");
           editor.putString("warn1level_"+i,"80");
           editor.putString("warn2level_"+i,"90");
           editor.putString("custom_bg_path_"+i,"");

           editor.putBoolean("isreversed_"+i,false);
           editor.putInt("arch_indent_"+i,0);
           editor.putInt("arch_startpos_"+i,0);
           editor.putInt("arch_length_"+i,0);
           editor.putInt("gaugestyle_"+i,0);
           editor.putBoolean("showneedle_"+i,true);
           editor.putBoolean("showscale_"+i,true);
           editor.putBoolean("showunit_"+i,true);
           editor.putBoolean("showtext_"+i,true);
           editor.putBoolean("usegradienttext_"+i,false);
           editor.putBoolean("showdecimal_"+i,true);
           editor.putBoolean("use_custom_bg_"+i,false);
           editor.putFloat("minval_"+i,0);
           editor.putFloat("maxval_"+i,100);
           editor.putBoolean("locked_"+i,false);
           editor.apply();
           editor.commit();
        }
    }
    public void removegauge(int newgauge)
    {
        for (int i = newgauge+1; i <= gauge_number; i++) {
            editor.remove("gaugepid_"+i);
            editor.remove("gaugename_"+i);
            editor.remove("gauge_orig_name_"+i);
            editor.remove("gaugeunit_"+i);
            editor.remove("warn1level_"+i);
            editor.remove("warn2level_"+i);
            editor.remove("isreversed_"+i);
            editor.remove("gaugestyle_"+i);
            editor.remove("showneedle_"+i);
            editor.remove("showscale_"+i);
            editor.remove("showunit_"+i);
            editor.remove("showtext_"+i);
            editor.remove("usegradienttext_"+i);
            editor.remove("showdecimal_"+i);
            editor.remove("minval_"+i);
            editor.remove("maxval_"+i);
            editor.remove("locked_"+i);
            editor.remove("custom_bg_path_"+i);
            editor.remove("arch_indent_"+i);
            editor.remove("arch_startpos_"+i);
            editor.remove("arch_length_"+i);
            editor.remove("use_custom_bg_"+i);
            editor.apply();
            editor.commit();
            LinearLayout lp = (LinearLayout) findViewById(R.id.parrent_container);
            lp.removeView(lp.findViewWithTag("gaugewrapper_"+i));
        }

    }
    public void showgaugesettings(int fromgauge,int newgauge)
    {

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout parent = (LinearLayout) (LinearLayout) findViewById(R.id.parrent_container);
        for (int i=fromgauge;i<=newgauge;i++)
        {

            final View custom = inflater.inflate(R.layout.gauge, null);
            custom.setTag("gaugewrapper_"+i);
            TextView tv = (TextView) custom.findViewById(R.id.gaugetitle);
            tv.setText(getResources().getString(R.string.gauge_name)+" " + i+" - "+ prefs.getString("gauge_orig_name_"+i,"")+": ");
            tv.setTag("gaugetitle_"+i);
            EditText et = (EditText) custom.findViewById(R.id.gaugepid);

            et.setOnClickListener(pidwatcher(et));
            et.setTag("gaugepid_"+i);
            et.setText(prefs.getString("gaugepid_"+i,""));
            et = (EditText) custom.findViewById(R.id.gaugename);
            et.addTextChangedListener(getTextWatcher(et));
            et.setTag("gaugename_"+i);
            et.setText(prefs.getString("gaugename_"+i,""));

            et = (EditText) custom.findViewById(R.id.minval);
            et.addTextChangedListener(getTextWatcher(et));
            et.setTag("minval_"+i);
            try {
                et.setText(Float.toString(prefs.getFloat("minval_" + i, 0)));
            }
            catch (Exception E)
                {//Oh crap, we had integers before so now it's all failing;
                    int aux=prefs.getInt("minval_"+i,0);
                    editor.putFloat("minval_"+i,aux);
                    editor.apply();
                    editor.commit();
                    et.setText(Integer.toString(aux));
                }
            et = (EditText) custom.findViewById(R.id.maxval);
            et.addTextChangedListener(getTextWatcher(et));
            et.setTag("maxval_"+i);
            try {
                et.setText(Float.toString(prefs.getFloat("maxval_" + i, 100)));
                }
                catch (Exception E)
                {//Oh crap, we had integers before so now it's all failing;
                    int aux=prefs.getInt("maxval_"+i,0);
                    editor.putFloat("maxval_"+i,aux);
                    editor.apply();
                    editor.commit();
                    et.setText(Integer.toString(aux));
                }

            et = (EditText) custom.findViewById(R.id.warn1level);
            et.addTextChangedListener(getTextWatcher(et));
            et.setTag("warn1level_"+i);
            et.setText(prefs.getString("warn1level_"+i,""));
            et = (EditText) custom.findViewById(R.id.warn2level);
            et.addTextChangedListener(getTextWatcher(et));
            et.setTag("warn2level_"+i);
            et.setText(prefs.getString("warn2level_"+i,""));

            et = (EditText) custom.findViewById(R.id.custom_bg_path);
            et.setTag("custom_bg_path_"+i);
            et.setText(prefs.getString("custom_bg_path_"+i,""));
            et.setOnClickListener(custom_bg_watcher(et));
            if (!prefs.getBoolean("use_custom_bg_"+i,false))
            {
                View tmpview = (View) et.getParent();
                tmpview.setVisibility(View.GONE);
            }

            et = (EditText) custom.findViewById(R.id.arch_indent);
            et.setTag("arch_indent_"+i);
            try {
                et.setText(String.valueOf(prefs.getInt("arch_indent_" + i, 0)));
            }
            catch (Exception E)
            { //no indent defined yet
                Log.e("OBD2AA","Could not get arch indent!" + E);
                editor.putInt("arch_indent_"+i,0);
                editor.apply();
                editor.commit();
                et.setText("0");
            }
            et.addTextChangedListener(getTextWatcher(et));
            if (!prefs.getBoolean("use_custom_bg_"+i,false))
            {
                View tmpview = (View) et.getParent();
                tmpview.setVisibility(View.GONE);
            }

            et = (EditText) custom.findViewById(R.id.arch_startpos);
            et.setTag("arch_startpos_"+i);
            try {
                et.setText(String.valueOf(prefs.getInt("arch_startpos_" + i, 270)));
            }
            catch (Exception E)
            { //no indent defined yet
                Log.e("OBD2AA","Could not get arch indent!" + E);
                editor.putInt("arch_startpos_"+i,270);
                editor.apply();
                editor.commit();
                et.setText("270");
            }
            et.addTextChangedListener(getTextWatcher(et));
            if (!prefs.getBoolean("use_custom_bg_"+i,false))
            {
                View tmpview = (View) et.getParent();
                tmpview.setVisibility(View.GONE);
            }

            et = (EditText) custom.findViewById(R.id.arch_length);
            et.setTag("arch_length_"+i);
            try {
                et.setText(String.valueOf(prefs.getInt("arch_length_" + i, 263)));
            }
            catch (Exception E)
            { //no indent defined yet
                Log.e("OBD2AA","Could not get arch indent!" + E);
                editor.putInt("arch_length_"+i,263);
                editor.apply();
                editor.commit();
                et.setText("263");
            }
            et.addTextChangedListener(getTextWatcher(et));
            if (!prefs.getBoolean("use_custom_bg_"+i,false))
            {
                View tmpview = (View) et.getParent();
                tmpview.setVisibility(View.GONE);
            }

            CheckBox cb = (CheckBox) custom.findViewById(R.id.isreversed);
            cb.setTag("isreversed_"+i);
            cb.setChecked(prefs.getBoolean("isreversed_"+i,false));
            cb = (CheckBox) custom.findViewById(R.id.showscale);
            cb.setTag("showscale_"+i);
            cb.setChecked(prefs.getBoolean("showscale_"+i,false));
            cb = (CheckBox) custom.findViewById(R.id.showneedle);
            cb.setTag("showneedle_"+i);
            cb.setChecked(prefs.getBoolean("showneedle_"+i,false));
            cb = (CheckBox) custom.findViewById(R.id.showtext);
            cb.setTag("showtext_"+i);
            cb.setChecked(prefs.getBoolean("showtext_"+i,false));
            cb = (CheckBox) custom.findViewById(R.id.usegradienttext);
            cb.setTag("usegradienttext_"+i);
            cb.setChecked(prefs.getBoolean("usegradienttext_"+i,false));
            cb = (CheckBox) custom.findViewById(R.id.showdecimal);
            cb.setTag("showdecimal_"+i);
            cb.setChecked(prefs.getBoolean("showdecimal_"+i,false));
            cb = (CheckBox) custom.findViewById(R.id.showunit);
            cb.setTag("showunit_"+i);
            cb.setChecked(prefs.getBoolean("showunit_"+i,false));
            cb = (CheckBox) custom.findViewById(R.id.use_custom_bg);
            cb.setTag("use_custom_bg_"+i);
            cb.setChecked(prefs.getBoolean("use_custom_bg_"+i,false));
            cb = (CheckBox) custom.findViewById(R.id.use_custom_needle);
            cb.setTag("use_custom_needle_"+i);
            cb.setChecked(prefs.getBoolean("use_custom_needle_"+i,false));

            et = (EditText) custom.findViewById(R.id.custom_needle_path);
            et.setTag("custom_needle_path_"+i);
            et.setText(prefs.getString("custom_needle_path_"+i,""));
            et.setOnClickListener(custom_bg_watcher(et));
            if (!prefs.getBoolean("use_custom_needle_"+i,false))
            {
                View tmpview = (View) et.getParent();
                tmpview.setVisibility(View.GONE);
            }

            Spinner sp=(Spinner) custom.findViewById(R.id.gauges_style_spinner);
            SpinnerAdapter adapter=new SpinnerAdapter(this,
                    R.layout.spinner_layout,R.id.txt,list);
            sp.setAdapter(adapter);
            sp.setTag("gaugestyle_"+i);
            sp.setSelection(prefs.getInt("gaugestyle_"+i,0));
            sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    Log.d("HU","BG Spinner callback  pos: "+position);
                    String mytag= (String) parentView.getTag();
                    Log.d("HU",mytag);
                    editor.putInt(mytag,position);
                    editor.apply();
                    editor.commit();

                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                    // your code here
                }

            });

            parent.addView(custom);

            custom.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

                @Override
                public void onGlobalLayout() {

                    ImageButton ib=(ImageButton) custom.findViewById(R.id.toggler);
                    int compactheight = ib.getHeight();
                    custom.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, compactheight));
                    custom.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });


        }

    }

    protected void updateview(String position, int i) {

        for (PidList d : pidlist) {
            if (d.getPidName() != null && d.getPidName().equalsIgnoreCase(position)) {
                Log.d("OBD2AA", "Item possition: " + d.getShortPidName() + d.getMaxValue() + d.getMinValue() + "unit:" + d.getUnit());

                LinearLayout lp = (LinearLayout) findViewById(R.id.parrent_container);
                EditText et = (EditText) lp.findViewWithTag("gaugepid_" + i);
                et.setText(d.getPid());

                TextView tv = (TextView) lp.findViewWithTag("gaugetitle_" + i);
                tv.setText("Gauge " + i+" - "+ d.getShortPidName()+": ");


                editor.putString("gaugepid_" + i, d.getPid());

                et = (EditText) lp.findViewWithTag("gaugename_" + i);
                et.setText(d.getShortPidName());
                et.requestFocus();

                et = (EditText) lp.findViewWithTag("minval_"+i);
                et.setText(d.getMinValue());
                et = (EditText) lp.findViewWithTag("maxval_"+i);
                et.setText(d.getMaxValue());

                editor.putString("gaugename_" + i, d.getShortPidName());
                editor.putString("gauge_orig_name_" + i, d.getShortPidName());
                editor.putString("gaugeunit_" + i, d.getUnit());
                editor.putFloat("minval_" + i, parseInt(d.getMinValue()));
                editor.putFloat("maxval_" + i, parseInt(d.getMaxValue()));
                editor.putBoolean("locked_"+i,false);
                editor.apply();
            }

        }
    }


    private  View.OnClickListener custom_bg_watcher(final EditText et){
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String pidnumber=v.getTag().toString();
                //final String pidnumber=v.getTag().toString().split("_")[3];
               // if (!prefs.getBoolean("use_custom_bg_"+pidnumber,false))
                 //   return;
                DialogProperties properties = new DialogProperties();
                properties.selection_mode = DialogConfigs.SINGLE_MODE;
                properties.selection_type = DialogConfigs.FILE_SELECT;
                properties.root = new File(DialogConfigs.DEFAULT_DIR);
                properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
                properties.offset = new File(DialogConfigs.DEFAULT_DIR);
                properties.extensions = null;
                filedialog = new FilePickerDialog(AppSettings.this,properties);
                filedialog.setTitle("Select a File");
                filedialog.show();
                filedialog.setDialogSelectionListener(new DialogSelectionListener() {
                    @Override
                    public void onSelectedFilePaths(String[] files) {
                        //files is the array of the paths of files selected by the Application User.
                        Log.d("OBD2AA","Selected file"+files[0]);
                       // editor.putString("custom_bg_path_"+pidnumber,files[0] );
                        editor.putString(pidnumber,files[0] );
                        editor.commit();
                        et.setText(files[0]);
                    }
                });


            }
        };
    }

    private View.OnClickListener pidwatcher(EditText et) {
       return new View.OnClickListener() {

           public void onClick(final View v) {
               Log.d("HU","Caller tag: "+v.getTag());
               final String i=v.getTag().toString().split("_")[1];
                new PIDSearch(AppSettings.this, pidlist, i, AppSettings.this, null);

           }
       };
    }

    public void  checkboxupdater (View V)
    {
        String mytag= (String) V.getTag();
        Log.d("HU",mytag);
        CheckBox cb = (CheckBox) V;
        editor.putBoolean(mytag,cb.isChecked());
        editor.apply();
        editor.commit();
        if (mytag.contains("use_custom_bg_"))
        {
            String pidnumber=mytag.split("_")[3];
            View tmptview = (View) V.getRootView().findViewWithTag("custom_bg_path_" + pidnumber).getParent();
            View archindent = (View) V.getRootView().findViewWithTag("arch_indent_" + pidnumber).getParent();
            View archlength = (View) V.getRootView().findViewWithTag("arch_length_" + pidnumber).getParent();
            View archstart = (View) V.getRootView().findViewWithTag("arch_startpos_" + pidnumber).getParent();
            if (cb.isChecked())
            {
                tmptview.setVisibility(View.VISIBLE);
                archindent.setVisibility(View.VISIBLE);
                archlength.setVisibility(View.VISIBLE);
                archstart.setVisibility(View.VISIBLE);
            }
            else {
                tmptview.setVisibility(View.GONE);
                archindent.setVisibility(View.GONE);
                archlength.setVisibility(View.GONE);
                archstart.setVisibility(View.GONE);
            }
        }
        else if (mytag.contains("use_custom_needle_"))
        {
            String pidnumber=mytag.split("_")[3];
            View tmptview = (View) V.getRootView().findViewWithTag("custom_needle_path_" + pidnumber).getParent();
            if (cb.isChecked())
                tmptview.setVisibility(View.VISIBLE);
            else
                tmptview.setVisibility(View.GONE);
        }
    }

    private TextWatcher getTextWatcher(final EditText editText) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String tagname=editText.getTag().toString();
                if (tagname.contains("minval_")|| tagname.contains("maxval_"))
                {
                    float myval=0;

                    if (editText.getText().toString().length()!=0)
                        try {
                            myval = Float.parseFloat(editText.getText().toString());
                        }
                        catch (NumberFormatException e)
                        {

                        }
                    String temp_gauge_number = tagname.split("_")[1];
                    editor.putBoolean("locked_"+temp_gauge_number,true);
                    editor.putFloat(tagname, myval);
                }
                else if (tagname.contains("arch_indent_") || tagname.contains("arch_startpos_") || tagname.contains("arch_length_"))
                {

                    int myval=0;
                    if (editText.getText().toString().length()!=0)
                        try {
                            myval = parseInt(editText.getText().toString());
                        }
                        catch (NumberFormatException e)
                        {

                        }

                    editor.putInt(tagname, myval);
                }
                else
                    editor.putString(tagname, editText.getText().toString());
                editor.apply();
                editor.commit();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        };
    }

    private ServiceConnection connection = new ServiceConnection() {

        public void onServiceConnected(ComponentName arg0, IBinder service) {
            Log.d("HU","SERVICE CONNECTED!");
            torqueService = ITorqueService.Stub.asInterface(service);

            try {
                if (torqueService.getVersion() < 19) {
                    Toast.makeText(AppSettings.this,"Incorrect version. You are using an old version of Torque with this plugin.\n\nThe plugin needs the latest version of Torque to run correctly.\n\nPlease upgrade to the latest version of Torque from Google Play",Toast.LENGTH_LONG);
                    return;
                }
            } catch(RemoteException e) {

            }
            Log.d("HU","Have Torque service connection!");

            /*try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/

            try {
                pids=torqueService.listAllPIDs();
                pidsdesc=torqueService.getPIDInformation(pids);
                for (int i = 0; i < pids.length; i++) {
                    pidsdesc[i]=pidsdesc[i]+","+pids[i];
                }
                Arrays.sort(pidsdesc,String.CASE_INSENSITIVE_ORDER);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            for (int i = 0; i < pids.length; i++) {
                String[] info=pidsdesc[i].split(",");
                if (isdebugging)
                try {
                    Log.d ("OBD2AA","Pid name: " + info[0] + " Units: " + info[2] + " Unit conversion: " + torqueService.getPreferredUnit(info[2]));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                pidlist.add(new PidList(pidsdesc[i]));
            }


        };
        public void onServiceDisconnected(ComponentName name) {
            torqueService = null;
        };
    };

    public ITorqueService getTorqueService() {
        return torqueService;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mymenu, menu);
        return super.onCreateOptionsMenu(menu);
    }



    public void openpref(MenuItem item) {
        Log.d("OBD2AA","Menu Clicked");
        canclose=false;
        Intent i=null;
        if (item.getItemId()==R.id.pref_menu)
            i = new Intent(getBaseContext(),AppPreferences.class);
        else if (item.getItemId()==R.id.speedcam_menu)
            i = new Intent(getBaseContext(),AppCameraSettings.class);
        else if (item.getItemId()==R.id.tpms_menu)
        {
            i = new Intent(getBaseContext(),TpmsSettings.class);
            String listSerializedToJson = new Gson().toJson(pidlist);
            i.putExtra("pidlist",  listSerializedToJson);
        }
        startActivity(i);

    }
    public  void export_settings(MenuItem item)
    {
        Map<String, ?> all = prefs.getAll();
        List list = new ArrayList();
        for (Map.Entry<String, ?> entry : all.entrySet())
        {
            if (entry.getValue() instanceof Boolean) {
                list.add(new String[]{entry.getKey(), entry.getValue().toString(), "B"});
            }
            else if (entry.getValue() instanceof String) {
                list.add(new String[]{entry.getKey(), entry.getValue().toString(), "S"});
            }
            else if (entry.getValue() instanceof Integer) {
                list.add(new String[]{entry.getKey(), entry.getValue().toString(), "I"});
            }
            else if (entry.getValue() instanceof Float) {
                list.add(new String[]{entry.getKey(), entry.getValue().toString(), "F"});
            }
        }
        //String listSerializedToJson = new Gson().toJson(prefs.getAll());
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String listSerializedToJson = gson.toJson(list);


        Log.d("OBD2AA",listSerializedToJson);

            String filename = "obd2aa_settings.json";

            FileOutputStream outputStream;
            File file = new File(getApplicationContext().getExternalFilesDir(null), filename);
            final Uri installURI = FileProvider.getUriForFile(AppSettings.this, getApplicationContext().getPackageName() + ".uk.co.boconi.emil.obd2aa.provider", file);

        try {
                file.createNewFile();
                outputStream = new FileOutputStream(file);
                outputStream.write(listSerializedToJson.getBytes());
                outputStream.close();
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/*");
                sharingIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (Build.VERSION.SDK_INT <= 23)
                sharingIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + file.getAbsolutePath()));
            else
                sharingIntent.putExtra(Intent.EXTRA_STREAM, installURI);
                startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_settings)));
            } catch (Exception e) {
                Toast.makeText(AppSettings.this,getResources().getString(R.string.file_creation_error),Toast.LENGTH_LONG);
                e.printStackTrace();

            }
    }

    public  void import_settings(MenuItem item)
    {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = new File(DialogConfigs.DEFAULT_DIR);
        properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
        properties.offset = new File(DialogConfigs.DEFAULT_DIR);
        properties.extensions = null;
        filedialog = new FilePickerDialog(AppSettings.this,properties);
        filedialog.setTitle("Select a File");
        filedialog.show();
        filedialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {
                //files is the array of the paths of files selected by the Application User.
                Log.d("OBD2AA","Selected file"+files[0]);
                File file = new File(files[0]);
                int length = (int) file.length();
                byte[] bytes = new byte[length];

                try {
                    FileInputStream in = new FileInputStream(file);
                    in.read(bytes);
                    in.close();
                    String contents = new String(bytes);
                    JSONArray jsonArr = new JSONArray(contents);
                    editor.clear();
                    for (int i=0;i<jsonArr.length();i++)
                    {

                        JSONArray temp=  jsonArr.getJSONArray(i);

                        if (temp.getString(2).equalsIgnoreCase("S"))
                            editor.putString(temp.getString(0),temp.getString(1));
                        else if (temp.getString(2).equalsIgnoreCase("I"))
                            editor.putInt(temp.getString(0),parseInt(temp.getString(1)));
                        else if (temp.getString(2).equalsIgnoreCase("F"))
                            editor.putFloat(temp.getString(0),Float.parseFloat(temp.getString(1)));
                        else
                            editor.putBoolean(temp.getString(0), Boolean.parseBoolean(temp.getString(1)));
                    }
                    editor.apply();
                            AlertDialog.Builder builder = new AlertDialog.Builder(AppSettings.this);
                            builder.setTitle(getResources().getString(R.string.imp_comp_tit));
                            builder.setMessage(getResources().getString(R.string.imp_comp_text));
                            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    canclose=false;
                                    should_Restart=true;
                                    AppSettings.this.finish();

                                }
                            });

                            builder.show();
                }
                catch (Exception E)
                {
                    Log.e("OBD2AA",E.toString());
                }

            }
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Log.d("OBD2AA","Permission granted: " + requestCode+ "permissions: " + permissions.toString());
        switch (requestCode) {
            case FilePickerDialog.EXTERNAL_READ_PERMISSION_GRANT: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(filedialog!=null)
                    {   //Show dialog if the read permission has been granted.
                        filedialog.show();
                    }
                }
                else {
                    //Permission has not been granted. Notify the user.
                    Toast.makeText(AppSettings.this,"Permission is Required for getting list of files",Toast.LENGTH_SHORT).show();
                }
            }
            case 1983:{
                new DownloadHelper(1, this, 3000);
                new DownloadHelper(2, this, 1);
            }
        }

    }
    public void doPreview(View view)
    {
        canclose=false;
        Intent i = new Intent(getBaseContext(),PreviewActivity.class);
        startActivity(i);
    }


    public void togglelayout(View view)
    {
        LinearLayout l = (LinearLayout) (view.getParent()).getParent();
        ImageButton ib = (ImageButton) view;
     if(view.getTag().toString().equalsIgnoreCase("collapsed"))
     {
         l.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
         ib.setImageResource(R.drawable.ic_remove_black_24dp);
         view.setTag("expanded");
     }
        else
     {
         l.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, view.getHeight()));
         ib.setImageResource(R.drawable.ic_add_black_24dp);
         view.setTag("collapsed");
     }
    }
    private final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            if(msg.arg1 == 1)
                CarToast.makeText(getBaseContext(), getString(R.string.mobile_updated), Toast.LENGTH_LONG).show();
            else
                CarToast.makeText(getBaseContext(),getString(R.string.static_updated),Toast.LENGTH_LONG).show();
        }
    };


    public void showDownload_comp_message(int type){
        Message msg = handler.obtainMessage();
        msg.arg1 = type;
        handler.sendMessage(msg);
  }
}
