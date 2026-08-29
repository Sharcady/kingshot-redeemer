package com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase

import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.ActiveGiftCodeRepositoryPort
import org.springframework.stereotype.Component

@Component
class ReadActiveGiftCodesUseCase(
    private val activeGiftCodeRepositoryPort: ActiveGiftCodeRepositoryPort,
) {
    fun execute(): List<String>? =
        activeGiftCodeRepositoryPort.findActiveGiftCodes()
}
