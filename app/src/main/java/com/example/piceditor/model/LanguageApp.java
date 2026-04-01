package com.example.piceditor.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

@Entity
public class LanguageApp implements Serializable {

    @PrimaryKey(autoGenerate = true)
    @SerializedName("languageId")
    private int languageId;
    @SerializedName("languageName")
    private String languageName;
    @SerializedName("signLanguage")
    private String signLanguage;
    @SerializedName("flag")
    private String flag;

    public int getLanguageId() {
        return languageId;
    }

    public void setLanguageId(int languageId) {
        this.languageId = languageId;
    }

    public String getLanguageName() {
        return languageName;
    }

    public void setLanguageName(String languageName) {
        this.languageName = languageName;
    }

    public String getSignLanguage() {
        return signLanguage;
    }

    public void setSignLanguage(String signLanguage) {
        this.signLanguage = signLanguage;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }
}
