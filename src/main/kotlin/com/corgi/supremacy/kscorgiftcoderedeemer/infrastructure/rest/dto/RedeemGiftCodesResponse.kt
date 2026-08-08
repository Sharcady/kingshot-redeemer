package com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.rest.dto

data class RedeemGiftCodesResponse(
    val message: String,
    val image: DiscordImageResponse?,
)

data class DiscordImageResponse(
    val fileName: String,
    val mediaType: String,
    val base64Content: String,
)
