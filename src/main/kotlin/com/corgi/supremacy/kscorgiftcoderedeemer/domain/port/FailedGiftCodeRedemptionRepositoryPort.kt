package com.corgi.supremacy.kscorgiftcoderedeemer.domain.port

import com.corgi.supremacy.kscorgiftcoderedeemer.domain.FailedGiftCodeRedemption

interface FailedGiftCodeRedemptionRepositoryPort {
    fun findAll(): List<FailedGiftCodeRedemption>

    fun replaceAll(failedRedemptions: List<FailedGiftCodeRedemption>)
}
