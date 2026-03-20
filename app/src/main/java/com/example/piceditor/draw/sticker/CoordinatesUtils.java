package com.example.piceditor.draw.sticker;

public class CoordinatesUtils {

    public static float calculateAngle(float f, float f2, float f3, float f4) {
        float degrees = (float) Math.toDegrees(Math.atan2(f3 - f, f4 - f2));
        return (float) (degrees + (Math.ceil((-degrees) / 360) * 360));
    }

    public static float calculateScale(float[] srcCenter, float[] srcCorner, float[] dstCenter, float f, float f2) {
        return ((float) Math.hypot(f - getX(dstCenter), f2 - getY(dstCenter))) / ((float) Math.hypot(getX(srcCorner) - getX(srcCenter), getY(srcCorner) - getY(srcCenter)));
    }

    public static float calculateScale(float f, float f2, float f3, float f4, float f5, float f6) {
        return ((float) Math.hypot(f5 - f, f6 - f2)) / ((float) Math.hypot(f3 - f, f4 - f2));
    }

    public static float calculateScaleX(float[] srcCenter, float[] srcCorner, float[] dstCenter, float f) {
        return Math.abs(f - getX(dstCenter)) / Math.abs(getX(srcCorner) - getX(srcCenter));
    }

    public static float calculateScaleY(float[] srcCenter, float[] srcCorner, float[] dstCenter, float f) {
        return Math.abs(f - getY(dstCenter)) / Math.abs(getY(srcCorner) - getY(srcCenter));
    }

    public static float calculateLength(float[] point1, float[] point2) {
        return (float) Math.hypot(getX(point1) - getX(point2), getY(point1) - getY(point2));
    }

    public static float getXLine(float[] point1, float[] point2, float f) {
        return getX(point1) + (((f - getY(point1)) * (getX(point2) - getX(point1))) / (getY(point2) - getY(point1)));
    }

    public static float getYLine(float[] point1, float[] point2, float f) {
        return getY(point1) + (((f - getX(point1)) * (getY(point2) - getY(point1))) / (getX(point2) - getX(point1)));
    }

    /* --- point --- */

    public static float getX(float[] point) {
        if (!(point.length == 0)) {
            return point[0];
        }
        return 0.0f;
    }

    public static float getY(float[] point) {
        if (point.length > 1) {
            return point[1];
        }
        return 0.0f;
    }

    public static boolean checkTouchPoint(float[] points, float x, float y, float f3) {
        return points.length > 1 && ((float) Math.hypot(points[0] - x, points[1] - y)) < f3;
    }
}
