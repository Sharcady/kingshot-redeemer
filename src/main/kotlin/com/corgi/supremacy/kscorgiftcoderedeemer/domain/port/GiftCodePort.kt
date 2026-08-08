package com.corgi.supremacy.kscorgiftcoderedeemer.domain.port

import com.corgi.supremacy.kscorgiftcoderedeemer.domain.GiftCodeRedeemingException
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.GiftCodeRedemptionResult
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.GiftCodesRetrievalException
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.Player

interface GiftCodePort {
    @Throws(GiftCodesRetrievalException::class)
    fun findActiveGiftCodes(): List<String>

    @Throws(GiftCodeRedeemingException::class)
    fun redeemGiftCodes(player: Player, giftCodes: List<String>): List<GiftCodeRedemptionResult>
}
