package com.example.piceditor.draw.model;

import com.example.piceditor.draw.model.draw.DrawPath;
import com.example.piceditor.draw.model.sticker.StickerData;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class DrawData implements Serializable {

    private @SerializedName("id") String id;
    private final @SerializedName("lpt") List<DrawPath> listPath;
    private final @SerializedName("lst") List<StickerData> listSticker;
    private @SerializedName("lbg") String linkBackground;
    private @SerializedName("tbg") long timeSetBackground;
    private @SerializedName("ltc") long lastTimeClearPath;
    private @SerializedName("tup") long timeUpdate;

    public DrawData() {
        this(UUID.randomUUID().toString());
    }

    public DrawData(String id) {
        this.id = id;
        this.listPath = new ArrayList<>();
        this.listSticker = new ArrayList<>();
        this.linkBackground = "";
        this.timeSetBackground = 0;
        this.lastTimeClearPath = 0;
        this.timeUpdate = 0;
    }

    public DrawData(String id, List<DrawPath> listPath, List<StickerData> listSticker, String linkBackground, long timeSetBackground, long lastTimeClearPath, long timeUpdate) {
        this.id = id;
        this.listPath = listPath;
        this.listSticker = listSticker;
        this.linkBackground = linkBackground;
        this.timeSetBackground = timeSetBackground;
        this.lastTimeClearPath = lastTimeClearPath;
        this.timeUpdate = timeUpdate;
    }

    public void setData(DrawData drawData) {
        if (this == drawData) {
            return;
        }
        this.id = drawData.getId();
        this.listPath.clear();
        this.listPath.addAll(drawData.getListPath());
        this.listSticker.clear();
        this.listSticker.addAll(drawData.getListSticker());
        this.linkBackground = drawData.getLinkBackground();
        this.timeSetBackground = drawData.getTimeSetBackground();
        this.lastTimeClearPath = drawData.getLastTimeClearPath();
        this.timeUpdate = drawData.getTimeUpdate();
    }

    public DrawData copy() {
        return new DrawData(id, new ArrayList<>(listPath), new ArrayList<>(listSticker), linkBackground, timeSetBackground, lastTimeClearPath, timeUpdate);
    }

    public DrawData deepCopy() {
        return new DrawData(
                id,
                Optional.ofNullable(listPath).orElseGet(ArrayList::new).stream()
                        .map(DrawPath::deepCopy)
                        .collect(Collectors.toList()),
                Optional.ofNullable(listSticker).orElseGet(ArrayList::new).stream()
                        .map(StickerData::deepCopy)
                        .collect(Collectors.toList()),
                linkBackground,
                timeSetBackground,
                lastTimeClearPath,
                timeUpdate
        );
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setListPath(List<DrawPath> listPath) {
        this.listPath.clear();
        this.listPath.addAll(listPath);
    }

    public List<DrawPath> getListPath() {
        return listPath;
    }

    public void setListSticker(List<StickerData> listSticker) {
        this.listSticker.clear();
        this.listSticker.addAll(listSticker);
    }

    public List<StickerData> getListSticker() {
        return listSticker;
    }

    public String getLinkBackground() {
        return linkBackground;
    }

    public void setLinkBackground(String linkBackground) {
        this.linkBackground = linkBackground;
    }

    public long getTimeSetBackground() {
        return timeSetBackground;
    }

    public void setTimeSetBackground(long timeSetBackground) {
        this.timeSetBackground = timeSetBackground;
    }

    public long getLastTimeClearPath() {
        return lastTimeClearPath;
    }

    public void setLastTimeClearPath(long lastTimeClearPath) {
        this.lastTimeClearPath = lastTimeClearPath;
    }

    public long getTimeUpdate() {
        return timeUpdate;
    }

    public void setTimeUpdate(long timeUpdate) {
        this.timeUpdate = timeUpdate;
    }

}
