package com.example.piceditor.draw.preview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.piceditor.R;
import com.example.piceditor.draw.model.draw.DrawPoint;
import com.example.piceditor.draw.model.draw.style.BrushStyle;
import com.example.piceditor.draw.model.draw.style.PaintStyle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DrawPreview extends View {
    private int color;
    private int strokeWidth;
    private BrushStyle brushStyle;
    private PaintStyle paintStyle;
    private Path path;

    public DrawPreview(Context context) {
        this(context, null);
    }

    public DrawPreview(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DrawPreview(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        BrushStyle defBrushStyle = BrushStyle.GESTURE;
        PaintStyle defPaintStyle = PaintStyle.STROKE;
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.DrawPreview);
        int color = a.getColor(R.styleable.DrawPreview_dp_color, Color.BLACK);
        int strokeWidth = a.getDimensionPixelSize(R.styleable.DrawPreview_dp_stroke_width, 24);
        int brush = a.getInt(R.styleable.DrawPreview_dp_brush_style, defBrushStyle.value);
        int paint = a.getInt(R.styleable.DrawPreview_dp_paint_style, defPaintStyle.value);

        setColor(color);
        setStrokeWidth(strokeWidth);
        setBrushStyle(Arrays.stream(BrushStyle.values()).filter(b -> b.value == brush).findFirst().orElse(defBrushStyle));
        setPaintStyle(Arrays.stream(PaintStyle.values()).filter(p -> p.value == paint).findFirst().orElse(defPaintStyle));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            a.close();
        } else {
            a.recycle();
        }
    }

    public void setColor(@ColorInt int color) {
        this.color = color;
        invalidate();
    }

    public void setStrokeWidth(int strokeWidth) {
        this.strokeWidth = strokeWidth;
        invalidate();
    }

    public void setBrushStyle(BrushStyle brushStyle) {
        this.brushStyle = brushStyle;
        invalidate();
    }

    public void setPaintStyle(PaintStyle paintStyle) {
        this.paintStyle = paintStyle;
        invalidate();
    }

    public int getColor() {
        return color;
    }

    public int getStrokeWidth() {
        return strokeWidth;
    }

    public BrushStyle getBrushStyle() {
        return brushStyle;
    }

    public PaintStyle getPaintStyle() {
        return paintStyle;
    }

    @Override
    protected void onMeasure(int size, int height) {
        if (brushStyle == BrushStyle.GESTURE) {
            super.onMeasure(size, height);
            return;
        }
        super.onMeasure(size, size);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        makePath();
        Path path = this.path;
        Paint paint = paintStyle.getPaint();
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth);
        int paddingHoz = getPaddingLeft() + getPaddingRight();
        int paddingVer = getPaddingTop() + getPaddingBottom();
        float scaleX = 1 - (float) paddingHoz / getWidth();
        float scaleY = 1 - (float) paddingVer / getHeight();
        canvas.save();
        canvas.scale(scaleX, scaleY, getWidth() / 2f, getHeight() / 2f);
        canvas.drawPath(path, paint);
        canvas.restore();
    }

    private void makePath() {
        float w = getWidth();
        float h = getHeight();
        if (path == null) {
            path = new Path();
        } else {
            path.reset();
        }
        float size;
        if (brushStyle == BrushStyle.GESTURE) {
            path.moveTo(w * 0.1f, h * 0.5f);
            path.quadTo(w * 0.3f, h * 0.3f, w * 0.5f, h * 0.5f);
            path.quadTo(w * 0.7f, h * 0.7f, w * 0.9f, h * 0.5f);
        } else {
            List<DrawPoint> drawPoints = new ArrayList<>();
            switch (brushStyle) {
                case LINE:
                    drawPoints.add(new DrawPoint(w * 0.1f, h * 0.9f));
                    drawPoints.add(new DrawPoint(w * 0.9f, h * 0.1f));
                    break;
                case HEART:
                    size = Math.min(w, h);
                    drawPoints.add(new DrawPoint(0, 0));
                    drawPoints.add(new DrawPoint(size, size));
                    break;
                case MOON:
                    size = Math.min(w, h);
                    drawPoints.add(new DrawPoint(size * 0.05f, size * 0.05f));
                    drawPoints.add(new DrawPoint(size * 0.95f, size * 0.95f));
                    break;
                case TRIANGLE:
                    size = Math.min(w, h) / 2f;
                    drawPoints.add(new DrawPoint(size, size + size * 0.2f));
                    drawPoints.add(new DrawPoint(size * 2f, size * 2f));
                    break;
                case RHOMBUS:
                case HEXAGON:
                case OCTAGON:
                case STAR4:
                case STAR6:
                case STAR8:
                case STAR10:
                    size = Math.min(w, h) / 2f;
                    drawPoints.add(new DrawPoint(size, size));
                    drawPoints.add(new DrawPoint(size * 1.95f - size * 0.05f, size * 1.95f - size * 0.05f));
                    break;
                case PENTAGON:
                case STAR5:
                    size = Math.min(w, h) / 2f;
                    drawPoints.add(new DrawPoint(size, size + size * 0.1f));
                    drawPoints.add(new DrawPoint(size * 2f, size * 2f));
                    break;
                case HEPTAGON:
                case STAR7:
                case STAR9:
                    size = Math.min(w, h) / 2f;
                    drawPoints.add(new DrawPoint(size, size + size * 0.05f));
                    drawPoints.add(new DrawPoint(size * 1.95f - size * 0.05f, size * 1.95f - size * 0.05f));
                    break;
                default:
                    drawPoints.add(new DrawPoint(w * 0.05f, h * 0.05f));
                    drawPoints.add(new DrawPoint(w * 0.95f, h * 0.95f));
                    break;
            }
            brushStyle.makePath(path, drawPoints);
        }
    }

}
