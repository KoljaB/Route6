package com.example.routemap.app;

import android.content.Context;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.text.format.Time;

import org.mapsforge.core.model.GeoPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Lonli on 01.05.2014.
 */
public class Audio {

    final String SPEAK_LOGFILE = "SpeakEvents.log";

    public final static double AHEAD = 0.2;
    public final static double VV_FORK = 0.4;
    public final static double V_FORK = 0.7;
    public final static double FORK = 1.7;
    public final static double TURN = 3.7;
    public final static double HARD = 4.8;
    public final static double V_HARD = 5.9;

    //final int CYCLIC_STATUS_INTERVAL = 80000;
    //final int CYCLIC_STATUS_INTERVAL_OFFROAD = 30000;
    //final int MIN_SILENCE_BEFORE_PREVIEW = 4000;


    final int SEC = 1000;
    final int MIN = 60 * SEC;
    final int CYCLIC_STATUS_INTERVAL = 150 * SEC;
    final int CYCLIC_STATUS_INTERVAL_OFFROAD = 90 * SEC;
    final int CYCLIC_DISTANCE_INTERVAL = 5 * MIN;
    final int MIN_SILENCE_BEFORE_PREVIEW = 4000;

    final int MIN_SILENCE_BETWEEN_UH_OH = 3000;
    final int MIN_SILENCE_BETWEEN_OFFTRACK = 20000;

    public String SpeakReason;

    Context context;
    TextToSpeech tts;
    GpsRoute route;
    String lastSpeakText = "";
    long lastSpeakTime = 0;
    long lastDistanceSpeakTime = 0;
    long lastUhOh = 0;
    long lastOfftrack = 0;
    LogFile logFile;
    HeadingDetection heading;
    boolean firstSpeak;
    boolean init;
    boolean statusError;
    public int LastSpeakEdgeIndex = -1;
    float speachSpeed = 1.8f;

    List<Integer> IndicesPreviewed;


    public boolean IsInitialized()
    {
         return init || statusError;
    }

    public Audio(final Context context, GpsRoute route, HeadingDetection heading)
    {
        if (!MainActivity.EXTERNRELEASE) {
            logFile = new LogFile(SPEAK_LOGFILE);
        }

        this.context = context;
        this.route = route;
        this.heading = heading;

        IndicesPreviewed = new ArrayList<Integer>();

        Clear();
        firstSpeak = true;
        init = false;
        statusError = false;
        tts = new TextToSpeech(context.getApplicationContext(),
            new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status)
                {
                    if(status != TextToSpeech.ERROR) {
                        init = true;

//                        AudioManager am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
//                        int amStreamMusicMaxVol = am.getStreamMaxVolume(am.STREAM_MUSIC);
//                        am.setStreamVolume(am.STREAM_MUSIC, amStreamMusicMaxVol, 0);

                        //tts.setPitch(1.0f);            // slightly pitch up
                        tts.setSpeechRate(speachSpeed);       // slightly accelerate
                        tts.setLanguage(Locale.UK);
                        //tts.setLanguage(Locale.GERMAN);
                    } else {
                        statusError = true;
                    }
                }
            });
    }

    public void Clear() {
        lastSpeakTime = 0;
        lastDistanceSpeakTime = 0;
    }

    public void CyclicSpeakCheck()
    {
        Time curTime = new Time();
        curTime.setToNow();
        long currentMillis = curTime.toMillis(false);

        synchronized (this) {
            if (IsFastSpeak() && (currentMillis - lastSpeakTime > CYCLIC_STATUS_INTERVAL_OFFROAD)
                || (currentMillis - lastSpeakTime > CYCLIC_STATUS_INTERVAL))
            {
                SpeakState("cyclic");
            }
        }
    }

    public boolean IsFastSpeak()
    {
        return route.getOffRoad() || route.CurrentDestinationPointIndex == 0;
    }

    public void SetSpeakNoise(int speakNoise) {

        switch (speakNoise) {
            case 1:
            case 2:
                break;
            case 3:
            case 4:
            case 5:
                AudioManager am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
                int amStreamMusicMaxVol = am.getStreamMaxVolume(am.STREAM_MUSIC);
                am.setStreamVolume(am.STREAM_MUSIC, amStreamMusicMaxVol, 0);

                break;
        }
    }

    public void SetSpeakSpeed(int speakSpeed) {

        switch (speakSpeed) {
            case 1: speachSpeed = 1.2f;       // very slow
                break;
            case 2: speachSpeed = 1.4f;       // slow
                break;
            case 3: speachSpeed = 1.6f;       // normal
                break;
            case 4: speachSpeed = 1.8f;       // fast
                break;
            case 5: speachSpeed = 2.0f;       // insane
                break;
        }

        tts.setSpeechRate(speachSpeed);
    }

    public boolean CanPreviewSegment() {
        Time curTime = new Time();
        curTime.setToNow();
        long currentMillis = curTime.toMillis(false);

        if (currentMillis - lastSpeakTime > MIN_SILENCE_BEFORE_PREVIEW && route.CurrentDestinationPointIndex >= 0) {
            if (!IndicesPreviewed.contains(route.CurrentDestinationPointIndex)) {
                return true;
            }
        }
        return false;
    }

    public void PreviewSegment() {
        if (CanPreviewSegment()) {
            IndicesPreviewed.add(route.CurrentDestinationPointIndex);
            SpeakState("prview");
        }
    }

    public String GetDistanceString(double distance) {

        String distanceString;

        if (distance < 1000) {
            int dist = (int) distance;
            distanceString = dist + " meters";
        } else if (distance < 10000) {
            distanceString = String.format("%.2f",distance / 1000) + " kilometers";
        } else if (distance < 100000) {
            distanceString = String.format("%.1f",distance / 1000) + " kilometers";
        } else {
            int dist = (int) (distance / 1000);
            distanceString = dist  + " kilometers";
        }

        return distanceString.replace(",", ".");

//        if (distance < 1000) {
//            int dist = (int) distance;
//            return dist + " meters";
//        } else if (distance < 10000) {
//            return String.format("%.2f",distance / 1000) + " kilometers";
//        } else if (distance < 100000) {
//            return String.format("%.1f",distance / 1000) + " kilometers";
//        }
//
//        int dist = (int) (distance / 1000);
//        return dist  + " kilometers";
    }

    public String GetRawDirectionString(double direction) {
        if (direction >= (12 - FORK) || direction <= FORK)
            return "ahead";
            //return "geradeaus";

//        if (direction >= 10)
//            return "leicht links";
//        if (direction >= 8.2)
//            return "links";
//        if (direction >= 7.0)
//            return "scharf links";
//        if (direction <= 0.5)
//            return "ganz, ganz leicht rechts";
//        if (direction <= 1)
//            return "ganz leicht rechts";
//        if (direction <= 2)
//            return "leicht rechts";
//        if (direction <= 3.8)
//            return "rechts";
//        if (direction <= 5.0)
//            return "scharf rechts";

        return "not ahead";
    }

    public String GetDirectionString(double direction) {

        if (direction >= (12 - AHEAD) || direction <= AHEAD)
            return "ahead";
        if (direction >= (12 - VV_FORK))
            return "gently left";
        if (direction >= (12 - V_FORK))
            return "slightly left";
        if (direction >= (12 - FORK))
            return "fork left";
        if (direction >= (12 - TURN))
            return "left";
        if (direction >= (12 - HARD))
            return "sharp left";
        if (direction >= (12 - V_HARD))
            return "veer left";
        if (direction <= VV_FORK)
            return "gently right";
        if (direction <= V_FORK)
            return "slightly right";
        if (direction <= FORK)
            return "fork right";
        if (direction <= TURN)
            return "right";
        if (direction <= HARD)
            return "sharp right";
        if (direction <= V_HARD)
            return "veer right";


        return "backwards";
    }

    public void SpeakState(String speakReason) {

        int speakSpeed = 2; // fastest 0, detailed 2

        Time curTime = new Time();
        curTime.setToNow();
        long currentMillis = curTime.toMillis(false);
        String curTimeString = curTime.format("%H:%M:%S");


        if (!init) {
            if (!MainActivity.EXTERNRELEASE) {
                if (statusError) {
                    logFile.Log(curTimeString + ", status error");
                } else {
                    logFile.Log(curTimeString + ", waiting for status");
                }
            }
            return;
        }

        synchronized (this) {

            // only let previw pass
            if (speakReason == "onroad")
                return;

            if (speakReason == "ofroad")
                return;


            String distanceNextEdge = String.valueOf((int) Math.floor(route.DistanceNextEdgeMeters + 0.5));
            String distanceAfterNextEdge = String.valueOf((int) Math.floor(route.DistanceAfterNextEdgeMeters + 0.5));
            //String remainDistance = String.valueOf((int) Math.floor(route.RemainDistance + 0.5));


            String remainDistance = GetDistanceString(route.RemainDistance);
            String rawDirection = GetRawDirectionString(route.ClockDirection);
            String currentDirection = GetDirectionString(route.ClockDirection);
            String nextDirection = GetDirectionString(route.NextClockDirection);
            String afterNextDirection = GetDirectionString(route.AfterNextClockDirection);

            String speakText = "";

            if (firstSpeak) {
                firstSpeak = false;
//                tts.speak("Willkommen!", TextToSpeech.QUEUE_ADD, null);
//                tts.playSilence(1000, TextToSpeech.QUEUE_ADD, null);
            }




            if (speakReason == "approx") {
                // finish reached?
                if (route.IsFinishReached()) {
                    speakText = "You have reached your target!";
                } else if (route.IsFinishLine()) {
                    speakText += "Your destination lies ";

                    speakText += currentDirection;
                    if (!heading.ValidHeading) {
                        speakText += " from north ";
                    }
                    speakText += " in " + distanceNextEdge + " meters.";
                } else {

                    if (route.IsBeforeEdge()) {
                        speakText += "Now " + nextDirection;

                        if (route.DistanceAfterNextEdgeMeters < 1.5 * route.proximity.GetClosestDistance()) {
                            speakText += ", then in " + distanceAfterNextEdge + " meters " + afterNextDirection;
                        }

                    } else if (route.IsStartReached()) {
                        speakText += "Start of track reached. Now ";

                        if (rawDirection != "ahead") { // TBD: Sprachkonstanten ausführen

                            speakText += currentDirection;

                            if (!heading.ValidHeading) {
                                speakText += "from north ";
                            }
                            speakText +=  ", then ";
                        }

                        speakText += "in " + distanceNextEdge + " meters " + nextDirection;
                    }
                    else {
                        // standard approx announcement
                        speakText += "In " + distanceNextEdge + " meters " + nextDirection;

                        if (route.DistanceNextEdgeMeters < 4 * route.proximity.GetClosestDistance() &&
                                route.DistanceAfterNextEdgeMeters < 1.5 * route.proximity.GetClosestDistance()) {
                            speakText += ", then in " + distanceAfterNextEdge + " meters " + afterNextDirection;
                        }


                        if (route.RemainDistance > 0 && (currentMillis - lastDistanceSpeakTime) > CYCLIC_DISTANCE_INTERVAL)
                        {
                            lastDistanceSpeakTime = curTime.toMillis(false);
                            speakText += ". You're " + remainDistance + " away from target.";
                        }
                    }


//                        if (route.IsStartReached()) {
//                            speakText += "Sie haben den Start der Route erreicht. Nun ";
//                        }
//                        if (!heading.ValidHeading) {
//                            speakText += "von Norden aus ";
//                        }
//                        speakText += currentDirection + ", dann in " + distanceNextEdge + " Metern " + nextDirection;
//                    }
                }
            }
            else if (speakReason == "cyclic") {
                if (route.CurrentDestinationPointIndex == 0) {
                    speakText += "Start of track lies ";
                    speakText += currentDirection;
                    if (!heading.ValidHeading) {
                        speakText += " from north ";
                    }
                    speakText += " in " + distanceNextEdge + " meters.";
                } else {
                    if (route.getOffRoad()) {

                        speakText += currentDirection;
                        if (!heading.ValidHeading) {
                            speakText += " from north ";
                        }
                        speakText += ", then in " + distanceNextEdge + " meters " + nextDirection;
                    } else {
                        speakText += "In " + distanceNextEdge + " meters" + nextDirection;
                    }
                }
            } else if (speakReason == "warning") {

                if ((currentMillis - lastUhOh) < MIN_SILENCE_BETWEEN_UH_OH) {
                    return;
                }

                speakText += "uh-oh";

                lastUhOh = currentMillis;
            }
            else if (speakReason == "offtrack") {

                if ((currentMillis - lastOfftrack) < MIN_SILENCE_BETWEEN_OFFTRACK) {
                    return;
                }

                speakText += "Track lies " + currentDirection;
                if (!heading.ValidHeading) {
                    speakText += " from north ";
                }
                speakText += " in " + distanceNextEdge + " meters.";

                //speakText += ". You're " + remainDistance + " away from target.";
                //speakText += ". Your target is " + remainDistance + " away.";

                lastOfftrack = currentMillis;
            }


            if (speakText == "Now ahead") {
                return;
            }


            lastSpeakText = speakText;
            SpeakReason = speakReason;

            try {
                if (!route.getOffRoad()) {
                    LastSpeakEdgeIndex = route.CurrentDestinationPointIndex;
                }
                lastSpeakTime = curTime.toMillis(false);
                //tts.speak(speakText, TextToSpeech.QUEUE_ADD, null);
                tts.speak(speakText, TextToSpeech.QUEUE_FLUSH, null);

                if (!MainActivity.EXTERNRELEASE) {
                    logFile.Log(curTimeString
                    + ", c-1 " + String.format("%02d", route.PrevRoughClockDirection)
                    + ", c-0 " + String.format("%02d", route.RoughClockDirection)
                    + ", c+1 " + String.format("%02d", route.NextRoughClockDirection)
                    + ", dP " + String.format("%.2f", route.DistanceNextEdgeMeters)
                    + ", dSeg " + String.format("%02d", route.CurrentDestinationPointIndex)
                    + ", on " + (route.getOffRoad() ? "0" : "1")
                    + ", TEXT \"" + speakText + "\""
                    + ", src: " + speakReason
                    );
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
