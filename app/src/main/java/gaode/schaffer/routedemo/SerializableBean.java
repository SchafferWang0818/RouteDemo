package gaode.schaffer.routedemo;

import java.io.Serializable;

/**
 * Created by SchafferW on 2016/11/20.
 */

public class SerializableBean implements Serializable {
    //    LatLng latLng;
    String positionName;
    public double latitude;
    public double longitude;

//    public SerializableBean(LatLng latLng, String positionName) {
//        this.latLng = latLng;
//        this.positionName = positionName;
//    }

    public SerializableBean(String positionName, double latitude, double longitude) {
        this.positionName = positionName;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
