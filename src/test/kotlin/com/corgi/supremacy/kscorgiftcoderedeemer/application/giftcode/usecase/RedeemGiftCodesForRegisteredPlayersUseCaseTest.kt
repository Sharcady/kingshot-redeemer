package com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase

import com.corgi.supremacy.kscorgiftcoderedeemer.domain.FailedGiftCodeRedemption
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.GiftCodeRedemptionResult
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.GiftCodeRedemptionStatus
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.Player
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.FailedGiftCodeRedemptionRepositoryPort
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.GiftCodePort
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.PlayerRepositoryPort
import kotlin.test.Test
import kotlin.test.assertEquals

class RedeemGiftCodesForRegisteredPlayersUseCaseTest {

    @Test
    fun `continues with every player after an earlier player fails`() {
        val firstPlayer = Player(id = "1", kingdom = 23, name = "First")
        val secondPlayer = Player(id = "2", kingdom = 23, name = "Second")
        val thirdPlayer = Player(id = "3", kingdom = 23, name = "Third")
        val giftCodePort = FailingFirstPlayerGiftCodePort(firstPlayer)
        val useCase = RedeemGiftCodesForRegisteredPlayersUseCase(
            FakePlayerRepositoryPort(listOf(firstPlayer, secondPlayer, thirdPlayer)),
            RedeemGiftCodesForPlayerUseCase(giftCodePort, FakeFailedGiftCodeRedemptionRepositoryPort()),
        )

        val result = useCase.execute(listOf("CODE"))

        assertEquals(listOf(firstPlayer, secondPlayer, thirdPlayer), giftCodePort.attemptedPlayers)
        assertEquals(listOf(firstPlayer), result.failedPlayers)
        assertEquals(listOf(secondPlayer, thirdPlayer), result.redeemedPlayers)
    }

    private class FailingFirstPlayerGiftCodePort(
        private val failingPlayer: Player,
    ) : GiftCodePort {
        val attemptedPlayers = mutableListOf<Player>()

        override fun findActiveGiftCodes(): List<String> = emptyList()

        override fun redeemGiftCodes(player: Player, giftCodes: List<String>): List<GiftCodeRedemptionResult> {
            attemptedPlayers += player
            if (player == failingPlayer) {
                throw Exception("Temporary browser failure")
            }
            return giftCodes.map { GiftCodeRedemptionResult(it, GiftCodeRedemptionStatus.REDEEMED, "Redeemed") }
        }
    }

    private class FakePlayerRepositoryPort(
        private val players: List<Player>,
    ) : PlayerRepositoryPort {
        override fun save(player: Player) = Unit

        override fun removeById(playerId: Long): Boolean = false

        override fun findAll(): List<Player> = players
    }

    private class FakeFailedGiftCodeRedemptionRepositoryPort : FailedGiftCodeRedemptionRepositoryPort {
        private var failedRedemptions: List<FailedGiftCodeRedemption> = emptyList()

        override fun findAll(): List<FailedGiftCodeRedemption> = failedRedemptions

        override fun replaceAll(failedRedemptions: List<FailedGiftCodeRedemption>) {
            this.failedRedemptions = failedRedemptions
        }
    }
}
