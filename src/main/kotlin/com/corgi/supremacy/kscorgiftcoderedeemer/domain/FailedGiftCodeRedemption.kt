package com.corgi.supremacy.kscorgiftcoderedeemer.domain

data class FailedGiftCodeRedemption(
    val player: Player,
    val giftCode: String,
    val retryAttempts: Int = 0,
)
