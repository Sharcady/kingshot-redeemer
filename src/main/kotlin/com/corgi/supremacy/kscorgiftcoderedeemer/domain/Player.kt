package com.corgi.supremacy.kscorgiftcoderedeemer.domain

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A player registered for scheduled gift-code redemption.")
data class Player(
    @field:Schema(description = "Kingshot player ID.", example = "123456789")
    val id: String,
    @field:Schema(description = "Kingshot kingdom number.", example = "23")
    val kingdom: Long,
    @field:Schema(description = "Player name used in responses.", example = "Arkadiy")
    val name: String = id,
)
