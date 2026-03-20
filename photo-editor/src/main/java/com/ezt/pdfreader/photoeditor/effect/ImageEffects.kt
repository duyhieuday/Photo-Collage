package com.ezt.pdfreader.photoeditor.effect

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter

/**
 * Utility class for creating ColorMatrix effects.
 *
 * Used for Adjust panel (brightness, contrast, sharpen).
 * These effects are applied directly on ImageView via ColorFilter,
 * not stored in cache.
 */
object ImageEffects {

    /**
     * Create ColorFilter from adjust values
     */
    fun createAdjustFilter(
        brightness: Float = 0f,
        contrast: Float = 0f,
        sharpen: Float = 0f
    ): ColorMatrixColorFilter? {
        if (brightness == 0f && contrast == 0f && sharpen == 0f) {
            return null // No filter needed
        }
        return ColorMatrixColorFilter(createAdjustMatrix(brightness, contrast, sharpen))
    }

    /**
     * Create a combined ColorMatrix for all adjust effects
     *
     * @param brightness Value from -100 to 100 (0 = no change)
     * @param contrast Value from -100 to 100 (0 = no change)
     * @param sharpen Value from -100 to 100 (0 = no change) - affects saturation/sharpness perception
     */
    private fun createAdjustMatrix(
        brightness: Float = 0f,
        contrast: Float = 0f,
        sharpen: Float = 0f
    ): ColorMatrix {
        val result = ColorMatrix()

        // Apply brightness
        if (brightness != 0f) {
            result.postConcat(createBrightnessMatrix(brightness))
        }

        // Apply contrast
        if (contrast != 0f) {
            result.postConcat(createContrastMatrix(contrast))
        }

        // Apply sharpen (saturation boost for perception of sharpness)
        if (sharpen != 0f) {
            result.postConcat(createSharpenMatrix(sharpen))
        }

        return result
    }

    /**
     * Brightness matrix
     * @param value -100 to 100
     */
    private fun createBrightnessMatrix(value: Float): ColorMatrix {
        val brightness = value * 255f / 100f
        return ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, brightness,
                0f, 1f, 0f, 0f, brightness,
                0f, 0f, 1f, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    /**
     * Contrast matrix
     * @param value -100 to 100
     */
    private fun createContrastMatrix(value: Float): ColorMatrix {
        val contrast = (value + 100f) / 100f // Convert to 0..2 range
        val translate = 128f * (1f - contrast)
        return ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    /**
     * Details matrix (saturation adjustment for perceived sharpness)
     * @param value -100 to 100
     */
    private fun createSharpenMatrix(value: Float): ColorMatrix {
        // Positive sharpen = increase saturation slightly
        // Negative sharpen = decrease saturation
        val saturation = 1f + (value / 200f) // 0.5 to 1.5 range

        val matrix = ColorMatrix()
        matrix.setSaturation(saturation)
        return matrix
    }

}
