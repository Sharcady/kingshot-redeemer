package com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase

import com.corgi.supremacy.kscorgiftcoderedeemer.domain.FailedGiftCodeRedemption
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.GiftCodeRedemptionResult
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.GiftCodeRedemptionStatus
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.Player
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.ActiveGiftCodeRepositoryPort
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.FailedGiftCodeRedemptionRepositoryPort
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.GiftCodePort
import kotlin.test.Test
import kotlin.test.assertEquals

class RetryFailedGiftCodeRedemptionsUseCaseTest {

    @Test
    fun `retries pending pairs but does not retry pairs that reached the limit`() {
        val player = Player(id = "202667117", kingdom = 1416, name = "Arkadiy")
        val failedRedemptions = FakeFailedGiftCodeRedemptionRepositoryPort(
            listOf(
                FailedGiftCodeRedemption(player, "RETRY-ME", retryAttempts = 9),
                FailedGiftCodeRedemption(player, "EXHAUSTED", retryAttempts = 10),
            ),
        )
        val giftCodePort = FakeGiftCodePort()
        val retryUseCase = RetryFailedGiftCodeRedemptionsUseCase(
            FakeActiveGiftCodeRepositoryPort(listOf("RETRY-ME", "EXHAUSTED")),
            failedRedemptions,
            RedeemGiftCodesForPlayerUseCase(giftCodePort, failedRedemptions),
        )

        val result = retryUseCase.execute()

        assertEquals(listOf("RETRY-ME"), giftCodePort.redeemedGiftCodes)
        assertEquals(1, result.processedPairs)
        assertEquals(listOf(FailedGiftCodeRedemption(player, "RETRY-ME", retryAttempts = 9)), result.failedPairs)
        assertEquals(
            listOf(
                FailedGiftCodeRedemption(player, "EXHAUSTED", retryAttempts = 10),
                FailedGiftCodeRedemption(player, "RETRY-ME", retryAttempts = 10),
            ),
            failedRedemptions.failedRedemptions.sortedBy { it.giftCode },
        )
    }

    @Test
    fun `removes a pair after a successful retry`() {
        val player = Player(id = "202667117", kingdom = 1416, name = "Arkadiy")
        val failedRedemptions = FakeFailedGiftCodeRedemptionRepositoryPort(
            listOf(FailedGiftCodeRedemption(player, "RETRY-ME", retryAttempts = 2)),
        )
        val retryUseCase = RetryFailedGiftCodeRedemptionsUseCase(
            FakeActiveGiftCodeRepositoryPort(listOf("RETRY-ME")),
            failedRedemptions,
            RedeemGiftCodesForPlayerUseCase(SuccessfulGiftCodePort(), failedRedemptions),
        )

        retryUseCase.execute()

        assertEquals(emptyList(), failedRedemptions.failedRedemptions)
    }

    @Test
    fun `removes inactive pairs without attempting another redemption`() {
        val player = Player(id = "202667117", kingdom = 1416, name = "Arkadiy")
        val failedRedemptions = FakeFailedGiftCodeRedemptionRepositoryPort(
            listOf(FailedGiftCodeRedemption(player, "EXPIRED-CODE", retryAttempts = 2)),
        )
        val giftCodePort = FakeGiftCodePort()
        val retryUseCase = RetryFailedGiftCodeRedemptionsUseCase(
            FakeActiveGiftCodeRepositoryPort(listOf("CURRENT-CODE")),
            failedRedemptions,
            RedeemGiftCodesForPlayerUseCase(giftCodePort, failedRedemptions),
        )

        val result = retryUseCase.execute()

        assertEquals(emptyList(), giftCodePort.redeemedGiftCodes)
        assertEquals(0, result.processedPairs)
        assertEquals(1, result.discardedInactivePairs)
        assertEquals(emptyList(), failedRedemptions.failedRedemptions)
    }

    private class FakeGiftCodePort : GiftCodePort {
        var redeemedGiftCodes: List<String> = emptyList()

        override fun findActiveGiftCodes(): List<String> = emptyList()

        override fun redeemGiftCodes(player: Player, giftCodes: List<String>): List<GiftCodeRedemptionResult> {
            redeemedGiftCodes = giftCodes
            return giftCodes.map { GiftCodeRedemptionResult(it, GiftCodeRedemptionStatus.INVALID, "Invalid") }
        }
    }

    private class SuccessfulGiftCodePort : GiftCodePort {
        override fun findActiveGiftCodes(): List<String> = emptyList()

        override fun redeemGiftCodes(player: Player, giftCodes: List<String>): List<GiftCodeRedemptionResult> =
            giftCodes.map { GiftCodeRedemptionResult(it, GiftCodeRedemptionStatus.REDEEMED, "Redeemed") }
    }

    private class FakeFailedGiftCodeRedemptionRepositoryPort(
        initialRedemptions: List<FailedGiftCodeRedemption>,
    ) : FailedGiftCodeRedemptionRepositoryPort {
        var failedRedemptions = initialRedemptions

        override fun findAll(): List<FailedGiftCodeRedemption> = failedRedemptions

        override fun replaceAll(failedRedemptions: List<FailedGiftCodeRedemption>) {
            this.failedRedemptions = failedRedemptions
        }
    }

    private class FakeActiveGiftCodeRepositoryPort(
        private val activeGiftCodes: List<String>?,
    ) : ActiveGiftCodeRepositoryPort {
        override fun findActiveGiftCodes(): List<String>? = activeGiftCodes

        override fun replaceActiveGiftCodes(giftCodes: List<String>) = Unit
    }
}
