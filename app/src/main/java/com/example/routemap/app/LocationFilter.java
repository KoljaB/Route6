package com.example.routemap.app;

import android.location.Location;

import org.mapsforge.core.model.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class LocationFilter
{
    Location lastVerifiedLocation = null;
    double minMovementDistance;

    public LocationFilter(double minMovementDistance) {
        this.minMovementDistance = minMovementDistance;
    }

    public boolean Feed(Location location)
    {
        try {
            if (lastVerifiedLocation == null) {
                lastVerifiedLocation = location;
                return true;
            }

            double distance = lastVerifiedLocation.distanceTo(location);
            if (distance < minMovementDistance) {
                return false;
            }

            lastVerifiedLocation = location;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return true;
    }
}
