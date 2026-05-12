package com.example.piceditor.sever.ai_remove_bg.model

import com.example.piceditor.sever.ai_remove_bg.Url
import com.google.gson.annotations.SerializedName

/**
 * Created by Huann on 3/13/2026 1:56 PM
 */


data class VideoModelRoot(
    val data: VideoModelData,
)

data class VideoModelData(
    @SerializedName("current_page")
    val currentPage: Long,
    val data: List<VideoModel>,
    @SerializedName("first_page_url")
    val firstPageUrl: String,
    val from: Long,
    @SerializedName("next_page_url")
    val nextPageUrl: Any?,
    val path: String,
    @SerializedName("per_page")
    val perPage: Long,
    @SerializedName("prev_page_url")
    val prevPageUrl: Any?,
    val to: Long,
)

data class VideoModel(
    val id: Long,
    val label: String,
    val order: Long,
    val thumbnail: Thumbnail,
    val video: Video,
    @SerializedName("image_model_id")
    val imageModelId: Long?,
    @SerializedName("count_generate")
    val countGenerate: Long,
    @SerializedName("daily_generate")
    val dailyGenerate: Long,
    val prompt: String,
    val active: Long,
)

data class Thumbnail(
    val disk: String,
    val path: String,
    val size: Size,
    val url: Url,
)

data class Size(
    val width: Long,
    val height: Long,
)

data class Url(
    val full: String,
    val large: String,
    val medium: String,
    val small: String,
    @SerializedName("extra_small")
    val extraSmall: String,
)

data class Video(
    val disk: String,
    val path: String,
    val size: Size2,
    val url: Url2,
)
data class WebpFile(
    val disk: String,
    val path: String,
    val size: Size2,
    val url: Url2,
)

data class Size2(
    val width: Long,
    val height: Long,
)

data class Url2(
    val full: String,
)

data class SynapzModel(
    val id: Long,
    val label: String,
    val prompt: String,
)