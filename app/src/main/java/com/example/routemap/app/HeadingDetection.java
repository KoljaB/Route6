package com.example.routemap.app;

import android.location.Location;
import android.text.format.Time;

import org.mapsforge.core.model.GeoPoint;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HeadingDetection
{
    final String RAW_HEADING_LOGFILE = "HeadingCalculation.log";
    final int MAX_LOCATIONS = 300;

    final double DIRECTION_LENGTH = 10f;

    // Walk:
    final int MAX_LOCATIONS_AGE = 120000;

    public final static double OPTIMAL_HISTORY_SPEED_TIME = 8000;
    public final static double MIN_HISTORY_SPEED_TIME = 3000;
    public final static double MAX_HISTORY_SPEED_TIME = 20000;

    final double MIN_HEADING_DISTANCE = 2f;
    public double MaxHistoryDistance = 0;

    public double Speed;

    private boolean startup = true;
    public boolean ValidHeading = false;

    public Location CurrentLocation = null;
    LocationFilter locationFilter;

    double directionLatitude = 0.001;
    double directionLongitude = 0;

    double bearing;

    List<Location> locations;
    LogFile logFile;

    public HeadingDetection() {

        if (!MainActivity.EXTERNRELEASE) {
            logFile = new LogFile(RAW_HEADING_LOGFILE);
        }

        SetToNorth();

        locations = new ArrayList<Location>();
        locationFilter = new LocationFilter(MIN_HEADING_DISTANCE);
    }

    public void AddLocation(Location location) {

        CurrentLocation = location;

        TrimHeading(); // kill old heading locations

        if (locationFilter.Feed(location)) {
            locations.add(location);
            while (locations.size() > MAX_LOCATIONS) {
                locations.remove(0);
            }
            CalculateSpeeding();
        }

        CalculateHeading();
    }


    public void SetToNorth() {
        bearing = 0;
    }


    void TrimHeading() {
        long currentMillis = Calendar.getInstance().getTimeInMillis();
        for (int i = locations.size() - 1; i >= 0; i--) {
            if ((currentMillis - locations.get(i).getTime()) > MAX_LOCATIONS_AGE ) {
                locations.remove(i);
            }
        }
    }

    public GeoPoint GetPredictedNextPoint() {

        GeoPoint currentPoint = Geo.LocationToGeopoint(CurrentLocation);

        return new GeoPoint(currentPoint.latitude + directionLatitude, currentPoint.longitude + directionLongitude);
    }


    void CalculateHeading() {

        if (CurrentLocation == null) {
            return;
        }

        GeoPoint currentPoint = Geo.LocationToGeopoint(CurrentLocation);

        ValidHeading = false;
        final float HEADING_DISTANCE = 5.0f;
        final long MAX_AGE = 40 * 1000;

        int i = locations.size() - 2;
        while (i >= 0) {
            double iDistance = locations.get(i).distanceTo(CurrentLocation);

            if (CurrentLocation.getTime() - locations.get(i).getTime() > MAX_AGE) {
                break;
            }

            if (iDistance >= HEADING_DISTANCE) {
                ValidHeading = true;

                directionLatitude = CurrentLocation.getLatitude() - locations.get(i).getLatitude();
                directionLongitude = CurrentLocation.getLongitude() - locations.get(i).getLongitude();

                bearing = Geo.Bearing(Geo.LocationToGeopoint(locations.get(i)), currentPoint);
                break;
            }

            i--;
        }

        Time curTime = new Time();
        curTime.setToNow();
        String curTimeString = curTime.format("%H:%M:%S");

        if (ValidHeading) {

            if (!MainActivity.EXTERNRELEASE) {
                logFile.Log(locations.size()
                + " " + curTimeString + " optimum found"
                + ", current location: " + LocationToString(CurrentLocation)
                + ", new bearing: " + String.format("%.2f", bearing)
                );
            }
        } else {
            SetToNorth();

            directionLatitude = 0.001;
            directionLongitude = 0;

            if (!MainActivity.EXTERNRELEASE) {
                logFile.Log(locations.size()
                + " " + curTimeString + " no optimum found"
                + ", current location: " + LocationToString(CurrentLocation)
                + ", current bearing: " + String.format("%.2f", bearing));
            }
        }

        if (CurrentLocation != null) {
            NormalizeDirection(currentPoint, 10.0);
        }
    }

    private void NormalizeDirection(GeoPoint reference, double meters) {
        double currentDistance = Geo.GetDistance(reference, new GeoPoint(reference.latitude + directionLatitude, reference.longitude + directionLongitude));
        double factor = meters / currentDistance;

        directionLatitude = directionLatitude * factor;
        directionLongitude = directionLongitude * factor;
    }

//    String GpsVectorToString(GpsVector vector) {
//        return "Lat: " + String.format("%.7f", vector.Latitude) + ", Lon: " + String.format("%.7f", vector.Longitude);
//    }
    String LocationToString(Location location) {
        return "Lat: " + String.format("%.7f", location.getLatitude()) + ", Lon: " + String.format("%.7f", location.getLongitude());
    }


    void CalculateSpeeding() {

        Time curTime = new Time();
        curTime.setToNow();

        // try to find a history position with optimal time distance
        int iOptimum = -1;
        long ageOfOptimum = Long.MAX_VALUE;
        double minDistanceToOptimum = Double.MAX_VALUE;

        MaxHistoryDistance = 0;
        int i = locations.size() - 2;
        while (i >= 0) {

            long age = CurrentLocation.getTime() - locations.get(i).getTime();
            double optimumDistance = Math.abs(OPTIMAL_HISTORY_SPEED_TIME  - age);

            if (age > MAX_HISTORY_SPEED_TIME) {
                break;
            }

            if (optimumDistance < minDistanceToOptimum && age > MIN_HISTORY_SPEED_TIME)
            {
                iOptimum = i;
                minDistanceToOptimum = optimumDistance;
                ageOfOptimum = age;
            }

            i--;
        }

        if (iOptimum >= 0 && ageOfOptimum != Long.MAX_VALUE) {
            double distance = CurrentLocation.distanceTo(locations.get(iOptimum));
            Speed = distance / (ageOfOptimum / 1000);
        }
        else
        {
            Speed = 0;
        }
    }
}
