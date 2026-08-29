package com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase

import com.corgi.supremacy.kscorgiftcoderedeemer.domain.Player
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.PlayerRepositoryPort
import org.springframework.stereotype.Component

@Component
class RedeemGiftCodesForRegisteredPlayersUseCase(
    private val playerRepositoryPort: PlayerRepositoryPort,
    private val redeemGiftCodesForPlayerUseCase: RedeemGiftCodesForPlayerUseCase,
) {
    fun execute(giftCodes: List<String>): RegisteredPlayersRedeemResult {
        val players = playerRepositoryPort.findAll()
        val redeemedPlayers = mutableListOf<Player>()
        val failedPlayers = mutableListOf<Player>()

        players.forEach { player ->
            try {
                redeemGiftCodesForPlayerUseCase.execute(
                    player = player,
                    giftCodes = giftCodes,
                )
                redeemedPlayers.add(player)
            } catch (exception: RuntimeException) {
                failedPlayers.add(player)
            }
        }

        return RegisteredPlayersRedeemResult(
            giftCodes = giftCodes,
            redeemedPlayers = redeemedPlayers,
            failedPlayers = failedPlayers,
        )
    }
}

data class RegisteredPlayersRedeemResult(
    val giftCodes: List<String>,
    val redeemedPlayers: List<Player>,
    val failedPlayers: List<Player>,
)
