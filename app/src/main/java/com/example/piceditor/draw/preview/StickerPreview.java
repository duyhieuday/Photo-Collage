package com.example.piceditor.draw.preview;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.piceditor.draw.model.sticker.StickerData;
import com.example.piceditor.draw.sticker.StickerImage;


public class StickerPreview extends View {

    private StickerImage stickerImage;

    public StickerPreview(Context context) {
        this(context, null);
    }

    public StickerPreview(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StickerPreview(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setStickerData(StickerData stickerData) {
        if (stickerImage == null) {
            // @formatter:off
            stickerImage = new StickerImage(stickerData, false,
                    data -> {},
                    data -> {},
                    (lastData, newData) -> {},
                    this::invalidate);
            // @formatter:on
        } else {
            stickerImage.setData(stickerData);
            stickerImage.loadImageRx();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (stickerImage != null && changed) {
            stickerImage.setViewSize(getWidth(), getHeight());
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (stickerImage != null) {
            stickerImage.draw(canvas);
        }
    }
}
