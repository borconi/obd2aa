package uk.co.borconi.emil.obd2aa.helpers;

import static java.lang.Integer.parseInt;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import java.math.BigDecimal;

import uk.co.borconi.emil.obd2aa.R;

/**
 * Created by Emil on 20/10/2017.
 */

public class NearbyCameras {

    private final double lat;
    private final int id;
    private final double lng;
    private final int bearing;
    private final String speed;
    private final String type;
    private final String street;
    private final int dbtype;
    private final boolean[] show_warrning = {false, false, false};
    private int DistanceToCam;
    private int cameratext = R.string.danger;
    private boolean shownot = true;

    public NearbyCameras(double lat, double lng, int id, Integer bearing, String speed, String type, String street, int dbtype, double latitude, double longitude) {
        this.DistanceToCam = (int) SphericalUtil.computeDistanceBetween(new LatLng(lat, lng), new LatLng(latitude, longitude));
        this.lat = lat;
        this.lng = lng;
        this.id = id;
        if (id < 0) {
            if (bearing == null) {
                this.bearing = -1;
            } else {
                this.bearing = bearing.intValue();
            }
        } else if (bearing == null) {
            this.bearing = -1;
        } else {
            this.bearing = decodeHeading(lat, lng, bearing.intValue());
        }
        //  Log.d("OBD2AA","Decoded heading is: " + this.bearing);
        this.speed = speed;
        this.type = type;
        this.street = street;
        this.dbtype = dbtype;
    }

    public double getlat() {
        return lat;
    }

    public double getlng() {
        return lng;
    }

    public int getbearing() {
        return bearing;
    }

    public int getDbtype() {
        return dbtype;
    }

    public int getType() {
        return parseInt(type);
    }

    public String getspeed() {
        if (speed != null && !speed.isEmpty())
            return speed;
        else
            return "?";
    }

    private int decodeHeading(double d, double d2, int i) {
        String substring;
        String toPlainString = BigDecimal.valueOf(d).toPlainString();
        String toPlainString2 = BigDecimal.valueOf(d2).toPlainString();
        int indexOf = toPlainString.indexOf(".");
        int indexOf2 = toPlainString2.indexOf(".");
        if (indexOf > 0) {
            toPlainString = toPlainString.substring(indexOf + 1);
            substring = toPlainString.length() >= 3 ? toPlainString.substring(0, 3) : toPlainString.length() >= 2 ? toPlainString.substring(0, 2) + "0" : toPlainString.length() >= 1 ? toPlainString.charAt(0) + "00" : "000";
        } else {
            substring = "0";
        }
        if (indexOf2 > 0) {
            toPlainString = toPlainString2.substring(indexOf2 + 1);
            toPlainString = toPlainString.length() >= 3 ? toPlainString.substring(0, 3) : toPlainString.length() >= 2 ? toPlainString.substring(0, 2) + "0" : toPlainString.length() >= 1 ? toPlainString.charAt(0) + "00" : "000";
        } else {
            toPlainString = "0";
        }
        return Integer.valueOf(toPlainString).intValue() + (i - Integer.valueOf(substring).intValue());
    }

    public String getstreet() {
        return street;
    }

    public void setDistanceToCam(int distance) {
        DistanceToCam = distance;
    }

    public int getDistaceToCam() {
        return DistanceToCam;
    }

    public void setCameratext(int textid) {
        cameratext = textid;
    }

    public int[] geticon() {
        Log.d("OBD2AA", "Db type = " + dbtype + " type = " + type);
        if ((dbtype == 1 && type.equalsIgnoreCase("1")) || (dbtype == 2 && type.equalsIgnoreCase("7"))) {
            Log.d("OBD2AA", "This should be a speed camera");
            setCameratext(R.string.speedcam);
            return new int[]{R.drawable.speed_camera_sam_b, R.string.speedcam, 0};
        } else if ((dbtype == 1 && type.equalsIgnoreCase("2")) || (dbtype == 2 && type.equalsIgnoreCase("11"))) {

            return new int[]{R.drawable.speed_camera_sam_b, R.string.traffic_cam, 1};
        } else if (dbtype == 2 && type.equalsIgnoreCase("12")) {

            return new int[]{R.drawable.speed_camera_sam_b, R.string.section_cam_beg, 0};
        } else if (dbtype == 2 && type.equalsIgnoreCase("13")) {

            return new int[]{R.drawable.speed_camera_sam_b, R.string.section_cam_over, 0};
        } else if (dbtype == 2 && type.equalsIgnoreCase("14")) {

            return new int[]{R.drawable.speed_camera_sam_b, R.string.tunnel, 0};
        } else {
            return new int[]{R.drawable.ic_danger_r, R.string.danger, 0};
        }
    }

    public int getstring() {
        return cameratext;
    }

    public int getid() {
        return id;
    }

    public void setShow_warrning(int i, boolean b) {
        show_warrning[i] = b;
    }

    public boolean getShow_warrning(int i) {
        return show_warrning[i];
    }

    public boolean getShownot() {
        return shownot;
    }

    public void setShownot(boolean shownot) {
        this.shownot = shownot;
    }

    @Override
    public String toString() {
        return "Camera pos: " + this.lat + "," + this.lng + ", Bearing: " + this.bearing + ", distance: " + this.DistanceToCam;
    }
}
