package com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase

import com.corgi.supremacy.kscorgiftcoderedeemer.domain.Player
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.GiftCodeRedemptionResult
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.PlayerRepositoryPort
import org.springframework.stereotype.Component

@Component
class RegisterAndRedeemGiftCodesUseCase(
    private val playerRepositoryPort: PlayerRepositoryPort,
    private val retrieveActiveGiftCodesUseCase: RetrieveActiveGiftCodesUseCase,
    private val redeemGiftCodesForPlayerUseCase: RedeemGiftCodesForPlayerUseCase,
    private val updateActiveGiftCodesUseCase: UpdateActiveGiftCodesUseCase,
) {
    fun execute(playerId: Long, kingdom: Long, name: String): RedeemGiftCodeResult {
        val player = Player(id = playerId.toString(), kingdom = kingdom, name = name)
        playerRepositoryPort.save(player)

        val giftCodes = retrieveActiveGiftCodesUseCase.execute()
        val redemptionResults = try {
            redeemGiftCodesForPlayerUseCase.execute(player, giftCodes)
        } finally {
            updateActiveGiftCodesUseCase.execute(giftCodes)
        }

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
