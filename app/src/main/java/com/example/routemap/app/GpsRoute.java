package com.example.routemap.app;

import android.util.Log;

import com.google.common.collect.Lists;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.core.model.GeoPoint;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Lonli on 30.04.2014.
 */
public class GpsRoute
{
//    final static double OFFROAD_THRESHOLD = 200; // metres ### TBD:FBC
//    final static double ONROAD_THRESHOLD = 60; // metres ### TBD:FBC


    final static double OFFROAD_THRESHOLD_WALK = 35; // metres ### TBD:FBC
    final static double OFFROAD_THRESHOLD_BIKE = 50; // metres ### TBD:FBC
    final static double OFFROAD_THRESHOLD_CAR = 75; // metres ### TBD:FBC
    public static double OffroadDistance = OFFROAD_THRESHOLD_BIKE;

//    final static double OFFROAD_THRESHOLD = 20; // metres ### TBD:FBC
//    final static double ONROAD_THRESHOLD = 10; // metres ### TBD:FBC

    final static float SKIPEDGE_THRESHOLD_WALK = 11; // metres ### TBD:FBC
    final static float SKIPEDGE_THRESHOLD_BIKE = 14; // metres ### TBD:FBC
    final static float SKIPEDGE_THRESHOLD_CAR = 17; // metres ### TBD:FBC
    final static float SKIPEDGE_THRESHOLD_FIRST = 30; // metres ### TBD:FBC
    public float SkipEdgeDistance = SKIPEDGE_THRESHOLD_BIKE;

    //final static float SKIPEDGE_THRESHOLD = 10; // metres ### TBD:FBC
    public final static double NOSPEAK_AFTER_EDGE_THRESHOLD = 13; // metres ### TBD:FBC

    public double DistanceNextEdgeMeters = 0;
    public double DistanceSegMeters = 0;
    public double RemainDistance = 0;
    public boolean SkipEdge = false;


    public int RoughClockDirection = -1;
    public int PrevRoughClockDirection = -1;
    public double ClockDirection = -1;
    public int NextRoughClockDirection = -1;
    public double NextClockDirection = -1;
    public double DistanceAfterNextEdgeMeters = -1;
    public double AfterNextClockDirection = -1;
    public double LastSavedClockDirection = 0;

    public int LastDestinationPointIndex = 0;
    public int CurrentDestinationPointIndex = 0;
    public boolean Corrected = false;


    public double ClosestProximityDistance = -1;

    boolean isReverse = false;
    boolean routeLoaded = false;
    boolean offRoad = false;
    boolean navigationReady = false;
    boolean redoPossible = false;

    double RouteDistanceMetres;
    double destinationDistanceMetres;
    List<Float> routeDistances;

    int smoothness = 5;
    String fileName;
    MapView map;
    List<GeoPoint> route;
    List<GeoPoint> undoRoute;
    List<GeoPoint> originalRoute;
    List<GeoPoint> branches;
    List<String> branchDescription;
    //List<GeoPoint> trimmedRoute;
    List<Double> trackDistances;
    ProximityDetection proximity;
    Routing routing;

    public GpsRoute(MapView map)
    {
        this.map = map;

        this.route = new ArrayList<GeoPoint>();
        this.originalRoute = new ArrayList<GeoPoint>();
        this.branches = new ArrayList<GeoPoint>();
        this.branchDescription = new ArrayList<String>();
        this.undoRoute = new ArrayList<GeoPoint>();
        //this.trimmedRoute = new ArrayList<GeoPoint>();
        this.routeDistances = new ArrayList<Float>();
        this.fileName = "";
        this.routing = new Routing();

        trackDistances = new ArrayList<Double>();
    }

    public boolean SafeAddRoute(GeoPoint source, GeoPoint destination, String mode, boolean avoidHills, boolean preferBikeroutes) throws Routing.CalculateRouteException {
        boolean retVal = AddRoute(source, destination, mode, avoidHills, preferBikeroutes);

        if (retVal) {
            SafeTrimRoute(smoothness);
        }
        return retVal;
    }

    // requires retrimmimg
    // requires distance recalculation
    private boolean AddRoute(GeoPoint source, GeoPoint destination, String mode, boolean avoidHills, boolean preferBikeroutes) throws Routing.CalculateRouteException {

        RoutingResult routingResult = routing.CalculateRouteMapQuest(source, destination, mode, avoidHills, preferBikeroutes);

        if (routingResult.FineCourse.size() > 1) {

            boolean first = true;

            if (!isReverse) {
                for (GeoPoint point : routingResult.FineCourse) {
                    if (first) {
                        first = false;
                    } else {
                        originalRoute.add(point);
                    }
                }

                for (GeoPoint point : routingResult.RawCourse) {
                    branches.add(point);
                }
                for (String descript : routingResult.RawDirections) {
                    branchDescription.add(descript);
                }

            } else {
                for (int i = routingResult.FineCourse.size() - 2; i >= 0; i--) {
                    originalRoute.add(0, routingResult.FineCourse.get(i));
                }
            }

            route = new ArrayList<GeoPoint>(originalRoute);
            return true;
        }
        return false;
    }

    public void SafeReverseRoute() {
        ReverseRoute();
        SafeTrimRoute(smoothness);
    }

    // requires retrimmimg
    // requires distance recalculation
    private void ReverseRoute() {
        isReverse = !isReverse;

        route = Lists.reverse(originalRoute);
    }

    public void SafeToggleUndoRedoState()
    {
        ToggleUndoRedoState();
        route = new ArrayList<GeoPoint>(originalRoute);
        SafeTrimRoute(smoothness);
    }

    // requires resetting of route due to originalroute
    // requires retrimmimg
    // requires distance recalculation
    private void ToggleUndoRedoState()
    {
        List<GeoPoint> helper = new ArrayList<GeoPoint>(originalRoute);
        originalRoute = new ArrayList<GeoPoint>(undoRoute);
        undoRoute = new ArrayList<GeoPoint>(helper);
    }

    public void SafeTrimRoute(int strength) {
        TrimRoute(strength);
        CalculateRouteDistances();
    }

    // overrides existing route
    // requires distance recalculation
    private void TrimRoute(int strength) {

        route = new ArrayList<GeoPoint>(originalRoute);

        double factor = 0.00008;

        if (strength <= 1)
            strength = 1;

        switch (strength)
        {
            case 1: factor = 0; break;
            case 2: factor = 0.00002; break;
            case 3: factor = 0.00004; break;
            case 4: factor = 0.00006; break;
            case 5: factor = 0.00008; break;
            case 6: factor = 0.00011; break;
            case 7: factor = 0.00015; break;
            case 8: factor = 0.00020; break;
            case 9: factor = 0.00026; break;
            default: factor = 0.00035; break;
        }

        smoothness = strength;

        if (route.size() == 0) {
            return;
        }

        if (factor != 0) {
            route = DouglasPeuckerReducer.reduceWithTolerance((ArrayList) route, factor); // mittel, alle wesentlichen Änderungen erkennbar
        }

        //TrimNarrowAnglePoints(Audio.AHEAD);
    }

    public void EnsureRoute(GeoPoint startPoint) {

        if (originalRoute == null || originalRoute.size() == 0) {

            originalRoute = new ArrayList<GeoPoint>();
            originalRoute.add(startPoint);

            route = new ArrayList<GeoPoint>(originalRoute);
        }
    }



    public void SetProximity(ProximityDetection proximity) {
        this.proximity = proximity;
    }

    public boolean getOffRoad() { return offRoad; }
    public int getSmoothness() { return smoothness; }
    public void SetNavigationUnprepared() { navigationReady = false; }

    public boolean SetOffroad() {
        boolean newOff = RouteDistanceMetres > OffroadDistance;
        boolean newOn = RouteDistanceMetres < OffroadDistance;

        boolean newOffroadValue = offRoad;
        if (newOff) {
            newOffroadValue = true;
        }
        if (newOn) {
            newOffroadValue = false;
        }

        boolean changed = offRoad != newOffroadValue;
        offRoad = newOffroadValue;
        return changed;
    }

    public void Clear() {

        navigationReady = false;

        branchDescription.clear();
        branches.clear();
        originalRoute.clear();
        route.clear();
    }

    public void SaveState()
    {
        //redoPossible = false;
        undoRoute = new ArrayList<GeoPoint>(originalRoute);
    }

    public List<GeoPoint> GetBranches() { return branches; }
    public List<GeoPoint> GetRoute()
    {
        return route;
    }
    public String GetFileName() { return fileName; }

    public boolean IsFinishReached()
    {
        return (IsFinishLine() && DistanceNextEdgeMeters < ClosestProximityDistance);
    }

    public boolean IsFinishLine()
    {
        return (CurrentDestinationPointIndex == route.size() - 1);
    }

    public boolean IsBeforeEdge()
    {
        return DistanceNextEdgeMeters < ClosestProximityDistance;
    }

    public boolean IsStartReached()
    {
        return (CurrentDestinationPointIndex == 1 && LastDestinationPointIndex == 0);
    }

    public void ImportFromGPX(String gpxFile, String filePath) {

        isReverse = false;
        final File file = new File(filePath, gpxFile);
        fileName = file.getName();

        InputStream inputStream = getInputStreamFromFile(file);

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(inputStream, null);

            route.clear();

            while (MoveToNextTag(parser, "trkpt")) {
                String lat = parser.getAttributeValue(null, "lat");
                String lon = parser.getAttributeValue(null, "lon");

                route.add(new GeoPoint(Float.parseFloat(lat), Float.parseFloat(lon)));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("[PARSE XML]", "Parsing data xml exception", e);
        }

        CurrentDestinationPointIndex = route.size() > 0 ? 0 : -1;

        originalRoute = new ArrayList<GeoPoint>(route);
        routeLoaded = true;
    }

    public float GetDistanceToNextEdge(int i) {
        if (i < 0 || i >= routeDistances.size() - 1) {
            return 0;
        }

        return routeDistances.get(i);
    }

    public void CalculateRouteDistances() {
        routeDistances.clear();
        for (int i = 0; i < route.size() - 1; i++) {
            routeDistances.add(Geo.GetDistance(route.get(i), route.get(i + 1)));
        }
    }

    public File ExportToGPX(String title, String fileName, File root) {

        File file = new File(root, fileName);

        if (file.exists()) {
            file.delete();
        }

        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileWriter fWriter = null;
        try {
            fWriter = new FileWriter(file, true);
            fWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\r\n");
            fWriter.write("<gpx xmlns=\"http://www.topografix.com/GPX/1/0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\" creator=\"GPS Tour Guide\" version=\"1.0\">" + "\r\n");
            fWriter.write("  <author>GPS Tour Guide</author>" + "\r\n");
            fWriter.write("  <trk>" + "\r\n");
            fWriter.write("    <Name>" + title + "</Name>" + "\r\n");
            fWriter.write("    <trkseg>" + "\r\n");

            for (GeoPoint point : originalRoute) {
                fWriter.write("      <trkpt lat=\"" + MainActivity.FormatDecimal(point.latitude, 9) + "\" lon=\"" + MainActivity.FormatDecimal(point.longitude, 9) + "\">" + "\r\n");
                fWriter.write("      </trkpt>" + "\r\n");
            }

            fWriter.write("    </trkseg>\n" + "\r\n");
            fWriter.write("  </trk>" + "\r\n");
            fWriter.write("</gpx>" + "\r\n");

            fWriter.flush();
            fWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return file;
    }


    public void SetLoaded() { routeLoaded = true; }

    public boolean IsRouteLoaded() {
        if (routeLoaded) {
            routeLoaded = false;
            return true;
        }
        return false;
    }

    public boolean IsReverse() { return isReverse; }

    public GeoPoint GetDestination() {
        if (route.size() > 0)
            return route.get(route.size() - 1);

        return null;
    }

    public void AddAbroadDistance(double currentMinRouteDistance)
    {
        trackDistances.add(currentMinRouteDistance);

        while (trackDistances.size() >= 10)
            trackDistances.remove(0);
    }

    public double GetCurrentAbroadAmount() {
        double diffSum = 0;
        for (int i = trackDistances.size() - 1; i >= 1; i--)
        {
            diffSum += trackDistances.get(i) - trackDistances.get(i - 1);
        }

        return diffSum;
    }

//    public static List<GeoPoint> cloneList(List<GeoPoint> list) {
//        List<GeoPoint> clone = new ArrayList<GeoPoint>(list.size());
//        for(GeoPoint item: list) clone.add(item);
//        return clone;
//    }

    public double GetRemainDistance(GeoPoint currentPoint) {
        if (CurrentDestinationPointIndex < 0) {
            return 0;
        }

        double distance = Geo.GetDistance(currentPoint, route.get(CurrentDestinationPointIndex));

        for (int i = CurrentDestinationPointIndex; i < route.size() - 1; i++) {
            distance += GetDistanceToNextEdge(i);
        }

        RemainDistance = distance;

        return distance;
    }

    private double GetSlowRemainDistance(List<GeoPoint> points, GeoPoint currentPoint) {

        double distance = 0;

        if (CurrentDestinationPointIndex < 0) {
            return 0;
        }

        distance += Geo.GetDistance(currentPoint, points.get(CurrentDestinationPointIndex));
        for (int i = CurrentDestinationPointIndex + 1; i < points.size(); i++) {
            distance += Geo.GetDistance(points.get(i - 1), points.get(i));
        }

        return distance;
    }

//    public double GetRemainDistance(GeoPoint currentPoint) {
//
//        double distance = 0;
//
//        if (CurrentDestinationPointIndex < 0) {
//            return 0;
//        }
//
//        distance += Geo.GetDistance(currentPoint, route.get(CurrentDestinationPointIndex));
//        for (int i = CurrentDestinationPointIndex + 1; i < route.size(); i++) {
//            distance += Geo.GetDistance(route.get(i - 1), route.get(i));
//        }
//
//        return distance;
//    }

//    private void TrimNarrowAnglePoints(double minClockAngle) {
//
//        trimmedRoute = new ArrayList<GeoPoint>();
//        trimmedRoute.add(route.get(0));
//        for(int i = 1; i < route.size() - 1; i++)
//        {
//            GeoPoint prev = route.get(i - 1);
//            GeoPoint current = route.get(i);
//            GeoPoint next = route.get(i + 1);
//
//            GeoPoint predictedDestinationOverrunPoint = new GeoPoint(
//                    2 * current.latitude - prev.latitude,
//                    2 * current.longitude - prev.longitude);
//
//            double angle = Geo.GetAngleBetween(current, predictedDestinationOverrunPoint, next);
//            double clockAngle = Geo.ConvertAngleToClock(angle);
//            if (clockAngle >= minClockAngle && clockAngle <= (12 - minClockAngle)) {
//                trimmedRoute.add(current);
//            }
////            int roughAngle = Geo.RoundClockTime(Geo.ConvertAngleToClock(angle));
////
////            if (roughAngle != 12) {
////                trimmedRoute.add(current);
////            }
//        }
//        trimmedRoute.add(route.get(route.size() - 1));
//    }

    private boolean MoveToNextTag(XmlPullParser parser, String tag) {
        try {
            int eventType = parser.next();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG)
                {
                    String tagName = parser.getName();
                    if (tagName.equals(tag))
                        return true;
                }

                eventType = parser.next();
            }
        } catch (Exception e) {
            Log.e("[PARSE XML]", "Parsing data xml exception", e);
        }

        return false;
    }

    private InputStream getInputStreamFromFile(File file) {
        try {
            return new FileInputStream(file);
        }
        catch (FileNotFoundException e) {
            Log.e("[READ FILE]", "Read config stream file not found exception", e);
        }

        return null;
    }

    public void SetMobilityType(int mobilityType) {

        switch (mobilityType) {
            case MainActivity.MOBILITY_TYPE_WALK:
                OffroadDistance = OFFROAD_THRESHOLD_WALK;
                SkipEdgeDistance = SKIPEDGE_THRESHOLD_WALK;
                break;
            case MainActivity.MOBILITY_TYPE_CAR:
                OffroadDistance = OFFROAD_THRESHOLD_CAR;
                SkipEdgeDistance = SKIPEDGE_THRESHOLD_CAR;
                break;
            default:
                OffroadDistance = OFFROAD_THRESHOLD_BIKE;
                SkipEdgeDistance = SKIPEDGE_THRESHOLD_BIKE;
                break;
        }
    }


    // Calculate "Bearing" (from start of the line to point distance from each I want to know) + "Cross-talk distance".
    // find gpx route edge index with lowest distance to given position
    // great: http://stackoverflow.com/questions/20231258/minimum-distance-between-a-point-and-a-line-in-latitude-longitude
    // great: http://stackoverflow.com/questions/7803004/distance-from-point-to-line-on-earth (own answer)
    // http://stackoverflow.com/questions/1299567/how-to-calculate-distance-from-a-point-to-a-line-segment-on-a-sphere
    public int FindNearestRouteEdgeIndex(GeoPoint point, boolean global) {

        int minDistanceEdgeIndex = -1;
        double minDistance = Double.MAX_VALUE;

        int edgeStart = CurrentDestinationPointIndex - 2;
        if (edgeStart < 1) edgeStart = 1;

        //for (int edge = edgeStart; edge < edgeStart + 2 && edge < route.size(); edge++) {
        for (int edge = edgeStart; edge < route.size(); edge++) {
            GeoPoint prevRoutePoint = route.get(edge - 1);
            GeoPoint currentRoutePoint = route.get(edge);

            //double distanceEdge = Geo.GetPreciseDistanceToSegment(point, prevRoutePoint, currentRoutePoint);
            GeoPoint projectedPoint = Geo.GetPreciseProjectedTrackpoint(point, prevRoutePoint, currentRoutePoint);
            double distanceEdge = Geo.GetDistance(point, projectedPoint);


            // DouglasPeuckerReducer.orthogonalDistance does not regard edge boundaries
            // double distanceEdge = DouglasPeuckerReducer.orthogonalDistance(point, prevRoutePoint, currentRoutePoint);
//            double distanceEdge = distToSegment(point, prevRoutePoint, currentRoutePoint);

//            double distanceMetre = Geo.GeoDistanceToMetre(distanceEdge);
//            double distancePrecise = Geo.GetPreciseDistanceToSegment(point, prevRoutePoint, currentRoutePoint);
//            double distanceExact = Geo.GetSegmentDistance(point, prevRoutePoint, currentRoutePoint);
//            double distanceCrossTrack = Geo.CrossTrackDistance(point, prevRoutePoint, currentRoutePoint );
//            double alongTrackDistance = Geo.AlongTrackDistance(point, prevRoutePoint, currentRoutePoint);
//            double normalDistance = Geo.GetDistance(prevRoutePoint, currentRoutePoint);

            // abort search, when we depart from current nearest edge
            if (!global && distanceEdge > (minDistance + 30) && distanceEdge > minDistance * 1.1 && minDistance < 100) {
                break;
            }

            if (distanceEdge < minDistance) {
                minDistance = distanceEdge;
                minDistanceEdgeIndex = edge - 1;
            }
        }

        //currentMinRouteDistance = minDistance;
        return minDistanceEdgeIndex;
    }
}
