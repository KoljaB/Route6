package com.example.routemap.app;

import org.mapsforge.core.model.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class RoutingResult
{
    List<GeoPoint> FineCourse;
    List<GeoPoint> RawCourse;
    List<String> RawDirections;

    public RoutingResult() {
        FineCourse = new ArrayList<GeoPoint>();
        RawCourse = new ArrayList<GeoPoint>();
        RawDirections = new ArrayList<String>();
    }
}
