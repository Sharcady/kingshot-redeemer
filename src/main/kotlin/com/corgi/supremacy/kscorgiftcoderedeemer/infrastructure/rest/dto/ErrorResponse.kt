package com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.rest.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Error returned when external gift-code automation fails.")
data class ErrorResponse(
    @field:Schema(description = "Description of the failure.", example = "GiftCode redeeming failed")
    val message: String,
    @field:Schema(description = "Optional underlying error detail.", example = "Timed out waiting for redemption result.", nullable = true)
    val cause: String?,
)
