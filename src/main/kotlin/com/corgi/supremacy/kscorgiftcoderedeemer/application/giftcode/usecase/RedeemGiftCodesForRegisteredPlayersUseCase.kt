package com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase

import com.corgi.supremacy.kscorgiftcoderedeemer.domain.Player
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.PlayerRepositoryPort
import org.slf4j.LoggerFactory
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
            logger.info(
                "Starting redemption of {} active gift code(s) for registered player {} in kingdom {}.",
                giftCodes.size,
                player.id,
                player.kingdom,
            )
            try {
                redeemGiftCodesForPlayerUseCase.execute(
                    player = player,
                    giftCodes = giftCodes,
                )
                redeemedPlayers.add(player)
                logger.info("Finished redemption for registered player {} in kingdom {}.", player.id, player.kingdom)
            } catch (exception: Exception) {
                logger.error("Unable to redeem gift codes for registered player {} in kingdom {}.", player.id, player.kingdom, exception)
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

private val logger = LoggerFactory.getLogger(RedeemGiftCodesForRegisteredPlayersUseCase::class.java)

data class RegisteredPlayersRedeemResult(
    val giftCodes: List<String>,
    val redeemedPlayers: List<Player>,
    val failedPlayers: List<Player>,
)
