package com.example.piceditor.model;

import androidx.annotation.Keep;

import java.io.Serializable;

@Keep
public class Template implements Serializable {

    private int id;
    private String image;

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
}
