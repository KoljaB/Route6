package com.example.routemap.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapScaleBar;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.overlay.Circle;
//import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.android.maps.overlay.ListOverlay;
import org.mapsforge.android.maps.overlay.Marker;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.android.maps.overlay.PolygonalChain;
import org.mapsforge.android.maps.overlay.Polyline;
import org.mapsforge.core.model.GeoPoint;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TreeMap;

import gps.ContinuousGPS;

public class MainActivity extends MapActivity
        implements LocationEventListener {

    final String VERSION_STRING = "v32";

    public final static boolean EXTERNRELEASE = true;
    public final static boolean RELEASE = true;
    public final static boolean UNITTEST = false;
    public final static boolean NOWRITE_ON_SDCARD = false;

    public static final int MOBILITY_TYPE_WALK = 0;
    public static final int MOBILITY_TYPE_BIKE = 1;
    public static final int MOBILITY_TYPE_CAR = 2;

    public static final float LATDEF = -1.234567f;
    public static final float LONDEF = 7.654321f;

    final TreeMap<Double, Long> PROXIMITY_DISTANCES_TIMEOUTS_WALK = new TreeMap<Double, Long>(){{
        put(20.0, 48 * 1000l);
        put(50.0, 2 * 60 * 1000l);
        put(100.0, 4 * 60 * 1000l);
        put(300.0, 12 * 60 * 1000l);
        put(1000.0, 40 * 60 * 1000l);
        put(2000.0, 80 * 60 * 1000l);
        put(5000.0, 200 * 60 * 1000l);
        put(100000.0, 4000 * 60 * 1000l);
    }};

    final TreeMap<Double, Long> PROXIMITY_DISTANCES_TIMEOUTS_BIKE = new TreeMap<Double, Long>(){{
        put(37.0, 42 * 1000l);
        put(160.0, 180 * 1000l);
        put(320.0, 10 * 60 * 1000l);
        put(900.0, 40 * 60 * 1000l);
        put(3600.0, 80 * 60 * 1000l);
        put(10000.0, 200 * 60 * 1000l);
        put(200000.0, 4000 * 60 * 1000l);
    }};

    final TreeMap<Double, Long> PROXIMITY_DISTANCES_TIMEOUTS_CAR = new TreeMap<Double, Long>(){{
        put(60.0, 48 * 1000l);
        put(300.0, 4 * 60 * 1000l);
        put(900.0, 12 * 60 * 1000l);
        put(3000.0, 40 * 60 * 1000l);
        put(6000.0, 80 * 60 * 1000l);
        put(15000.0, 200 * 60 * 1000l);
        put(300000.0, 4000 * 60 * 1000l);
    }};

    //public final double DIRECTION_CHANGE_CLOCKANGLE = 1.5;
    public double DirectionChangeClockangle = 1.5;


//    final String MAP_FILE = "/storage/emulated/0/Locus/mapsVector/nordrhein-westfalen.map";
//    final String GPX_FILE = "/storage/emulated/0/Dev/RouteMap/RouteMapRoute.gpx";

    final float MIN_MOVEMENT_DISTANCE = 0.3f;

    public static final double MOVED_PIXEL_THRESHOLD = 70;

    public final static String MAIN_DIRECTORY = "/GpsTourGuide/";
    public final static String TRACK_DIRECTORY = MAIN_DIRECTORY + "TrackLog/";
    public final static String THEMES_DIRECTORY = MAIN_DIRECTORY + "Themes/";

    final float ROUTELOG_FILTER_HIGHRES = 0.5f;
    final float ROUTELOG_FILTER_FINE = 2f;
    final float ROUTELOG_FILTER_MED = 6f;
    final float ROUTELOG_FILTER_RAW = 18f;
    final String ROUTELOG_FILTER_HIGHRES_LOGFILE = TRACK_DIRECTORY + "RouteLogHighRes.gpx";
    final String ROUTELOG_FILTER_FINE_LOGFILE = TRACK_DIRECTORY + "RouteLogFine.gpx";
    final String ROUTELOG_FILTER_MED_LOGFILE = TRACK_DIRECTORY + "RouteLogMed.gpx";
    final String ROUTELOG_FILTER_RAW_LOGFILE = TRACK_DIRECTORY + "RouteLogRaw.gpx";
    final String THEME_FILE = "SimpleTheme3.xml";
    final long OFFROAD_AUTOROUTING_INTERVAL = 5 * 1000;

    MapView map = null;
    TextView txtDistance = null;
    TextView txtCompleteDistance = null;
    TextView txtTrackDistance = null;
    TextView txtNextClock = null;
    TextView txtOffroad = null;
    TextView txtClock = null;
    TextView status1 = null;
    TextView status2 = null;
    TextView status3 = null;
    TextView txtTime = null;
    TextView txtFileName = null;
    TextView txtPause = null;
    TextView txtFinished = null;
    TextView txtIgnoreDirection = null;
    ImageButton btnPos1 = null;
    ImageButton btnPos2 = null;
    ImageButton btnPos3 = null;
    ImageButton btnCenter = null;
    ImageButton btnRecord = null;
    ImageButton btnUndo = null;
    ImageButton btnRedo = null;
    ImageButton btnSetPoint = null;
    ImageButton imgCrosshair = null;
    Boolean firstLocation = false;
    Boolean ignoreLocation = false;

    ListOverlay hideOverlay = new ListOverlay();
    ListOverlay gpxRouteOverlay = new ListOverlay();
    ListOverlay autoRouteOverlay = new ListOverlay();
    ListOverlay historyPathOverlay = new ListOverlay();
    ListOverlay finishOverlay = new ListOverlay();
    ListOverlay nextPositionOverlay = new ListOverlay();
    ListOverlay lastPositionOverlay = new ListOverlay();
    ListOverlay customOverlay = new ListOverlay();
    ListOverlay nextSegmentOverlay = new ListOverlay();
    ListOverlay velocityOverlay = new ListOverlay();
    ListOverlay currentPositionOverlay = new ListOverlay();
    ContinuousGPS continuousGPS;

    int nextSegmentIndex = -1;
    int smoothingStrength = 5;
    //double sumDistance = 0;
    double distanceTrack = 0;
    double distanceCovered = 0;
    boolean avoidHills = false;
    boolean preferBikeRoutes = false;
    boolean useAutoRouting = false;
    boolean ApplicationIsRunning = true;
    boolean useMap = true;
    boolean isPause = false;
    boolean isAutoCenter = true;
    boolean isRecording = false;
    boolean isFinish = false;
    boolean isPopupVisible = false;
    long lastBackRouteTime = 0;

    int processedLocations = 0;
    int currentMobilityType = 0;
    int currentSpeakSpeed = 3;
    int currentSpeakNoise = 3;
    int toleranceAngle = 15;
    double currentFraction = 0;
    String mapFileName;

    byte zoomSize = 17;
    float strokeSize;
    float smallStrokeSize;
    final int NO_POS = 3;
    double[] posLat = new double[NO_POS];
    double[] posLon = new double[NO_POS];
    File trackFile = null;
    FileWriter trackFileWriter;
    List<ImageButton> posImages;
//    double pos1Lat = 0;
//    double pos1Lon = 0;
//    double pos2Lat = 0;
//    double pos2Lon = 0;
//    double pos3Lat = 0;
//    double pos3Lon = 0;

    List<GeoPoint> locations;
    Point clickPoint;
    GeoPoint longClickedPoint;
//    GeoPoint lastMapCenter;

    GpxLog routeLogHighRes;
    GpxLog routeLogFine;
    GpxLog routeLogMed;
    GpxLog routeLogRaw;
    GpsRoute gpsRoute;
    GpsRoute offroadRoute;
    LocationFeeder locationFeeder;
    LocationFilter locationFilter;
    LocationFilter routeLogFilterHighRes;
    LocationFilter routeLogFilterFine;
    LocationFilter routeLogFilterMed;
    LocationFilter routeLogFilterRaw;
    HeadingDetection heading;
    Audio audio;
    Audio offRoadAudio;
    ProximityDetection proximity;
    FileDialog fileDialog;
    Location currentLocation = null;
    GeoPoint currentPosition = null;
    File mainFolder;
    File themesFolder;
    File trackFolder;
    GeoPoint mainRouteOffTrackPoint;

    //Object navigationLockObject = new Object();

    final String PREF_STRING = "com.example.routemap.app.SHARED_PREFERENCES";
    final String PREFKEY_GPX = PREF_STRING + "_GPXFILE";
    final String PREFKEY_RECORDFILE = PREF_STRING + "_RECORDFILE";
    final String PREFKEY_MAP = PREF_STRING + "_MAPFILE";
    final String PREFKEY_ISREVERSE = PREF_STRING + "_ISREVERSE";
    final String PREFKEY_ISAUTOCENTER = PREF_STRING + "_ISAUTOCENTER";
    final String PREFKEY_ISRECORDING = PREF_STRING + "_ISRECORDING";
    final String PREFKEY_USEMAP = PREF_STRING + "_USEMAP";
    final String PREFKEY_ISPAUSE = PREF_STRING + "_ISPAUSE";
    final String PREFKEY_MOBILITYTYPE = PREF_STRING + "_MOBILITYTYPE";
    final String PREFKEY_SPEAKSPEED = PREF_STRING + "_SPEAKSPEED";
    final String PREFKEY_SPEAKNOISE = PREF_STRING + "_SPEAKNOISE";
    final String PREFKEY_ROUTEFILEPATH = PREF_STRING + "_ROUTEFILEPATH";
    final String PREFKEY_MAPFILEPATH = PREF_STRING + "_MAPFILEPATH";
    final String PREFKEY_TOLERANCEANGLE = PREF_STRING + "_TOLERANCEANGLE";
    final String PREFKEY_SMOOTHSTRENGTH = PREF_STRING + "_SMOOTHSTRENGTH";
    final String PREFKEY_LASTLATITUDE = PREF_STRING + "_LASTLATITUDE";
    final String PREFKEY_LASTLONGITUDE = PREF_STRING + "_LASTLONGITUDE";
    final String PREFKEY_PREFERBIKEROUTES = PREF_STRING + "_PREFERBIKEROUTES";
    final String PREFKEY_AVOIDHILLS = PREF_STRING + "_AVOIDHILLS";
    final String PREFKEY_AUTOROUTE = PREF_STRING + "_AUTOROUTE";
    final String PREFKEY_POSLATITUDE = PREF_STRING + "_LATITUDE_POS_";
    final String PREFKEY_POSLONGITUDE = PREF_STRING + "_LONGITUDE_POS_";
//    final String PREFKEY_POS1LATITUDE = PREF_STRING + "_POS1LATITUDE";
//    final String PREFKEY_POS1LONGITUDE = PREF_STRING + "_POS1LONGITUDE";
//    final String PREFKEY_POS2LATITUDE = PREF_STRING + "_POS2LATITUDE";
//    final String PREFKEY_POS2LONGITUDE = PREF_STRING + "_POS2LONGITUDE";
//    final String PREFKEY_POS3LATITUDE = PREF_STRING + "_POS3LATITUDE";
//    final String PREFKEY_POS3LONGITUDE = PREF_STRING + "_POS3LONGITUDE";

    final String EXCEPTION_LOGFILE = MAIN_DIRECTORY + "ExceptionCrashDump.log";

    LogFile exceptionLogFile;
    SharedPreferences prefs;
//    long beginTime;
//    boolean actionMoved;
//    final long TOUCH_LONG_PRESS = 1000;

    public interface OnLongpressListener {
        public void onLongpress(MapView view, GeoPoint longpressLocation);
    }
    static final int LONGPRESS_THRESHOLD = 200;
    private Timer longpressTimer = new Timer();
    private MainActivity.OnLongpressListener longpressListener;
    public void setOnLongpressListener(MainActivity.OnLongpressListener listener) {
        longpressListener = listener;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        InitApp();
    }

    private void AddRoute(GeoPoint source, GeoPoint destination, final boolean avoidHills, final boolean preferBikeroutes) {
        final GeoPoint src = new GeoPoint(source.latitude, source.longitude);
        final GeoPoint dst = new GeoPoint(destination.latitude, destination.longitude);

        new Thread()
        {
            @Override
            public void run() {

                SaveState();

                final int currentRouteSize = gpsRoute.GetRoute().size();

                try {
                    if (gpsRoute.SafeAddRoute(src, dst, getMode(), avoidHills, preferBikeroutes)) {

                        if (currentLocation != null) {
                            gpsRoute.CurrentDestinationPointIndex = gpsRoute.FindNearestRouteEdgeIndex(Geo.LocationToGeopoint(currentLocation), true);
                        }

                        runOnUiThread(new Runnable() {
                            public void run() {
                                DrawRoute();
                                DrawDestinationEdge(gpsRoute, -1, false, true);

                                if (currentLocation != null) {
                                    DrawDestinationPoint(Geo.LocationToGeopoint(currentLocation), false, true);
                                }

                                if (currentRouteSize <= 1) {
                                    audio.Clear();
                                }
                                Navigate(false);
                                DrawFinishCircle(true);
                                map.redraw();
                            }
                        });
                    } else {
                        Log.d("CALCERR", "Route konnte nicht berechnet werden.");
                    }
                } catch (final Routing.CalculateRouteException crE) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(), crE.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }.start();
    }

    private String getMode() {
        String mode = "walk";
        if (currentMobilityType == MOBILITY_TYPE_BIKE)
            mode = "bike";
        else if (currentMobilityType == MOBILITY_TYPE_CAR)
            mode = "car";
        return mode;
    }


    private void TryExtendRoute() {

        SetPause(true);
        SetPauseUI();

        if (currentLocation != null) {

            final List<GeoPoint> route = gpsRoute.GetRoute();

            if (gpsRoute.GetRoute().size() == 0) {
                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("From here?")
                        .setMessage("Start the route from current location?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ExtendRoute(route);
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                gpsRoute.EnsureRoute(map.getMapViewPosition().getCenter());
                                DrawFinishCircle(true);
                                map.redraw();
                            }
                        })
                        .show();
            }
            else
            {
                ExtendRoute(route);
            }

        } else {
            if (gpsRoute.GetRoute().size() == 0) {
                gpsRoute.EnsureRoute(map.getMapViewPosition().getCenter());
                DrawFinishCircle(true);
                map.redraw();
            } else {
                AddRoute(gpsRoute.GetDestination(), map.getMapViewPosition().getCenter(), avoidHills, preferBikeRoutes);
                DrawRemainingDistance();
//                Toast.makeText(getApplicationContext(), "Wait for first gps location fix", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void ExtendRoute(List<GeoPoint> route) {
        gpsRoute.EnsureRoute(Geo.LocationToGeopoint(currentLocation));

        GeoPoint calculationSource = gpsRoute.GetDestination();
        if (calculationSource == null) {
            calculationSource = //map.getMapViewPosition().getCenter();
                    Geo.LocationToGeopoint(currentLocation);

            route.add(calculationSource);
        }

        AddRoute(calculationSource, map.getMapViewPosition().getCenter(), avoidHills, preferBikeRoutes);
        DrawRemainingDistance();
    }

//    private void showPopupMenu(View v){
//        if (currentLocation == null) {
//            //Toast.makeText(getApplicationContext(), "No location", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        isPopupVisible = true;
//
//        //PopupMenu popupMenu = new PopupMenu(MainActivity.this, v);
//        PopupMenu popupMenu = new PopupMenu(MainActivity.this, findViewById(R.id.actionBar));
//        popupMenu.getMenuInflater().inflate(R.menu.clickmap, popupMenu.getMenu());
//
//        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//
//                List<GeoPoint> route = gpsRoute.GetRoute();
//                switch (item.getItemId()) {
//                    case R.id.contextExtendRoute:
//                        gpsRoute.EnsureRoute(Geo.LocationToGeopoint(currentLocation));
//                        GeoPoint calculationSource = gpsRoute.GetDestination();
//                        if (calculationSource == null) {
//                            calculationSource = Geo.LocationToGeopoint(currentLocation);
//                            route.add(calculationSource);
//                        }
//                        AddRoute(calculationSource, longClickedPoint);
//                        break;
//                    case R.id.contextNewRoute:
//                        ClearRoute();
//                        gpsRoute.EnsureRoute(Geo.LocationToGeopoint(currentLocation));
//                        route.add(Geo.LocationToGeopoint(currentLocation));
//                        AddRoute(Geo.LocationToGeopoint(currentLocation), longClickedPoint);
//                        break;
//                }
//                return true;
//            }
//        });
//
//
//        popupMenu.setOnDismissListener(new PopupMenu.OnDismissListener(){
//            @Override
//            public void onDismiss(PopupMenu menu){
//                isPopupVisible = false;
//            }
//        });
//
////        Log.d("Show Popup", "Show Pupup");
//        popupMenu.show();
//    }

//    @Override
//    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
//        super.onCreateContextMenu(menu, v, menuInfo);
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.clickmap, menu);
//    }
//
//    @Override
//    public boolean onContextItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.contextExtendRoute:
//                Log.d("EXTROUT", "Extend Route");
//                break;
//        }
//        return super.onContextItemSelected(item);
//    }

    public GeoPoint getMapCenter()
    {
        return map.getMapViewPosition().getCenter();
    }

    @Override
    public boolean dispatchTouchEvent (final MotionEvent event) {

        // logic:
        // when autoCenter
        //  =>

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                clickPoint = new Point((int)event.getX(), (int)event.getY());
//                longClickedPoint = map.getProjection().fromPixels((int)event.getX(), (int)event.getY());
//
//                longpressTimer = new Timer();
//                longpressTimer.schedule(new TimerTask() {
//                    @Override
//                    public void run() {
//                        runOnUiThread(new Runnable() {
//                            public void run() {
//                                Log.d("BeforePopup", "BeforePupup");
//                                showPopupMenu(findViewById(R.id.topRightLayout));
//                            }
//                        });
//
//                    }
//                }, LONGPRESS_THRESHOLD);
//
                break;

            case MotionEvent.ACTION_MOVE:
                Point currentPoint = new Point((int)event.getX(), (int)event.getY());
                double distance = Geo.GetPointDistance(currentPoint, clickPoint);

                if (!isAutoCenter) {
                    GeoPoint currentCenter = map.getMapViewPosition().getCenter();

                    prefs.edit().putFloat(PREFKEY_LASTLATITUDE, (float) currentCenter.latitude).commit();
                    prefs.edit().putFloat(PREFKEY_LASTLONGITUDE, (float) currentCenter.longitude).commit();
                }

                if (distance > MOVED_PIXEL_THRESHOLD) {
                    if (isAutoCenter) {
                        Vibrate(50);
                        ToggleCenter();
                    }
                    //longpressTimer.cancel();
                }

                break;

            case MotionEvent.ACTION_UP:
//                longpressTimer.cancel();
                break;
        }

//        if (event.getPointerCount() > 1) {
//            // This is a multitouch event, probably zooming.
//            longpressTimer.cancel();
//        }

        return super.dispatchTouchEvent(event);
    }


    private void InitApp() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread paramThread, Throwable paramThrowable) {

                String curFullTimeString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());

                Time curTime = new Time();
                curTime.setToNow();
                String curTimeString = curTime.format("%H:%M:%S");

                exceptionLogFile = new LogFile(EXCEPTION_LOGFILE, false);
                exceptionLogFile.Log("Application Crashtime: " + curFullTimeString);
                exceptionLogFile.Log("================================================================================");
                exceptionLogFile.Log("-  Exception  ------------------------------------------------------------------");
                exceptionLogFile.Log(paramThrowable.toString());
                exceptionLogFile.Log("-  Stacktrace ------------------------------------------------------------------");
                StringWriter sw = new StringWriter();
                paramThrowable.printStackTrace(new PrintWriter(sw));
                exceptionLogFile.Log(sw.toString());
                exceptionLogFile.Log("--------------------------------------------------------------------------------");
                exceptionLogFile.Log("");

                Log.d("Exception", paramThrowable.toString() + "\r\n" + "Stacktrace: \r\n" + sw.toString());

                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(10);
            }
        });

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        mainFolder = new File(Environment.getExternalStorageDirectory() + MAIN_DIRECTORY);
        if (!mainFolder.exists()) {
            mainFolder.mkdirs();
        }

        themesFolder = new File(Environment.getExternalStorageDirectory() + THEMES_DIRECTORY);
        if (!themesFolder.exists()) {
            themesFolder.mkdirs();
        }

        trackFolder = new File(Environment.getExternalStorageDirectory() + TRACK_DIRECTORY);
        if (!trackFolder.exists()) {
            trackFolder.mkdirs();
        }

        //if (!EXTERNRELEASE) {
        //}

        // read persisted app state
        prefs = this.getSharedPreferences(PREF_STRING, Context.MODE_PRIVATE);
        String gpxFileName = prefs.getString(PREFKEY_GPX, "");
        toleranceAngle = prefs.getInt(PREFKEY_TOLERANCEANGLE, 15);
        smoothingStrength = prefs.getInt(PREFKEY_SMOOTHSTRENGTH, 5);
        isAutoCenter = prefs.getBoolean(PREFKEY_ISAUTOCENTER, true);
        isRecording = prefs.getBoolean(PREFKEY_ISRECORDING, false);
        useMap = prefs.getBoolean(PREFKEY_USEMAP, true);
        useAutoRouting = prefs.getBoolean(PREFKEY_AUTOROUTE, true);
        preferBikeRoutes = prefs.getBoolean(PREFKEY_PREFERBIKEROUTES, true);
        avoidHills = prefs.getBoolean(PREFKEY_AVOIDHILLS, true);

        useMap = true;

        locationFeeder = LocationFeeder.getInstance();
        locationFeeder.SetContext(getApplicationContext());
        locationFeeder.AddOnLocationProvidedListener(this);
        locationFeeder.Start();
//        if (!InitialStartupProcessed) {
//        }

        SetPause(prefs.getBoolean(PREFKEY_ISPAUSE, false));

        locationFilter = new LocationFilter(MIN_MOVEMENT_DISTANCE);
        routeLogFilterHighRes = new LocationFilter(ROUTELOG_FILTER_HIGHRES);
        routeLogFilterFine = new LocationFilter(ROUTELOG_FILTER_FINE);
        routeLogFilterMed = new LocationFilter(ROUTELOG_FILTER_MED);
        routeLogFilterRaw = new LocationFilter(ROUTELOG_FILTER_RAW);

        if (!EXTERNRELEASE) {
            routeLogHighRes = new GpxLog(ROUTELOG_FILTER_HIGHRES_LOGFILE);
            routeLogFine = new GpxLog(ROUTELOG_FILTER_FINE_LOGFILE);
            routeLogMed = new GpxLog(ROUTELOG_FILTER_MED_LOGFILE);
            routeLogRaw = new GpxLog(ROUTELOG_FILTER_RAW_LOGFILE);
        }

        map = (MapView) findViewById(R.id.map);
        txtDistance = (TextView) findViewById(R.id.txtDistance);
        txtCompleteDistance = (TextView) findViewById(R.id.txtCompleteDistance);
        txtTrackDistance = (TextView) findViewById(R.id.txtTrackDistance);
        txtClock = (TextView) findViewById(R.id.txtClock);
        txtNextClock = (TextView) findViewById(R.id.txtNextClock);
        status1 = (TextView) findViewById(R.id.status1);
        status2 = (TextView) findViewById(R.id.status2);
        status3 = (TextView) findViewById(R.id.status3);
        txtTime = (TextView) findViewById(R.id.txtTime);
        txtFileName = (TextView) findViewById(R.id.txtFileName);
        txtPause = (TextView) findViewById(R.id.txtPause);
        txtFinished = (TextView) findViewById(R.id.txtFinished);
        txtPause.setVisibility(isPause ? View.VISIBLE : View.GONE);
        txtOffroad = (TextView) findViewById(R.id.txtOffroad);
        txtIgnoreDirection = (TextView) findViewById(R.id.txtIgnoreDirection);
        btnCenter = (ImageButton) findViewById(R.id.btnCenter);
        btnRecord = (ImageButton) findViewById(R.id.btnRecord);
        btnPos1 = (ImageButton) findViewById(R.id.btnPos1);
        btnPos2 = (ImageButton) findViewById(R.id.btnPos2);
        btnPos3 = (ImageButton) findViewById(R.id.btnPos3);
        btnUndo = (ImageButton) findViewById(R.id.btnUndo);
        btnRedo = (ImageButton) findViewById(R.id.btnRedo);
        btnSetPoint = (ImageButton) findViewById(R.id.btnSetPoint);
        imgCrosshair = (ImageButton) findViewById(R.id.imgCrosshair);
        posImages = new ArrayList<ImageButton>();
        posImages.add(btnPos1);
        posImages.add(btnPos2);
        posImages.add(btnPos3);

        ((ImageButton) findViewById(R.id.btnPause)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                SetPause(!isPause);
                Toast.makeText(getApplicationContext(), "Pause mode " + (isPause ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
                Vibrate(400);
                SetPauseUI();
            }
        });

        btnUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                ToggleUndoRedoState();
                btnUndo.setVisibility(View.GONE);
                btnRedo.setVisibility(View.VISIBLE);
            }
        });

        btnRedo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                ToggleUndoRedoState();
                btnUndo.setVisibility(View.VISIBLE);
                btnRedo.setVisibility(View.GONE);
            }
        });

        btnCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                ToggleCenter();
                Toast.makeText(getApplicationContext(), "Auto Center " + (isAutoCenter ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
                Vibrate(200);
                SetCenterUI();
            }
        });

        btnRecord.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                if (!isRecording) {
                    PromptDialog dlg = new PromptDialog(MainActivity.this, R.string.recordTrackTitle, R.string.recordTrackComment) {
                        @Override
                        public boolean onOkClicked(String trackName) {

                            String trackFileName = trackName.replaceAll("[^a-zA-Z0-9]", "");

                            if (!trackFileName.endsWith(".gpx"))
                                trackFileName = trackFileName + ".gpx";

                            prefs.edit().putString(PREFKEY_RECORDFILE, trackFileName).commit();

                            File file = new File(trackFolder, trackFileName);
                            if (file.exists()) {
                                file.delete();
                            }
                            try {
                                file.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            trackFile = new File(trackFolder, trackFileName);
                            try {
                                trackFileWriter = new FileWriter(trackFile, true);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            LogFile.WriteLine(trackFileWriter, "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>");
                            LogFile.WriteLine(trackFileWriter, "<gpx xmlns=\"http://www.topografix.com/GPX/1/0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\" creator=\"GPX Tour Guide\" version=\"1.0\">");
                            LogFile.WriteLine(trackFileWriter, "  <author>GPS Tour Guide</author>");
                            LogFile.WriteLine(trackFileWriter, "  <trk>");
                            LogFile.WriteLine(trackFileWriter, "    <Name>" + trackFileName + "</Name>");
                            LogFile.WriteLine(trackFileWriter, "    <trkseg>");

                            UI_ToggleRecording();

                            return true;
                        }
                    };
                    dlg.show();
                }
                else
                {
                    if (trackFileWriter != null) {
                        LogFile.WriteLine(trackFileWriter, "    </trkseg>");
                        LogFile.WriteLine(trackFileWriter, "  </trk>");
                        LogFile.WriteLine(trackFileWriter, "</gpx>");
                    }

                    UI_ToggleRecording();
                }

                return true;
            }
        });



        btnPos1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (PositionMapButton(1, btnPos1)) {
                    Vibrate(100);
                }
            }
        });
        btnPos1.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ToggleMapPositionButton(1, btnPos1);
                Vibrate(500);
                return true;
            }
        });

        btnPos2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (PositionMapButton(2, btnPos2)) {
                    Vibrate(100);
                }
            }
        });
        btnPos2.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ToggleMapPositionButton(2, btnPos2);
                Vibrate(500);
                return true;
            }
        });

        btnPos3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (PositionMapButton(3, btnPos3)) {
                    Vibrate(100);
                }
            }
        });
        btnPos3.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ToggleMapPositionButton(3, btnPos3);
                Vibrate(500);
                return true;
            }
        });

        btnSetPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                TryExtendRoute();
            }
        });

        if (isRecording)
        {
            InitRecording(false);
        }

        if (RELEASE) {
            status1.setVisibility(View.GONE);
            status2.setVisibility(View.GONE);
            status3.setVisibility(View.GONE);
        }

        mapFileName = prefs.getString(PREFKEY_MAP, "");
        if (mapFileName != "" && useMap) {
            try {
                map.setMapFile(new File(mapFileName));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        int cap = map.getFileSystemTileCache().getCapacity();
        Log.d("CAp: " + cap, "Cap " + cap);
                //.setCapacity();

        map.setBackgroundColor(Color.argb(255, 0, 0, 0));
        map.setBuiltInZoomControls(true); // show zoom buttons
        map.getFpsCounter().setFpsCounter(false); // may be obsolete
        map.getOverlays().add(historyPathOverlay);
        map.getOverlays().add(gpxRouteOverlay);
        map.getOverlays().add(autoRouteOverlay);
        map.getOverlays().add(customOverlay);
        map.getOverlays().add(nextSegmentOverlay);
        map.getOverlays().add(nextPositionOverlay);
        map.getOverlays().add(currentPositionOverlay);
        map.getOverlays().add(lastPositionOverlay);
        map.getOverlays().add(velocityOverlay);
        map.getOverlays().add(finishOverlay);
        map.getMapViewPosition().setZoomLevel(zoomSize);
        //File renderThemeFile = new File(themesFolder, THEME_FILE);
//        File renderThemeFile = new File(Environment.getExternalStorageDirectory(), THEMES_DIRECTORY + THEME_FILE);
//        try {
//            map.setRenderTheme(renderThemeFile);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }


        map.redraw();



        // show scale bar ("Maßstab")
        MapScaleBar mapScaleBar = map.getMapScaleBar();
        mapScaleBar.setShowMapScaleBar(true);

        offroadRoute = new GpsRoute(map);
        gpsRoute = new GpsRoute(map);
        SetMobilityType(null, prefs.getInt(PREFKEY_MOBILITYTYPE, 0));

        heading = new HeadingDetection();
        audio = new Audio(this, gpsRoute, heading);
        offRoadAudio = new Audio(this, offroadRoute, heading);
        locations = new ArrayList<GeoPoint>();

        SetSpeakSpeed(null, prefs.getInt(PREFKEY_SPEAKSPEED, 3));

        // tts thread
        new Thread(new Runnable() {
            public void run() {
                while (ApplicationIsRunning) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (offroadRoute != null && offroadRoute.GetRoute().size() > 0 && audio != null && !isPause) {
                        offRoadAudio.CyclicSpeakCheck();
                    }
                    else if (gpsRoute != null && gpsRoute.navigationReady && audio != null && !isPause) {
                        audio.CyclicSpeakCheck();
                    }

                    runOnUiThread(new Runnable() {
                        public void run() {
                            SetInfoText();
                        }
                    });
                }
            }
        }).start();

        strokeSize = RecalculateStrokeSize(zoomSize);
        smallStrokeSize = strokeSize / 3;

        if (gpxFileName != "") {

            boolean isReverse = prefs.getBoolean(PREFKEY_ISREVERSE, false);
            String filePath = prefs.getString(PREFKEY_ROUTEFILEPATH, "");

            LoadGpxFile(gpxFileName, filePath);

            if (isReverse) {
                ReverseRoute(null);
            }

            List<GeoPoint> route = gpsRoute.GetRoute();
            if (route.size() > 0)
                DrawFinishCircle(true);
        }

//        registerForContextMenu(map);

        continuousGPS = new ContinuousGPS(this);
        continuousGPS.setContinuous(10000);

        SetPauseUI();
        SetCenterUI();

        float latLongDefault = -1.23456f;
        float latitude = prefs.getFloat(PREFKEY_LASTLATITUDE, latLongDefault);
        float longitude = prefs.getFloat(PREFKEY_LASTLONGITUDE, latLongDefault);

        for (int i = 1; i <= NO_POS; i++) {
            posLat[i - 1] = prefs.getFloat(PREFKEY_POSLATITUDE + i, LATDEF);
            posLon[i - 1] = prefs.getFloat(PREFKEY_POSLONGITUDE + i, LONDEF);
        }

        UpdatePositionButtons();

        if (latitude != latLongDefault && longitude != latLongDefault) {
            currentPosition = new GeoPoint(latitude, longitude);
            map.getMapViewPosition().setCenter(currentPosition); // center on last known wposition
        }


        if (EXTERNRELEASE) {
            ((LinearLayout) findViewById(R.id.topLevelLayout)).setVisibility(View.GONE);
            ((LinearLayout) findViewById(R.id.topRightLayout)).setVisibility(View.GONE);
        } else {
            ((LinearLayout) findViewById(R.id.topLevelLayout)).setVisibility(View.VISIBLE);
            ((LinearLayout) findViewById(R.id.topRightLayout)).setVisibility(View.VISIBLE);
        }
    }

    private void UI_ToggleRecording() {
        InitRecording(true);
        Toast.makeText(getApplicationContext(), "Recording " + (isRecording ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
        Vibrate(200);
    }

    void UpdatePositionButtons() {
        for (int i = 1; i <= NO_POS; i++) {
            posImages.get(i - 1).setImageDrawable(getResources().getDrawable((posLat[i - 1] == LATDEF && posLon[i - 1] == LONDEF) ? R.drawable.ic_empty_button : R.drawable.ic_filled_button));
        }
    }

    boolean PositionMapButton(int index, ImageButton btn) {
        if (posLat[index - 1] != LATDEF || posLon[index - 1] != LONDEF) {
            map.getMapViewPosition().setCenter(new GeoPoint(posLat[index - 1], posLon[index - 1]));
            return true;
        }
        return false;
    }

    void ToggleMapPositionButton(int index, ImageButton btn) {
        if (posLat[index - 1] == LATDEF && posLon[index - 1] == LONDEF) {
            // map button unassigned ==> assign
            posLat[index - 1] = map.getMapViewPosition().getCenter().latitude;
            posLon[index - 1] = map.getMapViewPosition().getCenter().longitude;

        } else {
            // button assigned and current position reached
            posLat[index - 1] = LATDEF;
            posLon[index - 1] = LONDEF;
        }

        prefs.edit().putFloat(PREFKEY_POSLATITUDE + index, (float) posLat[index - 1]).commit();
        prefs.edit().putFloat(PREFKEY_POSLONGITUDE + index, (float) posLon[index - 1]).commit();

        UpdatePositionButtons();
    }


//    void PositionMapButton(int index, ImageButton btn) {
//        if (posLat[index - 1] == LATDEF && posLon[index - 1] == LONDEF) {
//            // map button unassigned ==> assign
//
//            posLat[index - 1] = map.getMapViewPosition().getCenter().latitude;
//            posLon[index - 1] = map.getMapViewPosition().getCenter().longitude;
//
//            prefs.edit().putFloat(PREFKEY_POSLATITUDE + index, (float) posLat[index - 1]).commit();
//            prefs.edit().putFloat(PREFKEY_POSLONGITUDE + index, (float) posLon[index - 1]).commit();
//        } else {
//            if (posLat[index - 1] == map.getMapViewPosition().getCenter().latitude
//                && posLon[index - 1] == map.getMapViewPosition().getCenter().longitude) {
//
//                // button assigned and current position reached
//                posLat[index - 1] = LATDEF;
//                posLon[index - 1] = LONDEF;
//
//                prefs.edit().putFloat(PREFKEY_POSLATITUDE + index, (float) posLat[index - 1]).commit();
//                prefs.edit().putFloat(PREFKEY_POSLONGITUDE + index, (float) posLon[index - 1]).commit();
//            } else {
//
//                map.getMapViewPosition().setCenter(new GeoPoint(posLat[index - 1], posLon[index - 1]));
//            }
//        }
//
//        UpdatePositionButtons();
//    }

    void ToggleUndoRedoState()
    {
        gpsRoute.SafeToggleUndoRedoState();

        if (currentLocation != null) {
            gpsRoute.CurrentDestinationPointIndex = gpsRoute.FindNearestRouteEdgeIndex(Geo.LocationToGeopoint(currentLocation), false);
        }

        DrawRoute();
        DrawDestinationEdge(gpsRoute, -1, false, true);
        DrawDestinationPoint(Geo.LocationToGeopoint(currentLocation), false, true);
        //audio.Clear();
        Navigate(false);
        DrawFinishCircle(true);
        map.redraw();
    }


    private void Vibrate(int time) {
        Vibrator mVibrator = (Vibrator)getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (mVibrator != null && mVibrator.hasVibrator()) {
            ((Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(time);
        }
    }

    private void SetPauseUI() {
        if (isPause) {
            ((ImageButton) findViewById(R.id.btnPause)).setBackgroundColor(Color.argb(255, 255, 80, 80));
        } else {
            ((ImageButton) findViewById(R.id.btnPause)).setBackgroundColor(Color.argb(0, 0, 0, 0));
        }
    }

    private void SetCenter() {
        if (currentLocation != null) {
            GeoPoint currentPoint = new GeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude());

            if (isAutoCenter) {
                map.getMapViewPosition().setCenter(currentPoint); // center on current position

                prefs.edit().putFloat(PREFKEY_LASTLATITUDE, (float) currentPoint.latitude).commit();
                prefs.edit().putFloat(PREFKEY_LASTLONGITUDE, (float) currentPoint.longitude).commit();
            }
        }
    }

    private void SetCenterUI() {
        ((LinearLayout) findViewById(R.id.actionBarEditing)).setVisibility(!isAutoCenter ? View.VISIBLE : View.GONE);
        ((LinearLayout) findViewById(R.id.actionBarPositions)).setVisibility(!isAutoCenter ? View.VISIBLE : View.GONE);


        btnCenter.setVisibility(!isAutoCenter ? View.VISIBLE : View.GONE);
        imgCrosshair.setVisibility(!isAutoCenter ? View.VISIBLE : View.GONE);

        if (isAutoCenter) {
            SetCenter();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();

//        locationFeeder.Stop();
    }

    @Override
    protected void onResume() {
        super.onResume();

//        locationFeeder.Resume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);

        //menu.findItem(R.id.menuMapCenter).setChecked(isAutoCenter);

        //menu.findItem(R.id.menuMapThemed).setChecked(useMap);
        menu.findItem(R.id.menuRouteReverse).setChecked(gpsRoute.IsReverse());
        menu.findItem(R.id.menuNaviAutoRouting).setChecked(useAutoRouting);
        menu.findItem(R.id.menuNaviAvoidHills).setChecked(avoidHills);
        menu.findItem(R.id.menuNaviBikeroutes).setChecked(preferBikeRoutes);

        //menu.findItem(R.id.menuNaviTogglePause).setChecked(isPause);

        if (currentMobilityType == MOBILITY_TYPE_WALK)
            menu.findItem(R.id.menuMobilityTypeFoot).setChecked(true);
        else if (currentMobilityType == MOBILITY_TYPE_BIKE)
            menu.findItem(R.id.menuMobilityTypeBike).setChecked(true);
        else if (currentMobilityType == MOBILITY_TYPE_CAR)
            menu.findItem(R.id.menuMobilityTypeCar).setChecked(true);



        menu.findItem(R.id.menuSmooth5).setChecked(true);

        switch (currentSpeakSpeed)
        {
            case 1: menu.findItem(R.id.menuSpeakSpeedVerySlow).setChecked(true); break;
            case 2: menu.findItem(R.id.menuSpeakSpeedSlow).setChecked(true); break;
            case 3: menu.findItem(R.id.menuSpeakSpeedNormal).setChecked(true); break;
            case 4: menu.findItem(R.id.menuSpeakSpeedFast).setChecked(true); break;
            default: menu.findItem(R.id.menuSpeakSpeedVeryFast).setChecked(true); break;
        }

        switch (toleranceAngle)
        {
            case 0: menu.findItem(R.id.menuToleranceAngle0).setChecked(true); break;
            case 5: menu.findItem(R.id.menuToleranceAngle5).setChecked(true); break;
            case 10: menu.findItem(R.id.menuToleranceAngle10).setChecked(true); break;
            case 15: menu.findItem(R.id.menuToleranceAngle15).setChecked(true); break;
            case 20: menu.findItem(R.id.menuToleranceAngle20).setChecked(true); break;
            case 25: menu.findItem(R.id.menuToleranceAngle25).setChecked(true); break;
            case 30: menu.findItem(R.id.menuToleranceAngle30).setChecked(true); break;
            case 40: menu.findItem(R.id.menuToleranceAngle40).setChecked(true); break;
            case 50: menu.findItem(R.id.menuToleranceAngle50).setChecked(true); break;
            case 60: menu.findItem(R.id.menuToleranceAngle60).setChecked(true); break;
            case 90: menu.findItem(R.id.menuToleranceAngle90).setChecked(true); break;
        }
//
        return true;
    }

    void SaveState() {
        gpsRoute.SaveState();

        runOnUiThread(new Runnable() {
            public void run() {
                btnUndo.setVisibility(View.VISIBLE);
                btnRedo.setVisibility(View.GONE);
            }
        });
    }

    void ClearRoute() {
        SaveState();

        gpsRoute.Clear();
        ResetRoute();
        audio.Clear();
        prefs.edit().putString(PREFKEY_GPX, "").commit();
        DrawRoute();
        DrawDestinationEdge(gpsRoute, -1, false, true);

        if (currentLocation != null) {
            DrawDestinationPoint(Geo.LocationToGeopoint(currentLocation), false, true);
        }

        DrawFinishCircle(false);
        map.redraw();
    }

    void StartRoute() {
        ResetRoute();
        DrawDestinationEdge(gpsRoute, -1, false, true);
        DrawDestinationPoint(Geo.LocationToGeopoint(currentLocation), false, true);
        DrawFinishCircle(false);
        audio.Clear();
        Navigate(false);
        DrawFinishCircle(true);
        map.redraw();
    }

    void PositionRoute() {

        if (currentLocation == null) {
            Toast.makeText(getApplicationContext(), "Wait for first gps location fix", Toast.LENGTH_SHORT).show();
            return;
        }

        ResetRoute();
        gpsRoute.CurrentDestinationPointIndex = gpsRoute.FindNearestRouteEdgeIndex(Geo.LocationToGeopoint(currentLocation), true);
        DrawDestinationEdge(gpsRoute, -1, false, true);
        DrawDestinationPoint(Geo.LocationToGeopoint(currentLocation), false, true);
        DrawFinishCircle(false);
        audio.Clear();
        Navigate(false);
        DrawFinishCircle(true);
        map.redraw();
    }

    void ResetRoute() {
        proximity.Clear();
        if (audio != null) {
            audio.Clear();
        }
        gpsRoute.CurrentDestinationPointIndex = 0;

        gpsRoute.RoughClockDirection = -1;
        gpsRoute.PrevRoughClockDirection = -1;
        gpsRoute.ClockDirection = -1;
        gpsRoute.NextRoughClockDirection = -1;
        gpsRoute.NextClockDirection = -1;
        gpsRoute.SetNavigationUnprepared();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuRouteLoadFile:        LoadGPXRouteFromFile(); return true;
            case R.id.menuRouteSaveFile:        SaveGPXRouteToFile(); return true;
            case R.id.menuRouteReverse:         ReverseRoute(item); return true;
            //case R.id.menuMapThemed:          ToggleUseMap(item); return true;
            case R.id.menuRouteClear:           ClearRoute(); return true;
            case R.id.menuNaviStart:            StartRoute(); return true;
            case R.id.menuNaviPosition:         PositionRoute(); return true;
            case R.id.menuNaviAutoRouting:      ToggleAutoRoute(item); return true;
            case R.id.menuNaviAvoidHills:       ToggleAvoidHills(item); return true;
            case R.id.menuNaviBikeroutes:       TogglePreferBikeRoutes(item); return true;
            //case R.id.menuNaviTogglePause:    ToggleNavigationPause(item); return true;
            case R.id.menuSpeakSpeedVerySlow:   SetSpeakSpeed(item, 1); return true;
            case R.id.menuSpeakSpeedSlow:       SetSpeakSpeed(item, 2); return true;
            case R.id.menuSpeakSpeedNormal:     SetSpeakSpeed(item, 3); return true;
            case R.id.menuSpeakSpeedFast:       SetSpeakSpeed(item, 4); return true;
            case R.id.menuSpeakSpeedVeryFast:   SetSpeakSpeed(item, 5); return true;
            case R.id.menuSpeakNoiseVeryQuiet:  SetSpeakNoise(item, 1); return true;
            case R.id.menuSpeakNoiseQuiet:      SetSpeakNoise(item, 2); return true;
            case R.id.menuSpeakNoiseNormal:     SetSpeakNoise(item, 3); return true;
            case R.id.menuSpeakNoiseLoud:       SetSpeakNoise(item, 4); return true;
            case R.id.menuSpeakNoiseVeryLoud:   SetSpeakNoise(item, 5); return true;
            case R.id.menuMapLoad:              LoadMapFromFile(); return true;
            //case R.id.menuMapCenter:          ToggleAutoCenter(item); return true;
            case R.id.menuMobilityTypeFoot:     SetMobilityType(item, MOBILITY_TYPE_WALK); return true;
            case R.id.menuMobilityTypeBike:     SetMobilityType(item, MOBILITY_TYPE_BIKE); return true;
            case R.id.menuMobilityTypeCar:      SetMobilityType(item, MOBILITY_TYPE_CAR); return true;
            case R.id.menuSmooth1:              SmoothTrack(item, 1); return true;
            case R.id.menuSmooth2:              SmoothTrack(item, 2); return true;
            case R.id.menuSmooth3:              SmoothTrack(item, 3); return true;
            case R.id.menuSmooth4:              SmoothTrack(item, 4); return true;
            case R.id.menuSmooth5:              SmoothTrack(item, 5); return true;
            case R.id.menuSmooth6:              SmoothTrack(item, 6); return true;
            case R.id.menuSmooth7:              SmoothTrack(item, 7); return true;
            case R.id.menuSmooth8:              SmoothTrack(item, 8); return true;
            case R.id.menuSmooth9:              SmoothTrack(item, 9); return true;
            case R.id.menuSmooth10:             SmoothTrack(item, 10); return true;
            case R.id.menuToleranceAngle0:      SetToleranceAngle(item, 0); return true;
            case R.id.menuToleranceAngle5:      SetToleranceAngle(item, 5); return true;
            case R.id.menuToleranceAngle10:      SetToleranceAngle(item, 10); return true;
            case R.id.menuToleranceAngle15:      SetToleranceAngle(item, 15); return true;
            case R.id.menuToleranceAngle20:      SetToleranceAngle(item, 20); return true;
            case R.id.menuToleranceAngle25:      SetToleranceAngle(item, 25); return true;
            case R.id.menuToleranceAngle30:      SetToleranceAngle(item, 30); return true;
            case R.id.menuToleranceAngle40:      SetToleranceAngle(item, 40); return true;
            case R.id.menuToleranceAngle50:      SetToleranceAngle(item, 50); return true;
            case R.id.menuToleranceAngle60:      SetToleranceAngle(item, 60); return true;
            case R.id.menuToleranceAngle90:      SetToleranceAngle(item, 90); return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void SetToleranceAngle(MenuItem item, int toleranceAngle) {
        if (item != null)
            item.setChecked(true);

        int rememberCurrentDestinationPointIndex = gpsRoute.CurrentDestinationPointIndex;
        this.toleranceAngle = toleranceAngle;

        DrawDestinationEdge(gpsRoute, -1, false, true);
        DrawDestinationPoint(Geo.LocationToGeopoint(currentLocation), false, true);
        DrawFinishCircle(false);

        ResetRoute();
        audio.Clear();

        RestoreDestinationPointIndex(rememberCurrentDestinationPointIndex);

        Navigate(false);
        DrawFinishCircle(true);
        map.redraw();

        prefs.edit().putInt(PREFKEY_TOLERANCEANGLE, toleranceAngle).commit();
    }

    void SetSpeakSpeed(MenuItem item, int speakSpeed) {
        if (speakSpeed <= 1)
            speakSpeed = 1;

        audio.SetSpeakSpeed(speakSpeed);
        currentSpeakSpeed = speakSpeed;

        if (item != null)
            item.setChecked(true);

        gpsRoute.SetLoaded();
        Navigate(false);
        DrawFinishCircle(true);

        prefs.edit().putInt(PREFKEY_SPEAKSPEED, speakSpeed).commit();
    }

    void SetSpeakNoise(MenuItem item, int speakNoise) {
        if (speakNoise <= 1)
            speakNoise = 1;

        audio.SetSpeakNoise(speakNoise);
        currentSpeakNoise = speakNoise;

        if (item != null)
            item.setChecked(true);

        gpsRoute.SetLoaded();
        Navigate(false);
        DrawFinishCircle(true);

        prefs.edit().putInt(PREFKEY_SPEAKNOISE, speakNoise).commit();
    }

    void SmoothTrack(MenuItem item, int smoothingStrength) {
        ResetRoute();

        this.smoothingStrength = smoothingStrength;
        prefs.edit().putInt(PREFKEY_SMOOTHSTRENGTH, smoothingStrength).commit();

        gpsRoute.SafeTrimRoute(smoothingStrength);
        DrawRoute();

        if (currentLocation != null) {
            gpsRoute.CurrentDestinationPointIndex = gpsRoute.FindNearestRouteEdgeIndex(Geo.LocationToGeopoint(currentLocation), false);
        }

        gpsRoute.SetLoaded();
        Navigate(false);
        DrawFinishCircle(true);
        map.redraw();
        item.setChecked(true);
    }

    void SetMobilityType(MenuItem item, int mobilityType) {

        currentMobilityType = mobilityType;

        if (mobilityType == MOBILITY_TYPE_WALK) {
            proximity = new ProximityDetection(PROXIMITY_DISTANCES_TIMEOUTS_WALK);
        } else if (mobilityType == MOBILITY_TYPE_CAR) {
            proximity = new ProximityDetection(PROXIMITY_DISTANCES_TIMEOUTS_CAR);
        } else { //if (mobilityType == MOBILITY_TYPE_BIKE) {
            proximity = new ProximityDetection(PROXIMITY_DISTANCES_TIMEOUTS_BIKE);
        }

        if (item != null) {
            item.setChecked(true);
        }

        int rememberCurrentDestinationPointIndex = gpsRoute.CurrentDestinationPointIndex;

        ResetRoute();
        gpsRoute.SetMobilityType(currentMobilityType);
        gpsRoute.SetLoaded();
        gpsRoute.SetProximity(proximity);
        gpsRoute.ClosestProximityDistance = proximity.GetClosestDistance();

        RestoreDestinationPointIndex(rememberCurrentDestinationPointIndex);

        Navigate(false);
        DrawFinishCircle(true);
        map.redraw();

        prefs.edit().putInt(PREFKEY_MOBILITYTYPE, mobilityType).commit();
    }

    void RestoreDestinationPointIndex(int index) {
        gpsRoute.CurrentDestinationPointIndex = index;

        if (currentLocation != null) {
            gpsRoute.CurrentDestinationPointIndex = gpsRoute.FindNearestRouteEdgeIndex(Geo.LocationToGeopoint(currentLocation), false);
        }
    }

    void SetPause(boolean pause) {
        if (pause == isPause) {
            return;
        }

        isPause = pause;
        prefs.edit().putBoolean(PREFKEY_ISPAUSE, isPause).commit();

        if (isPause) {
            locationFeeder.Pause();
        } else {
            locationFeeder.Resume();
        }
    }

//    void ToggleNavigationPause(MenuItem item) {
//        SetPause(!isPause);
//        txtPause.setVisibility(isPause ? View.VISIBLE : View.GONE);
//        item.setChecked(isPause);
//        prefs.edit().putBoolean(PREFKEY_ISPAUSE, isPause).commit();
//    }

//    void ToggleAutoCenter(MenuItem item) {
//        isAutoCenter = !isAutoCenter;
//        item.setChecked(isAutoCenter);
//        prefs.edit().putBoolean(PREFKEY_ISAUTOCENTER, isAutoCenter).commit();
//    }

    void ToggleAutoRoute(MenuItem item) {
        useAutoRouting = !useAutoRouting;
        item.setChecked(useAutoRouting);
        prefs.edit().putBoolean(PREFKEY_AUTOROUTE, useAutoRouting).commit();
    }

    void ToggleAvoidHills(MenuItem item) {
        avoidHills = !avoidHills;
        item.setChecked(avoidHills);
        prefs.edit().putBoolean(PREFKEY_AVOIDHILLS, avoidHills).commit();
    }

    void TogglePreferBikeRoutes(MenuItem item) {
        preferBikeRoutes = !preferBikeRoutes;
        item.setChecked(preferBikeRoutes);
        prefs.edit().putBoolean(PREFKEY_PREFERBIKEROUTES, preferBikeRoutes).commit();
    }


    void ToggleUseMap(MenuItem item) {
        useMap = !useMap;

        if (useMap && mapFileName != "") {
            try {
                map.setMapFile(new File(mapFileName));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        item.setChecked(useMap);
        prefs.edit().putBoolean(PREFKEY_USEMAP, useMap).commit();


        if (!useMap) {
            finish();
            startActivity(getIntent());
        }
    }

    void ToggleCenter() {
        isAutoCenter = !isAutoCenter;
        prefs.edit().putBoolean(PREFKEY_ISAUTOCENTER, isAutoCenter).commit();

        SetCenterUI();
    }

    void InitRecording(boolean toggle) {
        if (toggle) {
            isRecording = !isRecording;
        }

        if (isRecording) {
            String trackFileName = prefs.getString(PREFKEY_RECORDFILE, "");
            trackFile = new File(trackFolder, trackFileName);
            try {
                trackFileWriter = new FileWriter(trackFile, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            trackFile = null;
            trackFileWriter = null;
        }

        prefs.edit().putBoolean(PREFKEY_ISRECORDING, isRecording).commit();

        btnRecord.setImageDrawable(getResources().getDrawable(isRecording ? R.drawable.ic_recording_active : R.drawable.ic_recording_inactive));
    }

    void LoadGpxFile(String file, String parentDirectory) {
        SaveState();
        ResetRoute();

        gpsRoute.ImportFromGPX(file, parentDirectory);
        gpsRoute.SafeTrimRoute(smoothingStrength);

        if (currentLocation != null) {
            gpsRoute.CurrentDestinationPointIndex = gpsRoute.FindNearestRouteEdgeIndex(Geo.LocationToGeopoint(currentLocation), false);
            //gpsRoute.CurrentDestinationPointIndex = gpsRoute.FindNearestRouteEdgeIndex(Geo.LocationToGeopoint(currentLocation), true);
        }

        DrawRoute();
        Navigate(false);
        DrawFinishCircle(true);

        prefs.edit().putString(PREFKEY_GPX, file).commit();
        prefs.edit().putBoolean(PREFKEY_ISREVERSE, false).commit();
    }

    private void DrawRoute() {
        PolygonalChain polygonalChain = new PolygonalChain(gpsRoute.GetRoute());
        Polyline polyline = new Polyline(polygonalChain, StaticResources.PaintGpxPathStroke);
        List<OverlayItem> gpxPathOverlayItems = gpxRouteOverlay.getOverlayItems();
        gpxPathOverlayItems.clear();
        gpxPathOverlayItems.add(polyline);
    }

    private void DrawOffroadRoute() {
        PolygonalChain polygonalChain = new PolygonalChain(offroadRoute.GetRoute());
        Polyline polyline = new Polyline(polygonalChain, StaticResources.PaintAutoRoutePathStroke);
        List<OverlayItem> autoRoutePathOverlayItems = autoRouteOverlay.getOverlayItems();
        autoRoutePathOverlayItems.clear();
        autoRoutePathOverlayItems.add(polyline);
    }

    void ReverseRoute(MenuItem item){
        SaveState();

        int savedDestinationIndex = gpsRoute.CurrentDestinationPointIndex;

        ResetRoute();
        gpsRoute.SafeReverseRoute();

        if (currentLocation != null) {
            gpsRoute.CurrentDestinationPointIndex = gpsRoute.FindNearestRouteEdgeIndex(Geo.LocationToGeopoint(currentLocation), false);
        }

        gpsRoute.SetLoaded();

        gpsRoute.CurrentDestinationPointIndex = Math.max(gpsRoute.GetRoute().size() - savedDestinationIndex, 0);

        Navigate(false);
        DrawFinishCircle(true);
        map.redraw();
        if (item != null) {
            item.setChecked(gpsRoute.IsReverse());
        }
        prefs.edit().putBoolean(PREFKEY_ISREVERSE, gpsRoute.IsReverse()).commit();
    }

    private void SaveGPXRouteToFile() {
        PromptDialog dlg = new PromptDialog(MainActivity.this, R.string.saveGpxTitle, R.string.saveGpxComment) {
            @Override
            public boolean onOkClicked(String routeName) {

                String routeFileName = routeName.replaceAll("[^a-zA-Z0-9]", "");

                if (!routeFileName.endsWith(".gpx"))
                    routeFileName = routeFileName + ".gpx";

                File writeFile = gpsRoute.ExportToGPX(routeName, routeFileName, mainFolder);

                prefs.edit().putString(PREFKEY_GPX, routeFileName).commit();
                prefs.edit().putString(PREFKEY_ROUTEFILEPATH, writeFile.getParent()).commit();

                return true;
            }
        };
        dlg.show();
        //saveToFile(mainFolder);
    }

//    final int SAVE_FILE_RESULT_CODE = 419824;
//
//    void saveToFile(File aFile) {
//        Uri theUri = Uri.fromFile(aFile).buildUpon().scheme("file.new").build();
//        Intent theIntent = new Intent(Intent.ACTION_PICK);
//        theIntent.setData(theUri);
//        theIntent.putExtra(Intent.EXTRA_TITLE,"A Custom Title"); //optional
//        theIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS); //optional
//        try {
//            startActivityForResult(theIntent, SAVE_FILE_RESULT_CODE);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data){
//        switch (requestCode) {
//            case SAVE_FILE_RESULT_CODE: {
//                if (resultCode==RESULT_OK && data!=null && data.getData()!=null) {
//                    String theFilePath = data.getData().getPath();
//                }
//                break;
//            }
//        }
//    }


    private void LoadGPXRouteFromFile() {

        String filePath = prefs.getString(PREFKEY_ROUTEFILEPATH, "");
        File mPath = (filePath == null || filePath == "")
                ? new File(Environment.getExternalStorageDirectory() + "//DIR//")
                : new File(filePath);

        fileDialog = new FileDialog(this, mPath);
        fileDialog.setFileEndsWith(".gpx");
        fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
            public void fileSelected(File file) {

                String parentDirectory = file.getParent();
                prefs.edit().putString(PREFKEY_ROUTEFILEPATH, parentDirectory).commit();

                LoadGpxFile(file.getName(), parentDirectory);
                Navigate(false);
                DrawFinishCircle(true);
                map.redraw();
            }
        });
        fileDialog.showDialog();
    }

    private void LoadMapFromFile() {

        String filePath = prefs.getString(PREFKEY_MAPFILEPATH, "");
        File mPath = (filePath == null || filePath == "")
                ? new File(Environment.getExternalStorageDirectory() + "//DIR//")
                : new File(filePath);

        fileDialog = new FileDialog(this, mPath);
        fileDialog.setFileEndsWith(".map");
        fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
            public void fileSelected(File file) {
                try {
                    prefs.edit().putString(PREFKEY_MAPFILEPATH, file.getParent()).commit();

                    mapFileName = file.toString();
                    if (useMap) {
                        map.setMapFile(new File(mapFileName));
                    }
                    prefs.edit().putString(PREFKEY_MAP, file.toString()).commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        fileDialog.showDialog();
    }

    // does not get called => useless?
    @Override
    protected void onDestroy() {

        ApplicationIsRunning = false;

        if (!EXTERNRELEASE) {
            routeLogHighRes.Close();
            routeLogFine.Close();
            routeLogMed.Close();
            routeLogRaw.Close();
        }

        super.onDestroy();
    }


    public void OnLocationProvided(Location location)
    {
        synchronized (this) {

            GeoPoint currentPoint = Geo.LocationToGeopoint(location);

            DrawCurrentPositionMarker(currentPoint, true);

            heading.AddLocation(location);

            if (audio.IsInitialized()) {
                ProcessNewLocation(location);
            }


            // record track
            if (isRecording) {
                if (trackFileWriter != null) {
                    if (routeLogFilterRaw.Feed(location)) {
                        GpxLog.LogLocation(trackFileWriter, location);
                    }
                }else {
                    Toast.makeText(getApplicationContext(), "ERROR track file writer not available", Toast.LENGTH_SHORT).show();
                }

            }


            if (!EXTERNRELEASE) {
                if (routeLogFilterHighRes.Feed(location)) {
                    routeLogHighRes.LogLocation(location);
                }
                if (routeLogFilterFine.Feed(location)) {
                    routeLogFine.LogLocation(location);
                }
                if (routeLogFilterMed.Feed(location)) {
                    routeLogMed.LogLocation(location);
                }
//                if (routeLogFilterRaw.Feed(location)) {
//                    routeLogRaw.LogLocation(location);
//                }
            }

            DrawHeadingVector();
        }
    }

    private void DrawRemainingDistance() {
        List<GeoPoint> currentRoute = gpsRoute.GetRoute();

        if (currentLocation != null && gpsRoute.GetRoute().size() >= 1) {
            double remainDistance = gpsRoute.GetRemainDistance(Geo.LocationToGeopoint(currentLocation));

            String remainDistanceString = FormatDistance(remainDistance);
            txtCompleteDistance.setText(remainDistanceString.isEmpty() ? "" : remainDistanceString);

            String distanceCoveredString = FormatDistance(distanceCovered);
            txtTrackDistance.setText(distanceCoveredString.isEmpty() ? "" : distanceCoveredString);


//            String distanceTrackString = FormatDistance(distanceTrack);
//            txtTrackDistance.setText(distanceTrackString.isEmpty() ? "" : distanceTrackString );
        }
    }

    private String FormatDistance(double distance) {

        String distanceString;

        if (distance < 1000) {
            distanceString = String.format("%.0f",distance) + " m";
        } else if (distance < 10000) {
            distanceString = String.format("%.2f",distance / 1000) + " km";
        } else if (distance < 100000) {
            distanceString = String.format("%.1f",distance / 1000) + " km";
        } else {
            distanceString = String.format("%.0f",distance / 1000) + " km";
        }

        distanceString = distanceString.replace(",", ".");
        return distanceString;
    }

    private void ProcessNewLocation(Location location) {

        DrawRemainingDistance();

        processedLocations++;
        currentLocation = location;

        // center map on current position
        GeoPoint currentPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        SetCenter();

        // is it our first position?
        ProcessFirstTimeLocation(zoomSize);

        List<GeoPoint> route = gpsRoute.GetRoute();
        if (route.size() > 0)
            DrawFinishCircle(true);

        locations.add(currentPoint);

        if (locations.size() >= 250) {
                locations.remove(0);
        }

        DrawHistoryPath();

        Navigate(true);
        DrawFinishCircle(true);
        SetInfoText();

        map.redraw();
    }

    // Shortest distance between a point and a line segment
    //
    // from:
    // http://stackoverflow.com/questions/849211/shortest-distance-between-a-point-and-a-line-segment
    //
//    public static double dist2(GeoPoint x, GeoPoint y) { return Geo.Sqr(x.latitude - y.latitude) + Geo.Sqr(x.longitude - y.longitude); }
//    public static double distToSegment(GeoPoint p, GeoPoint v, GeoPoint w) { return Math.sqrt(distToSegmentSquared(p, v, w)); }
//    public static double distToSegmentSquared(GeoPoint p, GeoPoint v, GeoPoint w) {
//        double l2 = dist2(v, w);
//        if (l2 == 0) return dist2(p, v);
//        double t = ((p.latitude - v.latitude) * (w.latitude - v.latitude) + (p.longitude - v.longitude) * (w.longitude - v.longitude)) / l2;
//        if (t < 0) return dist2(p, v);
//        if (t > 1) return dist2(p, w);
//
//        GeoPoint gp = new GeoPoint(v.latitude + t * (w.latitude - v.latitude), v.longitude + t * (w.longitude - v.longitude));
//        return dist2(p, gp);
//    }

    private double GetTrackClockAngle(GeoPoint t1, GeoPoint t2, GeoPoint t3) {
        return Geo.ConvertAngleToClock(Geo.GetAngleBetween(t2, new GeoPoint(2 * t2.latitude - t1.latitude, 2 * t2.longitude - t1.longitude), t3));
    }

    private double GetTrackClockAngle(int index) {
        List<GeoPoint> route = gpsRoute.GetRoute();
        if (index - 1 < 0 || index + 1 >= route.size()) {
            return 0;
        }
        return GetTrackClockAngle(route.get(index - 1), route.get(index), route.get(index + 1));
    }

    private boolean Navigate(boolean newLocation)
    {
        boolean success = TryNavigate(newLocation, true);

        if (distanceTrack <= GpsRoute.OffroadDistance) {
            offroadRoute.Clear();
        }

        if (!success && distanceTrack > GpsRoute.OffroadDistance && offroadRoute.GetRoute().size() > 0) {
            TryNavigate(newLocation, false);
        }

        DrawOffroadRoute();

        return success;
    }

    private boolean TryNavigate(boolean newLocation, boolean useNormalRoute)
    {
        Location location = currentLocation;
        if (location == null)
            return false;

        GeoPoint currentPoint = new GeoPoint(location.getLatitude(), location.getLongitude());

        GpsRoute gRoute = useNormalRoute ? gpsRoute : offroadRoute;
        Audio gAudio = useNormalRoute ? audio : offRoadAudio;
        List<GeoPoint> route = gRoute.GetRoute();

        int minDistanceEdgeIndex = gRoute.FindNearestRouteEdgeIndex(currentPoint, !useNormalRoute);

        if (minDistanceEdgeIndex < 0)
            return false;

        float distanceToDestination = Geo.GetDistance(currentPoint, route.get(minDistanceEdgeIndex + 1));
        //float minDistanceBeforeSkip = minDistanceEdgeIndex == 0 ? GpsRoute.SKIPEDGE_THRESHOLD_FIRST : gpsRoute.SkipEdgeDistance;
        float minDistanceBeforeSkip = gpsRoute.SkipEdgeDistance;
        //float minDistanceBeforeSkip = (minDistanceEdgeIndex == 0 ? 3 : 1) * GpsRoute.SKIPEDGE_THRESHOLD;

        gRoute.SkipEdge = minDistanceEdgeIndex + 2 < route.size()
                &&  distanceToDestination < minDistanceBeforeSkip;
        if (gRoute.SkipEdge) {
            minDistanceEdgeIndex++;
        }

        gRoute.navigationReady = locations.size() >= 1;

        status1.setText("minIndex: " + minDistanceEdgeIndex + ", routesize: " + route.size() + ", curIndex: " + gRoute.CurrentDestinationPointIndex);

        final GeoPoint nextEdgeSource = route.get(minDistanceEdgeIndex);
        final GeoPoint nextEdgeDestination = route.get(minDistanceEdgeIndex + 1);

        final double nextSourceDist = Geo.GetDistance(currentPoint, nextEdgeSource);
        final double nextDestinationDist = Geo.GetDistance(currentPoint, nextEdgeDestination);
        final double closestPointDist = Math.min(nextSourceDist, nextDestinationDist);


//        GeoPoint projectedPoint = Geo.GetPreciseProjectedTrackpoint(currentPoint, nextEdgeSource, nextEdgeDestination);
//        DrawCustomPoint(projectedPoint, true);
//
//        double distanceStartProjected = Geo.GetDistance(nextEdgeSource, projectedPoint);
//
//        if (distanceStartProjected < 0.1) {
//
//        }

//        double edgeLength2 = Geo.GetDistance(nextEdgeSource, nextEdgeDestination);
//        edgeLength2 = edgeLength2 * edgeLength2;

        double edgeLength2 = Math.pow(nextEdgeSource.latitude - nextEdgeDestination.latitude, 2) + Math.pow(nextEdgeSource.longitude - nextEdgeDestination.longitude, 2);
        currentFraction = (edgeLength2 == 0) ? -1 :
                ((currentPoint.latitude - nextEdgeSource.latitude) * (nextEdgeDestination.latitude - nextEdgeSource.latitude)
                        + (currentPoint.longitude - nextEdgeSource.longitude) * (nextEdgeDestination.longitude - nextEdgeSource.longitude)) / edgeLength2;


        distanceTrack = 0;
        GeoPoint offTrackPoint;

        if (currentFraction < 0) {
            offTrackPoint = nextEdgeSource;
            distanceTrack = Geo.GetDistance(currentPoint, nextEdgeSource);
        }
        else if (currentFraction > 1) {
            offTrackPoint = nextEdgeDestination;
            distanceTrack = Geo.GetDistance(currentPoint, nextEdgeDestination);
        }
        else {
            GeoPoint projectedPoint = Geo.GetPreciseProjectedTrackpoint(currentPoint, nextEdgeSource, nextEdgeDestination);
            offTrackPoint = projectedPoint ;
//            DrawCustomPoint(projectedPoint, true);

            double distanceStartProjected = Geo.GetDistance(nextEdgeSource, projectedPoint);
            double distanceStartFinish = Geo.GetDistance(nextEdgeSource, nextEdgeDestination);

            if (distanceStartProjected > 0.1 && distanceStartProjected < distanceStartFinish - 0.1) {
                currentFraction = distanceStartProjected / distanceStartFinish;
            }

            distanceTrack = Geo.GetDistance(currentPoint, projectedPoint);
        }

        if (useNormalRoute) {
            mainRouteOffTrackPoint = offTrackPoint;
        }

        if (useAutoRouting) {
            if (useNormalRoute) {
                if (distanceTrack > gRoute.OffroadDistance && offroadRoute.GetRoute().size() == 0) {
                    HandleOffroad(offTrackPoint);
                    return false;
                }
            } else if (distanceTrack > gRoute.OffroadDistance) {
                HandleOffroad(mainRouteOffTrackPoint);
                return false;
            }
        }

//        if (distanceTrack > gRoute.OFFROAD_THRESHOLD) {
//            HandleOffroad(offTrackPoint);
//            return false;
//        }

        // calculate next destination point
        gRoute.LastDestinationPointIndex = gRoute.CurrentDestinationPointIndex;
        gRoute.CurrentDestinationPointIndex = minDistanceEdgeIndex + (gRoute.SkipEdge || currentFraction >= 0 ? 1 : 0);
        GeoPoint destinationPoint = route.get(gRoute.CurrentDestinationPointIndex);

        gRoute.RouteDistanceMetres = (currentFraction > 0 && currentFraction < 1) ?
                distanceTrack
                : Geo.GetDistance(currentPoint, destinationPoint);
//        gpsRoute.RouteDistanceMetres = (currentFraction > 0 && currentFraction < 1)
//            ? currentMinRouteDistance
//            : Geo.GetDistance(currentPoint, destinationPoint);

        gRoute.SetOffroad(); // let speak event occur, if offroad changed

        GeoPoint predictedNextPoint;
        double angle;

        int destinationPointIndex = gRoute.CurrentDestinationPointIndex;

        if (!gRoute.getOffRoad() && destinationPoint == nextEdgeDestination && destinationPointIndex - 1 >= 0 && destinationPointIndex + 1 < route.size()) {

            double clock = GetTrackClockAngle(destinationPointIndex);
            double clockAngle = toleranceAngle / 30; // 30° ≙ 1h
            while (destinationPointIndex + 2 < route.size() && destinationPointIndex - 1 >= 0 && (clock >= (12 - clockAngle / 2) || clock <= clockAngle / 2)) {
                clock = GetTrackClockAngle(++destinationPointIndex);            }

            destinationPoint = route.get(destinationPointIndex);
        }


//        if (currentFraction < 1 && gpsRoute.getOffRoad()) {
//            GeoPoint distanceVector = new GeoPoint(nextEdgeDestination.latitude - nextEdgeSource.latitude, nextEdgeDestination.longitude - nextEdgeSource.longitude);
//            GeoPoint addVector = new GeoPoint(distanceVector.latitude * currentFraction, distanceVector.longitude * currentFraction);
//
//            destinationPoint = new GeoPoint(nextEdgeSource.latitude + addVector.latitude, nextEdgeSource.longitude + addVector.longitude);
//            gpsRoute.CurrentDestinationPointIndex = -1;
//        }

        //GpsVector velocityVector = heading.GetNormalizedDirection();
        //double bearing = heading.GetBearing();

        String speakReason = "";
        boolean speakEvent = false;

        // wenn nicht offroad und index innerhalb der Route
        //predictedNextPoint = Geo.CalculateDestination(currentPoint, bearing, 10);
        predictedNextPoint = heading.GetPredictedNextPoint();
                //Geo.CalculateDestination(currentPoint, bearing, 10);
        angle = Geo.GetAngleBetween(currentPoint, predictedNextPoint, destinationPoint);

//        if (!gpsRoute.getOffRoad() && minDistanceEdgeIndex > 0 && destinationPointIndex + 1 < route.size()) {
//            predictedNextPoint = new GeoPoint(2 * nextEdgeDestination.latitude - nextEdgeSource.latitude, 2 * nextEdgeDestination.longitude - nextEdgeSource.longitude);
//            angle = Geo.GetAngleBetween(currentPoint, predictedNextPoint, destinationPoint);
//        } else {
//            predictedNextPoint = new GeoPoint(currentPoint.latitude + velocityVector.Latitude, currentPoint.longitude + velocityVector.Longitude);
//            angle = Geo.GetAngleBetween(currentPoint, predictedNextPoint, destinationPoint);
//        }

        gRoute.ClockDirection = Geo.ConvertAngleToClock(angle);
        gRoute.PrevRoughClockDirection = gRoute.RoughClockDirection;
        gRoute.RoughClockDirection = Geo.RoundClockTime(gRoute.ClockDirection);
        gRoute.DistanceNextEdgeMeters = Geo.GetDistance(currentPoint, destinationPoint);

        if (newLocation) {
            gRoute.AddAbroadDistance(distanceTrack);
        }


        double abroadAmount = gRoute.GetCurrentAbroadAmount();

        // Abroad-Calculation:
        boolean wrongDirection = heading.ValidHeading && gRoute.ClockDirection >= 2 && gRoute.ClockDirection <= 10;
        boolean distancingFromTrack = abroadAmount > 1.7 && distanceTrack > 20;
        boolean offTrack = distanceTrack > GpsRoute.OffroadDistance;


        status3.setText("abr: " + String.format("%.2f", abroadAmount) + ", curT: " + String.format("%.2f", distanceTrack));

        gRoute.AfterNextClockDirection = -1;
        gRoute.DistanceAfterNextEdgeMeters = -1;

        if (destinationPointIndex + 1 < route.size()) {
            if (destinationPoint == nextEdgeSource) {
                predictedNextPoint = new GeoPoint(2 * nextEdgeSource.latitude - currentPoint.latitude, 2 * nextEdgeSource.longitude - currentPoint.longitude);
                angle = Geo.GetAngleBetween(nextEdgeSource, predictedNextPoint, nextEdgeDestination);
                gRoute.NextClockDirection = Geo.ConvertAngleToClock(angle);
                gRoute.NextRoughClockDirection = Geo.RoundClockTime(gRoute.NextClockDirection);
            } else if (destinationPointIndex - 1 >= 0 && destinationPointIndex + 1 < route.size()) {
                angle = GetTrackClockAngle(destinationPointIndex);
                gRoute.NextClockDirection = angle;
                gRoute.NextRoughClockDirection = Geo.RoundClockTime(gpsRoute.NextClockDirection);

                if (destinationPointIndex + 2 < route.size()) {
                    angle = GetTrackClockAngle(destinationPointIndex + 1);
                    gRoute.AfterNextClockDirection = angle;
                    gRoute.DistanceAfterNextEdgeMeters = Geo.GetDistance(route.get(destinationPointIndex), route.get(destinationPointIndex + 1));
                }
            }
        } else {
            gRoute.NextClockDirection = -1;
            gRoute.NextRoughClockDirection = -1;
        }

        if (useNormalRoute) {
            if (gpsRoute.IsRouteLoaded()) {
                audio.Clear();
            }

            isFinish = gpsRoute.IsFinishReached();
            txtFinished.setVisibility(isFinish ? View.VISIBLE : View.GONE);
        }

        // TBD: nächste Ansage erfolgt aktuell SOFORT, wenn man um die Ecke ist
        // - besser: nochmal "HIER LINKS"
        // - nach 20m oder so "IN 280 Metern RECHTS"... (nach 1x Proximity.ClosestDistance (PCD)
        // - sofort um die Ecke nur, wenn die nächste Edge sehr nah ist (< 40m entfernt = 2 * PCD)

        ((TextView) findViewById(R.id.txtDistancingFromTrack)).setVisibility(distancingFromTrack ? View.VISIBLE : View.GONE);
        ((TextView) findViewById(R.id.txtWrongDirection)).setVisibility(wrongDirection? View.VISIBLE : View.GONE);


        if (offTrack) {
            angle = Geo.GetAngleBetween(currentPoint, predictedNextPoint, offTrackPoint);
            gRoute.ClockDirection = Geo.ConvertAngleToClock(angle);
            gRoute.PrevRoughClockDirection = gRoute.RoughClockDirection;
            gRoute.RoughClockDirection = Geo.RoundClockTime(gRoute.ClockDirection);
            gRoute.DistanceNextEdgeMeters = Geo.GetDistance(currentPoint, offTrackPoint);

            gAudio.SpeakState("offtrack");

        } else if (wrongDirection && distancingFromTrack) {

            gAudio.SpeakState("warning");

        } else  if (destinationPoint != nextEdgeSource
                && !gRoute.offRoad
                && (isFinish || !gRoute.SkipEdge)
                && (closestPointDist > GpsRoute.NOSPEAK_AFTER_EDGE_THRESHOLD || closestPointDist == nextDestinationDist)) {

            if (proximity.IsApproximate(location, destinationPoint, isFinish)) {
                gAudio.SpeakState("approx");
            }

        }



        DrawDestinationPoint(offTrackPoint, true, useNormalRoute);




        if (useNormalRoute) {
            DrawDestinationEdge(gpsRoute, gpsRoute.CurrentDestinationPointIndex - 1, true, true);
        } else {
            DrawDestinationEdge(offroadRoute, offroadRoute.CurrentDestinationPointIndex - 1, true, false);
        }

//        if (minDistanceEdgeIndex >= 0 && minDistanceEdgeIndex + 1 < route.size())
//        {
//            GeoPoint midPoint = Geo.MidPointEx(route.get(minDistanceEdgeIndex), route.get(minDistanceEdgeIndex + 1));
//            DrawCustomPoint(midPoint, true);
//        }

        return true;
    }


    private void HandleOffroad(final GeoPoint offTrackPoint) {

        Time curTime = new Time();
        curTime.setToNow();
        long currentMillis = curTime.toMillis(false);

        if (currentLocation != null && ( (currentMillis - lastBackRouteTime) > OFFROAD_AUTOROUTING_INTERVAL )) {
            lastBackRouteTime = currentMillis;

            new Thread()
            {
                @Override
                public void run() {

                    GeoPoint currentPoint = new GeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude());


                    offroadRoute.SaveState();

                    offroadRoute.Clear();
                    offroadRoute.EnsureRoute(currentPoint);

                    try {
                        if (offroadRoute.SafeAddRoute(currentPoint, offTrackPoint, getMode(), false, false)) {

                            if (currentLocation != null) {
                                offroadRoute.CurrentDestinationPointIndex = offroadRoute.FindNearestRouteEdgeIndex(Geo.LocationToGeopoint(currentLocation), false);
                            }

                            runOnUiThread(new Runnable() {
                                public void run() {
                                    DrawRoute();
                                    DrawDestinationEdge(gpsRoute, -1, false, true);
                                    DrawDestinationPoint(Geo.LocationToGeopoint(currentLocation), false, true);
                                    audio.Clear();
                                    Navigate(false);
                                    DrawFinishCircle(true);
                                    map.redraw();
                                }
                            });
                        } else {

                            // restore values before calculating new route
                            ToggleUndoRedoState();
                        }
                    } catch (Routing.CalculateRouteException rcEx) {
                        ToggleUndoRedoState();
                    }


//                    if (gpsRoute.AddRoute(src, dst, mode)) {
//                        gpsRoute.TrimRoute(smoothingStrength);
//
//                        if (currentLocation != null) {
//                            gpsRoute.CurrentDestinationPointIndex = FindNearestRouteEdgeIndex(Geo.LocationToGeopoint(currentLocation), true);
//                        }
//
//                        runOnUiThread(new Runnable() {
//                            public void run() {
//                                DrawRoute();
//                                DrawDestinationEdge(-1, false);
//                                DrawDestinationPoint(Geo.LocationToGeopoint(currentLocation), false);
//
//                                if (currentRouteSize <= 1) {
//                                    audio.Clear();
//                                }
//                                Navigate(false);
//                                DrawFinishCircle(true);
//                                map.redraw();
//                            }
//                        });
//                    }
//                    else {
//                        Log.d("CALCERR", "Route konnte nicht berechnet werden.");
//                    }
                }
            }.start();
        }
    }
//
//    // Calculate "Bearing" (from start of the line to point distance from each I want to know) + "Cross-talk distance".
//    // find gpx route edge index with lowest distance to given position
//    // great: http://stackoverflow.com/questions/20231258/minimum-distance-between-a-point-and-a-line-in-latitude-longitude
//    // great: http://stackoverflow.com/questions/7803004/distance-from-point-to-line-on-earth (own answer)
//    // http://stackoverflow.com/questions/1299567/how-to-calculate-distance-from-a-point-to-a-line-segment-on-a-sphere
//    private int FindNearestRouteEdgeIndex(GeoPoint point, boolean global) {
//
//        int minDistanceEdgeIndex = -1;
//        double minDistance = Double.MAX_VALUE;
//        List<GeoPoint> route = gpsRoute.GetRoute();
//
//        int edgeStart = gpsRoute.CurrentDestinationPointIndex - 1;
//        if (edgeStart < 1) edgeStart = 1;
//
//        //for (int edge = edgeStart; edge < edgeStart + 2 && edge < route.size(); edge++) {
//        for (int edge = edgeStart; edge < route.size(); edge++) {
//            GeoPoint prevRoutePoint = route.get(edge - 1);
//            GeoPoint currentRoutePoint = route.get(edge);
//
//            //double distanceEdge = Geo.GetPreciseDistanceToSegment(point, prevRoutePoint, currentRoutePoint);
//            GeoPoint projectedPoint = Geo.GetPreciseProjectedTrackpoint(point, prevRoutePoint, currentRoutePoint);
//            double distanceEdge = Geo.GetDistance(point, projectedPoint);
//
//
//            // DouglasPeuckerReducer.orthogonalDistance does not regard edge boundaries
//            // double distanceEdge = DouglasPeuckerReducer.orthogonalDistance(point, prevRoutePoint, currentRoutePoint);
////            double distanceEdge = distToSegment(point, prevRoutePoint, currentRoutePoint);
//
////            double distanceMetre = Geo.GeoDistanceToMetre(distanceEdge);
////            double distancePrecise = Geo.GetPreciseDistanceToSegment(point, prevRoutePoint, currentRoutePoint);
////            double distanceExact = Geo.GetSegmentDistance(point, prevRoutePoint, currentRoutePoint);
////            double distanceCrossTrack = Geo.CrossTrackDistance(point, prevRoutePoint, currentRoutePoint );
////            double alongTrackDistance = Geo.AlongTrackDistance(point, prevRoutePoint, currentRoutePoint);
////            double normalDistance = Geo.GetDistance(prevRoutePoint, currentRoutePoint);
//
//            // abort search, when we depart from current nearest edge
//            if (!global && distanceEdge > minDistance && minDistance < 300) {
//                break;
//            }
//
//            if (distanceEdge < minDistance) {
//                minDistance = distanceEdge;
//                minDistanceEdgeIndex = edge - 1;
//            }
//        }
//
//        currentMinRouteDistance = minDistance;
//        return minDistanceEdgeIndex;
//    }

//    public static double GetOptimalOrthogonalDistance(GeoPoint point, GeoPoint lineStart, GeoPoint lineEnd)
//    {
//        double orthogonalDistance = DouglasPeuckerReducer.orthogonalDistance(point, lineStart, lineEnd);
//        double distLineStart = Geo.GetDistance(point, lineStart);
//        double distLineEnd = Geo.GetDistance(point, lineEnd);
//
//        double min = distLineStart < distLineEnd ? distLineStart : distLineEnd;
//        double minOrtho = orthogonalDistance < min ? orthogonalDistance : min;
//    }

    public static double GetExactClockDifference(double clock1, double clock2)
    {
        double diff = Math.abs(clock1 - clock2);
        if (diff > 6) diff = 12 - diff;
        return diff;
    }

    public static String FormatDecimal(double number, int fractionDigits) {
//        NumberFormat nf = NumberFormat.getNumberInstance(Locale.GERMAN);
//        DecimalFormat df = (DecimalFormat)nf;
//        df.setMaximumFractionDigits(fractionDigits);
//        String formatted = nf.format(number);

        String schablone = "%." + String.valueOf(fractionDigits) + "f";
        String doubleString = String.format(schablone, number);
        return doubleString.replace(",", ".");
    }

    private void SetInfoText() {

        if (isPopupVisible) {
            return;
        }

        Time curTime = new Time();
        curTime.setToNow();
        String curTimeString = curTime.format("%H:%M:%S");

        //status1.setText("Next Point Index: " + gpsRoute.CurrentDestinationPointIndex);
        status2.setText("SkipEdge: " + (gpsRoute.SkipEdge ? "1" : "0") + ", Bearing: " + String.format("%.2f", Geo.ToNormalizedDegrees(heading.bearing)));
        //status3.setText("t: " + String.format("%.2f", currentFraction));

        ColorizeTexts();

        txtTime.setText(curTimeString);

        txtOffroad.setVisibility(gpsRoute.getOffRoad() ? View.VISIBLE : View.GONE);

        String distanceString = String.valueOf((int) Math.floor(gpsRoute.DistanceNextEdgeMeters + 0.5));

        if (gpsRoute.ClockDirection != -1) {
            txtClock.setText(String.format("%.1f", gpsRoute.ClockDirection));
            txtDistance.setText(distanceString);
        } else {
            txtClock.setText("-");
            txtDistance.setText("-");
        }
        if (gpsRoute.NextClockDirection != -1) {
            txtNextClock.setText(String.format("%.1f", gpsRoute.NextClockDirection));
        } else {
            txtNextClock.setText("-");
        }

        //txtFileName.setText(VERSION_STRING + "  -  " + String.format("%.1f", heading.Speed));
        if (RELEASE) {
            String fileName = gpsRoute.GetFileName().replace(".gpx", "");
            txtFileName.setText(fileName );
            txtFileName.setVisibility(!fileName.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            txtFileName.setText(String.format("%.1f", heading.Speed) + "  -  " + VERSION_STRING);
        }
    }

    private void ColorizeTexts() {
        int green = 147;
        int blue = 89;
        int textColor;

        if (gpsRoute.getOffRoad()) {
            textColor = Color.argb(255, 255, 80, 80);
        } else {

            double diff = GetExactClockDifference(gpsRoute.ClockDirection, 0);

            if (diff < 2) {
                green = 255;
                blue = (int) (blue + (255 - blue) * (2 - diff) / 2);
            } else {
                diff = diff - 2;
                green = (int) (green + (255 - green) * (4 - diff) / 4);
            }
            textColor = Color.argb(255, 255, green, blue);
        }

        txtClock.setTextColor(textColor);
        txtDistance.setTextColor(textColor);
        txtNextClock.setTextColor(textColor);
    }

    private void DrawHeadingVector() {

        if (currentLocation != null) {
            final GeoPoint currentPoint = Geo.LocationToGeopoint(currentLocation);
            final GeoPoint destinationPoint = heading.GetPredictedNextPoint();
                    //Geo.CalculateDestination(currentPoint, heading.GetBearing(), 15);

            List<GeoPoint> edgesVel = new ArrayList<GeoPoint>() {{
                add(currentPoint);
                add(destinationPoint);
            }};

            PolygonalChain polygonalChainVel = new PolygonalChain(edgesVel);
            Polyline polylineHeading = new Polyline(polygonalChainVel, StaticResources.PaintStrokeVelocityVector);
            List<OverlayItem> velOverlayItems = velocityOverlay.getOverlayItems();
            velOverlayItems.clear();
            velOverlayItems.add(polylineHeading);
        }
    }


    private void DrawHistoryPath() {
        PolygonalChain polygonalChain = new PolygonalChain(locations);
        Polyline polyline = new Polyline(polygonalChain, StaticResources.PaintHistoryPathStroke);
        List<OverlayItem> historyPathOverlayItems = historyPathOverlay.getOverlayItems();
        historyPathOverlayItems.clear();
        historyPathOverlayItems.add(polyline);
    }

//    private void DrawHistoryPositionMarkers(GeoPoint currentPoint) {
//        Circle newHistoryCircle = new Circle(currentPoint, smallStrokeSize, StaticResources.PaintStrokeHistoryPosition, StaticResources.PaintStrokeHistoryPosition);
//        final List<OverlayItem> historyOverlayItems = customOverlay.getOverlayItems();
//        historyOverlayItems.add(newHistoryCircle);
//
//        if (locations.size() > 1) {
//            GeoPoint lastLoc = locations.get(locations.size() - 2);
//
//            Circle lastPositionCircle = new Circle(lastLoc , smallStrokeSize * 1.5f, StaticResources.PaintStrokeLastPosition, StaticResources.PaintStrokeLastPosition);
//            final List<OverlayItem> lastPositionOverlayItems = lastPositionOverlay.getOverlayItems();
//            lastPositionOverlayItems.clear();
//            lastPositionOverlayItems.add(lastPositionCircle);
//        }
//    }

    private void DrawCurrentPositionMarker(final GeoPoint point, boolean draw) {

        synchronized (this) {

            final Drawable currentPositionDrawable = getResources().getDrawable(R.drawable.ic_current_position);
            final Marker currentPositionMarker = new Marker(point, Marker.boundCenter(currentPositionDrawable));

            final List<OverlayItem> currentPositionOverlayItems = currentPositionOverlay.getOverlayItems();
            currentPositionOverlayItems.clear();

            if (draw) {
                currentPositionOverlayItems.add(currentPositionMarker);
            }
        }

//        Circle finishPositionCircle1 = new Circle(point, 2.4f * strokeSize, StaticResources.PaintTransparent, StaticResources.PaintStrokeCurrentPosition);
//        Circle finishPositionCircle2 = new Circle(point, 2 * strokeSize, StaticResources.PaintTransparent, StaticResources.PaintStrokeCurrentPosition);
////        Circle finishPositionCircle3 = new Circle(point, 1.6f * strokeSize, StaticResources.PaintTransparent, StaticResources.PaintStrokeCurrentPosition);
//        Circle finishPositionCircle4 = new Circle(point, strokeSize / 2, StaticResources.PaintFillCurrentPosition, StaticResources.PaintStrokeCurrentPosition);
//
//        final List<OverlayItem> finishPositionOverlayItems = currentPositionOverlay.getOverlayItems();
//        finishPositionOverlayItems.clear();
//        if (draw) {
//            finishPositionOverlayItems.add(finishPositionCircle1);
//            finishPositionOverlayItems.add(finishPositionCircle2);
////            finishPositionOverlayItems.add(finishPositionCircle3);
//            finishPositionOverlayItems.add(finishPositionCircle4);
//
//            List<GeoPoint> edge1 = new ArrayList<GeoPoint>() {{
//                add(new GeoPoint(point.latitude - 0.0002, point.longitude));
//                add(new GeoPoint(point.latitude + 0.0002, point.longitude));
//            }};
//            List<GeoPoint> edge2 = new ArrayList<GeoPoint>() {{
//                add(new GeoPoint(point.latitude, point.longitude - 0.0003));
//                add(new GeoPoint(point.latitude, point.longitude + 0.0003));
//            }};
//
//            PolygonalChain polygonalChain1 = new PolygonalChain(edge1);
//            Polyline polyline1 = new Polyline(polygonalChain1, StaticResources.PaintStrokeCurrentPosition);
//            finishPositionOverlayItems.add(polyline1);
//            PolygonalChain polygonalChain2 = new PolygonalChain(edge2);
//            Polyline polyline2 = new Polyline(polygonalChain2, StaticResources.PaintStrokeCurrentPosition);
//            finishPositionOverlayItems.add(polyline2);
//        }
    }

    private void DrawDestinationPoint(GeoPoint point, boolean draw, boolean clear) {

        synchronized (this) {

            final Drawable nextPositionDrawable = getResources().getDrawable(R.drawable.ic_destination);
            final Marker nextPositionMarker = new Marker(point, Marker.boundCenter(nextPositionDrawable));

            final List<OverlayItem> nextPositionOverlayItems = nextPositionOverlay.getOverlayItems();

            if (clear) {
                nextPositionOverlayItems.clear();
            }

            if (draw) {
                nextPositionOverlayItems.add(nextPositionMarker);
            }
        }

//        Circle nextPositionCircle = new Circle(point, strokeSize, StaticResources.PaintStrokeNextPosition, StaticResources.PaintStrokeNextPosition);
//        final List<OverlayItem> nextPositionOverlayItems = nextPositionOverlay.getOverlayItems();
//        nextPositionOverlayItems.clear();
//        if (draw) {
//            nextPositionOverlayItems.add(nextPositionCircle);
//        }
    }

    private void DrawCustomPoint(GeoPoint point, boolean draw) {
        Circle customPositionCircle = new Circle(point, strokeSize, StaticResources.PaintStrokeNextPosition, StaticResources.PaintStrokeNextPosition);
        final List<OverlayItem> customOverlayItems = customOverlay.getOverlayItems();
        customOverlayItems.clear();
        if (draw) {
            customOverlayItems.add(customPositionCircle);
        }
    }

    private void DrawFinishCircle(boolean draw) {

        synchronized (this) {
            List<GeoPoint> route = gpsRoute.GetRoute();
            if (route.size() == 0) {
                final List<OverlayItem> currentPositionOverlayItems = finishOverlay.getOverlayItems();
                currentPositionOverlayItems.clear();
                return;
            }

            final List<OverlayItem> currentPositionOverlayItems = finishOverlay.getOverlayItems();
            currentPositionOverlayItems.clear();

            // draw waypoints
            if (draw) {
                final Drawable wayPointDrawable = getResources().getDrawable(R.drawable.ic_waypoint);
                for (GeoPoint wayPoint : gpsRoute.GetBranches()) {
                    final Marker wayPointMarker = new Marker(wayPoint, Marker.boundCenterBottom(wayPointDrawable));
                    currentPositionOverlayItems.add(wayPointMarker);
                }
            }

            // draw starting flag
            final GeoPoint startPoint = route.get(0);
            final Drawable startDrawable = getResources().getDrawable(R.drawable.ic_flag_green);
            final Marker startMarker = new Marker(startPoint, Marker.boundCenterBottom(startDrawable));
            if (draw) {
                currentPositionOverlayItems.add(startMarker);
            }

            // draw finish / target flag
            if (route.size() >= 2) {

                final GeoPoint finishPoint = route.get(route.size() - 1);
                final Drawable finishDrawable = getResources().getDrawable(R.drawable.ic_flag_red);
                final Marker finishMarker = new Marker(finishPoint, Marker.boundCenterBottom(finishDrawable));

                if (draw) {
                    currentPositionOverlayItems.add(finishMarker);
                }
            }
        }
    }

    private void DrawDestinationEdge(GpsRoute gRoute, int edgeIndex, boolean draw, boolean clear) {

        List<OverlayItem> edgeItems = nextSegmentOverlay.getOverlayItems();

        if (clear) {
            edgeItems.clear();
        }

        List<GeoPoint> route = gRoute.GetRoute();
        if (draw && edgeIndex >= 0 && edgeIndex + 1 < route.size()) {

            final GeoPoint nextEdgeSource = route.get(edgeIndex);
            final GeoPoint nextEdgeDestination = route.get(edgeIndex + 1);

            List<GeoPoint> edge = new ArrayList<GeoPoint>() {{
                add(nextEdgeSource);
                add(nextEdgeDestination);
            }};

            PolygonalChain polygonalChain = new PolygonalChain(edge);
            Polyline polyline = new Polyline(polygonalChain, StaticResources.PaintStrokeNavigationEdge);
            Polyline polylineOuter = new Polyline(polygonalChain, StaticResources.PaintStrokeOuterNavigationEdge);
            edgeItems.add(polylineOuter);
            edgeItems.add(polyline);
        }
    }

    private void ProcessFirstTimeLocation(byte zoomSize) {

        if (!firstLocation) {

            try {
                map.getMapViewPosition().setZoomLevel(zoomSize); // initialize zoom level
            } catch (IllegalStateException e) {

            }
            firstLocation = true;
        }
    }

    private float RecalculateStrokeSize(byte zoomSize ) {
        // 14 scales to 6km x 9 km
        // 17 scales to 500m x 750m

        float strokeSize = 60f;
        if (zoomSize == 15) strokeSize = 30f;
        if (zoomSize == 16) strokeSize = 15f;
        if (zoomSize >= 17) strokeSize = 7f;
        return strokeSize;
    }
}
