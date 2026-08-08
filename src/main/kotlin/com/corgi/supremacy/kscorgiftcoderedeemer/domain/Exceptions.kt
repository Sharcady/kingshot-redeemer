package com.corgi.supremacy.kscorgiftcoderedeemer.domain

class PlayerNotFoundException(playerId: Long, kingdom: Long): RuntimeException("Player $playerId kingdom $kingdom not found")

class ImageRetrievalException: RuntimeException("Image retrieval failed")

class GiftCodesRetrievalException: RuntimeException("GiftCode retrieval failed")

class GiftCodeRedeemingException: RuntimeException("GiftCode redeeming failed")