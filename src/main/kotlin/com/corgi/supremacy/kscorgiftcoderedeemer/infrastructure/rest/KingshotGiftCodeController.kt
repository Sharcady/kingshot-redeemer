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
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody as OpenApiRequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Base64

@RestController
@RequestMapping("/kingshot")
@Tag(
    name = "Gift-code redemption",
    description = "Register players and redeem the active Kingshot gift codes for them.",
)
class KingshotGiftCodeController(
    private val registerAndRedeemGiftCodesUseCase: RegisterAndRedeemGiftCodesUseCase,
    private val removePlayerUseCase: RemovePlayerUseCase,
    private val listPlayersUseCase: ListPlayersUseCase,
    private val imageRepositoryPort: ImageRepositoryPort,
) {

    @GetMapping("/register")
    @Operation(
        summary = "Register a player and redeem active codes",
        description = "Saves the player, retrieves the currently active gift codes, and redeems each code for that player.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "The redemption attempt completed.",
                content = [Content(schema = Schema(implementation = RedeemGiftCodesResponse::class))],
            ),
            ApiResponse(
                responseCode = "502",
                description = "The gift-code source or official redemption site could not be reached.",
                content = [Content(schema = Schema(implementation = com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.rest.dto.ErrorResponse::class))],
            ),
        ],
    )
    fun register(
        @Parameter(description = "Kingshot player ID.", example = "123456789", required = true)
        @RequestParam playerId: Long,
        @Parameter(description = "Kingshot kingdom number.", example = "23", required = true)
        @RequestParam kingdom: Long,
        @Parameter(description = "Player name to display in responses.", example = "Arkadiy", required = true)
        @RequestParam name: String,
    ): RedeemGiftCodesResponse =
        redeemAndBuildResponse(playerId, kingdom, name)

    @GetMapping("/removeplayer")
    @Operation(
        summary = "Remove a registered player",
        description = "Deletes the player from the local registration database when present.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "A human-readable removal result.",
                content = [Content(mediaType = "text/plain", schema = Schema(type = "string", example = "Removed player 123456789."))],
            ),
        ],
    )
    fun removePlayer(
        @Parameter(description = "Kingshot player ID to remove.", example = "123456789", required = true)
        @RequestParam playerId: Long,
    ): String =
        if (removePlayerUseCase.execute(playerId)) {
            "Removed player $playerId."
        } else {
            "Player $playerId was not registered."
        }

    @GetMapping("/listplayers")
    @Operation(
        summary = "List registered players",
        description = "Returns every player currently stored in the local registration database.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Registered players.",
                content = [Content(array = ArraySchema(schema = Schema(implementation = Player::class)))],
            ),
        ],
    )
    fun listPlayers(): List<Player> =
        listPlayersUseCase.execute()

    @PostMapping("/redeem-and-register")
    @Operation(
        summary = "Register a player and redeem active codes",
        description = "JSON equivalent of the register endpoint. Saves the player, retrieves active codes, and redeems them.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "The redemption attempt completed.",
                content = [Content(schema = Schema(implementation = RedeemGiftCodesResponse::class))],
            ),
            ApiResponse(
                responseCode = "502",
                description = "The gift-code source or official redemption site could not be reached.",
                content = [Content(schema = Schema(implementation = com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.rest.dto.ErrorResponse::class))],
            ),
        ],
    )
    fun redeem(
        @OpenApiRequestBody(
            required = true,
            description = "Player registration details used for gift-code redemption.",
            content = [Content(schema = Schema(implementation = RedeemGiftCodesRequest::class))],
        )
        @RequestBody request: RedeemGiftCodesRequest,
    ): RedeemGiftCodesResponse =
        redeemAndBuildResponse(request.playerId, request.kingdom, request.name)

    private fun redeemAndBuildResponse(playerId: Long, kingdom: Long, name: String): RedeemGiftCodesResponse {
        val result = registerAndRedeemGiftCodesUseCase.execute(
            playerId = playerId,
            kingdom = kingdom,
            name = name,
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
            append("Processed ${result.redemptionResults.size} gift code(s) for ${result.player.name} (${result.player.id}) in kingdom ${result.player.kingdom}.")
            if (redeemed.isNotEmpty()) append(" Redeemed: ${redeemed.joinToString()}.")
            if (alreadyRedeemed.isNotEmpty()) append(" Already redeemed: ${alreadyRedeemed.joinToString()}.")
            if (invalid.isNotEmpty()) append(" Failed or invalid: ${invalid.joinToString()}.")
        }
    }
}
