package com.corgi.supremacy.kscorgiftcoderedeemer.domain.port

import com.corgi.supremacy.kscorgiftcoderedeemer.domain.Player

interface PlayerRepositoryPort {
    fun save(player: Player)

    fun removeById(playerId: Long): Boolean

    fun findAll(): List<Player>
}
