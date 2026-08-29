package com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.scheduler

import com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase.RedeemGiftCodesForRegisteredPlayersUseCase
import com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase.ReadActiveGiftCodesUseCase
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class KingshotRedeemerScheduler(
    private val redeemGiftCodesForRegisteredPlayersUseCase: RedeemGiftCodesForRegisteredPlayersUseCase,
    private val readActiveGiftCodesUseCase: ReadActiveGiftCodesUseCase,
) {

    @Scheduled(cron = "\${application.kingshot.scheduler.registered-players-redemption-cron}")
    fun redeem() {
        val giftCodes = readActiveGiftCodesUseCase.execute()
        if (giftCodes == null) {
            logger.warn("Skipping registered-player redemption because active_giftcodes.json is not available yet.")
            return
        }
        if (giftCodes.isEmpty()) {
            logger.info("Skipping registered-player redemption because active_giftcodes.json contains no active codes.")
            return
        }
        val result = redeemGiftCodesForRegisteredPlayersUseCase.execute(giftCodes)
        logger.info(
            "Redeemed {} gift code(s) for {} player(s): {}. Failed player(s): {}",
            result.giftCodes.size,
            result.redeemedPlayers.size,
            result.redeemedPlayers.joinToString { "${it.name} (${it.id})" },
            result.failedPlayers.joinToString { "${it.name} (${it.id})" },
        )
    }

    private companion object {
        val logger = LoggerFactory.getLogger(KingshotRedeemerScheduler::class.java)
    }
}
