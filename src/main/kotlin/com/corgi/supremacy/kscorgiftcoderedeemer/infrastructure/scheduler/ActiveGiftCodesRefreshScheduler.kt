package com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.scheduler

import com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase.RetrieveActiveGiftCodesUseCase
import com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase.UpdateActiveGiftCodesUseCase
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ActiveGiftCodesRefreshScheduler(
    private val retrieveActiveGiftCodesUseCase: RetrieveActiveGiftCodesUseCase,
    private val updateActiveGiftCodesUseCase: UpdateActiveGiftCodesUseCase,
) {

    @Scheduled(cron = "\${application.kingshot.scheduler.active-gift-codes-retrieval-cron}")
    fun refresh() {
        val giftCodes = retrieveActiveGiftCodesUseCase.execute()
        updateActiveGiftCodesUseCase.execute(giftCodes)
        logger.info("Updated active_giftcodes.json with {} gift code(s).", giftCodes.size)
    }

    private companion object {
        val logger = LoggerFactory.getLogger(ActiveGiftCodesRefreshScheduler::class.java)
    }
}
