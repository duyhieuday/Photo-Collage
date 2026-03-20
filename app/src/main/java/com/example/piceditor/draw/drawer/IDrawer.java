package com.example.piceditor.draw.drawer;

import android.graphics.Canvas;

import androidx.annotation.Nullable;

import java.util.List;

public interface IDrawer<D> {

    void invalidateUndoRedo(long lastTimeClearPath);

    long getLastTimeUndo();

    long getLastTimeRedo();

    void undo();

    void redo();

    void clearRedo();

    void clearUndoRedo();

    void setCurrentData(D currentData);

    void setData(List<D> data);

    boolean interceptTouchEvent(float x, float y, int action);

    void onTouch(float x, float y, int action, long time);

    void onDraw(Canvas canvas);

    interface Callback<D> {

        void onClearRedo();

        void addData(D d);

        void removeData(D d);

        void updateData(D d);

        default void addData(int index, D d) {
        }

        default void swapData(int i1, int i2) {
        }

        default void onStickerFocusChange(@Nullable D d) {
        }

        void updateView();
    }
}
