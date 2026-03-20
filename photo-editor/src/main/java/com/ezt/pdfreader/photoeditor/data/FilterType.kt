package com.ezt.pdfreader.photoeditor.data

import android.content.Context
import com.bruno.nativeft.customize.base.BaseGPUFilter
import com.bruno.nativeft.customize.filters.BW1Filter
import com.bruno.nativeft.customize.filters.BlendAlphaFilter
import com.bruno.nativeft.customize.filters.ContrastFilter
import com.bruno.nativeft.customize.filters.CustomFilter
import com.bruno.nativeft.customize.filters.GrayscaleFilter
import com.bruno.nativeft.customize.filters.MiracleFilter
import com.bruno.nativeft.customize.filters.RawFilter
import com.bruno.nativeft.customize.filters.ReverseFilter

enum class FilterType(val displayName: String, val isPremium: Boolean = false) {
    NONE("Original"),
    AUTO("Auto", isPremium = true),
    CONTRAST("Contrast"),
    ENHANCE("Enhance", isPremium = true),
    MAGIC("Magic", isPremium = true),
    BW("BW", isPremium = true),
    GRAYSCALE("Grayscale"),
    INVERT("Invert");

    fun createGPUFilter(context: Context): BaseGPUFilter {
        return when (this) {
            AUTO -> MiracleFilter(context)
            CONTRAST -> ContrastFilter(context)
            ENHANCE -> CustomFilter(context)
            MAGIC -> BlendAlphaFilter(context)
            BW -> BW1Filter(context)
            GRAYSCALE -> GrayscaleFilter(context)
            INVERT -> ReverseFilter(context)
            NONE -> RawFilter(context)
        }
    }

    companion object {
        fun fromOrdinal(ordinal: Int): FilterType {
            return entries.getOrElse(ordinal) { NONE }
        }
    }
}
