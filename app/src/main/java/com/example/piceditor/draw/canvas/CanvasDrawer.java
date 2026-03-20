package com.example.piceditor.draw.canvas;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import androidx.annotation.NonNull;

import com.example.piceditor.draw.drawer.Drawer;
import com.example.piceditor.draw.drawer.IDrawer;
import com.example.piceditor.draw.model.draw.DrawPath;
import com.example.piceditor.draw.model.draw.DrawPoint;
import com.example.piceditor.draw.model.draw.style.PaintStyle;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class CanvasDrawer extends Drawer<DrawPath> {

    public static final float TOUCH_TOLERANCE = 3;

    private boolean drawCurrentPath;
    private DrawPathData currentPath;
    private final List<DrawPathData> listDrawPathData;
    private final List<DrawPath> listDraw;
    private final ArrayDeque<DrawPath> listRedo;
    private final ArrayDeque<DrawPath> listUndo;

    public CanvasDrawer(IDrawer.Callback<DrawPath> callback) {
        super(callback);
        listDrawPathData = new ArrayList<>();
        listDraw = new ArrayList<>();
        listRedo = new ArrayDeque<>();
        listUndo = new ArrayDeque<>();
    }

    @Override
    public void invalidateUndoRedo(long lastTimeClearPath) {
        listUndo.removeIf(drawPath -> drawPath.getTimeCreated() < lastTimeClearPath);
        listRedo.removeIf(drawPath -> drawPath.getTimeCreated() < lastTimeClearPath);
    }

    @Override
    public long getLastTimeUndo() {
        DrawPath last = listUndo.peekLast();
        if (last != null) {
            return last.getTimeCreated();
        }
        return 0L;
    }

    @Override
    public long getLastTimeRedo() {
        DrawPath last = listRedo.peekLast();
        if (last != null) {
            return last.getTimeCreated();
        }
        return 0L;
    }

    @Override
    public void undo() {
        DrawPath drawPath = listUndo.pollLast();
        if (drawPath != null) {
            listRedo.addLast(drawPath);
            getCallback().removeData(drawPath);
        }
    }

    @Override
    public void redo() {
        DrawPath drawPath = listRedo.pollLast();
        if (drawPath != null) {
            listUndo.addLast(drawPath);
            getCallback().addData(drawPath);
        }
    }

    @Override
    public void clearRedo() {
        listRedo.clear();
    }

    @Override
    public void clearUndoRedo() {
        listUndo.clear();
        listRedo.clear();
    }

    @Override
    public void setCurrentData(DrawPath currentData) {
        this.currentPath = new DrawPathData(currentData);
    }

    @Override
    public void setData(List<DrawPath> data) {
        listDraw.clear();
        listDraw.addAll(data);
        updateAllPath();
    }

    @Override
    public boolean interceptTouchEvent(float x, float y, int action) {
        return currentPath != null;
    }

    @Override
    public void onTouch(float x, float y, int action, long time) {
        if (currentPath == null) {
            return;
        }
        if (action == MotionEvent.ACTION_DOWN) {
            drawCurrentPath = true;
        }
        currentPath.onTouch(x, y, action);
        if (action == MotionEvent.ACTION_UP) {
            drawCurrentPath = false;
            currentPath.drawPath.setTimeCreated(System.currentTimeMillis());
            listUndo.add(currentPath.drawPath);
            listRedo.clear();
            getCallback().onClearRedo();
            getCallback().addData(currentPath.drawPath);

            // reset current data
            setCurrentData(currentPath.drawPath.copy());
        }
        getCallback().updateView();
    }

    @Override
    public void onDraw(Canvas canvas) {
        for (DrawPathData dataPathDraw : this.listDrawPathData) {
            drawPath(canvas, dataPathDraw);
        }
        if (drawCurrentPath) {
            drawPath(canvas, this.currentPath);
        }
    }

    public boolean isErase() {
        return currentPath != null && currentPath.drawPath.getPaintStyle() == PaintStyle.ERASE;
    }

    private void drawPath(Canvas canvas, DrawPathData dataPathDraw) {
        if (dataPathDraw == null) {
            return;
        }
        dataPathDraw.onDraw(canvas);
    }

    private void updateAllPath() {
        List<DrawPathData> pathData = new ArrayList<>();
        for (DrawPath drawPath : listDraw) {
            pathData.add(getListPathByData(drawPath));
        }
        listDrawPathData.clear();
        listDrawPathData.addAll(pathData);
    }

    private DrawPathData getListPathByData(DrawPath drawPath) {
        for (DrawPathData dataPathDraw : this.listDrawPathData) {
            if (dataPathDraw.drawPath.getTimeCreated() == drawPath.getTimeCreated()) {
                return dataPathDraw;
            }
        }
        return new DrawPathData(drawPath);
    }

    private static class DrawPathData {

        private final DrawPath drawPath;
        private final Path path;
        private boolean dirtyPath;
        private float lastX;
        private float lastY;

        public DrawPathData(DrawPath drawPath) {
            this(drawPath, new Path());
        }

        public DrawPathData(DrawPath drawPath, Path path) {
            this.drawPath = drawPath;
            this.path = path;
            this.dirtyPath = true;
        }

        public void onDraw(@NonNull Canvas canvas) {
            Paint paint = drawPath.getPaintStyle().getPaint();
            paint.setColor(drawPath.getColor());
            paint.setStrokeWidth(drawPath.getSize());
            if (dirtyPath) {
                dirtyPath = false;
                drawPath.makePath(path);
            }
            canvas.drawPath(path, paint);
        }

        public void onTouch(float x, float y, int action) {
            List<DrawPoint> drawPoints = drawPath.getDrawPoints();
            if (drawPoints.isEmpty()) {
                drawPoints.add(new DrawPoint(x, y));
                drawPoints.add(new DrawPoint(x, y));
            } else if (drawPoints.size() == 1) {
                drawPoints.add(new DrawPoint(x, y));
            }
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    drawPoints.get(0).setX(x);
                    drawPoints.get(0).setY(y);
                    drawPoints.get(1).setX(x);
                    drawPoints.get(1).setY(y);
                    lastX = x;
                    lastY = y;
                    break;
                case MotionEvent.ACTION_MOVE:
                    float dx = Math.abs(x - lastX);
                    float dy = Math.abs(y - lastY);
                    if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                        if (drawPath.getBrushStyle().isGesture()) {
                            drawPoints.add(new DrawPoint(x, y));
                        } else {
                            drawPoints.get(1).setX(x);
                            drawPoints.get(1).setY(y);
                        }
                        lastX = x;
                        lastY = y;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (drawPath.getBrushStyle().isGesture()) {
                        drawPoints.add(new DrawPoint(x, y));
                    } else {
                        drawPoints.get(1).setX(x);
                        drawPoints.get(1).setY(y);
                    }
                    break;
                default:
                    break;
            }
            dirtyPath = true;
        }
    }

}
