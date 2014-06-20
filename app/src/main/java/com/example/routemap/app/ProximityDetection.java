package com.example.routemap.app;

import android.location.Location;
import android.text.format.Time;

import org.mapsforge.core.model.GeoPoint;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class ProximityDetection {
    final String LOGFILE = "ProximityDetection.log";
    final int MIN_PAUSE_INTERVAL = 12000; // don't be a chatterbox
    final int FINISH_INTERVAL = 5 * 60 * 1000; // only tell we reached finish every 5 minutes

    double closestDistance;
    LogFile logFile;

    Map<GeoPoint,Location> nearestUserLocation;
    Map<Double,Long> distanceTimeouts;

    public ProximityDetection(TreeMap<Double, Long> distanceTimeouts) {

        if (!MainActivity.EXTERNRELEASE) {
            logFile = new LogFile(LOGFILE);
        }

        this.distanceTimeouts = distanceTimeouts;

        nearestUserLocation = new HashMap<GeoPoint, Location>();


        for (Map.Entry<Double, Long> entry : distanceTimeouts.entrySet()) {
            closestDistance =  entry.getKey();
            break;
        }
    }

    public double GetClosestDistance() { return closestDistance; }

    public void Clear() {
        nearestUserLocation.clear();
    }

    public boolean IsApproximate(Location currentLocation, GeoPoint approximationPoint, boolean isFinish) {

        synchronized (this) {

            Time curTime = new Time();
            curTime.setToNow();
            String curTimeString = curTime.format("%H:%M:%S");

            long lastNearestAge = 0;
            double existingNearestDistance = 999999999;
            GeoPoint currentPoint = Geo.LocationToGeopoint(currentLocation);

            if (nearestUserLocation.containsKey(approximationPoint)) {
                Location lastNearest = nearestUserLocation.get(approximationPoint);
                GeoPoint lastNearestPoint = Geo.LocationToGeopoint(lastNearest);
                existingNearestDistance = Geo.GetDistance(lastNearestPoint, approximationPoint);
                lastNearestAge = lastNearest.getTime();
            }

            double currentDistance = Geo.GetDistance(currentPoint, approximationPoint);
            long currentMillis = Calendar.getInstance().getTimeInMillis();

            // cycle through the proximity distances from near to far
            for (Map.Entry<Double, Long> entry : distanceTimeouts.entrySet()) {

                double proximityDistance = entry.getKey();
                long proximityMaxAge = entry.getValue();

                // are we in the proximity area of the approximation location?
                if (currentDistance < proximityDistance) {

                    // we don't want repeating appromity message, so skip when ...
                    if (existingNearestDistance < proximityDistance               //... we already spoke out to an location
                            && (currentMillis - lastNearestAge < proximityMaxAge)) {  //... and we spoke out within the timeout

                        if (!MainActivity.EXTERNRELEASE) {
                            logFile.Log(curTimeString + "   skip"
                            + ", CURD: " + String.format("%.2f", currentDistance)
                            + ", NEARD: " + String.format("%.2f", existingNearestDistance)
                            + ", PROXD: " + String.format("%.2f", proximityDistance)
                            + ", curMil: " + currentMillis
                            + ", lastAge: " + lastNearestAge
                            + ", maxAge: " + proximityMaxAge);
                        }

                        return false;
                    }
                    // skip, if the approxmationpoint is known and the last announcement has been made just moments ago
                    if (lastNearestAge != 0 && (currentMillis - lastNearestAge < MIN_PAUSE_INTERVAL)) {

                        if (!MainActivity.EXTERNRELEASE) {
                            logFile.Log(curTimeString + "   time"
                            + ", CURD: " + String.format("%.2f", currentDistance)
                            + ", NEARD: " + String.format("%.2f", existingNearestDistance)
                            + ", PROXD: " + String.format("%.2f", proximityDistance)
                            + ", curMil: " + currentMillis
                            + ", lastAge: " + lastNearestAge
                            + ", maxAge: " + proximityMaxAge);
                        }

                        return false;
                    }

                    // special treat finish, because we don't want so many repeats here
                    if (isFinish && lastNearestAge != 0 && (currentMillis - lastNearestAge < FINISH_INTERVAL)) {

                        if (!MainActivity.EXTERNRELEASE) {
                            logFile.Log(curTimeString + "   fins"
                            + ", CURD: " + String.format("%.2f", currentDistance)
                            + ", NEARD: " + String.format("%.2f", existingNearestDistance)
                            + ", PROXD: " + String.format("%.2f", proximityDistance)
                            + ", curMil: " + currentMillis
                            + ", lastAge: " + lastNearestAge
                            + ", maxAge: " + proximityMaxAge);
                        }

                        return false;
                    }

                    if (!MainActivity.EXTERNRELEASE) {
                        logFile.Log(curTimeString + "# MATCH"
                        + ", CURD: " + String.format("%.2f", currentDistance)
                        + ", NEARD: " + String.format("%.2f", existingNearestDistance)
                        + ", PROXD: " + String.format("%.2f", proximityDistance)
                        + ", curMil: " + currentMillis
                        + ", lastAge: " + lastNearestAge
                        + ", maxAge: " + proximityMaxAge
                        + ", nearestUserLocationSize: " + nearestUserLocation.size());
                    }

                    nearestUserLocation.put(approximationPoint, currentLocation);
                    return true;
                }
            }

            if (!MainActivity.EXTERNRELEASE) {
                logFile.Log(curTimeString + "   fail"
                + ", NEARD: " + String.format("%.2f", existingNearestDistance)
                + ", curMil: " + currentMillis
                + ", lastAge: " + lastNearestAge);
            }

            return false;
        }
    }
}
