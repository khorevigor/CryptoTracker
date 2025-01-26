package com.plcoding.cryptotracker.crypto.data.networking.dto

import kotlinx.serialization.Serializable

@Serializable
data class CoinPriceHistoryDto (
    val data: List<CoinPriceDto>
)
