package com.corgi.supremacy.kscorgiftcoderedeemer.domain

data class GiftCodeRedemptionResult(
    val giftCode: String,
    val status: GiftCodeRedemptionStatus,
    val message: String,
)

enum class GiftCodeRedemptionStatus {
    REDEEMED,
    ALREADY_REDEEMED,
    INVALID,
}
