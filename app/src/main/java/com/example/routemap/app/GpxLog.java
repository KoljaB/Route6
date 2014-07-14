package com.example.routemap.app;

import android.location.Location;
import android.text.format.DateFormat;
import android.text.format.Time;

import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Lonli on 03.05.2014.
 */
public class GpxLog extends LogFile {

    public GpxLog(String fileName) {
        super(fileName);

        Log("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>");
        Log("<gpx xmlns=\"http://www.topografix.com/GPX/1/0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\" creator=\"GPX Tour Guide\" version=\"1.0\">");
        Log("  <author>GPS Tour Guide</author>");
        Log("  <trk>");
        Log("    <Name>" + fileName + "</Name>");
        Log("    <trkseg>");
    }

    public void LogLocation(Location location) {

        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        date.setTime(location.getTime());
        String dateString =  fmt.format(date);

        Time locationTime = new Time();
        locationTime.set(location.getTime());
        String timeString = locationTime.format("%H:%M:%S");

        Log("      <trkpt lat=\"" + MainActivity.FormatDecimal(location.getLatitude(), 9) + "\" lon=\"" + MainActivity.FormatDecimal( location.getLongitude(), 9) + "\">");
        Log("        <time>" + dateString + "T" + timeString + "Z" + "</time>"); // 2010-01-01T00:03:19Z
        Log("      </trkpt>");
    }

    public static void LogLocation(FileWriter writer, Location location) {

        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        date.setTime(location.getTime());
        String dateString =  fmt.format(date);

        Time locationTime = new Time();
        locationTime.set(location.getTime());
        String timeString = locationTime.format("%H:%M:%S");

        LogFile.WriteLine(writer, "      <trkpt lat=\"" + MainActivity.FormatDecimal(location.getLatitude(), 9) + "\" lon=\"" + MainActivity.FormatDecimal( location.getLongitude(), 9) + "\">");
        LogFile.WriteLine(writer, "        <time>" + dateString + "T" + timeString + "Z" + "</time>");
        LogFile.WriteLine(writer, "      </trkpt>");

//        Log("      <trkpt lat=\"" + MainActivity.FormatDecimal(location.getLatitude(), 9) + "\" lon=\"" + MainActivity.FormatDecimal( location.getLongitude(), 9) + "\">");
//        Log("        <time>" + dateString + "T" + timeString + "Z" + "</time>"); // 2010-01-01T00:03:19Z
//        Log("      </trkpt>");
    }

    public void Close() {
        Log("    </trkseg>");
        Log("  </trk>");
        Log("</gpx>");
        /*
	 </trkseg>
   </trk>
 </gpx>
     */
    }
}

