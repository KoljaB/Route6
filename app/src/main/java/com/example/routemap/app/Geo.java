package com.example.routemap.app;

import android.graphics.Point;
import android.location.Location;

import org.mapsforge.core.model.GeoPoint;

public class Geo
{
    public static double EARTH_RADIUS = 6371000.785;
    static double metrePerLatitude = 0;

    static {
        GeoPoint point1 = new GeoPoint(51.668,7.53);
        GeoPoint point2 = new GeoPoint(52.668,7.53);
        metrePerLatitude = GetDistance(point1, point2);
    }

//    public static double MetreToGeoDistance(double metreDistance)
//    {
//        return metreDistance / metrePerLatitude;
//    }

    // great: http://stackoverflow.com/questions/20231258/minimum-distance-between-a-point-and-a-line-in-latitude-longitude
    // great: http://stackoverflow.com/questions/7803004/distance-from-point-to-line-on-earth (own answer)
    // http://stackoverflow.com/questions/1299567/how-to-calculate-distance-from-a-point-to-a-line-segment-on-a-sphere


//    // ### TBD: very bad: this is shit
//    public static double GeoDistanceToMetre(double geoDistance)
//    {
//        return geoDistance * metrePerLatitude / 1.56f; // corrects
//    }

//    public static GeoPoint AddGeopoint(GeoPoint geo1, GeoPoint geo2) {
//        return new GeoPoint(geo1.latitude + geo2.latitude, geo1.longitude + geo2.longitude);
//    }


    public static double ToNormalizedDegrees(double rad) {
        return (Math.toDegrees(rad) + 360) % 360;

    }

    public static double NormalizedBearingDegrees(GeoPoint point1, GeoPoint point2) {
        double degrees = Math.toDegrees(Bearing(point1, point2));
        return (degrees + 360) % 360;
    }

    public static double Bearing(GeoPoint point1, GeoPoint point2) {
        double lat1 = Math.toRadians(point1.latitude);
        double lat2 = Math.toRadians(point2.latitude);
        double lon1 = Math.toRadians(point1.longitude);
        double lon2 = Math.toRadians(point2.longitude);

        double deltaLong = lon2 - lon1;
        double y = Math.sin(deltaLong) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLong);

        double bearing = Math.atan2(y, x);
        double normalizedBearing = (bearing + 2 * Math.PI) % (2 * Math.PI);

        return normalizedBearing;
        //return Math.atan2(y, x);
    }

    public static GeoPoint GetPreciseProjectedTrackpoint(GeoPoint point, GeoPoint segStart, GeoPoint segFinish) {

        GeoPoint mid = MidPoint(segStart, segFinish);

        double distPointToStart = GetDistance(point, segStart);
        double distPointToMid = GetDistance(point, mid);
        double distPointToFinish = GetDistance(point, segFinish);

        if (Math.abs(distPointToStart - distPointToFinish) < 0.01 && Math.abs(distPointToStart - distPointToMid) < 0.01) {
            return mid;
        }

        if (distPointToStart < distPointToFinish) {
            return GetPreciseProjectedTrackpoint(point, segStart, mid);
        } else {
            return GetPreciseProjectedTrackpoint(point, mid, segFinish);
        }
    }

    public static double GetPointDistance(Point first, Point second) {
        return Math.sqrt(Math.pow(second.x - first.x, 2) + Math.pow(second.y - first.y, 2));

    }

//    public static double GetPreciseDistanceToSegment(GeoPoint point, GeoPoint segStart, GeoPoint segFinish) {
//
//        GeoPoint mid = MidPoint(segStart, segFinish);
//
//        double distPointToStart = GetDistance(point, segStart);
//        double distPointToMid = GetDistance(point, mid);
//        double distPointToFinish = GetDistance(point, segFinish);
//
//        if (Math.abs(distPointToStart - distPointToFinish) < 0.01 && Math.abs(distPointToStart - distPointToMid) < 0.01) {
//            return distPointToMid;
//        }
//
//        if (distPointToStart < distPointToFinish) {
//            return GetPreciseDistanceToSegment(point, segStart, mid);
//        } else {
//            return GetPreciseDistanceToSegment(point, mid, segFinish);
//        }
//    }


//    public static GeoPoint MidPointEx(GeoPoint point1, GeoPoint point2){
//
//        double Totweight = 0;
//        double xt = 0;
//        double yt = 0;
//        double zt = 0;
//
//        List<GeoPoint> points = new ArrayList<GeoPoint>();
//        points.add(point1);
//        points.add(point2);
//
//        for (GeoPoint point : points) {
//            Double latitude = point.latitude;
//            Double longitude = point.longitude;
//
//            /**
//             * Convert Lat and Lon from degrees to radians.
//             */
//            double latn = latitude * Math.PI / 180;
//            double lonn = longitude * Math.PI / 180;
//
//            /**
//             * Convert lat/lon to Cartesian coordinates
//             */
//            double xn = Math.cos(latn) * Math.cos(lonn);
//            double yn = Math.cos(latn) * Math.sin(lonn);
//            double zn = Math.sin(latn);
//
//            /**
//             * Compute weight (by time) If locations are to be weighted equally,
//             * set wn to 1
//             */
//            double years = 0;
//            double months = 0;
//            double days = 0;
//            double wn = true ? 1 : (years * 365.25) + (months * 30.4375) + days;
//
//            /**
//             * Compute combined total weight for all locations.
//             */
//            Totweight = Totweight + wn;
//            xt += xn * wn;
//            yt += yn * wn;
//            zt += zn * wn;
//        }
//
//        /**
//         * Compute weighted average x, y and z coordinates.
//         */
//        double x = xt / Totweight;
//        double y = yt / Totweight;
//        double z = zt / Totweight;
//
//        /**
//         * If abs(x) < 10-9 and abs(y) < 10-9 and abs(z) < 10-9 then the
//         * geographic midpoint is the center of the earth.
//         */
//        double lat = -0.001944;
//        double lon = -78.455833;
//        if (Math.abs(x) < Math.pow(10, -9) && Math.abs(y) < Math.pow(10, -9) && Math.abs(z) < Math.pow(10, -9)) {
//        } else {
//
//            /**
//             * Convert average x, y, z coordinate to latitude and longitude.
//             * Note that in Excel and possibly some other applications, the
//             * parameters need to be reversed in the atan2 function, for
//             * example, use atan2(X,Y) instead of atan2(Y,X).
//             */
//            lon = Math.atan2(y, x);
//            double hyp = Math.sqrt(x * x + y * y);
//            lat = Math.atan2(z, hyp);
//
//            /**
//             * Convert lat and lon to degrees.
//             */
//            lat = lat * 180 / Math.PI;
//            lon = lon * 180 / Math.PI;
//        }
//
//        //LOG.log(Level.INFO, "MidPoint: {0}, {1}", new Object[]{lat, lon});
//        return new GeoPoint(lat, lon);
//    }

    public static GeoPoint CalculateDestination(GeoPoint start, double bearing, double distance) {

        double latRad = Math.toRadians(start.latitude);
        double lonRad = Math.toRadians(start.longitude);

        double endLatRad = Math.asin((Math.sin(latRad) * Math.cos(distance / EARTH_RADIUS))
                + (Math.cos(latRad) * Math.sin(distance / EARTH_RADIUS) * Math.cos(bearing)));

        double endLonRad = lonRad + Math.atan2(Math.sin(bearing) * Math.sin(distance / EARTH_RADIUS) * Math.cos(latRad),
                Math.cos(distance - EARTH_RADIUS) - (Math.sin(latRad) * Math.sin(endLatRad)));


        endLonRad = (endLonRad + 3 * Math.PI) % (2 * Math.PI) - Math.PI;

        return new GeoPoint(Math.toDegrees(endLatRad), Math.toDegrees(endLonRad));
    }

    public static GeoPoint MidPoint(GeoPoint point1, GeoPoint point2){

        double dLon = Math.toRadians(point2.longitude - point1.longitude);

        //convert to radians
        double lat1 = Math.toRadians(point1.latitude);
        double lat2 = Math.toRadians(point2.latitude);
        double lon1 = Math.toRadians(point1.longitude);

        double Bx = Math.cos(lat2) * Math.cos(dLon);
        double By = Math.cos(lat2) * Math.sin(dLon);
        double lat3 = Math.atan2(Math.sin(lat1) + Math.sin(lat2), Math.sqrt((Math.cos(lat1) + Bx) * (Math.cos(lat1) + Bx) + By * By));
        double lon3 = lon1 + Math.atan2(By, Math.cos(lat1) + Bx);

        return new GeoPoint(Math.toDegrees(lat3), Math.toDegrees(lon3));
    }

    public static double GetSegmentDistance(GeoPoint point, GeoPoint segStart, GeoPoint segFinish) {

        double atd = AlongTrackDistance(point, segStart, segFinish);

        double d12 = GetDistance(point, segStart);
        double d13 = GetDistance(point, segFinish);
        double d23 = GetDistance(segStart, segFinish);

        double retVal = 0;

        if (atd < 0 || atd > d23)
        {
            retVal = Math.min(d12, d13);
        } else {
            retVal = CrossTrackDistance(point, segStart, segFinish);
        }

        return retVal;

//        if (atd > d23)
//            return d12;
//
//        if (atd < 0)
//            return d13;
//
//        return CrossTrackDistance(point, segStart, segFinish);
    }

    public static double CrossTrackDistance(GeoPoint point, GeoPoint segStart, GeoPoint segFinish) {

        double d21 = GetDistance(segStart, point);
        double θ21 = Bearing(segStart, point);
        double θ23 = Bearing(segStart, segFinish);

        return Math.asin(Math.sin(d21 / EARTH_RADIUS) * Math.sin(θ21 - θ23)) * EARTH_RADIUS;
    }

    public static double AlongTrackDistance(GeoPoint point, GeoPoint segStart, GeoPoint segFinish) {

        double d21 = GetDistance(segStart, point);
        double dxt = CrossTrackDistance(point, segStart, segFinish);

        return Math.acos(Math.cos(d21 / EARTH_RADIUS) / Math.cos(dxt / EARTH_RADIUS)) * EARTH_RADIUS;
    }

    public static float GetDistance(GeoPoint p1, GeoPoint p2)
    {
        float [] dist = new float[1];
        Location.distanceBetween(p1.latitude, p1.longitude, p2.latitude, p2.longitude, dist);
        return dist[0];
    }

//    public static GeoPoint Normalize(GeoPoint v)
//    {
//        double length = Math.sqrt(Sqr(v.longitude) + Sqr(v.latitude));
//        if (length == 0) return v;
//        return new GeoPoint(v.longitude / length, v.latitude / length);
//    }

    public static double Sqr(double x) { return x * x; }

    // ### TBD: very bad: this is plane x/y translation from lat to lon
    public static double GetAngleBetween(GeoPoint x, GeoPoint y)
    {
        double north_south = x.latitude - y.latitude;
        double east_west = x.longitude - y.longitude;
        return Math.atan2(east_west, north_south);
    }

    public static double GetAngleBetween(GeoPoint reference, GeoPoint x, GeoPoint y)
    {
        double bearing1 = Bearing(reference, x);
        double bearing2 = Bearing(reference, y);

        double angle1 = GetAngleBetween(reference, x);
        double angle2 = GetAngleBetween(reference, y);


        double bearDiff = bearing2 - bearing1;
        double angleDiff = angle2 - angle1;

        return bearDiff;


//        return angle2 - angle1;

//        GeoPoint a = new GeoPoint(x.latitude - reference.latitude, x.longitude - reference.longitude);
//        GeoPoint b = new GeoPoint(y.latitude - reference.latitude, y.longitude - reference.longitude);
//
//        if (a.latitude == 0 && a.longitude == 0) return 0;
//        if (b.latitude == 0 && b.longitude == 0) return 0;
//
//        a = Normalize(a);
//        b = Normalize(b);
//
//        return Math.atan2(a.longitude, a.latitude) - Math.atan2(b.longitude, b.latitude);
    }

    public static int RoundClockTime(double clockTime)
    {
        int roundedClockTime = (int) Math.floor(clockTime + 0.5);
        while (roundedClockTime > 12)
            roundedClockTime -= 12;
        while (roundedClockTime <= 0)
            roundedClockTime += 12;
        return roundedClockTime;
    }

    public static String ConvertToGoogleString(GeoPoint point) {
        return
            String.format("%.9f", point.latitude).replace(",", ".")
            + ","
            + String.format("%.9f", point.longitude).replace(",", ".");
    }

    public static double ConvertAngleToClock(double angle)
    {
        double deg = Math.toDegrees(angle);

        while (deg < 0)
            deg += 360;
        while (deg >= 360)
            deg -= 360;

        return deg / 30;
    }

    public static GeoPoint LocationToGeopoint(Location location)
    {
        return new GeoPoint(location.getLatitude(), location.getLongitude());
    }
}

