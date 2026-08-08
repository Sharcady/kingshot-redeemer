package com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase

import com.corgi.supremacy.kscorgiftcoderedeemer.domain.Player
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.GiftCodePort
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.PlayerRepositoryPort
import org.springframework.stereotype.Component

@Component
class RedeemGiftCodesForRegisteredPlayersUseCase(
    private val giftCodePort: GiftCodePort,
    private val playerRepositoryPort: PlayerRepositoryPort,
) {
    fun execute(): RegisteredPlayersRedeemResult {
        val players = playerRepositoryPort.findAll()
        val giftCodes = giftCodePort.findActiveGiftCodes()
        val redeemedPlayers = mutableListOf<Player>()
        val failedPlayers = mutableListOf<Player>()

        players.forEach { player ->
            try {
                giftCodePort.redeemGiftCodes(
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
