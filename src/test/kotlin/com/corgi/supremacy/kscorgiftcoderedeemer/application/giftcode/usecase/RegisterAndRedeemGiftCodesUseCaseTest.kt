package com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase

import com.corgi.supremacy.kscorgiftcoderedeemer.domain.GiftCodeRedemptionResult
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.GiftCodeRedemptionStatus
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.Player
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.FailedGiftCodeRedemption
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.ActiveGiftCodeRepositoryPort
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.FailedGiftCodeRedemptionRepositoryPort
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.GiftCodePort
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.PlayerRepositoryPort
import kotlin.test.Test
import kotlin.test.assertEquals

class RegisterAndRedeemGiftCodesUseCaseTest {

    @Test
    fun `saves player and redeems active gift codes`() {
        val giftCodePort = FakeGiftCodePort()
        val playerRepositoryPort = FakePlayerRepositoryPort()
        val activeGiftCodeRepositoryPort = FakeActiveGiftCodeRepositoryPort()
        val failedGiftCodeRedemptionRepositoryPort = FakeFailedGiftCodeRedemptionRepositoryPort()
        val useCase = RegisterAndRedeemGiftCodesUseCase(
            playerRepositoryPort,
            RetrieveActiveGiftCodesUseCase(giftCodePort),
            RedeemGiftCodesForPlayerUseCase(giftCodePort, failedGiftCodeRedemptionRepositoryPort),
            UpdateActiveGiftCodesUseCase(activeGiftCodeRepositoryPort),
        )

        val result = useCase.execute(playerId = 202667117, kingdom = 1416, name = "Arkadiy")

        val expectedPlayer = Player(id = "202667117", kingdom = 1416, name = "Arkadiy")
        assertEquals(expectedPlayer, playerRepositoryPort.savedPlayers.single())
        assertEquals(expectedPlayer, giftCodePort.redeemedPlayer)
        assertEquals(listOf("HAPPYCATDAY", "Kingshot888"), giftCodePort.redeemedGiftCodes)
        assertEquals(listOf("HAPPYCATDAY", "Kingshot888"), activeGiftCodeRepositoryPort.activeGiftCodes)
        assertEquals(
            listOf(FailedGiftCodeRedemption(expectedPlayer, "HAPPYCATDAY")),
            failedGiftCodeRedemptionRepositoryPort.failedRedemptions,
        )
        assertEquals(expectedPlayer, result.player)
        assertEquals(
            listOf(
                GiftCodeRedemptionResult("HAPPYCATDAY", GiftCodeRedemptionStatus.INVALID, "Gift code not found or invalid."),
                GiftCodeRedemptionResult("Kingshot888", GiftCodeRedemptionStatus.ALREADY_REDEEMED, "You have already redeemed this gift code."),
            ),
            result.redemptionResults,
        )
    }

    private class FakeGiftCodePort : GiftCodePort {
        var redeemedPlayer: Player? = null
        var redeemedGiftCodes: List<String> = emptyList()

        override fun findActiveGiftCodes(): List<String> =
            listOf("HAPPYCATDAY", "Kingshot888")

        override fun redeemGiftCodes(player: Player, giftCodes: List<String>): List<GiftCodeRedemptionResult> {
            redeemedPlayer = player
            redeemedGiftCodes = giftCodes
            return listOf(
                GiftCodeRedemptionResult("HAPPYCATDAY", GiftCodeRedemptionStatus.INVALID, "Gift code not found or invalid."),
                GiftCodeRedemptionResult("Kingshot888", GiftCodeRedemptionStatus.ALREADY_REDEEMED, "You have already redeemed this gift code."),
            )
        }
    }

    private class FakePlayerRepositoryPort : PlayerRepositoryPort {
        val savedPlayers = mutableListOf<Player>()

        override fun save(player: Player) {
            savedPlayers.add(player)
        }

        override fun removeById(playerId: Long): Boolean =
            false

        override fun findAll(): List<Player> =
            savedPlayers
    }

    private class FakeActiveGiftCodeRepositoryPort : ActiveGiftCodeRepositoryPort {
        var activeGiftCodes: List<String> = emptyList()

        override fun findActiveGiftCodes(): List<String>? = activeGiftCodes

        override fun replaceActiveGiftCodes(giftCodes: List<String>) {
            activeGiftCodes = giftCodes
        }
    }

    private class FakeFailedGiftCodeRedemptionRepositoryPort : FailedGiftCodeRedemptionRepositoryPort {
        var failedRedemptions: List<FailedGiftCodeRedemption> = emptyList()

        override fun findAll(): List<FailedGiftCodeRedemption> =
            failedRedemptions

        override fun replaceAll(failedRedemptions: List<FailedGiftCodeRedemption>) {
            this.failedRedemptions = failedRedemptions
        }
    }
}
