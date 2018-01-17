package uk.co.boconi.emil.obd2aa;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TableLayout;

import com.google.android.apps.auto.sdk.CarUiController;

import java.util.Random;

/**
 * Created by Emil on 21/09/2017.
 */

public class PreviewActivity extends Activity {

    private ArcProgress arcProgress;
    private SharedPreferences prefs;

    private int gauge_number;

    private String[] pids;
    private long[] lastpiddraw;
    private boolean isshowing;
    private boolean useDigital;
    private String[] units;
    private Boolean[] convertunits;
    private Boolean isdebugging;



    @Override
    public void onPause(){
        super.onPause();
        isshowing=false;
        Log.d("OBD2-APP","On Pause");

    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_odb2_a);



        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        gauge_number = prefs.getInt("gauge_number", 0);
        useDigital = prefs.getBoolean("usedigital", false);
        pids = new String[gauge_number];

        units = new String[gauge_number];
        convertunits = new Boolean[gauge_number];
        isdebugging=prefs.getBoolean("debugging",false);

        Log.d("OBD2AA","OBD2AA APP STARTED, BEFORE BIND.");




    }

    @Override
    public void onResume(){
        super.onResume();
        Log.d("OBD2AA","OBD2AA On resume");
        final ConstraintLayout layout = (ConstraintLayout) findViewById(R.id.activity_odb2_a);
        ViewTreeObserver vto = layout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener (new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                layout.setBackgroundColor(Color.BLACK);


                isshowing=true;
                Log.d("OBD2AA", "DO ECU CONNECTED....");
                TableLayout mywrapper = (TableLayout) findViewById(R.id.tv_log);
                //mywrapper.setBackgroundColor(Color.WHITE);
                int width = Math.round(mywrapper.getHeight() * 1.66f);
                int margins=Math.round((mywrapper.getWidth()-width)/2);

                ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) mywrapper.getLayoutParams();
                lp.setMargins(margins,0,margins,0);
                mywrapper.setLayoutParams(lp);
                DrawGauges gauge = new DrawGauges();
                gauge.SetUpandDraw(PreviewActivity.this, width, mywrapper.getHeight(), mywrapper,null);
                layout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                //do_random_data();
            }
        });

    }

    public void do_random_data() {
        Thread runnerthread = new Thread() {
            @Override
            public void run() {
                while (isshowing) {


                    for (int r = 0; r < gauge_number; r++) {
                        {
                            Random random = new Random();
                            int Low = prefs.getInt("minval_" + (r + 1), 0);
                            int High = prefs.getInt("maxval_" + (r + 1), 0);
                            float Result = (random.nextFloat()*(High - Low)) + Low;
                            Message msg = new Message(); //handler.obtainMessage();
                            msg.obj = Result;
                            msg.arg1 = (r+1);
                            handler.sendMessage(msg);

                        }
                    }
                    try {
                        Thread.sleep(210);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            ;
        };
        runnerthread.start();
    }

    public final Handler handler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            if (!isshowing)
                return;
            // Log.d("OBD2APP","Handler: "+msg.arg2+ " value: "+msg.arg1);
            TableLayout tv=null;
            try {
                tv = (TableLayout) findViewById(R.id.tv_log);
            }
            catch (NullPointerException E) {
                throw E;
            }

            if (tv==null)
                return;
            float myvals=(float) msg.obj;
            int i=msg.arg1;
            arcProgress = (ArcProgress) tv.findViewWithTag("gauge_" + (i+1));
            if (arcProgress==null)
                return;
            if (myvals>arcProgress.getMax())
            {
                //Check if the given PID needs the MAX value to be updated.

                myvals=arcProgress.getMax();
            }

            ArchAnimaton animation = new ArchAnimaton(arcProgress, myvals);
            if (animation==null)
                return;
            animation.setDuration(200);
            arcProgress.startAnimation(animation);


        }
    };


}
