package com.example.piceditor.draw.sticker;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;

public class ImageMatrix {

    private final float[] m = new float[4 * 5];
    private final ColorMatrix mColorMatrix = new ColorMatrix();
    private final ColorMatrix mTmpColorMatrix = new ColorMatrix();
    private boolean mDirty = false;
    private float mBrightness = 1;
    private float mSaturation = 1;
    private float mContrast = 1;
    private float mWarmth = 1;

    public void setBrightness(float brightness) {
        if (mBrightness == brightness) {
            return;
        }
        this.mDirty = true;
        this.mBrightness = brightness;
    }

    public void setSaturation(float saturation) {
        if (mSaturation == saturation) {
            return;
        }
        this.mDirty = true;
        this.mSaturation = saturation;
    }

    public void setContrast(float contrast) {
        if (mContrast == contrast) {
            return;
        }
        this.mDirty = true;
        this.mContrast = contrast;
    }

    public void setWarmth(float warmth) {
        if (mWarmth == warmth) {
            return;
        }
        this.mDirty = true;
        this.mWarmth = warmth;
    }

    public ColorMatrixColorFilter updateMatrixIfNeed() {
        boolean filter = false;
        if (mDirty) {
            mDirty = false;
            mColorMatrix.reset();
            if (mSaturation != 1.0f) {
                saturation(mSaturation);
                mColorMatrix.set(m);
                filter = true;
            }
            if (mContrast != 1.0f) {
                mTmpColorMatrix.setScale(mContrast, mContrast, mContrast, 1);
                mColorMatrix.postConcat(mTmpColorMatrix);
                filter = true;
            }
            if (mWarmth != 1.0f) {
                warmth(mWarmth);
                mTmpColorMatrix.set(m);
                mColorMatrix.postConcat(mTmpColorMatrix);
                filter = true;
            }
            if (mBrightness != 1.0f) {
                brightness(mBrightness);
                mTmpColorMatrix.set(m);
                mColorMatrix.postConcat(mTmpColorMatrix);
                filter = true;
            }
        } else {
            filter = mSaturation != 1.0f || mContrast != 1.0f || mWarmth != 1.0f || mBrightness != 1.0f;
        }

        if (filter) {
            return new ColorMatrixColorFilter(mColorMatrix);
        } else {
            return null;
        }
    }

    private void saturation(float saturationStrength) {
        float Rf = 0.2999f;
        float Gf = 0.587f;
        float Bf = 0.114f;
        float S = saturationStrength;

        float MS = 1.0f - S;
        float Rt = Rf * MS;
        float Gt = Gf * MS;
        float Bt = Bf * MS;

        m[0] = (Rt + S);
        m[1] = Gt;
        m[2] = Bt;
        m[3] = 0;
        m[4] = 0;

        m[5] = Rt;
        m[6] = (Gt + S);
        m[7] = Bt;
        m[8] = 0;
        m[9] = 0;

        m[10] = Rt;
        m[11] = Gt;
        m[12] = (Bt + S);
        m[13] = 0;
        m[14] = 0;

        m[15] = 0;
        m[16] = 0;
        m[17] = 0;
        m[18] = 1;
        m[19] = 0;
    }

    private void warmth(float warmth) {
        float baseTemperature = 5000;
        if (warmth <= 0) warmth = .01f;
        float tmpColor_r;
        float tmpColor_g;
        float tmpColor_b;

        float kelvin = baseTemperature / warmth;
        { // simulate a black body radiation
            float centiKelvin = kelvin / 100;
            float colorR, colorG, colorB;
            if (centiKelvin > 66) {
                float tmp = centiKelvin - 60.f;
                colorR = (329.698727446f * (float) Math.pow(tmp, -0.1332047592f));
                colorG = (288.1221695283f * (float) Math.pow(tmp, 0.0755148492f));

            } else {
                colorG = (99.4708025861f * (float) Math.log(centiKelvin) - 161.1195681661f);
                colorR = 255;
            }
            if (centiKelvin < 66) {
                if (centiKelvin > 19) {
                    colorB = (138.5177312231f * (float) Math.log(centiKelvin - 10) - 305.0447927307f);
                } else {
                    colorB = 0;
                }
            } else {
                colorB = 255;
            }
            tmpColor_r = Math.min(255, Math.max(colorR, 0));
            tmpColor_g = Math.min(255, Math.max(colorG, 0));
            tmpColor_b = Math.min(255, Math.max(colorB, 0));
        }

        float color_r = tmpColor_r;
        float color_g = tmpColor_g;
        float color_b = tmpColor_b;
        kelvin = baseTemperature;
        { // simulate a black body radiation
            float centiKelvin = kelvin / 100f;
            float colorR, colorG, colorB;
            if (centiKelvin > 66) {
                float tmp = centiKelvin - 60.f;
                colorR = (329.698727446f * (float) Math.pow(tmp, -0.1332047592f));
                colorG = (288.1221695283f * (float) Math.pow(tmp, 0.0755148492f));

            } else {
                colorG = (99.4708025861f * (float) Math.log(centiKelvin) - 161.1195681661f);
                colorR = 255;
            }
            if (centiKelvin < 66) {
                if (centiKelvin > 19) {
                    colorB = (138.5177312231f * (float) Math.log(centiKelvin - 10) - 305.0447927307f);
                } else {
                    colorB = 0;
                }
            } else {
                colorB = 255;
            }
            tmpColor_r = Math.min(255, Math.max(colorR, 0));
            tmpColor_g = Math.min(255, Math.max(colorG, 0));
            tmpColor_b = Math.min(255, Math.max(colorB, 0));
        }

        color_r /= tmpColor_r;
        color_g /= tmpColor_g;
        color_b /= tmpColor_b;
        m[0] = color_r;
        m[1] = 0;
        m[2] = 0;
        m[3] = 0;
        m[4] = 0;

        m[5] = 0;
        m[6] = color_g;
        m[7] = 0;
        m[8] = 0;
        m[9] = 0;

        m[10] = 0;
        m[11] = 0;
        m[12] = color_b;
        m[13] = 0;
        m[14] = 0;

        m[15] = 0;
        m[16] = 0;
        m[17] = 0;
        m[18] = 1;
        m[19] = 0;
    }

    private void brightness(float brightness) {

        m[0] = brightness;
        m[1] = 0;
        m[2] = 0;
        m[3] = 0;
        m[4] = 0;

        m[5] = 0;
        m[6] = brightness;
        m[7] = 0;
        m[8] = 0;
        m[9] = 0;

        m[10] = 0;
        m[11] = 0;
        m[12] = brightness;
        m[13] = 0;
        m[14] = 0;

        m[15] = 0;
        m[16] = 0;
        m[17] = 0;
        m[18] = 1;
        m[19] = 0;
    }
}
