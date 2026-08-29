package com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.adapter

import com.corgi.supremacy.kscorgiftcoderedeemer.domain.FailedGiftCodeRedemption
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.ActiveGiftCodeRepositoryPort
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.FailedGiftCodeRedemptionRepositoryPort
import com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.configuration.ApplicationConfiguration
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

@Component
class JsonGiftCodeStateRepositoryAdapter(
    private val objectMapper: ObjectMapper,
    private val applicationConfiguration: ApplicationConfiguration,
) : ActiveGiftCodeRepositoryPort, FailedGiftCodeRedemptionRepositoryPort {

    @Synchronized
    override fun findActiveGiftCodes(): List<String>? {
        val path = Path.of(applicationConfiguration.kingshot.activeGiftCodesDbPath)
        if (!path.exists()) {
            return null
        }

        return objectMapper.readValue(
            path.toFile(),
            objectMapper.typeFactory.constructCollectionType(List::class.java, String::class.java),
        )
    }

    @Synchronized
    override fun replaceActiveGiftCodes(giftCodes: List<String>) {
        write(
            Path.of(applicationConfiguration.kingshot.activeGiftCodesDbPath),
            giftCodes.distinct().sorted(),
        )
    }

    override fun findAll(): List<FailedGiftCodeRedemption> {
        val path = Path.of(applicationConfiguration.kingshot.failedGiftCodeRedemptionsDbPath)
        if (!path.exists()) {
            return emptyList()
        }

        return objectMapper.readValue(
            path.toFile(),
            objectMapper.typeFactory.constructCollectionType(List::class.java, FailedGiftCodeRedemption::class.java),
        )
    }

    @Synchronized
    override fun replaceAll(failedRedemptions: List<FailedGiftCodeRedemption>) {
        write(
            Path.of(applicationConfiguration.kingshot.failedGiftCodeRedemptionsDbPath),
            failedRedemptions.sortedWith(
                compareBy<FailedGiftCodeRedemption> { it.player.kingdom }
                    .thenBy { it.player.id }
                    .thenBy { it.giftCode },
            ),
        )
    }

    private fun write(path: Path, value: Any) {
        path.parent?.let { Files.createDirectories(it) }
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), value)
    }
}
