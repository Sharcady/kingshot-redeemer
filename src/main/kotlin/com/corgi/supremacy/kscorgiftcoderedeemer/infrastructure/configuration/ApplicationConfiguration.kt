package com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "application")
data class ApplicationConfiguration(
    val kingshot: Kingshot = Kingshot(),
) {
    data class Kingshot(
        val baseUrl: String = "https://kingshot.net",
        val redeemUrl: String = "https://kingshot.net/gift-codes/redeem",
        val giftCodesUrl: String = "https://kingshot.net/gift-codes",
        val playersDbPath: String = "data/players.json",
        val imagesPath: String = "data/images",
        val scheduler: Scheduler = Scheduler(),
        val browser: Browser = Browser(),
    )

    data class Scheduler(
        val cron: String = "0 0 */2 * * *",
    )

    data class Browser(
        val headless: Boolean = true,
        val timeoutSeconds: Long = 60,
        val redemptionResultSettleMillis: Long = 1500,
    )
}
