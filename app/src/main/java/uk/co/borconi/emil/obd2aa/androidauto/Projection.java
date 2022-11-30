package uk.co.borconi.emil.obd2aa.androidauto;

import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.widget.TableLayout;

import androidx.constraintlayout.widget.ConstraintLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import uk.co.borconi.emil.obd2aa.ArcProgress;
import uk.co.borconi.emil.obd2aa.R;
import uk.co.borconi.emil.obd2aa.gauge.ArchAnimation;
import uk.co.borconi.emil.obd2aa.gauge.DrawGauges;
import uk.co.borconi.emil.obd2aa.gauge.GaugeUpdate;
import uk.co.borconi.emil.obd2aa.helpers.PreferencesHelper;

public class Projection extends Presentation {

    private final Context context;

    private PreferencesHelper prefs;
    private ConstraintLayout contentLayout;

    public Projection(Context outerContext, Display display) {
        super(outerContext, display);
        this.context = outerContext;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferencesHelper.getPreferences(context);
        String layoutStyle = prefs.getLayoutStyle();

        if ("AUTO".equalsIgnoreCase(layoutStyle)) {
            setContentView(R.layout.gauge_layout_auto);
            contentLayout = findViewById(R.id.wrapper_layout);
            DrawGauges.renderAutoLayout(contentLayout, context);
        } else {
            setContentView(DrawGauges.getLayoutForStyle(layoutStyle));
            contentLayout = findViewById(R.id.wrapper_layout);
            DrawGauges.renderSetLayout(contentLayout, context);
        }
        Log.d("Projection", "Started up correctly");
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDisplayRemoved() {
        EventBus.getDefault().unregister(this);
        super.onDisplayRemoved();
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onEvent(GaugeUpdate gauge) {
        if (gauge.isMin()) {
            update_gauge_min(gauge.getGauge() + 1, gauge.getVal());
            return;
        }
        if (gauge.isMax()) {
            update_gauge_max(gauge.getGauge() + 1, gauge.getVal());
            return;
        }
        ArcProgress arcProgress = contentLayout.findViewWithTag("gauge_" + (gauge.getGauge() + 1));
        if (arcProgress == null) {
            return;
        }
        if (gauge.getVal() > arcProgress.getMax()) {
            gauge.setVal(arcProgress.getMax());
        }
        ArchAnimation animation = new ArchAnimation(arcProgress, gauge.getVal());
        animation.setDuration(170);
        arcProgress.startAnimation(animation);
    }


    public void update_gauge_max(final int i, final float i1) {
        TableLayout tv = findViewById(R.id.tv_log);
        if (tv == null) {
            return;
        }
        ArcProgress arcProgress = tv.findViewWithTag("gauge_" + i);
        arcProgress.setMax(i1);
        float min = prefs.getMaxValueForGauge(i);
        float warn1 = prefs.getWarn1LevelForGauge(i);
        float warn2 = prefs.getWarn2LevelForGauge(i);

        arcProgress.setWarn1(Math.round(warn1 * (i1 - min) / 100));
        arcProgress.setWarn2(Math.round(warn2 * (i1 - min) / 100));
    }

    public void update_gauge_min(final int i, final Float i1) {
        TableLayout tv = findViewById(R.id.tv_log);
        if (tv == null)
            return;
        ArcProgress arcProgress = tv.findViewWithTag("gauge_" + i);
        arcProgress.setMin(i1);
        float max = prefs.getMaxValueForGauge(i);

        float warn1 = prefs.getWarn1LevelForGauge(i);
        float warn2 = prefs.getWarn2LevelForGauge(i);

        arcProgress.setWarn1(Math.round(warn1 * (max - i1) / 100));
        arcProgress.setWarn2(Math.round(warn2 * (max - i1) / 100));
    }
}