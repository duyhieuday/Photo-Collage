package com.example.piceditor.draw.sticker;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import androidx.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.example.piceditor.WeatherApplication;
import com.example.piceditor.draw.ScreenUtils;
import com.example.piceditor.draw.model.sticker.StickerData;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class StickerImage {

    public interface OnCopySticker {
        void onCopy(StickerData data);
    }

    public interface OnRemoveSticker {
        void onRemove(StickerData data);
    }

    public interface OnUpdateSticker {
        void onUpdate(StickerData lastData, StickerData newData);
    }

    private final OnCopySticker copyData;
    private final OnRemoveSticker removeData;
    private final OnUpdateSticker updateData;
    private final Runnable updateView;
    private StickerData data;
    private StickerData lastData;

    private final Matrix matrix;
    private final Matrix matrixInvert;
    private final Matrix matrixBitmap;
    private final ImageMatrix imageMatrix;

    private final PointF pointDown;
    private final PointF centerDown;
    private final RectF rectBorder;
    private final RectF rectDst;
    private final Rect rectScr;
    private final float padding;
    private float scaleDown;
    private float lastDegrees;
    private float viewHeight;
    private float viewWidth;
    private TypeTouch typeTouch;
    private final Paint paintBitmap;
    private Bitmap bitmap;
    private Disposable loadImageDisposable;

    public StickerImage(StickerData data, boolean syncLoading,
                        OnCopySticker copyData,
                        OnRemoveSticker removeData,
                        OnUpdateSticker updateData,
                        Runnable updateView) {
        this.data = data;
        this.copyData = copyData;
        this.removeData = removeData;
        this.updateData = updateData;
        this.updateView = updateView;
        this.rectScr = new Rect();
        this.rectDst = new RectF();
        this.rectBorder = new RectF();
        this.padding = ScreenUtils.dp2px(4.0f);
        this.pointDown = new PointF();
        this.centerDown = new PointF();
        this.scaleDown = 1.0f;
        this.typeTouch = TypeTouch.NONE;
        this.matrix = new Matrix();
        this.matrixInvert = new Matrix();
        this.matrixBitmap = new Matrix();
        this.imageMatrix = new ImageMatrix();
        this.paintBitmap = new Paint(1);
        if (syncLoading) {
            loadImage();
        } else {
            loadImageRx();
        }
    }

    public StickerData getData() {
        return this.data;
    }

    public void setData(@NonNull StickerData stickerData) {
        this.data = stickerData;
    }

    public void updateData(StickerData sticker) {
        this.data.setData(sticker);
    }

    public void setViewSize(float width, float height) {
        this.viewWidth = width;
        this.viewHeight = height;
    }

    public void setMatrix(Canvas canvas) {
        canvas.rotate(this.data.getRotate(), this.data.getCenterX(this.viewWidth), this.data.getCenterY(this.viewHeight));
    }

    public RectF getBorder() {
        return this.rectBorder;
    }

    public void draw(Canvas canvas) {
        Bitmap bitmap = this.bitmap;
        if (bitmap != null) {
            updateRect();
            updatePaint();
            updateState();
            canvas.save();
            canvas.concat(this.matrixBitmap);
            canvas.drawBitmap(bitmap, this.rectScr, this.rectDst, this.paintBitmap);
            canvas.restore();
        }
    }

    private void updateState() {
        int flipX, flipY;
        fl:
        {
            if (this.data.isFlipHorizontal() && this.data.isFlipVertical()) {
                flipX = flipY = -1;
                break fl;
            }
            if (this.data.isFlipHorizontal()) {
                flipX = -1;
                flipY = 1;
                break fl;
            }
            if (this.data.isFlipVertical()) {
                flipX = 1;
                flipY = -1;
                break fl;
            }
            flipX = flipY = 1;
        }
        float centerX = this.data.getCenterX(this.viewWidth);
        float centerY = this.data.getCenterY(this.viewHeight);
        matrixBitmap.reset();
        matrixBitmap.postScale(flipX, flipY, centerX, centerY);
        matrixBitmap.postRotate(this.data.getRotate(), centerX, centerY);
    }

    private void updatePaint() {
        this.imageMatrix.setBrightness(this.data.getBrightness());
        this.imageMatrix.setSaturation(this.data.getSaturation());
        this.imageMatrix.setContrast(this.data.getContrast());
        this.imageMatrix.setWarmth(this.data.getWarmth());

        this.paintBitmap.setColorFilter(this.imageMatrix.updateMatrixIfNeed());
        this.paintBitmap.setAlpha((int) (this.data.getOpacity() * 255));
    }

    private void updateRect() {
        float scalePercentWidth = this.data.getScalePercentWidth() * this.viewWidth;
        float height = (this.rectScr.width() <= 0 || this.rectScr.height() <= 0) ? 0.0f : (this.rectScr.height() * scalePercentWidth) / this.rectScr.width();
        float halfWidth = scalePercentWidth / 2.0f;
        float halfHeight = height / 2.0f;
        this.rectDst.set(
                this.data.getCenterX(this.viewWidth) - halfWidth,
                this.data.getCenterY(this.viewHeight) - halfHeight,
                this.data.getCenterX(this.viewWidth) + halfWidth,
                this.data.getCenterY(this.viewHeight) + halfHeight
        );
        this.rectBorder.set(
                (this.data.getCenterX(this.viewWidth) - halfWidth) - this.padding,
                (this.data.getCenterY(this.viewHeight) - halfHeight) - this.padding,
                this.data.getCenterX(this.viewWidth) + halfWidth + this.padding,
                this.data.getCenterY(this.viewHeight) + halfHeight + this.padding
        );
    }

    public boolean interceptTouch(float x, float y) {
        updateRect();
        float[] mapPointInvert = mapPointInvert(x, y);
        return this.rectDst.contains(mapPointInvert[0], mapPointInvert[1])
                || checkRotate(x, y)
                || checkScale(x, y)
                || checkCopy(x, y)
                || checkRemove(x, y);
    }

    public void touch(float x, float y, int action, long time) {
        if (action == MotionEvent.ACTION_DOWN) {
            lastData = data.copy();
            touchDown(x, y);
            return;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            touchMove(x, y);
            return;
        }
        if (action == MotionEvent.ACTION_UP) {
            TypeTouch ltt = typeTouch;
            typeTouch = TypeTouch.NONE;
            data.setTimeUpdated(System.currentTimeMillis());
            if (time < 150) {
                if (ltt == TypeTouch.REMOVE) {
                    removeData.onRemove(data);
                }
                if (ltt == TypeTouch.COPY) {
                    copyData.onCopy(data);
                }
            }
            if (ltt != TypeTouch.NONE) {
                StickerData lastData = this.lastData;
                this.lastData = null;
                if (lastData == null || lastData.isSameData(data)) {
                    return;
                }
                updateData.onUpdate(lastData, data);
            }
        }
    }

    private void touchDown(float x, float y) {
        updateRect();
        this.pointDown.set(x, y);
        if (checkTouchSticker(x, y)) {
            this.centerDown.set(this.data.getCenterX(this.viewWidth), this.data.getCenterY(this.viewHeight));
            this.typeTouch = TypeTouch.MOVE;
        }
        if (checkRotate(x, y)) {
            this.lastDegrees = CoordinatesUtils.calculateAngle(this.data.getCenterX(this.viewWidth), this.data.getCenterY(this.viewHeight), x, y);
            this.typeTouch = TypeTouch.ROTATE;
        }
        if (checkScale(x, y)) {
            this.scaleDown = this.data.getScalePercentWidth();
            this.typeTouch = TypeTouch.SCALE;
        }
        if (checkCopy(x, y)) {
            this.typeTouch = TypeTouch.COPY;
        }
        if (checkRemove(x, y)) {
            this.typeTouch = TypeTouch.REMOVE;
        }
    }

    private void touchMove(float x, float y) {
        this.matrix.setRotate(this.data.getRotate(), this.data.getCenterX(this.viewWidth), this.data.getCenterY(this.viewHeight));
        this.matrix.invert(this.matrixInvert);
        switch (typeTouch) {
            // @formatter:off
            case MOVE:          moveSticker(x, y);          break;
            case ROTATE:        rotateSticker(x, y);        break;
            case SCALE:         scaleSticker(x, y);         break;
            case SCALE_ROTATE:  scaleRotateSticker(x, y);   break;
            default: return;
            // @formatter:on
        }
        this.updateView.run();
    }

    private void scaleRotateSticker(float x, float y) {
        scaleSticker(x, y);
        rotateSticker(x, y);
    }

    private void scaleSticker(float x, float y) {
        this.data.setScalePercentWidth(this.scaleDown * CoordinatesUtils.calculateScale(this.data.getCenterX(this.viewWidth), this.data.getCenterY(this.viewHeight), this.pointDown.x, this.pointDown.y, x, y));
        if (this.data.getScalePercentWidth() < 0.1f) {
            this.data.setScalePercentWidth(0.1f);
        }
    }

    private void rotateSticker(float x, float y) {
        float calculateAngle = CoordinatesUtils.calculateAngle(this.data.getCenterX(this.viewWidth), this.data.getCenterY(this.viewHeight), x, y);
        StickerData stickerData = this.data;
        stickerData.setRotate(stickerData.getRotate() + (this.lastDegrees - calculateAngle));
        this.lastDegrees = calculateAngle;
    }

    private void moveSticker(float x, float y) {
        this.data.setCenterX(this.centerDown.x + (x - this.pointDown.x), this.viewWidth);
        this.data.setCenterY(this.centerDown.y + (y - this.pointDown.y), this.viewHeight);
    }

    private float[] mapPointInvert(float x, float y) {
        float[] points = {x, y};
        this.matrixInvert.mapPoints(points);
        return points;
    }

    private float[] mapPoint(float x, float y) {
        float[] points = {x, y};
        this.matrix.mapPoints(points);
        return points;
    }

    private boolean checkRotate(float x, float y) {
        return CoordinatesUtils.checkTouchPoint(mapPoint(rectBorder.right, rectBorder.top), x, y, 50.0f);
    }

    private boolean checkScale(float x, float y) {
        return CoordinatesUtils.checkTouchPoint(mapPoint(rectBorder.right, rectBorder.bottom), x, y, 50.0f);
    }

    private boolean checkScaleRotate(float x, float y) {
        return CoordinatesUtils.checkTouchPoint(mapPoint(rectBorder.left, rectBorder.bottom), x, y, 50.0f);
    }

    private boolean checkCopy(float x, float y) {
        return CoordinatesUtils.checkTouchPoint(mapPoint(rectBorder.left, rectBorder.bottom), x, y, 50.0f);
    }

    private boolean checkRemove(float x, float y) {
        return CoordinatesUtils.checkTouchPoint(mapPoint(rectBorder.left, rectBorder.top), x, y, 50.0f);
    }

    public boolean checkTouchSticker(float x, float y) {
        float[] mapPointInvert = mapPointInvert(x, y);
        return this.rectDst.contains(mapPointInvert[0], mapPointInvert[1]);
    }

    public void loadImageRx() {
        if (loadImageDisposable != null) {
            loadImageDisposable.dispose();
        }
        loadImageDisposable = Single.fromCallable(this::loadImage)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((success, throwable) -> {
                    if (success) {
                        updateView.run();
                    }
                    loadImageDisposable = null;
                });
    }

    @NonNull
    public Boolean loadImage() {
        try {
            bitmap = Glide.with(WeatherApplication.getInstance()).asBitmap().load(data.getPathImage()).submit().get();
            if (bitmap != null) {
                rectScr.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
                return true;
            }
        } catch (Throwable ignored) {
            bitmap = null;
        }
        return false;
    }

}
