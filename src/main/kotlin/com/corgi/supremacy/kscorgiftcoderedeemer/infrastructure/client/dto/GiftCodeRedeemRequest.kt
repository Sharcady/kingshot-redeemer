package com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.client.dto

data class GiftCodeRedeemRequest(
    val playerId: String,
    val giftCode: String,
    val kingdom: Long
)
