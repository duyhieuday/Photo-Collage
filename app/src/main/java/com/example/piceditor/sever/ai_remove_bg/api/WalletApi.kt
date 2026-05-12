package com.example.piceditor.sever.ai_remove_bg.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Created by Huann on 3/16/2026 9:39 AM
 */

data class FetchCreditsResponse(
    val data: FetchCreditsResponseData,
)

data class FetchCreditsResponseData(
    val credit: Long,
    val fee: Fee,
    val promotion: Promotion,
)

data class Fee(
    @SerializedName("video_gen")
    val videoGen: Long,
)

data class Promotion(
    @SerializedName("view_ads")
    val viewAds: Long,
    @SerializedName("daily_claim")
    val dailyClaim: Long,
)

data class CreditsResponse(
    @SerializedName("data")
    val data: CreditsResponseData
)

data class CreditsResponseData(
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("error")
    val error: String? = null,
    @SerializedName("credit")
    val credit: Credit? = null
)

data class Credit(val balance: Long)

data class CreditsEncryptedRequest(
    @SerializedName("data")
    val data: String
)

interface WalletApi {

    @GET("credits")
    suspend fun getCredits(
        @Header("Accept") accept: String = "application/json"
    ): FetchCreditsResponse

    @POST("credits")
    suspend fun addCredits(
        @Header("Accept") accept: String = "application/json",
        @Header("Content-Type") contentType: String = "application/json",
        @Body body: CreditsEncryptedRequest
    ): CreditsResponse

    @POST("credits/consume")
    suspend fun consumeCredits(
        @Header("Content-Type") contentType: String = "application/json",
        @Body body: CreditsEncryptedRequest
    ): CreditsResponse
}