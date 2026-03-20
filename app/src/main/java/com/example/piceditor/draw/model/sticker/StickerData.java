package com.example.piceditor.draw.model.sticker;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class StickerData implements Serializable {

    private @SerializedName("cpx") float centerPercentX;
    private @SerializedName("cpy") float centerPercentY;
    private @SerializedName("cpw") float scalePercentWidth;
    private @SerializedName("rtt") float rotate;
    private @SerializedName("brt") float brightness;    // 0 = black, 1 = original, 2 = twice as bright
    private @SerializedName("sat") float saturation;    // 0 = grayscale, 1 = original, 2 = hyper saturated
    private @SerializedName("ctr") float contrast;      // 0 = gray, 1 = unchanged, 2 = high contrast
    private @SerializedName("tem") float warmth;        // 0.5 = cold, 1 = neutral, 2 = warm
    private @SerializedName("opa") float opacity;       // 0 = transparent, 1 = original
    private @SerializedName("flh") boolean flipHorizontal;
    private @SerializedName("flv") boolean flipVertical;
    private @SerializedName("pim") String pathImage;
    private @SerializedName("tcr") long timeCreated;
    private @SerializedName("tup") long timeUpdated;

    public StickerData(String pathImage) {
        this(0.5f, 0.5f, 0.68f, 0, 1, 1, 1, 1, 1, false, false,
                pathImage, System.currentTimeMillis(), System.currentTimeMillis());
    }

    public StickerData(float centerPercentX, float centerPercentY, float scalePercentWidth, float rotate,
                       float brightness, float saturation, float contrast, float warmth, float opacity,
                       boolean flipHorizontal, boolean flipVertical,
                       String pathImage, long timeCreated, long timeUpdated) {
        this.centerPercentX = centerPercentX;
        this.centerPercentY = centerPercentY;
        this.scalePercentWidth = scalePercentWidth;
        this.rotate = rotate;
        this.brightness = brightness;
        this.saturation = saturation;
        this.contrast = contrast;
        this.warmth = warmth;
        this.opacity = opacity;
        this.flipHorizontal = flipHorizontal;
        this.flipVertical = flipVertical;
        this.pathImage = pathImage;
        this.timeCreated = timeCreated;
        this.timeUpdated = timeUpdated;
    }

    public void setData(@NonNull StickerData stickerData) {
        this.centerPercentX = stickerData.centerPercentX;
        this.centerPercentY = stickerData.centerPercentY;
        this.scalePercentWidth = stickerData.scalePercentWidth;
        this.rotate = stickerData.rotate;
        this.brightness = stickerData.brightness;
        this.saturation = stickerData.saturation;
        this.contrast = stickerData.contrast;
        this.warmth = stickerData.warmth;
        this.opacity = stickerData.opacity;
        this.flipHorizontal = stickerData.flipHorizontal;
        this.flipVertical = stickerData.flipVertical;
        this.pathImage = stickerData.pathImage;
        this.timeCreated = stickerData.timeCreated;
        this.timeUpdated = stickerData.timeUpdated;
    }

    public boolean isSameData(@NonNull StickerData stickerData) {
        return centerPercentX == stickerData.centerPercentX
                && centerPercentY == stickerData.centerPercentY
                && scalePercentWidth == stickerData.scalePercentWidth
                && rotate == stickerData.rotate
                && brightness == stickerData.brightness
                && saturation == stickerData.saturation
                && contrast == stickerData.contrast
                && warmth == stickerData.warmth
                && opacity == stickerData.opacity
                && flipHorizontal == stickerData.flipHorizontal
                && flipVertical == stickerData.flipVertical
                && pathImage.equals(stickerData.pathImage);
    }

    public StickerData copy() {
        return new StickerData(centerPercentX, centerPercentY, scalePercentWidth, rotate,
                brightness, saturation, contrast, warmth, opacity,
                flipHorizontal, flipVertical,
                pathImage, timeCreated, timeUpdated
        );
    }

    public StickerData deepCopy() {
        return copy();
    }

    public float getCenterPercentX() {
        return centerPercentX;
    }

    public void setCenterPercentX(float centerPercentX) {
        this.centerPercentX = centerPercentX;
    }

    public float getCenterPercentY() {
        return centerPercentY;
    }

    public void setCenterPercentY(float centerPercentY) {
        this.centerPercentY = centerPercentY;
    }

    public float getScalePercentWidth() {
        return scalePercentWidth;
    }

    public void setScalePercentWidth(float scalePercentWidth) {
        this.scalePercentWidth = scalePercentWidth;
    }

    public float getRotate() {
        return rotate;
    }

    public void setRotate(float rotate) {
        this.rotate = rotate;
    }

    public void rotateLeft() {
        this.rotate = (this.rotate - 90) % 360;
    }

    public void rotateRight() {
        this.rotate = (this.rotate + 90) % 360;
    }

    public float getBrightness() {
        return brightness;
    }

    public void setBrightness(float brightness) {
        this.brightness = brightness;
    }

    public float getSaturation() {
        return saturation;
    }

    public void setSaturation(float saturation) {
        this.saturation = saturation;
    }

    public float getContrast() {
        return contrast;
    }

    public void setContrast(float contrast) {
        this.contrast = contrast;
    }

    public float getWarmth() {
        return warmth;
    }

    public void setWarmth(float warmth) {
        this.warmth = warmth;
    }

    public float getOpacity() {
        return opacity;
    }

    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }

    public boolean isFlipHorizontal() {
        return flipHorizontal;
    }

    public void setFlipHorizontal(boolean flipHorizontal) {
        this.flipHorizontal = flipHorizontal;
    }

    public void flipHorizontal() {
        this.flipHorizontal = !this.flipHorizontal;
    }

    public boolean isFlipVertical() {
        return flipVertical;
    }

    public void setFlipVertical(boolean flipVertical) {
        this.flipVertical = flipVertical;
    }

    public void flipVertical() {
        this.flipVertical = !this.flipVertical;
    }

    public String getPathImage() {
        return pathImage;
    }

    public void setPathImage(String pathImage) {
        this.pathImage = pathImage;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
    }

    public long getTimeUpdated() {
        return timeUpdated;
    }

    public void setTimeUpdated(long timeUpdated) {
        this.timeUpdated = timeUpdated;
    }

    public final void setCenterX(float x, float width) {
        this.centerPercentX = x / width;
    }

    public final void setCenterY(float y, float height) {
        this.centerPercentY = y / height;
    }

    public final float getCenterX(float f) {
        return this.centerPercentX * f;
    }

    public final float getCenterY(float f) {
        return this.centerPercentY * f;
    }

}
