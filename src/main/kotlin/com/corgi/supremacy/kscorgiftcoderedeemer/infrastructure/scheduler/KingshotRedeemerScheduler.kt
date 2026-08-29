package com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.scheduler

import com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase.RedeemGiftCodesForRegisteredPlayersUseCase
import com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase.RetrieveActiveGiftCodesUseCase
import com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase.UpdateActiveGiftCodesUseCase
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class KingshotRedeemerScheduler(
    private val redeemGiftCodesForRegisteredPlayersUseCase: RedeemGiftCodesForRegisteredPlayersUseCase,
    private val retrieveActiveGiftCodesUseCase: RetrieveActiveGiftCodesUseCase,
    private val updateActiveGiftCodesUseCase: UpdateActiveGiftCodesUseCase,
) {

    @Scheduled(cron = "\${application.kingshot.scheduler.cron}")
    fun redeem() {
        val giftCodes = retrieveActiveGiftCodesUseCase.execute()
        val result = try {
            redeemGiftCodesForRegisteredPlayersUseCase.execute(giftCodes)
        } finally {
            updateActiveGiftCodesUseCase.execute(giftCodes)
        }
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
