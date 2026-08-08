package com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.adapter

import com.corgi.supremacy.kscorgiftcoderedeemer.domain.Image
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.ImageRetrievalException
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.ImageRepositoryPort
import com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.configuration.ApplicationConfiguration
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.random.Random

@Component
class ImageRepositoryAdapter(
    private val applicationConfiguration: ApplicationConfiguration,
) : ImageRepositoryPort {

    @Throws(ImageRetrievalException::class)
    override fun findRandom(): Image? {
        try {
            val imagesDirectory = Path.of(applicationConfiguration.kingshot.imagesPath)
            if (Files.notExists(imagesDirectory)) {
                Files.createDirectories(imagesDirectory)
                return null
            }

            val images = Files.list(imagesDirectory).use { files ->
                files
                    .filter { it.isRegularFile() }
                    .filter { it.extension.lowercase() in SUPPORTED_EXTENSIONS }
                    .toList()
            }

            if (images.isEmpty()) {
                return null
            }

            val image = images[Random.nextInt(images.size)]
            return Image(
                fileName = image.fileName.toString(),
                mediaType = Files.probeContentType(image) ?: mediaTypeByExtension(image),
                content = Files.readAllBytes(image),
            )
        } catch (exception: RuntimeException) {
            throw ImageRetrievalException()
        }
    }

    private fun mediaTypeByExtension(path: Path): String =
        when (path.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            else -> "image/png"
        }

    private companion object {
        val SUPPORTED_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "webp")
    }
}
