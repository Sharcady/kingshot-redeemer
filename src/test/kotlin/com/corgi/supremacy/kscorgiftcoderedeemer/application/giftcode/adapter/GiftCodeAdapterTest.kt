package com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.adapter

import com.corgi.supremacy.kscorgiftcoderedeemer.domain.GiftCodeRedemptionStatus
import com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.configuration.ApplicationConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GiftCodeAdapterTest {
    private val adapter = GiftCodeAdapter(ApplicationConfiguration())

    @Test
    fun `recognizes live terminal modal results`() {
        val invalidMessages = listOf(
            "Character info is incorrect. Please confirm and try again. Confirm",
            "Claim limit exceeded. Unable to claim. Confirm",
            "Insufficient Town Center Level. Unable to claim. Confirm",
            "Prerequisite unmet Confirm",
            "Expired, unable to claim. Confirm",
            "Server busy. Please try again later. Confirm",
        )

        invalidMessages.forEach { message ->
            adapter.run {
                assertTrue(message.isTerminalRedeemMessage())
                assertEquals(GiftCodeRedemptionStatus.INVALID, message.toRedemptionStatus())
            }
        }
    }

    @Test
    fun `classifies redeemed and already-redeemed modal results`() {
        adapter.run {
            assertEquals(
                GiftCodeRedemptionStatus.REDEEMED,
                "Redeemed successfully. Please check your mail for rewards! Confirm".toRedemptionStatus(),
            )
            assertEquals(
                GiftCodeRedemptionStatus.ALREADY_REDEEMED,
                "Gift has already been claimed! Confirm".toRedemptionStatus(),
            )
        }
    }
}
