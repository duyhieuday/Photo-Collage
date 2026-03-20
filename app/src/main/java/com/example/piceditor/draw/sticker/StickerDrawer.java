package com.example.piceditor.draw.sticker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.example.piceditor.R;
import com.example.piceditor.WeatherApplication;
import com.example.piceditor.draw.ScreenUtils;
import com.example.piceditor.draw.drawer.Drawer;
import com.example.piceditor.draw.model.sticker.StickerData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class StickerDrawer extends Drawer<StickerData> {

    private final List<StickerImage> listSticker;
    private StickerImage currentSticker;
    private final ArrayDeque<ModifySticker> listRedo;
    private final ArrayDeque<ModifySticker> listUndo;
    private final Paint paint;
    private final Path pathLine;
    private final int sizeControlIcon;
    private final float standardHeight;
    private final float standardWidth;
    private boolean forceHideControls;
    private boolean syncLoading;
    private Bitmap bitmapRotate;
    private Bitmap bitmapResize;
    private Bitmap bitmapCopy;
    private Bitmap bitmapDelete;

    public StickerDrawer(float w, float h, Callback<StickerData> callback) {
        super(callback);
        this.standardWidth = w;
        this.standardHeight = h;
        this.listSticker = new ArrayList<>();
        this.listUndo = new ArrayDeque<>();
        this.listRedo = new ArrayDeque<>();
        this.sizeControlIcon = ScreenUtils.dp2px(24.0f);
        this.paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setColor(Color.parseColor("#444444"));
        this.paint.setStrokeWidth(ScreenUtils.dp2px(1.5f));
        this.paint.setPathEffect(new DashPathEffect(new float[]{ScreenUtils.dp2px(6.0f), ScreenUtils.dp2px(6.0f)}, 0.0f));
        this.pathLine = new Path();

        loadControlsImageRx();
    }

    public void setForceHideControls(boolean forceHideControls) {
        this.forceHideControls = forceHideControls;
    }

    public void setSyncLoading(boolean syncLoading) {
        this.syncLoading = syncLoading;
    }

    @Override
    public void invalidateUndoRedo(long lastTimeClearPath) {
        this.listUndo.removeIf(sticker -> sticker.nextData != null && sticker.nextData.getTimeUpdated() < lastTimeClearPath);
        this.listRedo.removeIf(sticker -> sticker.nextData != null && sticker.nextData.getTimeUpdated() < lastTimeClearPath);
    }

    @Override
    public long getLastTimeUndo() {
        ModifySticker undoRedo = this.listUndo.peekLast();
        if (undoRedo != null) {
            return undoRedo.prevData.getTimeUpdated();
        }
        return 0L;
    }

    @Override
    public long getLastTimeRedo() {
        ModifySticker undoRedo = this.listRedo.peekLast();
        if (undoRedo != null) {
            return undoRedo.nextData.getTimeUpdated();
        }
        return 0L;
    }

    @Override
    public void undo() {
        final ModifySticker removeLastOrNull = this.listUndo.pollLast();
        if (removeLastOrNull == null) {
            return;
        }
        StickerData stickerData = removeLastOrNull.prevData.copy();
        stickerData.setTimeUpdated(System.currentTimeMillis());
        this.listRedo.add(removeLastOrNull);
        if (removeLastOrNull instanceof AddSticker) {
            getCallback().removeData(stickerData);
            return;
        }
        if (removeLastOrNull instanceof RemoveSticker) {
            getCallback().addData(((RemoveSticker) removeLastOrNull).index, stickerData);
            return;
        }
        if (removeLastOrNull instanceof UpdateSticker) {
            StickerImage stickerImage = findSticker(stickerData.getTimeCreated());
            if (stickerImage != null) {
                stickerImage.updateData(stickerData);
                getCallback().updateData(stickerImage.getData());
            }
            return;
        }
        if (removeLastOrNull instanceof SwapSticker) {
            SwapSticker swapSticker = (SwapSticker) removeLastOrNull;
            getCallback().swapData(swapSticker.i1, swapSticker.i2);
        }
    }

    @Override
    public void redo() {
        final ModifySticker removeLastOrNull = this.listRedo.pollLast();
        if (removeLastOrNull == null) {
            return;
        }
        StickerData stickerData = removeLastOrNull.nextData.copy();
        stickerData.setTimeUpdated(System.currentTimeMillis());
        this.listUndo.add(removeLastOrNull);
        if (removeLastOrNull instanceof AddSticker) {
            getCallback().addData(stickerData);
            return;
        }
        if (removeLastOrNull instanceof RemoveSticker) {
            getCallback().removeData(stickerData);
            return;
        }
        if (removeLastOrNull instanceof UpdateSticker) {
            StickerImage stickerImage = findSticker(stickerData.getTimeCreated());
            if (stickerImage != null) {
                stickerImage.updateData(stickerData);
                getCallback().updateData(stickerImage.getData());
            }
            return;
        }
        if (removeLastOrNull instanceof SwapSticker) {
            SwapSticker swapSticker = (SwapSticker) removeLastOrNull;
            getCallback().swapData(swapSticker.i1, swapSticker.i2);
        }
    }

    @Override
    public void clearRedo() {
        this.listRedo.clear();
    }

    @Override
    public void clearUndoRedo() {
        this.listUndo.clear();
        this.listRedo.clear();
    }

    @Override
    public void setCurrentData(StickerData currentData) {

    }

    @Override
    public void setData(List<StickerData> data) {
        List<StickerImage> stickerImages = new ArrayList<>();
        for (StickerData stickerData : data) {
            StickerImage stickerByStickerData = findSticker(stickerData.getTimeCreated());
            if (stickerByStickerData == null) {
                StickerImage stickerImage = new StickerImage(stickerData, syncLoading, this::copyData, this::removeData, this::updateData, getCallback()::updateView);
                stickerImage.setViewSize(this.standardWidth, this.standardHeight);
                stickerByStickerData = stickerImage;
            } else if (stickerData.getTimeUpdated() > stickerByStickerData.getData().getTimeUpdated()) {
                stickerByStickerData.updateData(stickerData);
            }
            stickerImages.add(stickerByStickerData);
        }
        listSticker.clear();
        listSticker.addAll(stickerImages);
        if (!listSticker.isEmpty()) {
            StickerImage sticker = currentSticker;
            if (sticker != null) {
                sticker = findSticker(sticker.getData().getTimeCreated());
            }
            if (sticker == null) {
                sticker = listSticker.get(listSticker.size() - 1);
            }
            focusSticker(sticker);
        } else {
            focusSticker(null);
        }
    }

    @Override
    public boolean interceptTouchEvent(float x, float y, int action) {
        StickerImage findSticker = null;
        List<StickerImage> stickers = this.listSticker;
        int i = stickers.size() - 1;
        while (i >= 0) {
            StickerImage stickerImage = stickers.get(i);
            if (stickerImage.interceptTouch(x, y)) {
                findSticker = stickerImage;
                break;
            }
            i--;
        }
        focusSticker(findSticker);
        /*/
        if (currentSticker != null) {
            this.listSticker.remove(currentSticker);
            this.listSticker.add(currentSticker);
        }
        /*/
        return currentSticker != null;
    }

    @Override
    public void onTouch(float x, float y, int action, long time) {
        if (currentSticker == null) {
            return;
        }
        currentSticker.touch(x, y, action, time);
        if (action == MotionEvent.ACTION_UP) {
            if (currentSticker != null) { // must check because can be null when remove
                getCallback().updateData(currentSticker.getData());
            }
        } else {
            getCallback().updateView();
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (forceHideControls) {
            for (StickerImage stickerImage : this.listSticker) {
                stickerImage.draw(canvas);
            }
        } else {
            for (StickerImage stickerImage : this.listSticker) {
                stickerImage.draw(canvas);
                if (stickerImage == currentSticker) {
                    drawController(canvas);
                }
            }
        }
    }

    @Nullable
    public StickerImage getCurrentSticker() {
        return currentSticker;
    }

    @Nullable
    public StickerData getCurrentStickerData() {
        return currentSticker != null ? currentSticker.getData() : null;
    }

    ///////////////////////////////////////////////////////////////////////////
    // START ADJUST STICKER
    ///////////////////////////////////////////////////////////////////////////

    /* --- rotate --- */

    public void rotateLeft() {
        updateStickerData((d, v) -> d.rotateLeft());
    }

    public void rotateRight() {
        updateStickerData((d, v) -> d.rotateRight());
    }

    /* --- flip --- */

    public void flipHorizontal() {
        updateStickerData((d, v) -> d.flipHorizontal());
    }

    public void flipVertical() {
        updateStickerData((d, v) -> d.flipVertical());
    }

    /* --- translate --- */

    public void translateCenterHorizontal() {
        updateStickerData(null, true, 0.5f, StickerData::setCenterPercentX);
    }

    public void translateCenterVertical() {
        updateStickerData(null, true, 0.5f, StickerData::setCenterPercentY);
    }

    public void translateLeft(boolean commit) {
        updateStickerData("trL", commit, 0, (d, v) -> d.setCenterPercentX(d.getCenterPercentX() - 0.002f));
    }

    public void translateUp(boolean commit) {
        updateStickerData("trU", commit, 0, (d, v) -> d.setCenterPercentY(d.getCenterPercentY() - 0.002f));
    }

    public void translateRight(boolean commit) {
        updateStickerData("trR", commit, 0, (d, v) -> d.setCenterPercentX(d.getCenterPercentX() + 0.002f));
    }

    public void translateDown(boolean commit) {
        updateStickerData("trD", commit, 0, (d, v) -> d.setCenterPercentY(d.getCenterPercentY() + 0.002f));
    }

    /* --- filter --- */

    public void setBrightness(float brightness, boolean commit) {
        updateStickerData("adjB", commit, brightness, StickerData::setBrightness);
    }

    public void setSaturation(float saturation, boolean commit) {
        updateStickerData("adjS", commit, saturation, StickerData::setSaturation);
    }

    public void setContrast(float contrast, boolean commit) {
        updateStickerData("adjC", commit, contrast, StickerData::setContrast);
    }

    public void setWarmth(float warmth, boolean commit) {
        updateStickerData("adjW", commit, warmth, StickerData::setWarmth);
    }

    public void setOpacity(float opacity, boolean commit) {
        updateStickerData("adjO", commit, opacity, StickerData::setOpacity);
    }

    /* --- layer --- */

    public void bringToFront() {
        int size = this.listSticker.size();
        if (size <= 1) {
            return;
        }
        StickerImage stickerImage = this.currentSticker;
        if (stickerImage == null) {
            return;
        }
        int index = this.listSticker.indexOf(stickerImage);
        if (index == -1 || index == size - 1) { // sticker is not found or it's already at the top
            return;
        }
        swap(index, index + 1);
    }

    public void sendToBack() {
        int size = this.listSticker.size();
        if (size <= 1) {
            return;
        }
        StickerImage stickerImage = this.currentSticker;
        if (stickerImage == null) {
            return;
        }
        int index = this.listSticker.indexOf(stickerImage);
        if (index == -1 || index == 0) { // sticker is not found or it's already at the bottom
            return;
        }
        swap(index, index - 1);
    }

    public void swapLayer(int i1, int i2) {
        swap(i1, i2);
    }

    /* --- other --- */

    private final Map<String, StickerData> cacheValues = new HashMap<>();

    private <V> void updateStickerData(BiConsumer<StickerData, V> setData) {
        updateStickerData(null, true, null, setData);
    }

    private <V> void updateStickerData(String tag, boolean commit, V v, BiConsumer<StickerData, V> setData) {
        StickerData data = getCurrentStickerData();
        if (data == null) {
            return;
        }
        if (commit) {
            StickerData lastData = null;
            StickerData cacheData = cacheValues.remove(tag);
            if (cacheData != null) {
                if (cacheData.getTimeCreated() == data.getTimeCreated()) {
                    lastData = cacheData;
                }
            }
            if (lastData == null) {
                lastData = data.copy();
            }
            setData.accept(data, v);
            if (data.isSameData(lastData)) {
                return;
            }
            data.setTimeUpdated(System.currentTimeMillis());
            updateData(lastData, data);
            getCallback().updateData(data);
        } else {
            if (!cacheValues.containsKey(tag)) {
                cacheValues.put(tag, data.copy());
            }
            setData.accept(data, v);
            getCallback().updateView();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // END ADJUST STICKER
    ///////////////////////////////////////////////////////////////////////////

    private void focusSticker(StickerImage stickerImage) {
        boolean changed;
        StickerData currentData = Optional.ofNullable(this.currentSticker).map(StickerImage::getData).orElse(null);
        StickerData newData = Optional.ofNullable(stickerImage).map(StickerImage::getData).orElse(null);
        if (currentData == null && newData == null) {
            changed = false;
        } else if (currentData != null && newData != null) {
            changed = !Objects.equals(currentData.getTimeCreated(), newData.getTimeCreated());
        } else {
            changed = true;
        }
        if (changed) {
            this.currentSticker = stickerImage;
            getCallback().onStickerFocusChange(newData);
            getCallback().updateView();
        }
    }

    private void drawController(Canvas canvas) {
        pathLine.reset();
        StickerImage stickerImage = this.currentSticker;
        if (stickerImage != null) {
            RectF border = stickerImage.getBorder();
            float halfControlSize = sizeControlIcon / 2.0f;
            canvas.save();
            stickerImage.setMatrix(canvas);
            pathLine.moveTo(border.left, border.top);
            pathLine.lineTo(border.right, border.top);
            pathLine.lineTo(border.right, border.bottom);
            pathLine.lineTo(border.left, border.bottom);
            pathLine.close();
            canvas.drawPath(pathLine, paint);
            int lastColor = paint.getColor();
            paint.setColor(Color.BLACK);
            if (bitmapRotate != null) {
                canvas.drawBitmap(bitmapRotate, border.right - halfControlSize, border.top - halfControlSize, paint);
            }
            if (bitmapResize != null) {
                canvas.drawBitmap(bitmapResize, border.right - halfControlSize, border.bottom - halfControlSize, paint);
            }
            if (bitmapCopy != null) {
                canvas.drawBitmap(bitmapCopy, border.left - halfControlSize, border.bottom - halfControlSize, paint);
            }
            if (bitmapDelete != null) {
                canvas.drawBitmap(bitmapDelete, border.left - halfControlSize, border.top - halfControlSize, paint);
            }
            paint.setColor(lastColor);
            canvas.restore();
        }
    }

    private void addData(StickerData stickerData) {
        if (stickerData == null) {
            return;
        }
        this.listUndo.add(new AddSticker(stickerData.copy()));
        this.listRedo.clear();
        getCallback().onClearRedo();
        getCallback().addData(stickerData);
        focusSticker(findSticker(stickerData.getTimeCreated()));
    }

    private void copyData(StickerData stickerData) {
        if (stickerData == null) {
            return;
        }
        StickerData s = stickerData.copy();
        s.setTimeCreated(System.currentTimeMillis());
        s.setCenterPercentX(s.getCenterPercentX() + 0.01f);
        s.setCenterPercentY(s.getCenterPercentY() + 0.01f);
        this.listUndo.add(new AddSticker(s.copy()));
        this.listRedo.clear();
        getCallback().onClearRedo();
        getCallback().addData(s);
        focusSticker(findSticker(s.getTimeCreated()));
    }

    private void removeData(StickerData stickerData) {
        if (stickerData == null) {
            return;
        }
        StickerImage stickerImage = findSticker(stickerData.getTimeCreated());
        if (stickerImage == null) {
            return;
        }
        this.listUndo.add(new RemoveSticker(stickerData.copy(), this.listSticker.indexOf(stickerImage)));
        this.listRedo.clear();
        getCallback().onClearRedo();
        getCallback().removeData(stickerData);
    }

    private void updateData(StickerData lastStickerData, StickerData stickerData) {
        if (lastStickerData != null) {
            StickerData nextData = stickerData.copy();
            StickerData prevData = lastStickerData.copy();
            this.listUndo.add(new UpdateSticker(nextData, prevData));
            this.listRedo.clear();
            getCallback().onClearRedo();
            getCallback().updateView();
        }
    }

    private void swap(int i1, int i2) {
        int size = this.listSticker.size();
        if (i1 < 0 || i1 >= size || i2 < 0 || i2 >= size) {
            return; // invalid indices
        }
        StickerImage s1 = this.listSticker.get(i1);
        StickerImage s2 = this.listSticker.get(i2);
        this.listUndo.add(new SwapSticker(s1.getData(), s2.getData(), i1, i2));
        this.listRedo.clear();
        getCallback().onClearRedo();
        getCallback().swapData(i1, i2);
    }

    private StickerImage findSticker(long timeCreated) {
        List<StickerImage> stickers = this.listSticker;
        int i = stickers.size() - 1;
        while (i >= 0) {
            StickerImage stickerImage = stickers.get(i);
            if (stickerImage.getData().getTimeCreated() == timeCreated) {
                return stickerImage;
            }
            i--;
        }
        return null;
    }

    public final void setSelectedSticker(StickerData stickerData) {
        focusSticker(stickerData == null ? null : findSticker(stickerData.getTimeCreated()));
    }

    public final List<StickerData> getStickers() {
        List<StickerData> list = this.listSticker.stream().map(StickerImage::getData).collect(Collectors.toList());
        Collections.reverse(list);
        return list;
    }

    public final void addSticker(StickerData stickerData) {
        if (stickerData == null) {
            return;
        }
        addData(stickerData);
    }

    public final void copySticker(StickerData stickerData) {
        if (stickerData == null) {
            return;
        }
        copyData(stickerData);
    }

    public final void removeSticker(StickerData stickerData) {
        if (stickerData == null) {
            return;
        }
        removeData(stickerData);
    }

    public void loadControlsImageRx() {
        Single.fromCallable(() -> {
                    StickerDrawer d = StickerDrawer.this;
                    Context c = WeatherApplication.getInstance();
                    int size = d.sizeControlIcon;
                    d.bitmapRotate = Glide.with(c).asBitmap().load(R.drawable.rotate).override(size).submit().get();
                    d.bitmapResize = Glide.with(c).asBitmap().load(R.drawable.resize).override(size).submit().get();
                    d.bitmapCopy = Glide.with(c).asBitmap().load(R.drawable.copy).override(size).submit().get();
                    d.bitmapDelete = Glide.with(c).asBitmap().load(R.drawable.close).override(size).submit().get();
                    return true;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Boolean>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull Boolean success) {
                        if (success) {
                            getCallback().updateView();
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                    }
                });
    }

    private abstract static class ModifySticker {

        final StickerData nextData;
        final StickerData prevData;

        public ModifySticker(StickerData nextData, StickerData prevData) {
            this.nextData = nextData;
            this.prevData = prevData;
        }
    }

    private static class AddSticker extends ModifySticker {

        public AddSticker(StickerData data) {
            super(data, data);
        }
    }

    private static class RemoveSticker extends ModifySticker {

        final int index;

        public RemoveSticker(StickerData data, int index) {
            super(data, data);
            this.index = index;
        }
    }

    private static class UpdateSticker extends ModifySticker {

        public UpdateSticker(StickerData nextData, StickerData prevData) {
            super(nextData, prevData);
        }
    }

    private static class SwapSticker extends ModifySticker {

        final int i1;
        final int i2;

        public SwapSticker(StickerData nextData, StickerData prevData, int i1, int i2) {
            super(nextData, prevData);
            this.i1 = i1;
            this.i2 = i2;
        }
    }
}
