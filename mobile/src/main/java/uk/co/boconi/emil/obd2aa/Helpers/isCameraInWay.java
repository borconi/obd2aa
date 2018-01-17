package uk.co.boconi.emil.obd2aa.Helpers;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import uk.co.boconi.emil.obd2aa.OBD2_Background;

/**
 * Created by Emil on 20/10/2017.
 */

public class isCameraInWay {
    NearbyCameras camera=null;

    public isCameraInWay(Location location,CameraDataBaseHelper mobileDB, CameraDataBaseHelper staticDB,int seacrhdistance,String mobilefilter, String staticfilter ) {
       // Log.d("OBD2AA","New location received, accuracry: "+location.getAccuracy() + " Bearing: " + location.getBearing() + "Lat: " + location.getLatitude() + " , Long: "+location.getLongitude());
        double longdiff = (1500*0.0000089) / Math.cos(location.getLatitude() * 0.018);
        List<NearbyCameras> nearbycamera = new ArrayList<>();
        nearbycamera.addAll(mobileDB.getNearbyCameras(location.getLatitude()-0.014,location.getLatitude()+0.014,location.getLongitude()-longdiff,location.getLongitude()+longdiff,location.getLatitude(),location.getLongitude(),mobilefilter));
        nearbycamera.addAll(staticDB.getNearbyCameras(location.getLatitude()-0.014,location.getLatitude()+0.014,location.getLongitude()-longdiff,location.getLongitude()+longdiff,location.getLatitude(),location.getLongitude(),staticfilter));

       // Log.d("OBD2AA","Camere list" + nearbycamera.toString());
        Collections.sort(nearbycamera, new Comparator<NearbyCameras>(){
            @Override
            public int compare(NearbyCameras o1, NearbyCameras o2) {
                return Integer.valueOf(o1.getDistaceToCam()).compareTo(o2.getDistaceToCam());
            }
        });
       // Log.d("OBD2AA","Sorted Camere list" + nearbycamera.toString());

        LatLng car_possiotion = new LatLng(location.getLatitude(), location.getLongitude());
        List<LatLng> latLngs = new ArrayList<>(); // Construct a tirangle for camera view/range
        latLngs.add(car_possiotion);
        if (OBD2_Background.isdebugging) {
            Log.d("OBD2AA", "Car possition: " + car_possiotion);
            Log.d("OBD2AA", "Tirangle 1:" + SphericalUtil.computeOffset(car_possiotion, seacrhdistance, (double) (location.getBearing() - 15)));
            Log.d("OBD2AA", "Tirangle 2:" + SphericalUtil.computeOffset(car_possiotion, seacrhdistance, (double) (location.getBearing() + 15)));
        }
        latLngs.add(SphericalUtil.computeOffset(car_possiotion, seacrhdistance, (double) (location.getBearing() - 15)));
        latLngs.add(SphericalUtil.computeOffset(car_possiotion, seacrhdistance, (double) (location.getBearing() + 15)));

        for (NearbyCameras currentcamera : nearbycamera) {
            if (PolyUtil.containsLocation(currentcamera.getlat(),currentcamera.getlng(),latLngs,false) || PolyUtil.isLocationOnEdge(new LatLng(currentcamera.getlat(),currentcamera.getlng()),latLngs,false,10))
            {
                if (OBD2_Background.isdebugging)
                Log.d("OBD2AA","Found camera in way.... Checking bearing! Camera bearing: " + currentcamera.getbearing() + " Car bearing is: " + location.getBearing());

                if (location.getBearing()>=currentcamera.getbearing()-45 && location.getBearing()<=currentcamera.getbearing()+45)
                {
                    if (OBD2_Background.isdebugging)
                    Log.d("OBD2AA","Found camera in way.... Camera bearing: " + currentcamera.getbearing() + " Car bearing is: " + location.getBearing());
                    camera=currentcamera;
                    return;
                }
            }
        }



    }
    public NearbyCameras getCamera(){return camera;}
    public static boolean stillinRange(Location location, NearbyCameras camera, int seacrhdistance)
    {

        LatLng camera_coordinates = new LatLng(camera.getlat(), camera.getlng());  // get camera position
        LatLng current_possition=new LatLng(location.getLatitude(), location.getLongitude());
        double distance_to_camera = SphericalUtil.computeDistanceBetween(camera_coordinates, current_possition);

        List<LatLng> latLngs = new ArrayList<>(); // Construct a tirangle for camera view/range
        latLngs.add(current_possition);
        latLngs.add(SphericalUtil.computeOffset(current_possition, seacrhdistance, (double) (location.getBearing() - 15)));
        latLngs.add(SphericalUtil.computeOffset(current_possition, seacrhdistance, (double) (location.getBearing() + 15)));

        boolean camera_in_way=false;
          if (PolyUtil.containsLocation(camera.getlat(),camera.getlng(),latLngs,false) || PolyUtil.isLocationOnEdge(new LatLng(camera.getlat(),camera.getlng()),latLngs,false,10))
            {
                if (location.getBearing()>=camera.getbearing()-45 && location.getBearing()<=camera.getbearing()+45) {
                    camera_in_way = true;
                    if (OBD2_Background.isdebugging)
                    Log.d("OBD2AA", "Found still in way....");
                }
            }


        if (camera_in_way)
            camera.setDistanceToCam((int) distance_to_camera);

      return camera_in_way;



    }
}
