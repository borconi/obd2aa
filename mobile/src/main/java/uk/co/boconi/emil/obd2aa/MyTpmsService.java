package uk.co.boconi.emil.obd2aa;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarActivityService;

public class MyTpmsService extends CarActivityService {
    @Override
    public Class<? extends CarActivity> getCarActivity() {return TPMS.class; }
}
