package com.example.piceditor.sever.ai_remove_bg.model;

import com.google.gson.annotations.SerializedName;

public class Data<D> {

    private @SerializedName("data") D data;

    public Data() {
    }

    public Data(D data) {
        this.data = data;
    }

    public D getData() {
        return data;
    }

    public void setData(D data) {
        this.data = data;
    }
}
