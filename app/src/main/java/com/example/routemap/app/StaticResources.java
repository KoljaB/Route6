package com.example.routemap.app;

import android.graphics.Color;
import android.graphics.Paint;

/**
 * Created by Lonli on 30.04.2014.
 */
public class StaticResources {
    public static Paint PaintTransparent = null;
    public static Paint PaintFillFinish = null;
    public static Paint PaintStrokeLastPosition = null;
    public static Paint PaintStrokeHistoryPosition = null;
    public static Paint PaintStrokeVelocityVector = null;
    public static Paint PaintHistoryPathStroke = null;
    public static Paint PaintGpxPathStroke = null;
    public static Paint PaintAutoRoutePathStroke = null;
    public static Paint PaintStrokeNavigationEdge = null;
    public static Paint PaintStrokeOuterNavigationEdge = null;
    public static Paint PaintStrokeNextPosition = null;
    public static Paint PaintFillCurrentPosition = null;
    public static Paint PaintFillFinishReached = null;
    public static Paint PaintStrokeCurrentPosition = null;
    //public static Paint PaintStrokeFinishReached = null;

    static {
        PaintTransparent = new Paint();
        PaintTransparent.setAntiAlias(true);
        PaintTransparent.setColor(Color.argb(0, 0, 0, 0));

        PaintFillFinish = new Paint();
        PaintFillFinish.setAntiAlias(true);
        PaintFillFinish.setColor(Color.argb(255, 255, 0, 0));

        PaintStrokeLastPosition = new Paint();
        PaintStrokeLastPosition.setAntiAlias(true);
        PaintStrokeLastPosition.setColor(Color.argb(180, 180, 0, 0));

        PaintStrokeHistoryPosition = new Paint();
        PaintStrokeHistoryPosition.setAntiAlias(true);
        PaintStrokeHistoryPosition.setColor(Color.argb(180, 255, 255, 0));

        PaintStrokeNextPosition = new Paint();
        PaintStrokeNextPosition.setAntiAlias(true);
        PaintStrokeNextPosition.setColor(Color.argb(255, 243, 86, 255));

        PaintFillCurrentPosition = new Paint();
        PaintFillCurrentPosition.setAntiAlias(true);
        PaintFillCurrentPosition.setColor(Color.argb(255, 255, 0, 0));

        PaintFillFinishReached = new Paint();
        PaintFillFinishReached.setAntiAlias(true);
        PaintFillFinishReached.setColor(Color.argb(255, 30, 255, 30));

        PaintHistoryPathStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        PaintHistoryPathStroke.setStyle(Paint.Style.STROKE);
        PaintHistoryPathStroke.setColor(Color.argb(255, 255, 255, 0));
        PaintHistoryPathStroke.setAlpha(80);
        PaintHistoryPathStroke.setStrokeWidth(7);

        PaintGpxPathStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        PaintGpxPathStroke.setStyle(Paint.Style.STROKE);
        PaintGpxPathStroke.setColor(Color.argb(255, 255, 0, 255));
        PaintGpxPathStroke.setStrokeWidth(7);

        PaintAutoRoutePathStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        PaintAutoRoutePathStroke.setStyle(Paint.Style.STROKE);
        PaintAutoRoutePathStroke.setColor(Color.argb(255, 255, 64, 64));
        PaintAutoRoutePathStroke.setStrokeWidth(7);

        PaintStrokeNavigationEdge = new Paint(Paint.ANTI_ALIAS_FLAG);
        PaintStrokeNavigationEdge.setStyle(Paint.Style.STROKE);
        PaintStrokeNavigationEdge.setColor(Color.argb(255, 50, 255, 55));
        PaintStrokeNavigationEdge.setStrokeWidth(7);

        PaintStrokeOuterNavigationEdge = new Paint(Paint.ANTI_ALIAS_FLAG);
        PaintStrokeOuterNavigationEdge.setStyle(Paint.Style.STROKE);
        PaintStrokeOuterNavigationEdge.setColor(Color.argb(255, 255, 30, 255));
        PaintStrokeOuterNavigationEdge.setStrokeWidth(13);

        PaintStrokeVelocityVector = new Paint(Paint.ANTI_ALIAS_FLAG);
        PaintStrokeVelocityVector.setStyle(Paint.Style.STROKE);
        PaintStrokeVelocityVector.setColor(Color.argb(180, 255, 0, 0));
        PaintStrokeVelocityVector.setStrokeWidth(6);

        PaintStrokeCurrentPosition = new Paint(Paint.ANTI_ALIAS_FLAG);
        PaintStrokeCurrentPosition.setStyle(Paint.Style.STROKE);
        PaintStrokeCurrentPosition.setColor(Color.argb(255, 255, 0, 0));
        PaintStrokeCurrentPosition.setStrokeWidth(2);
//
//        PaintStrokeFinishReached = new Paint(Paint.ANTI_ALIAS_FLAG);
//        PaintStrokeFinishReached.setStyle(Paint.Style.STROKE);
//        PaintStrokeFinishReached.setColor(Color.argb(255, 30, 255, 30));
//        PaintStrokeFinishReached.setStrokeWidth(3);
    }
}
