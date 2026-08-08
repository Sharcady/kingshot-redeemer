package com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase

import com.corgi.supremacy.kscorgiftcoderedeemer.domain.Player
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.GiftCodeRedemptionResult
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.GiftCodePort
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.PlayerRepositoryPort
import org.springframework.stereotype.Component

@Component
class RegisterAndRedeemGiftCodesUseCase(
    private val giftCodePort: GiftCodePort,
    private val playerRepositoryPort: PlayerRepositoryPort,
) {
    fun execute(playerId: Long, kingdom: Long): RedeemGiftCodeResult {
        val player = Player(playerId.toString(), kingdom)
        playerRepositoryPort.save(player)

        val giftCodes = giftCodePort.findActiveGiftCodes()
        val redemptionResults = giftCodePort.redeemGiftCodes(player, giftCodes)

        return RedeemGiftCodeResult(
            player = player,
            redemptionResults = redemptionResults,
        )
    }
}

data class RedeemGiftCodeResult(
    val player: Player,
    val redemptionResults: List<GiftCodeRedemptionResult>,
)
