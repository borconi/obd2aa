package uk.co.borconi.emil.obd2aa.helpers;


import android.location.Location;

import java.util.Calendar;

/**
 * Created by Emil on 27/09/2017.
 */

public class SunSet {

    private static double sunrise;
    private static double sunset;

    public static int Calculate_Sunset_Sunrise(Location loc) {
        Calendar rightNow = Calendar.getInstance();

        double latitude = loc.getLatitude();
        double longitude = loc.getLongitude();
        double zenith = 90.833333333;
        double D2R = Math.PI / 180;
        double R2D = 180 / Math.PI;
        int day;
        double t1;
        double t2;
        double M1;
        double M2;
        double L1;
        double L2;
        double UT1;
        double UT2;
        double RA1;
        double RA2;

        double cosH1;
        double cosH2;


        day = rightNow.get(Calendar.DAY_OF_YEAR);


        t1 = day + ((6 - (longitude / 15)) / 24);
        t2 = day + ((18 - (longitude / 15)) / 24);

        M1 = (0.9856 * t1) - 3.289;
        M2 = (0.9856 * t2) - 3.289;

        L1 = M1 + (1.916 * Math.sin(M1 * D2R)) + (0.020 * Math.sin(2 * M1 * D2R)) + 282.634;
        L2 = M2 + (1.916 * Math.sin(M2 * D2R)) + (0.020 * Math.sin(2 * M2 * D2R)) + 282.634;

        if (L1 > 360)
            L1 = L1 - 360;
        else if (L1 < 0)
            L1 = L1 + 360;

        if (L2 > 360)
            L2 = L2 - 360;
        else if (L2 < 0)
            L2 = L2 + 360;

        RA1 = R2D * Math.atan(0.91764 * Math.tan(L1 * D2R));
        RA2 = R2D * Math.atan(0.91764 * Math.tan(L2 * D2R));
        if (RA1 > 360)
            RA1 = RA1 - 360;
        else if (RA1 < 0)
            RA1 = RA1 + 360;

        if (RA2 > 360)
            RA2 = RA2 - 360;
        else if (RA2 < 0)
            RA2 = RA2 + 360;


        RA1 = (RA1 + ((Math.floor(L1 / (90))) * 90 - (Math.floor(RA1 / 90)) * 90)) / 15;
        RA2 = (RA2 + ((Math.floor(L2 / (90))) * 90 - (Math.floor(RA2 / 90)) * 90)) / 15;


        cosH1 = (Math.cos(zenith * D2R) - (0.39782 * Math.sin(L1 * D2R) * Math.sin(latitude * D2R))) / (Math.cos(Math.asin(0.39782 * Math.sin(L1 * D2R))) * Math.cos(latitude * D2R));
        cosH2 = (Math.cos(zenith * D2R) - (0.39782 * Math.sin(L2 * D2R) * Math.sin(latitude * D2R))) / (Math.cos(Math.asin(0.39782 * Math.sin(L2 * D2R))) * Math.cos(latitude * D2R));

        sunrise = (360 - R2D * Math.acos(cosH1)) / 15;
        sunset = R2D * Math.acos(cosH2) / 15;

        sunrise = sunrise + RA1 - (0.06571 * t1) - 6.622;
        sunset = sunset + RA2 - (0.06571 * t2) - 6.622;

        UT1 = sunrise - (longitude / 15);
        UT2 = sunset - (longitude / 15);
        if (UT1 > 24) {
            UT1 = UT1 - 24;
        } else if (UT1 < 0) {
            UT1 = UT1 + 24;
        }

        if (UT2 > 24) {
            UT2 = UT2 - 24;
        } else if (UT2 < 0) {
            UT2 = UT2 + 24;
        }


        int offsetFromUtc = rightNow.get(Calendar.ZONE_OFFSET) / 1000;
        int winterSummerOffset = rightNow.get(Calendar.DST_OFFSET) / 1000;

        sunrise = UT1 * 3600 + offsetFromUtc + winterSummerOffset;
        sunset = UT2 * 3600 + offsetFromUtc + winterSummerOffset;

        int now_sec = (rightNow.get(Calendar.HOUR_OF_DAY) * 3600 + rightNow.get(Calendar.MINUTE) * 60 + rightNow.get(Calendar.SECOND));
        //Log.d("HU-SERVICE","Night mode:"+nightmode+", isnightset="+isnightset+", now_sec:"+now_sec+",sunset: "+sunset+",sunrise: "+sunrise + "m_stopping: "+m_stopping);
        if (now_sec < sunrise || now_sec > sunset)
            return 2;
        else if (now_sec > sunrise && now_sec < sunset)
            return 1;

        return 2;
    }

}
