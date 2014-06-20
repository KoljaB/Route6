package com.example.routemap.app;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.text.format.Time;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Created by Lonli on 29.04.2014.
 */
public class LocationFeeder {

    public static boolean ONLY_USE_GPS = false;

    private static LocationFeeder instance = null;

    /**
     * Statische Methode, liefert die einzige Instanz dieser
     * Klasse zurück
     */
    public static LocationFeeder getInstance() {
        if (instance == null) {
            instance = new LocationFeeder();
        }
        return instance;
    }

    public static boolean Started = false;
    boolean Running = false;

    private static final int TWO_MINUTES = 1000 * 60 * 2; // two minutes

    final String RAW_LOCATION_CACHEFILE = "RawLocations.txt";
    final String BETTER_LOCATION_CACHEFILE = "BetterLocations.txt";

    protected Vector listeners;

    private Context context;
    private LocationManager locationManager;

    private ServiceLocationListener gpsLocationListener;
    private ServiceLocationListener networkLocationListener;
    private ServiceLocationListener passiveLocationListener;

    private LocationProvider gpsProvider;
    private LocationProvider networkProvider;
    private LocationProvider passiveProvider;

    private Location currentBestLocation = null;
    private int locationFeedCount = 0;

    LogFile rawDataLogFile;
    LogFile betterDataLogFile;

    private LocationFeeder()
    {
        if (!MainActivity.EXTERNRELEASE) {
            rawDataLogFile = new LogFile(RAW_LOCATION_CACHEFILE);
            betterDataLogFile = new LogFile(BETTER_LOCATION_CACHEFILE);
        }
    }

    public void SetContext(Context context) {
        this.context = context;
    }

    public void AddOnLocationProvidedListener(LocationEventListener listener)
    {
        if (listeners == null)
            listeners = new Vector();

        if (!listeners.contains(listener)) {
            listeners.addElement(listener);
        }
    }

    public void FireOnLocationProvidedEvent(Location location)
    {
        if (listeners != null && !listeners.isEmpty())
        {
            Enumeration e = listeners.elements();
            while (e.hasMoreElements())
            {
                LocationEventListener le = (LocationEventListener)e.nextElement();
                le.OnLocationProvided(location);
            }
        }
    }

    public void Start() {

        if (Started) {
            return;
        }

        Started = true;
        Running = true;

        synchronized (this) {

            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

            try {
                gpsProvider = locationManager.getProvider(LocationManager.GPS_PROVIDER);
                if (gpsProvider != null) {
                    Location lastKnownGPSLocation = locationManager.getLastKnownLocation(gpsProvider.getName());
                    if (isBetterLocation(lastKnownGPSLocation, currentBestLocation))
                        currentBestLocation = lastKnownGPSLocation;
                }
                gpsLocationListener = new ServiceLocationListener();

                if (gpsProvider != null) {
                    locationManager.requestLocationUpdates(gpsProvider.getName(), 0l, 0.0f, gpsLocationListener);
                }

                if (!ONLY_USE_GPS) {
                    networkProvider = locationManager.getProvider(LocationManager.NETWORK_PROVIDER);
                    if (networkProvider != null) {
                        Location lastKnownNetworkLocation = locationManager.getLastKnownLocation(networkProvider.getName());
                        if (isBetterLocation(lastKnownNetworkLocation, currentBestLocation))
                            currentBestLocation = lastKnownNetworkLocation;
                    }
                    networkLocationListener = new ServiceLocationListener();
                    if (networkProvider != null) {
                        locationManager.requestLocationUpdates(networkProvider.getName(), 0l, 0.0f, networkLocationListener);
                    }


                    passiveProvider = locationManager.getProvider(LocationManager.PASSIVE_PROVIDER);
                    if (passiveProvider != null) {
                        Location lastKnownPassiveLocation = locationManager.getLastKnownLocation(passiveProvider.getName());
                        if (isBetterLocation(lastKnownPassiveLocation, currentBestLocation)) {
                            currentBestLocation = lastKnownPassiveLocation;
                        }
                    }
                    passiveLocationListener = new ServiceLocationListener();
                    if (passiveProvider != null) {
                        locationManager.requestLocationUpdates(passiveProvider.getName(), 0l, 0.0f, passiveLocationListener);
                    }
                }

            } catch (SecurityException se) {
                se.printStackTrace();
            }
        }
    }

    public void Pause() {

        synchronized (this) {
            if (!Running) {
                Log.d("RETURN", "Can't stop location feeder, already paused/stopped.");
                return;
            }

            Running = false;

            if (locationManager != null) {
                locationManager.removeUpdates(gpsLocationListener);

                if (!ONLY_USE_GPS) {
                    locationManager.removeUpdates(networkLocationListener);
                    locationManager.removeUpdates(passiveLocationListener);
                }
            }
        }
    }

    public void Resume() {

        synchronized (this) {

            if (!Started) {
                Start();
            }

            if (Running) {
                Log.d("RETURN", "Can't resume location feeder, already running.");
                return;
            }

            if (locationManager != null) {
                if (gpsProvider != null) {
                    locationManager.requestLocationUpdates(gpsProvider.getName(), 0l, 0.0f, gpsLocationListener);
                }

                if (!ONLY_USE_GPS) {
                    if (networkProvider != null) {
                        locationManager.requestLocationUpdates(networkProvider.getName(), 0l, 0.0f, networkLocationListener);
                    }

                    if (passiveProvider != null) {
                        locationManager.requestLocationUpdates(passiveProvider.getName(), 0l, 0.0f, passiveLocationListener);
                    }
                }
            }
        }
    }

    public int GetLocationFeedCount() {
        return locationFeedCount;
    }


    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        float distance = location.distanceTo(currentBestLocation);

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) { //  && distance > 10) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    String DumpLocation(Location location, boolean isBetter)
    {
        Time locationTime = new Time();
        locationTime.set(location.getTime());

        String dumpLocation = "";

        dumpLocation += locationTime.format("%H:%M:%S");
        dumpLocation += String.format(" lat: %.7f", location.getLatitude());
        dumpLocation += String.format(" lon: %.7f", location.getLongitude());
        dumpLocation += String.format(" acc: %.3f", location.getAccuracy());
        dumpLocation += (isBetter ? " BEST" : "     ");
        dumpLocation += " ms: " + locationTime.toMillis(false);
        dumpLocation += " by: " + location.getProvider();

        return dumpLocation;
    }

    private class ServiceLocationListener implements android.location.LocationListener {

        @Override
        public void onLocationChanged(Location newLocation) {
            synchronized (this) {

                boolean isBetter = isBetterLocation(newLocation, currentBestLocation);

                if (!MainActivity.EXTERNRELEASE) {
                    rawDataLogFile.Log(DumpLocation(newLocation, isBetter));
                }

                if(isBetter) {

                    locationFeedCount++;
                    currentBestLocation = newLocation;

                    if (!MainActivity.EXTERNRELEASE) {
                        betterDataLogFile.Log(DumpLocation(newLocation, isBetter));
                    }

                    FireOnLocationProvidedEvent(currentBestLocation);
                }
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {}

        @Override
        public void onProviderEnabled(String s) {}

        @Override
        public void onProviderDisabled(String s) {}
    }

}
