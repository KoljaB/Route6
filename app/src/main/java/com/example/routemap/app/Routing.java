package com.example.routemap.app;

import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.util.IOUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Lonli on 15.05.2014.
 */
public class Routing {

    public class CalculateRouteException extends Exception {

        public CalculateRouteException (String message){
            super(message);
        }

    }

    public List<GeoPoint> CalculateRouteGoogle(GeoPoint source, GeoPoint destination, String mode) {

        List<GeoPoint> route = new ArrayList<GeoPoint>();

        route.add(source);

        String url = "http://maps.googleapis.com/maps/api/directions/xml"
                + "?mode=" + mode
                + "&origin=" + Geo.ConvertToGoogleString(source)
                + "&destination=" + Geo.ConvertToGoogleString(destination)
                + "&sensor=true"
                + "&avoid=highways";

        InputStream inputStream = getInputStreamFromUrl(url);

        if (inputStream == null) {
            Log.d("NOCONN", "No connection to routing server");
        }

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(inputStream, null);

            while (MoveToNextTag(parser, "step")) {

                String strLat = "";
                String strLng = "";

                if (MoveToNextTag(parser, "end_location")) {
                    if (MoveToNextTag(parser, "lat")) {
                        strLat = parser.nextText();
                    }
                    if (MoveToNextTag(parser, "lng")) {
                        strLng = parser.nextText();
                    }
                }

                if (!strLat.isEmpty() && !strLng.isEmpty()) {

                    double lat = Double.parseDouble(strLat);
                    double lng = Double.parseDouble(strLng);

                    route.add(new GeoPoint(lat, lng));
                }
            }
        } catch (Exception e) {
            Log.e("[PARSE XML]", "Parsing data xml exception", e);
        }

        return route;
    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }


    public RoutingResult CalculateRouteMapQuest(GeoPoint source, GeoPoint destination, String mode, boolean avoidHills, boolean preferBikeroutes) throws CalculateRouteException {

        RoutingResult routingResult = new RoutingResult();

        String routeType = "bicycle";
        if (mode == "walk") {
            routeType = "pedestrian";
        } else if (mode == "car") {
            routeType = "fastest";
        }

        String url = "http://www.mapquestapi.com/directions/v2/route" +
                "?key=Fmjtd%7Cluur2duzn5%2C8l%3Do5-9ar51w" +
                "&from=" + Geo.ConvertToGoogleString(source) +
                "&to=" + Geo.ConvertToGoogleString(destination) +
                "&callback=renderAdvancedNarrative" +
                "&outFormat=json" +
                "&routeType=" + routeType +
                (mode != "car" && avoidHills ? "&roadGradeStrategy=AVOID_UP_HILL" : "") +
                // (mode != "car" && avoidHills ? "&roadGradeStrategy=AVOID_ALL_HILLS" : "") +
                (mode != "car" && preferBikeroutes ? "&cyclingRoadFactor=1.9" : "") +
                (mode != "car" ? "&avoids=Limited%20Access" : "") +
                "&timeType=1" +
                "&enhancedNarrative=false" +
                "&shapeFormat=raw" +
                "&generalize=0" +
                "&locale=en_US" +
                "&unit=m" +
                "&drivingStyle=2" +
                "&highwayEfficiency=21.0";
        //"&cyclingRoadFactor=10.0" +
        //"&fullShape=true" +
        //"&unit=k" +
        //"&locale=de_DE" +

        InputStream inputStream = getInputStreamFromUrl(url);
        if (inputStream == null) {
            throw new CalculateRouteException("Network unavailable");
        }

        String retString = convertStreamToString(inputStream);

        if (retString.contains("Unable to calculate route")) {
            throw new CalculateRouteException("Unable to calculate route");
        }
        if (retString.contains("Error processing route request")) {
            throw new CalculateRouteException("Error processing route request");
        }


        int startOfPoints = retString.indexOf("\"shapePoints\"") + 15;
        int endOfPoints = retString.indexOf("]", startOfPoints + 1);

        if (startOfPoints == -1 || endOfPoints == -1) {
            return routingResult;
        }

        String latLongs = retString.substring(startOfPoints, endOfPoints);

        try {
            int last = 0;
            boolean isLat = true;
            double lat = 0;

            int start = latLongs.indexOf(",");
            while (start != -1) {
                String coordStr = latLongs.substring(last, start);
                if (isLat) {
                    lat = Double.parseDouble(coordStr);
                } else {
                    routingResult.FineCourse.add(new GeoPoint(lat, Double.parseDouble(coordStr)));
                }

                last = start + 1;
                start = latLongs.indexOf(",", last);
                isLat = !isLat;
            }

            if (last > 0) {
                String coordStr = latLongs.substring(last, latLongs.length());
                routingResult.FineCourse.add(new GeoPoint(lat, Double.parseDouble(coordStr)));
            }

            /*
            start = retString.indexOf(",\"narrative\":\"");
            while (start != -1) {
                routingResult.RawDirections.add(retString.substring(start + 14, retString.indexOf("\"", start + 15)));

                start = retString.indexOf("\"startPoint\":{\"lng\":", start + 15);
                int comma = retString.indexOf(",\"lat\":", start + 20);
                String lonStr = retString.substring(start + 20, comma - 1);
                String latStr = retString.substring(comma + 7, retString.indexOf("}", comma + 7));
                routingResult.RawCourse.add(new GeoPoint(Double.parseDouble(latStr), Double.parseDouble(lonStr)));

                last = start + 1;
                start = retString.indexOf(",\"narrative\":\"", last);
            }
            */

//            start = retString.indexOf("\"startPoint\":{\"lng\":");
//            while (start != -1) {
//                int comma = retString.indexOf(",\"lat\":", start + 20);
//
//                String lonStr = retString.substring(start + 20, comma - 1);
//                String latStr = retString.substring(comma + 7, retString.indexOf("}", comma + 7));
//
//                routingResult.RawCourse.add(new GeoPoint(Double.parseDouble(latStr), Double.parseDouble(lonStr)));
//
//                int narrativeIndex = retString.indexOf(",\"narrative\":\"", comma + 7);
//                routingResult.RawDirections.add(retString.substring(narrativeIndex + 14, retString.indexOf("\"", narrativeIndex + 15)));
//
//                last = start + 1;
//                start = retString.indexOf("\"startPoint\":{\"lng\":", last);
//            }

        } catch (Exception e) {
            throw new CalculateRouteException("Error parsing mapquest response");
        }




        // to find brach off information search for
        // "narrative":"

        // to find lng and lat information search for
        // "startPoint":{"lng":
        // "startPoint":{"lng":7.531301,"lat":51.667724},
        // "narrative":"Turn right onto walkway. Proceed south (See map for

//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        return routingResult;
    }



    // PRIVATE AREA
    // ===============================================================================================================

    private InputStream getInputStreamFromUrl(String url) {
        InputStream content = null;

        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response = httpclient.execute(new HttpGet(url));
            content = response.getEntity().getContent();
        } catch (Exception e) {
            Log.e("[GET REQUEST]", "Network exception", e);
        }
        return content;
    }

    private  boolean MoveToNextTag(XmlPullParser parser, String tag)
    {
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


}
