package com.huann305.app.data.sever.model

import com.google.gson.annotations.SerializedName

data class VideoCategoryRoot(
    val data: VideoCategoryData,
)

data class VideoCategoryData(
    @SerializedName("current_page")
    val currentPage: Long,
    val data: List<VideoCategoryModel>,
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

data class VideoCategoryModel(
    val id: Long,
    val label: String,
    @SerializedName("label_md5")
    val labelMd5: String,
    val order: Long,
    @SerializedName("count_generate")
    val countGenerate: Long,
    @SerializedName("daily_generate")
    val dailyGenerate: Long,
    val active: Long,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    val models: List<Model>,
)

data class Model(
    val id: Long,
    val label: String,
    @SerializedName("image_model_id")
    val imageModelId: Long?,
    val prompt: String,
    val thumbnail: Thumbnail,
    val video: Video,
    @SerializedName("webp_file")
    val webpFile: WebpFile,
    val order: Long,
    val active: Long,
    @SerializedName("count_generate")
    val countGenerate: Long,
    @SerializedName("daily_generate")
    val dailyGenerate: Long,
    val pivot: Pivot,
)

data class Pivot(
    @SerializedName("category_id")
    val categoryId: Long,
    @SerializedName("model_id")
    val modelId: Long,
)
