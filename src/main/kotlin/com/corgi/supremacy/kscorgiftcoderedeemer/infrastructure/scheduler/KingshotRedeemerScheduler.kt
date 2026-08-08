package com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.scheduler

import com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase.RedeemGiftCodesForRegisteredPlayersUseCase
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class KingshotRedeemerScheduler(
    private val redeemGiftCodesForRegisteredPlayersUseCase: RedeemGiftCodesForRegisteredPlayersUseCase,
) {

    @Scheduled(cron = "\${application.kingshot.scheduler.cron}")
    fun redeem() {
        val result = redeemGiftCodesForRegisteredPlayersUseCase.execute()
        logger.info(
            "Redeemed {} gift code(s) for {} player(s). Failed player(s): {}",
            result.giftCodes.size,
            result.redeemedPlayers.size,
            result.failedPlayers.joinToString { it.id },
        )
    }

    private companion object {
        val logger = LoggerFactory.getLogger(KingshotRedeemerScheduler::class.java)
    }
}
