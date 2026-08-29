package com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.scheduler

import com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase.RetryFailedGiftCodeRedemptionsUseCase
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class FailedGiftCodeRedemptionRetryScheduler(
    private val retryFailedGiftCodeRedemptionsUseCase: RetryFailedGiftCodeRedemptionsUseCase,
) {

    @Scheduled(cron = "\${application.kingshot.scheduler.failed-gift-code-retry-cron}")
    fun retry() {
        val result = retryFailedGiftCodeRedemptionsUseCase.execute()
        if (!result.activeGiftCodesSnapshotAvailable) {
            logger.info("Skipped failed gift-code retries because active_giftcodes.json is not available yet.")
            return
        }
        logger.info(
            "Retried {} failed gift-code redemption pair(s). Pairs still failing: {}. Removed inactive pairs: {}.",
            result.processedPairs,
            result.failedPairs.size,
            result.discardedInactivePairs,
        )
    }

    private companion object {
        val logger = LoggerFactory.getLogger(FailedGiftCodeRedemptionRetryScheduler::class.java)
    }
}
