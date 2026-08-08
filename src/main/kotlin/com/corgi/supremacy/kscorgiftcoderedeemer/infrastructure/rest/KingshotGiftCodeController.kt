package com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.rest

import com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase.ListPlayersUseCase
import com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase.RegisterAndRedeemGiftCodesUseCase
import com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase.RedeemGiftCodeResult
import com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.usecase.RemovePlayerUseCase
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.GiftCodeRedemptionStatus
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
    private val registerAndRedeemGiftCodesUseCase: RegisterAndRedeemGiftCodesUseCase,
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
        val result = registerAndRedeemGiftCodesUseCase.execute(
            playerId = playerId,
            kingdom = kingdom,
        )
        val image = imageRepositoryPort.findRandom()

        return RedeemGiftCodesResponse(
            message = buildRedeemMessage(result),
            image = image?.let {
                DiscordImageResponse(
                    fileName = it.fileName,
                    mediaType = it.mediaType,
                    base64Content = Base64.getEncoder().encodeToString(it.content),
                )
            },
        )
    }

    private fun buildRedeemMessage(result: RedeemGiftCodeResult): String {
        val redeemed = result.redemptionResults
            .filter { it.status == GiftCodeRedemptionStatus.REDEEMED }
            .map { it.giftCode }
        val alreadyRedeemed = result.redemptionResults
            .filter { it.status == GiftCodeRedemptionStatus.ALREADY_REDEEMED }
            .map { it.giftCode }
        val invalid = result.redemptionResults
            .filter { it.status == GiftCodeRedemptionStatus.INVALID }
            .map { it.giftCode }

        return buildString {
            append("Processed ${result.redemptionResults.size} gift code(s) for player ${result.player.id} in kingdom ${result.player.kingdom}.")
            if (redeemed.isNotEmpty()) append(" Redeemed: ${redeemed.joinToString()}.")
            if (alreadyRedeemed.isNotEmpty()) append(" Already redeemed: ${alreadyRedeemed.joinToString()}.")
            if (invalid.isNotEmpty()) append(" Failed or invalid: ${invalid.joinToString()}.")
        }
    }
}
