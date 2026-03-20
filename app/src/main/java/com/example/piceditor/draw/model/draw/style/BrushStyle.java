package com.example.piceditor.draw.model.draw.style;

import android.graphics.Path;
import android.graphics.RectF;
import androidx.annotation.NonNull;

import com.example.piceditor.draw.canvas.CanvasDrawer;
import com.example.piceditor.draw.model.draw.DrawPoint;

import java.util.List;
import java.util.Optional;

public enum BrushStyle {

    GESTURE(7892),
    LINE(3801),
    OVAL(1523),
    RECT(9174),
    HEART(2736),
    MOON(4082),
    TRIANGLE(5691),
    RHOMBUS(6310),
    PENTAGON(8425),
    HEXAGON(1307),
    HEPTAGON(9268),
    OCTAGON(2154),
    STAR4(5837),
    STAR5(4906),
    STAR6(3612),
    STAR7(7059),
    STAR8(1980),
    STAR9(6743),
    STAR10(2481),
    ;

    public final int value;
    private final ThreadLocal<RectF> mBounds = new ThreadLocal<RectF>() {
        protected @NonNull RectF initialValue() {
            return new RectF();
        }
    };

    BrushStyle(int value) {
        this.value = value;
    }

    public boolean isGesture() {
        return this == GESTURE;
    }

    public void makePath(@NonNull Path path, List<DrawPoint> points) {
        path.reset();
        if (points == null || points.isEmpty()) {
            return;
        }
        if (this == GESTURE) {
            DrawHelper.makeGesture(path, points);
            return;
        }
        if (points.size() < 2) {
            return;
        }
        RectF bounds = Optional.ofNullable(mBounds.get()).orElseGet(RectF::new);
        bounds.set(
                points.get(0).getX(),
                points.get(0).getY(),
                points.get(1).getX(),
                points.get(1).getY()
        );
        switch (this) {
            // @formatter:off
            case LINE:      DrawHelper.makeLine(path, bounds);                      break;
            case OVAL:      DrawHelper.makeOval(path, bounds);                      break;
            case RECT:      DrawHelper.makeRect(path, bounds);                      break;
            case HEART:     DrawHelper.makeHeart(path, bounds);                     break;
            case MOON:      DrawHelper.makeMoon(path, bounds);                      break;
            case TRIANGLE:  DrawHelper.makeShape(path, bounds, 3, - Math.PI / 2);   break;
            case RHOMBUS:   DrawHelper.makeShape(path, bounds, 4, 0);               break;
            case PENTAGON:  DrawHelper.makeShape(path, bounds, 5, - Math.PI / 2);   break;
            case HEXAGON:   DrawHelper.makeShape(path, bounds, 6, 0);               break;
            case HEPTAGON:  DrawHelper.makeShape(path, bounds, 7, - Math.PI / 2);   break;
            case OCTAGON:   DrawHelper.makeShape(path, bounds, 8, - Math.PI / 8);   break;
            case STAR4:     DrawHelper.makeStar(path, bounds, 4);                   break;
            case STAR5:     DrawHelper.makeStar(path, bounds, 5);                   break;
            case STAR6:     DrawHelper.makeStar(path, bounds, 6);                   break;
            case STAR7:     DrawHelper.makeStar(path, bounds, 7);                   break;
            case STAR8:     DrawHelper.makeStar(path, bounds, 8);                   break;
            case STAR9:     DrawHelper.makeStar(path, bounds, 9);                   break;
            case STAR10:    DrawHelper.makeStar(path, bounds, 10);                  break;
            // @formatter:on
        }
    }

    private static class DrawHelper {

        static void makeGesture(Path path, List<DrawPoint> points) {
            if (points == null || points.isEmpty()) {
                return;
            }
            float mX = 0;
            float mY = 0;
            for (int i = 0; i < points.size(); i++) {
                DrawPoint point = points.get(i);
                float x = point.getX();
                float y = point.getY();
                if (i == 0) {
                    path.moveTo(x, y);
                    path.lineTo(x, y);
                    mX = x;
                    mY = y;
                } else {
                    float dx = Math.abs(x - mX);
                    float dy = Math.abs(y - mY);
                    if (dx >= CanvasDrawer.TOUCH_TOLERANCE || dy >= CanvasDrawer.TOUCH_TOLERANCE) {
                        path.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
                        mX = x;
                        mY = y;
                    }
                }
            }
        }

        static void makeLine(Path path, RectF bounds) {
            path.moveTo(bounds.left, bounds.top);
            path.lineTo(bounds.right, bounds.bottom);
        }

        static void makeOval(Path path, RectF bounds) {
            path.addOval(bounds, Path.Direction.CW);
        }

        static void makeRect(Path path, RectF bounds) {
            path.addRect(bounds, Path.Direction.CW);
        }

        static void makeShape(Path path, RectF bounds, int edgeCount, double rotateAngle) {
            float centerX = bounds.left;
            float centerY = bounds.top;
            bounds.sort();
            float increment = (float) (Math.PI * 2 / edgeCount);
            float radius = (bounds.width() + bounds.height()) / 2f;

            for (int i = 0; i < edgeCount; i++) {
                double angle = i * increment + rotateAngle;

                float x = (float) (centerX + radius * Math.cos(angle));
                float y = (float) (centerY + radius * Math.sin(angle));

                if (i == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }
            path.close();
        }

        static void makeStar(Path path, RectF bounds, int edgeCount) {
            float x = bounds.left;
            float y = bounds.top;
            bounds.sort();
            float increment = (float) (Math.PI * 2 / edgeCount);
            float radius = (bounds.width() + bounds.height()) / 2f;
            float innerRadius = radius * 0.38196601125f;

            for (int i = 0; i < edgeCount; i++) {
                float outerAngle = (float) (increment * i - Math.PI / 2); // Outer vertices angle
                float innerAngle = (float) (increment * i + Math.PI / edgeCount - Math.PI / 2); // Inner vertices angle

                float x1 = (float) (x + radius * Math.cos(outerAngle));
                float y1 = (float) (y + radius * Math.sin(outerAngle));
                float x2 = (float) (x + innerRadius * Math.cos(innerAngle));
                float y2 = (float) (y + innerRadius * Math.sin(innerAngle));
                if (i == 0) {
                    path.moveTo(x1, y1);
                } else {
                    path.lineTo(x1, y1);
                }
                path.lineTo(x2, y2);
            }
            path.close();
        }

        static void makeMoon(Path path, RectF bounds) {
            bounds.sort();
            float cx = bounds.centerX();
            float cy = bounds.centerY();
            float radius = (bounds.width() + bounds.height()) / 4f;
            float innerRadius = radius * 0.75f;
            float innerOffset = radius * 0.25f;

            Path cachedPath = getCachedPath();
            cachedPath.reset();
            cachedPath.addCircle(cx + innerOffset, cy - innerOffset, innerRadius, Path.Direction.CW);
            path.addCircle(cx, cy, radius, Path.Direction.CW);
            path.op(cachedPath, Path.Op.DIFFERENCE);
        }

        static void makeHeart(Path path, RectF bounds) {
            bounds.sort();
            float l = bounds.left;
            float t = bounds.top;
            float r = bounds.right;
            float b = bounds.bottom;
            float w = r - l;
            float h = b - t;

            float pX = l + w * 0.5f;
            float pY = t + h * 0.3333f;
            float x1, y1, x2, y2, x3, y3;

            // right top
            x1 = l + w * 0.5f;
            y1 = t + h * 0.05f;
            x2 = l + w * 0.9f;
            y2 = t + h * 0.1f;
            x3 = l + w * 0.9f;
            y3 = t + h * 0.3333f;

            path.moveTo(pX, pY);
            path.cubicTo(x1, y1, x2, y2, x3, y3);

            // right bot
            x1 = l + w * 0.9f;
            y1 = t + h * 0.55f;
            x2 = l + w * 0.65f;
            y2 = t + h * 0.6f;
            x3 = l + w * 0.5f;
            y3 = t + h * 0.9f;

            path.cubicTo(x1, y1, x2, y2, x3, y3);

            // left top
            x1 = l + w * 0.5f;
            y1 = t + h * 0.05f;
            x2 = l + w * 0.1f;
            y2 = t + h * 0.1f;
            x3 = l + w * 0.1f;
            y3 = t + h * 0.3333f;

            path.moveTo(pX, pY);
            path.cubicTo(x1, y1, x2, y2, x3, y3);

            // left bot
            x1 = l + w * 0.1f;
            y1 = t + h * 0.55f;
            x2 = l + w * 0.35f;
            y2 = t + h * 0.6f;
            x3 = l + w * 0.5f;
            y3 = t + h * 0.9f;

            path.cubicTo(x1, y1, x2, y2, x3, y3);
            path.moveTo(x3, y3);
            path.close();
        }


        private static final ThreadLocal<Path> sCachedPath = new ThreadLocal<Path>() {
            protected @NonNull Path initialValue() {
                return new Path();
            }
        };

        private static Path getCachedPath() {
            return Optional.ofNullable(sCachedPath.get()).orElseGet(Path::new);
        }
    }
}
