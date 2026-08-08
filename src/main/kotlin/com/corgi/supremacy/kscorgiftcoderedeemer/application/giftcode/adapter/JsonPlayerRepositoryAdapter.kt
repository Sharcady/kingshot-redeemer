package com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.adapter

import com.corgi.supremacy.kscorgiftcoderedeemer.domain.Player
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.PlayerRepositoryPort
import com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.configuration.ApplicationConfiguration
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

@Component
class JsonPlayerRepositoryAdapter(
    private val objectMapper: ObjectMapper,
    private val applicationConfiguration: ApplicationConfiguration,
) : PlayerRepositoryPort {

    @Synchronized
    override fun save(player: Player) {
        val path = Path.of(applicationConfiguration.kingshot.playersDbPath)
        ensureParentDirectoryExists(path)

        val players = readPlayers(path)
            .filterNot { it.id == player.id }
            .plus(player)
            .sortedWith(compareBy<Player> { it.kingdom }.thenBy { it.id })

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), players)
    }

    @Synchronized
    override fun removeById(playerId: Long): Boolean {
        val path = Path.of(applicationConfiguration.kingshot.playersDbPath)
        ensureParentDirectoryExists(path)

        val players = readPlayers(path)
        val remainingPlayers = players.filterNot { it.id == playerId.toString() }

        if (remainingPlayers.size == players.size) {
            return false
        }

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), remainingPlayers)
        return true
    }

    override fun findAll(): List<Player> =
        readPlayers(Path.of(applicationConfiguration.kingshot.playersDbPath))

    private fun readPlayers(path: Path): List<Player> {
        ensureFileExists(path)

        return objectMapper.readValue(
            path.toFile(),
            objectMapper.typeFactory.constructCollectionType(List::class.java, Player::class.java),
        )
    }

    private fun ensureParentDirectoryExists(path: Path) {
        path.parent?.let { Files.createDirectories(it) }
    }

    private fun ensureFileExists(path: Path) {
        ensureParentDirectoryExists(path)
        if (!path.exists()) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), emptyList<Player>())
        }
    }
}
