package org.openstreetmap.osm.data.coordinates;

import static org.junit.Assert.*;

import org.junit.Test;

public class LatLonTest {

    @Test
    public void testDistanceInMeters() {
        LatLon point1 = new LatLon(0,0);
        LatLon point2 = new LatLon(1,1);
        double d = LatLon.distanceInMeters(point1, point2);
        assertTrue((d > 111000) && (d < 1113000));
    }
}
