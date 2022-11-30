package uk.co.borconi.emil.obd2aa.androidauto;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.car.app.AppManager;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.SurfaceCallback;
import androidx.car.app.SurfaceContainer;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.NavigationTemplate;

import uk.co.borconi.emil.obd2aa.ArcProgress;
import uk.co.borconi.emil.obd2aa.helpers.PreferencesHelper;
import uk.co.borconi.emil.obd2aa.services.OBD2Background;


public class OBD2AA extends Screen implements SurfaceCallback {

    private final CarContext carContext;
    private final Context context;
    private ArcProgress arcProgress;
    private final PreferencesHelper preferences;

    private final int gauge_number;
    private OBD2Background mOBD2Service;
    private final String[] pids;
    private final long[] lastpiddraw;
    private boolean isshowing;
    private final boolean useDigital;
    private final String[] units;
    private final Boolean[] convertunits;
    private final Boolean isdebugging;
    private MediaPlayer mediaPlayer;
    private boolean prepared;

    public OBD2AA(CarContext carContext, Context context) {

        super(carContext);
        this.carContext = carContext;
        this.context = context;

        preferences = PreferencesHelper.getPreferences(carContext);
        gauge_number = preferences.getNumberOfGauges();
        useDigital = preferences.shouldUseDigital();
        pids = new String[gauge_number];
        lastpiddraw = new long[gauge_number];
        units = new String[gauge_number];
        convertunits = new Boolean[gauge_number];
        isdebugging = preferences.isDebugging();
        Log.d("OBD2AA", "OBD2AA APP STARTED, BEFORE BIND.");

        carContext.getCarService(AppManager.class).setSurfaceCallback(this);
    }


    @NonNull
    @Override
    public Template onGetTemplate() {
        NavigationTemplate.Builder builder = new NavigationTemplate.Builder();
        builder.setActionStrip(new ActionStrip.Builder().addAction(new Action.Builder().setTitle("OBD2AA").build()).build());
        return builder.build();
    }

    @Override
    public void onSurfaceAvailable(@NonNull SurfaceContainer surfaceContainer) {
        Surface surface = surfaceContainer.getSurface();
        Log.d("OBD", "Surface available: " + surfaceContainer);
        int dpi = 160;
        if (surfaceContainer.getDpi() != 0) {
            dpi = surfaceContainer.getDpi();
        }
        DisplayManager displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        VirtualDisplay display = displayManager.createVirtualDisplay("OBD2", surfaceContainer.getWidth(), surfaceContainer.getHeight(), dpi, surface /* flags */, 0, null /* callback */, null /* handler */);
        Log.d("OBD", "Got a surface & display: " + display.getDisplay().toString());
        Projection mPresentation = new Projection(context, display.getDisplay());
        mPresentation.show();
    }

    @Override
    public void onVisibleAreaChanged(@NonNull Rect visibleArea) {
        Log.d("OBD2", "visible area changed");
    }

    @Override
    public void onStableAreaChanged(@NonNull Rect stableArea) {

    }

    @Override
    public void onSurfaceDestroyed(@NonNull SurfaceContainer surfaceContainer) {
        Log.d("OBD2", "Surface destroyed");
    }

    @Override
    public void onScroll(float distanceX, float distanceY) {

    }

    @Override
    public void onFling(float velocityX, float velocityY) {

    }

    @Override
    public void onScale(float focusX, float focusY, float scaleFactor) {

    }
}