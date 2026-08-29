package com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase

import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.GiftCodePort
import org.springframework.stereotype.Component

@Component
class RetrieveActiveGiftCodesUseCase(
    private val giftCodePort: GiftCodePort,
) {
    fun execute(): List<String> =
        giftCodePort.findActiveGiftCodes()
}
