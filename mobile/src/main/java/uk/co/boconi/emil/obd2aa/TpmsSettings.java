package uk.co.boconi.emil.obd2aa;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Emil on 22/09/2017.
 */
public class TpmsSettings extends AppCompatActivity {
    List<PidList> pidlist= new ArrayList<PidList>();
    SharedPreferences prefs;
    SharedPreferences.Editor editor;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tpms_setting_layout);
        prefs =  PreferenceManager.getDefaultSharedPreferences(this);
        editor = prefs.edit();
        Intent intent = getIntent();
        String listSerializedToJson = intent.getExtras().getString("pidlist");

        try {
            JSONArray jsonArray = new JSONArray(listSerializedToJson);
            for (int i=0;i<jsonArray.length();i++)
            {
                pidlist.add(new PidList(jsonArray.getJSONObject(i)));

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        EditText et = (EditText) findViewById(R.id.front_left_pressure);
        et.setText(prefs.getString("front_left_pressure"," "));
        et = (EditText) findViewById(R.id.front_right_pressure);
        et.setText(prefs.getString("front_right_pressure"," "));
        et = (EditText) findViewById(R.id.rear_left_pressure);
        et.setText(prefs.getString("rear_left_pressure"," "));
        et = (EditText) findViewById(R.id.rear_right_pressure);
        et.setText(prefs.getString("rear_right_pressure"," "));
    }



    public void pidwatcher(View v) {
                Log.d("HU","Caller tag: "+v.getTag());
                final String i= String.valueOf(v.getId());
                new PIDSearch(this, pidlist, i, null, this);
                }

    public void updateview(String s, int i) {
        String ss=null;
        for (PidList d : pidlist) {
            if (d.getPidName() != null && d.getPidName().equalsIgnoreCase(s)) {
                Log.d("OBD2AA", "Item possition: " + d.getShortPidName() + d.getMaxValue() + d.getMinValue() + "unit:" + d.getUnit());
                ss = d.getPid();
            }
        }
        EditText et = (EditText) findViewById(i);
        et.setText(ss);
        switch (i) {
            case R.id.front_left_pressure:
                editor.putString("front_left_pressure",ss);
            case R.id.front_right_pressure:
                editor.putString("front_right_pressure",ss);
            case R.id.rear_left_pressure:
                editor.putString("rear_left_pressure",ss);
            case R.id.rear_right_pressure:
                editor.putString("rear_right_pressure",ss);
        }
        editor.apply();
    }
}
