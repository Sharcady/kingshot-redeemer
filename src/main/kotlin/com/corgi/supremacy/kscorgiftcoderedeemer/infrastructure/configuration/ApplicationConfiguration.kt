package com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "application")
data class ApplicationConfiguration(
    val kingshot: Kingshot = Kingshot(),
) {
    data class Kingshot(
        val baseUrl: String = "https://kingshot.net",
        val redeemUrl: String = "https://ks-giftcode.centurygame.com/",
        val giftCodesUrl: String = "http://kingshotwiki.com/giftcodes/",
        val playersDbPath: String = "data/players.json",
        val activeGiftCodesDbPath: String = "data/active_giftcodes.json",
        val failedGiftCodeRedemptionsDbPath: String = "data/failed_giftcode_redemptions.json",
        val imagesPath: String = "data/images",
        val scheduler: Scheduler = Scheduler(),
        val browser: Browser = Browser(),
    )

    data class Scheduler(
        val registeredPlayersRedemptionCron: String = "0 0 */6 * * *",
        val activeGiftCodesRetrievalCron: String = "0 */30 * * * *",
        val failedGiftCodeRetryCron: String = "0 0 * * * *",
    )

    data class Browser(
        val headless: Boolean = true,
        val timeoutSeconds: Long = 60,
        val redemptionResultTimeoutSeconds: Long = 15,
        val redemptionResultSettleMillis: Long = 1500,
    )
}
