package com.example.piceditor.sever.ai_remove_bg.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Created by Huann on 3/18/2026 4:11 PM
 */

data class AuthDeviceRequest(
    val secret: String
)

data class AuthDeviceResponseRoot(
    val data: AuthDeviceResponse
)

data class AuthDeviceResponse(
    @SerializedName("access_token")
    val accessToken: String?,
    val device: Device,
)

data class Device(
    val id: Long,
    @SerializedName("user_id")
    val userId: Long,
    val name: String,
    @SerializedName("device_id")
    val deviceId: String,
    val active: Long,
    val type: Long,
    @SerializedName("status_package")
    val statusPackage: Long,
    @SerializedName("indentify_id")
    val indentifyId: Any?,
    val coins: Long,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("queue_count")
    val queueCount: Long,
    @SerializedName("queue_count_daily")
    val queueCountDaily: Long,
    @SerializedName("is_premium")
    val isPremium: Boolean,
    val premium: Premium?,
)

data class Premium(
    val id: Long,
    @SerializedName("device_id")
    val deviceId: Long,
    @SerializedName("expires_at")
    val expiresAt: Long,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
)

data class PremiumRequest(
    val data: String
)

data class PremiumResponse(
    val status: String,
    val message: String
)

interface DeviceApiService {
    @POST("auth/device")
    suspend fun authDevice(
        @Body request: AuthDeviceRequest
    ): AuthDeviceResponseRoot

    @POST("devices/premium")
    suspend fun getPremium(
        @Header("AuthorizationApi") token: String,
        @Body request: PremiumRequest
    ): Response<PremiumResponse>
}