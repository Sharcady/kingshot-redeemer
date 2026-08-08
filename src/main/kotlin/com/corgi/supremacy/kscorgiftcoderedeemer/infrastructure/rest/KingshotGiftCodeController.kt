package com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.rest

import com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase.ListPlayersUseCase
import com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase.RedeemGiftCodeUseCase
import com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase.RemovePlayerUseCase
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.Player
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.ImageRepositoryPort
import com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.rest.dto.DiscordImageResponse
import com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.rest.dto.RedeemGiftCodesRequest
import com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.rest.dto.RedeemGiftCodesResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Base64

@RestController
@RequestMapping("/kingshot")
class KingshotGiftCodeController(
    private val redeemGiftCodeUseCase: RedeemGiftCodeUseCase,
    private val removePlayerUseCase: RemovePlayerUseCase,
    private val listPlayersUseCase: ListPlayersUseCase,
    private val imageRepositoryPort: ImageRepositoryPort,
) {

    @GetMapping("/register")
    fun register(@RequestParam playerId: Long, @RequestParam kingdom: Long): RedeemGiftCodesResponse =
        redeemAndBuildResponse(playerId, kingdom)

    @GetMapping("/removeplayer")
    fun removePlayer(@RequestParam playerId: Long): String =
        if (removePlayerUseCase.execute(playerId)) {
            "Removed player $playerId."
        } else {
            "Player $playerId was not registered."
        }

    @GetMapping("/listplayers")
    fun listPlayers(): List<Player> =
        listPlayersUseCase.execute()

    @PostMapping("/redeem-and-register")
    fun redeem(@RequestBody request: RedeemGiftCodesRequest): RedeemGiftCodesResponse =
        redeemAndBuildResponse(request.playerId, request.kingdom)

    private fun redeemAndBuildResponse(playerId: Long, kingdom: Long): RedeemGiftCodesResponse {
        val result = redeemGiftCodeUseCase.execute(
            playerId = playerId,
            kingdom = kingdom,
        )
        val image = imageRepositoryPort.findRandom()

        return RedeemGiftCodesResponse(
            message = "Redeemed ${result.redeemedGiftCodes.size} gift code(s) for ${result.player.name} (${result.player.id}) in kingdom ${result.player.kingdom}: ${result.redeemedGiftCodes.joinToString()}.",
            image = image?.let {
                DiscordImageResponse(
                    fileName = it.fileName,
                    mediaType = it.mediaType,
                    base64Content = Base64.getEncoder().encodeToString(it.content),
                )
            },
        )
    }
}
