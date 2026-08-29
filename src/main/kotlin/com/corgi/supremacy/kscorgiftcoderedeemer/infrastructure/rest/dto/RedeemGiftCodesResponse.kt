package com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.rest.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Outcome of registering a player and redeeming the active gift codes.")
data class RedeemGiftCodesResponse(
    @field:Schema(description = "Human-readable redemption summary.", example = "Processed 3 gift code(s) for Arkadiy (123456789) in kingdom 23. Redeemed: CODE1.")
    val message: String,
    @field:Schema(description = "Optional random image encoded for use by a Discord bot.", nullable = true)
    val image: DiscordImageResponse?,
)

@Schema(description = "Image payload encoded as Base64 for a Discord-friendly response.")
data class DiscordImageResponse(
    @field:Schema(description = "Original image file name.", example = "success.png")
    val fileName: String,
    @field:Schema(description = "MIME type of the image.", example = "image/png")
    val mediaType: String,
    @field:Schema(description = "Base64-encoded image bytes.", example = "iVBORw0KGgoAAAANSUhEUg...")
    val base64Content: String,
)
