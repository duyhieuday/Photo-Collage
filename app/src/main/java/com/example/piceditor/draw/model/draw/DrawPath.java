package com.example.piceditor.draw.model.draw;

import android.graphics.Path;
import androidx.annotation.NonNull;

import com.example.piceditor.draw.model.draw.style.BrushStyle;
import com.example.piceditor.draw.model.draw.style.PaintStyle;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DrawPath implements Serializable {

    private @SerializedName("bst") BrushStyle brushStyle;
    private @SerializedName("pst") PaintStyle paintStyle;
    private @SerializedName("clr") int color;      // color
    private @SerializedName("siz") float size;     // stroke width
    private @SerializedName("dpt") List<DrawPoint> drawPoints;
    private @SerializedName("tcr") long timeCreated;

    public DrawPath(BrushStyle brushStyle, PaintStyle paintStyle, int color, float size) {
        this(brushStyle, paintStyle, color, size, new ArrayList<>(), System.currentTimeMillis());
    }

    public DrawPath(BrushStyle brushStyle, PaintStyle paintStyle, int color, float size, List<DrawPoint> drawPoints, long timeCreated) {
        this.brushStyle = brushStyle;
        this.paintStyle = paintStyle;
        this.color = color;
        this.size = size;
        this.drawPoints = Optional.ofNullable(drawPoints).orElseGet(ArrayList::new);
        this.timeCreated = timeCreated;
    }

    public void setData(@NonNull DrawPath drawPath) {
        this.brushStyle = drawPath.getBrushStyle();
        this.paintStyle = drawPath.getPaintStyle();
        this.color = drawPath.getColor();
        this.size = drawPath.getSize();
        this.drawPoints = Optional.ofNullable(drawPath.drawPoints).orElseGet(ArrayList::new);
        this.timeCreated = drawPath.getTimeCreated();
    }

    public DrawPath copy() {
        return new DrawPath(
                brushStyle,
                paintStyle,
                color,
                size,
                new ArrayList<>(),
                System.currentTimeMillis()
        );
    }

    public DrawPath deepCopy() {
        return new DrawPath(
                brushStyle,
                paintStyle,
                color,
                size,
                Optional.ofNullable(drawPoints).orElseGet(ArrayList::new).stream()
                        .map(DrawPoint::deepCopy)
                        .collect(Collectors.toList()),
                timeCreated
        );
    }

    public BrushStyle getBrushStyle() {
        return brushStyle;
    }

    public void setBrushStyle(BrushStyle brushStyle) {
        this.brushStyle = brushStyle;
    }

    public PaintStyle getPaintStyle() {
        return paintStyle;
    }

    public void setPaintStyle(PaintStyle paintStyle) {
        this.paintStyle = paintStyle;
    }

    public void setPathStyle(PaintStyle style) {
        this.paintStyle = style;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    public List<DrawPoint> getDrawPoints() {
        return drawPoints;
    }

    public void setDrawPoints(List<DrawPoint> drawPoints) {
        this.drawPoints = drawPoints;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
    }

    public void makePath(Path path) {
        brushStyle.makePath(path, drawPoints);
    }

}
