package com.example.piceditor.draw;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.piceditor.R;

public class BrushSizeSlider extends View {

    private static final int MIN_SIZE = ScreenUtils.dp2px(4);
    private static final int MAX_SIZE = ScreenUtils.dp2px(40);
    private static final int TRACK_STROKE_WIDTH = ScreenUtils.dp2px(1);
    private static final int THUMB_STROKE_WIDTH = ScreenUtils.dp2px(2);

    private float sizePercent;
    private int minSize;
    private int maxSize;

    private OnSliderTouchListener onSliderTouchListener;
    private OnBrushSizeChangedListener onBrushSizeChangedListener;

    private final Path trackPath;
    private final RectF trackBounds;
    private final Paint trackPaint;
    private final Paint trackStrokePaint;
    private final Paint thumbPaint;
    private final Paint thumbStrokePaint;

    public BrushSizeSlider(Context context) {
        this(context, null);
    }

    public BrushSizeSlider(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BrushSizeSlider(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        trackPath = new Path();
        trackBounds = new RectF();
        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.FILL);
        trackPaint.setPathEffect(new CornerPathEffect(ScreenUtils.dp2px(4)));
        trackStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackStrokePaint.setStyle(Paint.Style.STROKE);
        trackStrokePaint.setStrokeCap(Paint.Cap.ROUND);
        trackStrokePaint.setStrokeJoin(Paint.Join.ROUND);
        trackStrokePaint.setPathEffect(new CornerPathEffect(ScreenUtils.dp2px(4)));
        thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbPaint.setStyle(Paint.Style.FILL);
        thumbStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbStrokePaint.setStyle(Paint.Style.STROKE);

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.BrushSizeSlider);

        setMinSize(a.getDimensionPixelSize(R.styleable.BrushSizeSlider_bs_min_size, MIN_SIZE));
        setMaxSize(a.getDimensionPixelSize(R.styleable.BrushSizeSlider_bs_max_size, MAX_SIZE));
        setSize(a.getDimensionPixelSize(R.styleable.BrushSizeSlider_bs_size, MIN_SIZE));
        setTrackColor(a.getColor(R.styleable.BrushSizeSlider_bs_track_color, Color.BLACK));
        setTrackStrokeColor(a.getColor(R.styleable.BrushSizeSlider_bs_track_stroke_color, Color.GRAY));
        setTrackStrokeWidth(a.getDimensionPixelSize(R.styleable.BrushSizeSlider_bs_track_stroke_width, TRACK_STROKE_WIDTH));
        setThumbColor(a.getColor(R.styleable.BrushSizeSlider_bs_thumb_color, Color.RED));
        setThumbStrokeColor(a.getColor(R.styleable.BrushSizeSlider_bs_thumb_stroke_color, Color.BLUE));
        setThumbStrokeWidth(a.getDimensionPixelSize(R.styleable.BrushSizeSlider_bs_thumb_stroke_width, THUMB_STROKE_WIDTH));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            a.close();
        } else {
            a.recycle();
        }
    }

    public void setMinSize(int minSize) {
        this.minSize = minSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public float getSize() {
        return minSize + (maxSize - minSize) * sizePercent;
    }

    public void setSize(float size) {
        if (size < minSize) {
            size = minSize;
        }
        if (size > maxSize) {
            size = maxSize;
        }
        this.sizePercent = (size - minSize) / (maxSize - minSize);
        invalidate();
    }

    public void setSizePercent(float sizePercent) {
        if (sizePercent < 0) {
            sizePercent = 0;
        }
        if (sizePercent > 1) {
            sizePercent = 1;
        }
        this.sizePercent = sizePercent;
        invalidate();
    }

    public void setTrackColor(int color) {
        trackPaint.setColor(color);
        invalidate();
    }

    private void setTrackStrokeColor(int color) {
        trackStrokePaint.setColor(color);
        invalidate();
    }

    private void setTrackStrokeWidth(int width) {
        trackStrokePaint.setStrokeWidth(width);
        invalidate();
    }

    public void setThumbColor(int color) {
        thumbPaint.setColor(color);
        invalidate();
    }

    public void setThumbStrokeColor(int color) {
        thumbStrokePaint.setColor(color);
        invalidate();
    }

    public void setThumbStrokeWidth(int width) {
        thumbStrokePaint.setStrokeWidth(width);
        invalidate();
    }

    public void setOnBrushTouchListener(OnSliderTouchListener listener) {
        this.onSliderTouchListener = listener;
    }

    public void setOnBrushSizeChangedListener(OnBrushSizeChangedListener listener) {
        this.onBrushSizeChangedListener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float offset = thumbStrokePaint.getStrokeWidth() * 2;
        int width = (int) (maxSize + getPaddingStart() + getPaddingEnd() + offset);
        setMeasuredDimension(width, MeasureSpec.getSize(heightMeasureSpec));

        // init path and bounds
        float t = getPaddingTop() + offset + maxSize / 2f;
        float b = getMeasuredHeight() - getPaddingBottom() - offset - minSize / 2f;
        float l = getPaddingStart() + offset;
        float r = getMeasuredWidth() - getPaddingEnd() - offset;
        trackBounds.set(l, t, r, b);
        trackPath.rewind();
        trackPath.moveTo(l, t);
        trackPath.lineTo(r, t);
        trackPath.lineTo(trackBounds.centerX() + minSize / 2f, b);
        trackPath.lineTo(trackBounds.centerX() - minSize / 2f, b);
        trackPath.close();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float offset = thumbStrokePaint.getStrokeWidth();

        // draw track
        canvas.drawPath(trackPath, trackPaint);
        canvas.drawPath(trackPath, trackStrokePaint);

        // draw thumb
        float thumbY = trackBounds.top + (trackBounds.bottom - trackBounds.top) * (1 - sizePercent);
        float thumbRadius = (offset + minSize * (1 - sizePercent) + (maxSize - minSize) * sizePercent) / 2f;
        canvas.drawCircle(trackBounds.centerX(), thumbY, thumbRadius, thumbStrokePaint);
        canvas.drawCircle(trackBounds.centerX(), thumbY, thumbRadius - offset / 2f, thumbPaint);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (onSliderTouchListener != null) {
                    onSliderTouchListener.onTouchDown();
                }
                handleTouch(event.getY() - trackBounds.top);
                break;
            case MotionEvent.ACTION_MOVE:
                handleTouch(event.getY() - trackBounds.top);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handleTouch(event.getY() - trackBounds.top);
                if (onSliderTouchListener != null) {
                    onSliderTouchListener.onTouchUp();
                }
                break;
        }
        invalidate();
        return true;
    }

    private void handleTouch(float y) {
        float trackHeight = trackBounds.height();
        if (y < 0) {
            y = 0;
        }
        if (y > trackHeight) {
            y = trackHeight;
        }
        sizePercent = 1 - (y / trackHeight);
        if (onBrushSizeChangedListener != null) {
            onBrushSizeChangedListener.onBrushSizeChanged(getSize());
        }
    }

    public interface OnSliderTouchListener {
        void onTouchDown();

        void onTouchUp();
    }

    public interface OnBrushSizeChangedListener {
        void onBrushSizeChanged(float size);
    }
}
