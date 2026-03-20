package com.example.piceditor.draw.model.draw.style;

import android.graphics.BlurMaskFilter;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public enum PaintStyle {

    // @formatter:off
    ERASE           (2378, Flag.FLAG_STROKE | Flag.FLAG_ERASE, Mask.NONE),
    FILL            (7248, Flag.FLAG_FILL, Mask.NONE),
    FILL_SHADOW     (4129, Flag.FLAG_FILL, Mask.SHADOW),
    STROKE          (8503, Flag.FLAG_STROKE, Mask.NONE),
    STROKE_SOLID    (1254, Flag.FLAG_STROKE, Mask.SOLID),
    STROKE_NEON     (1820, Flag.FLAG_STROKE, Mask.NEON),
    STROKE_INNER    (5967, Flag.FLAG_STROKE, Mask.INNER),
    STROKE_BLUR     (3011, Flag.FLAG_STROKE, Mask.BLUR),
    STROKE_SHADOW   (6492, Flag.FLAG_STROKE, Mask.SHADOW);
    // @formatter:on

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true, value = {Flag.FLAG_ERASE, Flag.FLAG_FILL, Flag.FLAG_STROKE})
    public @interface Flag {
        int FLAG_ERASE = 1;
        int FLAG_FILL = 1 << 1;
        int FLAG_STROKE = 1 << 2;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {Mask.NONE, Mask.SOLID, Mask.NEON, Mask.INNER, Mask.BLUR, Mask.SHADOW})
    public @interface Mask {
        int NONE = 0;
        int SOLID = 1;
        int NEON = 2;
        int INNER = 3;
        int BLUR = 4;
        int SHADOW = 5;
    }

    public final int value;
    private final @Flag int flag;
    private final @Mask int mask;

    private Paint paint;

    PaintStyle(int value, @Flag int flag, @Mask int mask) {
        this.value = value;
        this.flag = flag;
        this.mask = mask;
    }

    public Paint getPaint() {
        if (paint == null) {
            paint = createPaint();
        }
        return paint;
    }

    @NonNull
    private Paint createPaint() {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        if (hasFlag(Flag.FLAG_FILL & Flag.FLAG_STROKE)) {
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
        } else if (hasFlag(Flag.FLAG_FILL)) {
            paint.setStyle(Paint.Style.FILL);
        } else if (hasFlag(Flag.FLAG_STROKE)) {
            paint.setStyle(Paint.Style.STROKE);
        }
        if (hasFlag(Flag.FLAG_ERASE)) {
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        }

        switch (mask) {
            // @formatter:off
            case Mask.SOLID:     paint.setMaskFilter(new BlurMaskFilter(20, BlurMaskFilter.Blur.SOLID));     break;
            case Mask.NEON:      paint.setMaskFilter(new BlurMaskFilter(20, BlurMaskFilter.Blur.OUTER));     break;
            case Mask.INNER:     paint.setMaskFilter(new BlurMaskFilter(20, BlurMaskFilter.Blur.INNER));     break;
            case Mask.BLUR:      paint.setMaskFilter(new BlurMaskFilter(20, BlurMaskFilter.Blur.NORMAL));    break;
            case Mask.SHADOW:    paint.setShadowLayer(5.0f, 5.0f, 5.0f, Color.BLACK);                        break;
            case Mask.NONE:      paint.setMaskFilter(null);                                                  break;
            // @formatter:on
        }
        return paint;
    }

    private boolean hasFlag(@Flag int flag) {
        return (this.flag & flag) != 0;
    }

}
