package uk.co.boconi.emil.obd2aa;

import android.util.Log;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarActivityService;


public class MyOdbService  extends CarActivityService {

    @Override
    public Class<? extends CarActivity> getCarActivity() {

        return OBD2AA.class;
    }

}

