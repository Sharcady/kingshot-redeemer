package com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.rest.dto

data class RedeemGiftCodesRequest(
    val playerId: Long,
    val kingdom: Long,
    val name: String,
)
