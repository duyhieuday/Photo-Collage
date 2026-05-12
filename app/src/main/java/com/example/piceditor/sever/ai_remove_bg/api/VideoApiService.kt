package com.example.piceditor.sever.ai_remove_bg.api

import com.example.piceditor.sever.ai_remove_bg.model.VideoCategoryRoot
import com.example.piceditor.sever.ai_remove_bg.model.VideoModelRoot
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

data class AiVideoResponse(
    @SerializedName("data")
    val data: AiVideoJobData? = null
)

data class AiVideoJobData(
    @SerializedName("type")
    val type: Int? = null,

    @SerializedName("status")
    val status: Int? = null,

    @SerializedName("source")
    val source: Int? = null,

    @SerializedName("device_id")
    val deviceId: Long? = null,

    @SerializedName("model_id")
    val modelId: String? = null,

    @SerializedName("ip")
    val ip: String? = null,

    @SerializedName("app_id")
    val appId: Int? = null,

    @SerializedName("ip_server")
    val ipServer: String? = null,

    @SerializedName("data")
    val payload: AiVideoPayload? = null,

    @SerializedName("server_name")
    val serverName: String? = null,

    @SerializedName("user_id")
    val userId: Long? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("id")
    val id: Long,

    @SerializedName("queue_name")
    val queueName: String? = null
)

data class AiVideoPayload(
    @SerializedName("path")
    val path: String? = null,

    @SerializedName("api_params")
    val apiParams: AiVideoApiParams? = null,

    @SerializedName("url")
    val url: String? = null
)

data class AiVideoApiParams(
    @SerializedName("prompt")
    val prompt: String? = null,
    @SerializedName("url")
    val url: String? = null,
)

data class NewChapterPromptGenArtResponse(
    val data: NewChapterPromptGenArtResponseData
)

data class NewChapterPromptGenArtResponseData(
    val id: Int
)

data class QueueResponseRoot(
    val data: QueueResponseData
)

data class QueueResponseData(
    val id: Int,
    val type: Int,
    val value: QueueResponseValue? = null,
    val status: Int
)

data class QueueResponseValue(
    val path: String,
    val type: String,
    val url: String,
    val prompt: String? = null
)

data class ApiErrorResponse(
    @SerializedName("message")
    val message: String? = null,

    @SerializedName("errors")
    val errors: Map<String, List<String>>? = null
)

interface VideoApiService {

    @GET("ai_videos/models")
    suspend fun getAllModel(
        @Query("app_id") appID: String,
        @Query("with") styleId: String = "synapzModel+id,label,prompt",
    ): VideoModelRoot

    @GET("ai_videos/categories")
    suspend fun getAllCategory(
        @Query("app_id") appID: String,
    ): VideoCategoryRoot

    @Multipart
    @POST("ai_videos")
    suspend fun generateAiVideo(
        @Part image: MultipartBody.Part,
        @Part("model_id") modelId: RequestBody
    ): Response<AiVideoResponse>

    @Multipart
    @POST("images/edit-image")
    suspend fun generateAiImage(
        @Part image: MultipartBody.Part?,
        @Part("app_id") appID: RequestBody?,
        @Part("accept") accept: RequestBody?,
        @Part("type") type: RequestBody?,
        @Part("model_id") modelId: RequestBody?,
    ): NewChapterPromptGenArtResponse

    @GET("queues-ai/{queue_id}")
    suspend fun getQueue(@Path("queue_id") queueId: Int,): QueueResponseRoot
}