package com.example.piceditor.draw.test;

import androidx.annotation.Keep;

import java.io.Serializable;

@Keep
public class Beard implements Serializable {

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
