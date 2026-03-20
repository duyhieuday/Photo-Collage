package com.example.piceditor.draw.model.draw;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class DrawPoint implements Serializable {
    private @SerializedName("x") float x;
    private @SerializedName("y") float y;

    public DrawPoint(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public DrawPoint copy() {
        return new DrawPoint(x, y);
    }

    public DrawPoint deepCopy() {
        return copy();
    }
}
