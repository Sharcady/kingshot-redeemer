package com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.rest.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Player details required to register the player and redeem the current gift codes.")
data class RedeemGiftCodesRequest(
    @field:Schema(description = "Kingshot player ID.", example = "123456789", requiredMode = Schema.RequiredMode.REQUIRED)
    val playerId: Long,
    @field:Schema(description = "Kingshot kingdom number.", example = "23", requiredMode = Schema.RequiredMode.REQUIRED)
    val kingdom: Long,
    @field:Schema(description = "Player name displayed in responses.", example = "Arkadiy", requiredMode = Schema.RequiredMode.REQUIRED)
    val name: String,
)
