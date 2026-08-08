package com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase

import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.PlayerRepositoryPort
import org.springframework.stereotype.Component

@Component
class RemovePlayerUseCase(
    private val playerRepositoryPort: PlayerRepositoryPort,
) {
    fun execute(playerId: Long): Boolean =
        playerRepositoryPort.removeById(playerId)
}
