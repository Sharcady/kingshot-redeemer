package com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase

import com.corgi.supremacy.kscorgiftcoderedeemer.domain.FailedGiftCodeRedemption
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.GiftCodeRedemptionResult
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.GiftCodeRedemptionStatus
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.Player
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.FailedGiftCodeRedemptionRepositoryPort
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.GiftCodePort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class RedeemGiftCodesForPlayerUseCase(
    private val giftCodePort: GiftCodePort,
    private val failedGiftCodeRedemptionRepositoryPort: FailedGiftCodeRedemptionRepositoryPort,
) {
    fun execute(player: Player, giftCodes: List<String>, isRetry: Boolean = false): List<GiftCodeRedemptionResult> {
        val normalizedGiftCodes = giftCodes.filter { it.isNotBlank() }.map { it.trim() }.distinct()
        if (normalizedGiftCodes.isEmpty()) {
            return emptyList()
        }

        return try {
            val results = giftCodePort.redeemGiftCodes(player, normalizedGiftCodes)
            recordResults(player, normalizedGiftCodes, results, isRetry)
            results
        } catch (exception: RuntimeException) {
            logger.error(
                "Unable to redeem {} gift code(s) for player {} in kingdom {}. Retry: {}.",
                normalizedGiftCodes.size,
                player.id,
                player.kingdom,
                isRetry,
                exception,
            )
            recordFailures(player, normalizedGiftCodes, isRetry)
            throw exception
        }
    }

    private fun recordResults(
        player: Player,
        requestedGiftCodes: List<String>,
        results: List<GiftCodeRedemptionResult>,
        isRetry: Boolean,
    ) {
        val resultsByGiftCode = results.associateBy { it.giftCode }
        val failedGiftCodes = requestedGiftCodes.filter { giftCode ->
            resultsByGiftCode[giftCode]?.status == GiftCodeRedemptionStatus.INVALID || giftCode !in resultsByGiftCode
        }
        val completedGiftCodes = results
            .filter { it.status != GiftCodeRedemptionStatus.INVALID }
            .map { it.giftCode }

        updateFailedRedemptions(player, failedGiftCodes, completedGiftCodes, isRetry)
    }

    private fun recordFailures(player: Player, giftCodes: List<String>, isRetry: Boolean) {
        updateFailedRedemptions(player, giftCodes, completedGiftCodes = emptyList(), isRetry)
    }

    @Synchronized
    private fun updateFailedRedemptions(
        player: Player,
        failedGiftCodes: List<String>,
        completedGiftCodes: List<String>,
        isRetry: Boolean,
    ) {
        val existing = failedGiftCodeRedemptionRepositoryPort.findAll().toMutableList()

        completedGiftCodes.forEach { giftCode ->
            existing.removeAll { it.belongsTo(player) && it.giftCode == giftCode }
        }

        failedGiftCodes.forEach { giftCode ->
            val existingIndex = existing.indexOfFirst { it.belongsTo(player) && it.giftCode == giftCode }
            if (existingIndex >= 0) {
                val previous = existing[existingIndex]
                if (isRetry) {
                    existing[existingIndex] = previous.copy(retryAttempts = previous.retryAttempts + 1)
                }
            } else {
                existing += FailedGiftCodeRedemption(
                    player = player,
                    giftCode = giftCode,
                    retryAttempts = if (isRetry) 1 else 0,
                )
            }
        }

        failedGiftCodeRedemptionRepositoryPort.replaceAll(existing)
    }

    private fun FailedGiftCodeRedemption.belongsTo(player: Player): Boolean =
        this.player.id == player.id && this.player.kingdom == player.kingdom

    private companion object {
        val logger = LoggerFactory.getLogger(RedeemGiftCodesForPlayerUseCase::class.java)
    }
}
