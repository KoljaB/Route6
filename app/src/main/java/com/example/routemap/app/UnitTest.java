package com.example.routemap.app;

import android.location.Location;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.TreeMap;

/**
 * Created by Lonli on 07.05.2014.
 */
public class UnitTest {

    final String success = "success";

    public String PerformTest() {

        String retVal = success ;

        if (retVal == success)
            retVal = PerformApproximityTest();

        return retVal;
    }

    public String PerformApproximityTest() {

        sendLocation(51.66830125740238, 7.529833093285561);

//        ProximityDetection proximity = new ProximityDetection(new TreeMap<Double, Long>(){{
//            put(20.0, 24 * 1000l);
//            put(100.0, 2 * 60 * 1000l);
//            put(300.0, 6 * 60 * 1000l);
//            put(1000.0, 20 * 60 * 1000l);
//            put(2000.0, 40 * 60 * 1000l);
//            put(5000.0, 100 * 60 * 1000l);
//            put(100000.0, 2000 * 60 * 1000l);
//        }});
//
//        Location location = new Location();

//        proximity.IsApproximate()



        return success;
    }


    static void sendLocation(double latitude, double longitude) {
        try {
            //Socket socket = new Socket("localhost", 5554); // usually 5554
            Socket socket = new Socket("10.0.2.2", 5554); // usually 5554
            socket.setKeepAlive(true);
            String str = "geo fix " + longitude + " " + latitude ;
            Writer w = new OutputStreamWriter(socket.getOutputStream());
            w.write(str + "\r\n");
            w.flush();
        }
        catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
