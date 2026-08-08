package com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase

import com.corgi.supremacy.kscorgiftcoderedeemer.domain.Player
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.GiftCodePort
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.PlayerRepositoryPort
import org.springframework.stereotype.Component

@Component
class RedeemGiftCodeUseCase(
    private val giftCodePort: GiftCodePort,
    private val playerRepositoryPort: PlayerRepositoryPort,
) {
    fun execute(playerId: Long, kingdom: Long): RedeemGiftCodeResult {
        val giftCodes = giftCodePort.findActiveGiftCodes()
        val player = giftCodePort.redeemGiftCodes(playerId, kingdom, giftCodes, playerRepositoryPort::save)

        return RedeemGiftCodeResult(
            player = player,
            redeemedGiftCodes = giftCodes,
        )
    }
}

data class RedeemGiftCodeResult(
    val player: Player,
    val redeemedGiftCodes: List<String>,
)
