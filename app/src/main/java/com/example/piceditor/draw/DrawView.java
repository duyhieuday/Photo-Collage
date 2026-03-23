package com.example.piceditor.draw;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DrawView extends View {

    private final DrawerManager manager;

    public DrawView(Context context) {
        this(context, null);
    }

    public DrawView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DrawView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        manager = new DrawerManager(this::invalidate);
    }

    public DrawerManager getDrawManager() {
        return manager;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        manager.setViewSize(getWidth(), getHeight());
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        manager.onDraw(canvas);
    }

    private boolean isDrawingEnabled = true;

    public void setDrawingEnabled(boolean enabled) {
        isDrawingEnabled = enabled;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isDrawingEnabled) {
            return false; // 🔥 nhường cho view dưới
        }

        getParent().requestDisallowInterceptTouchEvent(true);
        manager.onTouch(event);
        return true;
    }

}
