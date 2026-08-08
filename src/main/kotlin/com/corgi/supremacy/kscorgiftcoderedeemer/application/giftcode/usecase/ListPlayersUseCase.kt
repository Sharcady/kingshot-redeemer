package com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase

import com.corgi.supremacy.kscorgiftcoderedeemer.domain.Player
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.PlayerRepositoryPort
import org.springframework.stereotype.Component

@Component
class ListPlayersUseCase(
    private val playerRepositoryPort: PlayerRepositoryPort,
) {
    fun execute(): List<Player> =
        playerRepositoryPort.findAll()
}
