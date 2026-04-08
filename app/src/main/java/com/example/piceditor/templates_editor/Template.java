package com.example.piceditor.templates_editor;

import android.graphics.RectF;

import androidx.annotation.Keep;

import java.io.Serializable;
import java.util.List;

@Keep
public class Template implements Serializable {

    private int id;
    private String image;
    private Integer backgroundRes;
    private List<RectF> regions;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getImage() {
        return image;
    }
    public String getImageAsset(){
        return "file:///android_asset/" + getImage();
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Integer getBackgroundRes() {
        return backgroundRes;
    }

    public void setBackgroundRes(Integer backgroundRes) {
        this.backgroundRes = backgroundRes;
    }

    public List<RectF> getRegions() {
        return regions;
    }

    public void setRegions(List<RectF> regions) {
        this.regions = regions;
    }
}
