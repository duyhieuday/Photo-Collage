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

    private boolean isDrawingEnabled = false;

    // ✅ Gesture mode: cho phép di chuyển/scale sticker mà không vẽ tay
    // Mặc định true — sticker luôn di chuyển được dù không ở tab sticker
    private boolean isGestureEnabled = true;

    public void setDrawingEnabled(boolean enabled) {
        isDrawingEnabled = enabled;
    }

    public void setGestureEnabled(boolean enabled) {
        isGestureEnabled = enabled;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isDrawingEnabled && !isGestureEnabled) {
            return false; // Nhường hoàn toàn cho TemplateEditorView bên dưới
        }

        if (isDrawingEnabled) {
            // Tab draw: vẽ tay bình thường
            getParent().requestDisallowInterceptTouchEvent(true);
            manager.onTouch(event);
            return true;
        }

        // isGestureEnabled = true, isDrawingEnabled = false
        // Chỉ xử lý nếu chạm đúng vào sticker, không thì nhường cho view dưới
        boolean stickerHit = manager.isTouchingSticker(event.getX(), event.getY());
        if (stickerHit) {
            getParent().requestDisallowInterceptTouchEvent(true);
            manager.onTouch(event);
            return true;
        }
        return false; // Không chạm sticker → nhường cho TemplateEditorView
    }
}