package com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase

import com.corgi.supremacy.kscorgiftcoderedeemer.domain.FailedGiftCodeRedemption
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.GiftCodeRedemptionStatus
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.ActiveGiftCodeRepositoryPort
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.FailedGiftCodeRedemptionRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class RetryFailedGiftCodeRedemptionsUseCase(
    private val activeGiftCodeRepositoryPort: ActiveGiftCodeRepositoryPort,
    private val failedGiftCodeRedemptionRepositoryPort: FailedGiftCodeRedemptionRepositoryPort,
    private val redeemGiftCodesForPlayerUseCase: RedeemGiftCodesForPlayerUseCase,
) {
    fun execute(): FailedGiftCodeRedemptionRetryResult {
        val activeGiftCodes = activeGiftCodeRepositoryPort.findActiveGiftCodes()
            ?: return FailedGiftCodeRedemptionRetryResult(activeGiftCodesSnapshotAvailable = false)
        val activeGiftCodeSet = activeGiftCodes.toSet()
        val savedFailedRedemptions = failedGiftCodeRedemptionRepositoryPort.findAll()
        val inactivePairs = savedFailedRedemptions.filter { it.giftCode !in activeGiftCodeSet }
        if (inactivePairs.isNotEmpty()) {
            failedGiftCodeRedemptionRepositoryPort.replaceAll(savedFailedRedemptions - inactivePairs.toSet())
        }

        val retryable = savedFailedRedemptions
            .filter { it.giftCode in activeGiftCodeSet }
            .filter { it.retryAttempts < MAX_RETRY_ATTEMPTS }
        val failedPairs = mutableListOf<FailedGiftCodeRedemption>()

        retryable.groupBy { it.player }.forEach { (player, failedRedemptions) ->
            try {
                val results = redeemGiftCodesForPlayerUseCase.execute(
                    player = player,
                    giftCodes = failedRedemptions.map { it.giftCode },
                    isRetry = true,
                )
                val failedGiftCodes = results
                    .filter { it.status == GiftCodeRedemptionStatus.INVALID }
                    .map { it.giftCode }
                failedPairs += failedRedemptions.filter { it.giftCode in failedGiftCodes }
            } catch (exception: Exception) {
                logger.error(
                    "Unable to retry {} gift code(s) for player {} in kingdom {}.",
                    failedRedemptions.size,
                    player.id,
                    player.kingdom,
                    exception,
                )
                failedPairs += failedRedemptions
            }
        }

        return FailedGiftCodeRedemptionRetryResult(
            processedPairs = retryable.size,
            failedPairs = failedPairs,
            discardedInactivePairs = inactivePairs.size,
        )
    }

    private companion object {
        val logger = LoggerFactory.getLogger(RetryFailedGiftCodeRedemptionsUseCase::class.java)
        const val MAX_RETRY_ATTEMPTS = 10
    }
}

data class FailedGiftCodeRedemptionRetryResult(
    val processedPairs: Int = 0,
    val failedPairs: List<FailedGiftCodeRedemption> = emptyList(),
    val discardedInactivePairs: Int = 0,
    val activeGiftCodesSnapshotAvailable: Boolean = true,
)
