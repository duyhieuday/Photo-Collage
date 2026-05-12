package com.example.piceditor.sever.ai_remove_bg.repository

import com.example.piceditor.sever.ai_remove_bg.api.CreditsEncryptedRequest
import com.example.piceditor.sever.ai_remove_bg.api.WalletApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale.getDefault

/**
 * Created by Huann on 3/26/2026 9:46 AM
 */

class WalletRepository(
    private val walletApi: WalletApi
) {

    private val _coin = MutableStateFlow<Long?>(null)
    val coin: StateFlow<Long?> = _coin.asStateFlow()

    suspend fun fetchCoin(): Long {
        val response = walletApi.getCredits()

        val credits = response.data.credit

        _coin.value = credits
        return credits
    }

    suspend fun addCredits(
        encryptedData: String
    ): Long {
        val response = walletApi.addCredits(
            body = CreditsEncryptedRequest(data = encryptedData)
        )

        if (response.data.status?.lowercase(getDefault()) != "success") {
            throw IllegalStateException(response.data.error ?: "Update credits failed")
        }

        val credits = response.data.credit
            ?: throw IllegalStateException("Credits is null")

        _coin.value = credits.balance
        return credits.balance
    }

    suspend fun consumeCredits(
        encryptedData: String
    ): Long {
        val response = walletApi.consumeCredits(
            body = CreditsEncryptedRequest(data = encryptedData)
        )

        if (response.data.status?.lowercase(getDefault()) != "success") {
            throw IllegalStateException(response.data.error ?: "Consume credits failed")
        }

        val credits = response.data.credit
            ?: throw IllegalStateException("Credits is null")

        _coin.value = credits.balance
        return credits.balance
    }

    fun updateLocalCoin(credits: Long) {
        _coin.value = credits
    }

    fun clearCoin() {
        _coin.value = null
    }
}