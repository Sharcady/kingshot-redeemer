package com.corgi.supremacy.kscorgiftcoderedeemer.domain.port

import com.corgi.supremacy.kscorgiftcoderedeemer.domain.GiftCodeRedeemingException
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.GiftCodesRetrievalException
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.Player
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.PlayerNotFoundException

interface GiftCodePort {
    @Throws(PlayerNotFoundException::class)
    fun findPlayerByIdAndKingdom(playerId: Long, kingdom: Long): Player

    @Throws(GiftCodesRetrievalException::class)
    fun findActiveGiftCodes(): List<String>

    @Throws(GiftCodeRedeemingException::class)
    fun redeemGiftCodes(
        playerId: Long,
        kingdom: Long,
        giftCodes: List<String>,
        onPlayerFound: (Player) -> Unit = {},
    ): Player
}
