package com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase

import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.ActiveGiftCodeRepositoryPort
import org.springframework.stereotype.Component

@Component
class UpdateActiveGiftCodesUseCase(
    private val activeGiftCodeRepositoryPort: ActiveGiftCodeRepositoryPort,
) {
    fun execute(giftCodes: List<String>) {
        activeGiftCodeRepositoryPort.replaceActiveGiftCodes(giftCodes)
    }
}
