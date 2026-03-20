package com.example.piceditor.draw;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.MotionEvent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.example.piceditor.draw.canvas.CanvasDrawer;
import com.example.piceditor.draw.drawer.IDrawer;
import com.example.piceditor.draw.model.DrawData;
import com.example.piceditor.draw.model.draw.DrawPath;
import com.example.piceditor.draw.model.sticker.StickerData;
import com.example.piceditor.draw.sticker.StickerDrawer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DrawerManager {

    private final UpdateHandler updateHandler;
    private final Matrix interMatrix;
    private final Matrix matrix;
    private final float standardHeight;
    private final float standardWidth;
    private final float[] points;
    private final DrawData currentData;
    private final CanvasDrawer canvasDrawer;
    private final StickerDrawer stickerDrawer;

    private boolean ignoreMotionEventTillDown;
    private boolean isTouchDownSticker;

    public DrawerManager() {
        this(null);
    }

    public DrawerManager(Runnable update) {
        this.updateHandler = new UpdateHandler();
        this.updateHandler.setUpdateViewListener(update);
        this.matrix = new Matrix();
        this.interMatrix = new Matrix();
        this.standardHeight = 2160f;
        this.standardWidth = 1080f;
        this.points = new float[2];

        currentData = new DrawData();
        canvasDrawer = new CanvasDrawer(new IDrawer.Callback<DrawPath>() {

            @Override
            public void onClearRedo() {
                stickerDrawer.clearRedo();
            }

            @Override
            public void addData(DrawPath drawPath) {
                List<DrawPath> currentData = DrawerManager.this.currentData.getListPath();
                currentData.add(drawPath);
                canvasDrawer.setData(currentData);
                update();
            }

            @Override
            public void removeData(DrawPath drawPath) {
                currentData.getListPath().removeIf(drawPathData -> drawPathData.getTimeCreated() == drawPath.getTimeCreated());
                canvasDrawer.setData(currentData.getListPath());
                update();
            }

            @Override
            public void updateData(DrawPath drawPath) {
                for (DrawPath d : currentData.getListPath()) {
                    if (d.getTimeCreated() == drawPath.getTimeCreated()) {
                        d.setData(drawPath);
                        update();
                        break;
                    }
                }
            }

            @Override
            public void updateView() {
                updateHandler.sendUpdateViewImmediate();
            }
        });
        stickerDrawer = new StickerDrawer(standardWidth, standardHeight, new IDrawer.Callback<StickerData>() {
            @Override
            public void onClearRedo() {
                canvasDrawer.clearRedo();
            }

            @Override
            public void addData(StickerData sticker) {
                List<StickerData> currentData = DrawerManager.this.currentData.getListSticker();
                currentData.add(sticker);
                stickerDrawer.setData(currentData);
                update();
            }

            @Override
            public void removeData(StickerData sticker) {
                currentData.getListSticker().removeIf(stickerData -> stickerData.getTimeCreated() == sticker.getTimeCreated());
                stickerDrawer.setData(currentData.getListSticker());
                update();
            }

            @Override
            public void updateData(StickerData sticker) {
                for (StickerData d : currentData.getListSticker()) {
                    if (d.getTimeCreated() == sticker.getTimeCreated()) {
                        d.setData(sticker);
                        update();
                        break;
                    }
                }
            }

            @Override
            public void addData(int index, StickerData stickerData) {
                List<StickerData> currentData = DrawerManager.this.currentData.getListSticker();
                currentData.add(index, stickerData);
                stickerDrawer.setData(currentData);
                update();
            }

            @Override
            public void swapData(int i1, int i2) {
                int size = DrawerManager.this.currentData.getListSticker().size();
                if (i1 < 0 || i1 >= size || i2 < 0 || i2 >= size) {
                    return; // invalid indices
                }
                List<StickerData> currentData = DrawerManager.this.currentData.getListSticker();
                Collections.swap(currentData, i1, i2);
                stickerDrawer.setData(currentData);
                update();
            }

            @Override
            public void onStickerFocusChange(@Nullable StickerData stickerData) {
                updateHandler.sendStickerFocusChange(stickerData);
                updateHandler.sendUpdateView();
            }

            @Override
            public void updateView() {
                updateHandler.sendUpdateViewImmediate();
            }
        });
    }

    public DrawData getData() {
        return currentData;
    }

    public synchronized void setData(DrawData drawData) {
        if (drawData == null) {
            return;
        }
        currentData.setData(drawData);
        canvasDrawer.setData(currentData.getListPath());
        stickerDrawer.setData(currentData.getListSticker());
        canvasDrawer.invalidateUndoRedo(currentData.getLastTimeClearPath());
        stickerDrawer.invalidateUndoRedo(currentData.getLastTimeClearPath());
        update(true);
    }

    public void setStickerForceHideControls(boolean hide) {
        stickerDrawer.setForceHideControls(hide);
    }

    public void setStickerSyncLoading(boolean syncLoading) {
        stickerDrawer.setSyncLoading(syncLoading);
    }

    public void setViewSize(int width, int height) {
        updateMatrix(width, height);
        update(true);
    }

    public void updateMatrix(int width, int height) {
        float f = width / this.standardWidth;
        matrix.setScale(f, f);
        matrix.postTranslate(0.0f, (height - (standardHeight * f)) / 2.0f);
        matrix.invert(this.interMatrix);
    }

    public boolean isActiveUndo() {
        return canvasDrawer.getLastTimeUndo() > 0 || stickerDrawer.getLastTimeUndo() > 0;
    }

    public boolean isActiveRedo() {
        return canvasDrawer.getLastTimeRedo() > 0 || stickerDrawer.getLastTimeRedo() > 0;
    }

    public void undo() {
        long lastTimeCanvasUndo = canvasDrawer.getLastTimeUndo();
        long lastTimeStickerUndo = stickerDrawer.getLastTimeUndo();
        if (lastTimeCanvasUndo == 0 || lastTimeStickerUndo == 0) {
            if (lastTimeCanvasUndo != 0) {
                canvasDrawer.undo();
            }
            if (lastTimeStickerUndo != 0) {
                stickerDrawer.undo();
            }
        } else {
            if (lastTimeCanvasUndo > lastTimeStickerUndo) {
                canvasDrawer.undo();
            } else {
                stickerDrawer.undo();
            }
        }
    }

    public void redo() {
        long lastTimeCanvasRedo = canvasDrawer.getLastTimeRedo();
        long lastTimeStickerRedo = stickerDrawer.getLastTimeRedo();
        if (lastTimeCanvasRedo == 0 || lastTimeStickerRedo == 0) {
            if (lastTimeCanvasRedo != 0) {
                canvasDrawer.redo();
            }
            if (lastTimeStickerRedo != 0) {
                stickerDrawer.redo();
            }
        } else {
            if (lastTimeCanvasRedo < lastTimeStickerRedo) {
                canvasDrawer.redo();
            } else {
                stickerDrawer.redo();
            }
        }
    }

    public void clear() {
        long timestamp = System.currentTimeMillis();
        currentData.setLastTimeClearPath(timestamp);
        currentData.getListPath().clear();
        currentData.getListSticker().clear();
        canvasDrawer.setData(currentData.getListPath());
        stickerDrawer.setData(currentData.getListSticker());
        canvasDrawer.clearUndoRedo();
        stickerDrawer.clearUndoRedo();
        update();
    }

    public void addDrawInteractListener(DrawInteractListener listener) {
        updateHandler.addDrawInteractListener(listener);
    }

    public void removeDrawInteractListener(DrawInteractListener listener) {
        updateHandler.removeDrawInteractListener(listener);
    }

    public void setDrawPath(@NonNull DrawPath drawPath) {
        canvasDrawer.setCurrentData(drawPath);
    }

    public void setBackground(String url) {
        long timestamp = System.currentTimeMillis();
        currentData.setTimeSetBackground(timestamp);
        currentData.setLinkBackground(url);
        updateHandler.sendUpdateBackground(url);
    }

    public String getBackground() {
        return currentData.getLinkBackground();
    }

    public void addSticker(StickerData sticker) {
        stickerDrawer.addSticker(sticker);
    }

    public void copySticker(StickerData sticker) {
        stickerDrawer.copySticker(sticker);
    }

    public void removeSticker(StickerData sticker) {
        stickerDrawer.removeSticker(sticker);
    }

    public CanvasDrawer getCanvasDrawer() {
        return canvasDrawer;
    }

    public StickerDrawer getStickerDrawer() {
        return stickerDrawer;
    }

    public Bitmap exportBitmap(Context context, int width, int height, float cornerRadius) {
        // obtain background
        Bitmap backgroundLayer;
        try {
            backgroundLayer = Glide.with(context).asBitmap()
                    .load(currentData.getLinkBackground())
                    .centerCrop()
                    .submit(width, height)
                    .get();
        } catch (Exception e) {
            backgroundLayer = null;
        }

        // obtain foreground
        Bitmap foregroundLayer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas drawCanvas = new Canvas(foregroundLayer);
        updateMatrix(width, height);
        onDraw(drawCanvas);

        // create final bitmap
        Bitmap finalBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(finalBitmap);

        // clip corner rect
        Path path = new Path();
        path.addRoundRect(new RectF(0, 0, width, height), cornerRadius, cornerRadius, Path.Direction.CW);
        canvas.clipPath(path);

        // draw background
        if (backgroundLayer != null) {
            canvas.drawBitmap(backgroundLayer, 0.0f, 0.0f, null);
        }
        // draw foreground
        canvas.drawBitmap(foregroundLayer, 0.0f, 0.0f, null);

        return finalBitmap;
    }

    /* package */ void onDraw(@NonNull Canvas canvas) {
        int count = canvas.save();
        canvas.setMatrix(matrix);
        canvasDrawer.onDraw(canvas);
        stickerDrawer.onDraw(canvas);
        canvas.restoreToCount(count);
    }

    public boolean onTouch(@NonNull MotionEvent event) {
        int action = event.getAction();
        points[0] = event.getX();
        points[1] = event.getY();
        interMatrix.mapPoints(points);

        if (action == MotionEvent.ACTION_DOWN) {
            isTouchDownSticker = stickerDrawer.interceptTouchEvent(points[0], points[1], action);
            ignoreMotionEventTillDown = !isTouchDownSticker && canvasDrawer.isErase() && currentData.getListPath().isEmpty();
        }

        if (ignoreMotionEventTillDown) {
            return false;
        }

        if (action == MotionEvent.ACTION_DOWN) {
            updateHandler.sendTouchDown();
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            updateHandler.sendTouchUp();
        }

        if (isTouchDownSticker) {
            stickerDrawer.onTouch(points[0], points[1], action, event.getEventTime() - event.getDownTime());
            return true; // ✅ đang chạm sticker
        } else {
            canvasDrawer.onTouch(points[0], points[1], action, 0);
            return false; // ✅ không phải sticker → cho view dưới xử lý
        }
    }

    private void update() {
        update(false);
    }

    private void update(boolean updateBackground) {
        updateHandler.sendUpdateView();
        updateHandler.sendUndoRedoChange();
        if (updateBackground) {
            updateHandler.sendUpdateBackground(currentData.getLinkBackground());
        } else {
            currentData.setTimeUpdate(System.currentTimeMillis());
        }
    }

    private static class UpdateHandler extends Handler {

        private static final int MESSAGE_UPDATE_VIEW = 456217;
        private static final int MESSAGE_UPDATE_BACKGROUND = 356784;
        private static final int MESSAGE_ON_UNDO_REDO_CHANGE = 231589;
        private static final int MESSAGE_ON_TOUCH_DOWN = 129763;
        private static final int MESSAGE_ON_TOUCH_UP = 640139;
        private static final int MESSAGE_ON_STICKER_FOCUS_CHANGE = 785139;

        private Runnable updateViewListener;
        private final List<DrawInteractListener> drawInteractListeners;

        private UpdateHandler() {
            super(Looper.getMainLooper());
            drawInteractListeners = new ArrayList<>();
        }

        public void setUpdateViewListener(Runnable updateViewListener) {
            this.updateViewListener = updateViewListener;
        }

        public void addDrawInteractListener(DrawInteractListener drawInteractListener) {
            drawInteractListeners.add(drawInteractListener);
        }

        public void removeDrawInteractListener(DrawInteractListener drawInteractListener) {
            drawInteractListeners.remove(drawInteractListener);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == MESSAGE_UPDATE_VIEW) {
                if (updateViewListener != null) {
                    updateViewListener.run();
                }
                return;
            }
            if (drawInteractListeners.isEmpty()) {
                return;
            }
            switch (msg.what) {
                case MESSAGE_ON_UNDO_REDO_CHANGE:
                    for (DrawInteractListener drawInteractListener : drawInteractListeners) {
                        drawInteractListener.interactUndoRedoChange();
                    }
                    break;
                case MESSAGE_UPDATE_BACKGROUND:
                    if (msg.obj instanceof String) {
                        for (DrawInteractListener drawInteractListener : drawInteractListeners) {
                            drawInteractListener.interactUpdateBackground((String) msg.obj);
                        }
                    } else {
                        for (DrawInteractListener drawInteractListener : drawInteractListeners) {
                            drawInteractListener.interactUpdateBackground(null);
                        }
                    }
                    break;
                case MESSAGE_ON_TOUCH_DOWN:
                    for (DrawInteractListener drawInteractListener : drawInteractListeners) {
                        drawInteractListener.interactTouchDown();
                    }
                    break;
                case MESSAGE_ON_TOUCH_UP:
                    for (DrawInteractListener drawInteractListener : drawInteractListeners) {
                        drawInteractListener.interactTouchUp();
                    }
                    break;
                case MESSAGE_ON_STICKER_FOCUS_CHANGE:
                    if (msg.obj instanceof StickerData) {
                        for (DrawInteractListener drawInteractListener : drawInteractListeners) {
                            drawInteractListener.interactStickerFocusChange((StickerData) msg.obj);
                        }
                    } else {
                        for (DrawInteractListener drawInteractListener : drawInteractListeners) {
                            drawInteractListener.interactStickerFocusChange(null);
                        }
                    }
                    break;
            }
        }

        public void sendUpdateViewImmediate() {
            sendMessage(obtainMessage(MESSAGE_UPDATE_VIEW));
        }

        public void sendUpdateView() {
            sendDelay(obtainMessage(MESSAGE_UPDATE_VIEW), 50);
        }

        public void sendUpdateBackground(String url) {
            sendDelay(obtainMessage(MESSAGE_UPDATE_BACKGROUND, url), 50);
        }

        public void sendUndoRedoChange() {
            sendDelay(obtainMessage(MESSAGE_ON_UNDO_REDO_CHANGE), 50);
        }

        public void sendTouchDown() {
            sendDelay(obtainMessage(MESSAGE_ON_TOUCH_DOWN), 10);
        }

        public void sendTouchUp() {
            sendDelay(obtainMessage(MESSAGE_ON_TOUCH_UP), 25);
        }

        public void sendStickerFocusChange(StickerData stickerData) {
            sendDelay(obtainMessage(MESSAGE_ON_STICKER_FOCUS_CHANGE, stickerData), 25);
        }

        private void sendDelay(@NonNull Message message, long delay) {
            removeMessages(message.what);
            sendMessageDelayed(message, delay);
        }
    }
}
