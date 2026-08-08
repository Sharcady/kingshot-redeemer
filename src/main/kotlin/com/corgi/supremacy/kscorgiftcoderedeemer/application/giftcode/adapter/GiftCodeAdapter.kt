package com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.adapter

import com.corgi.supremacy.kscorgiftcoderedeemer.domain.GiftCodeRedeemingException
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.GiftCodesRetrievalException
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.Player
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.PlayerNotFoundException
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.GiftCodePort
import com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.configuration.ApplicationConfiguration
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class GiftCodeAdapter(
    private val applicationConfiguration: ApplicationConfiguration,
) : GiftCodePort {

    override fun findPlayerByIdAndKingdom(playerId: Long, kingdom: Long): Player {
        val driver = createDriver()
        return try {
            openAndFindPlayer(driver, playerId, kingdom)
        } finally {
            driver.quit()
        }
    }

    override fun findActiveGiftCodes(): List<String> {
        val driver = createDriver()
        return try {
            val wait = WebDriverWait(driver, Duration.ofSeconds(applicationConfiguration.kingshot.browser.timeoutSeconds))
            driver.get(applicationConfiguration.kingshot.giftCodesUrl)
            wait.until { bodyLines(driver).contains("Active Gift Codes") }

            val activeGiftCodes = bodyLines(driver)
                .dropWhile { it != "Active Gift Codes" }
                .drop(1)
                .takeWhile { it != "Expired Gift Codes" }
                .filter { it.isGiftCodeLine() }
                .distinct()

            if (activeGiftCodes.isEmpty()) {
                throw GiftCodesRetrievalException()
            }

            activeGiftCodes
        } catch (exception: GiftCodesRetrievalException) {
            throw exception
        } catch (exception: RuntimeException) {
            throw GiftCodesRetrievalException()
        } finally {
            driver.quit()
        }
    }

    override fun redeemGiftCodes(
        playerId: Long,
        kingdom: Long,
        giftCodes: List<String>,
        onPlayerFound: (Player) -> Unit,
    ): Player {
        val driver = createDriver()
        return try {
            val player = openAndFindPlayer(driver, playerId, kingdom)
            onPlayerFound(player)
            giftCodes.filter { it.isNotBlank() }.forEach { giftCode ->
                redeemGiftCode(driver, giftCode.trim())
            }
            player
        } catch (exception: PlayerNotFoundException) {
            throw exception
        } catch (exception: RuntimeException) {
            throw GiftCodeRedeemingException()
        } finally {
            driver.quit()
        }
    }

    private fun createDriver(): WebDriver {
        val options = ChromeOptions()
        if (applicationConfiguration.kingshot.browser.headless) {
            options.addArguments("--headless=new")
        }
        options.addArguments("--disable-gpu", "--no-sandbox", "--window-size=1440,1200")
        return ChromeDriver(options)
    }

    private fun openAndFindPlayer(driver: WebDriver, playerId: Long, kingdom: Long): Player {
        val wait = WebDriverWait(driver, Duration.ofSeconds(applicationConfiguration.kingshot.browser.timeoutSeconds))
        driver.get(applicationConfiguration.kingshot.redeemUrl)

        fillFirstVisibleInput(driver, wait, playerId.toString(), playerIdInputLocators())
        fillFirstVisibleInput(driver, wait, kingdom.toString(), kingdomInputLocators())
        val beforeLookupLines = bodyLines(driver)
        clickButton(wait, "Continue")

        return try {
            wait.until { findGiftCodeInput(driver).isEnabled }
            Player(playerId.toString(), findPlayerName(driver, beforeLookupLines, playerId, kingdom), kingdom)
        } catch (exception: TimeoutException) {
            throw PlayerNotFoundException(playerId, kingdom)
        }
    }

    private fun redeemGiftCode(driver: WebDriver, giftCode: String) {
        val wait = WebDriverWait(driver, Duration.ofSeconds(applicationConfiguration.kingshot.browser.timeoutSeconds))
        val giftCodeInput = wait.until { findGiftCodeInput(driver) }
        giftCodeInput.clear()
        giftCodeInput.sendKeys(giftCode)
        clickButton(wait, "Redeem Gift Code")
    }

    private fun fillFirstVisibleInput(driver: WebDriver, wait: WebDriverWait, value: String, locators: List<By>) {
        val input = wait.until {
            locators.asSequence()
                .flatMap { locator -> driver.findElements(locator).asSequence() }
                .firstOrNull { it.isDisplayed && it.isEnabled }
        }
        input.clear()
        input.sendKeys(value)
    }

    private fun clickButton(wait: WebDriverWait, label: String) {
        val button = wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath("//button[normalize-space()='$label' or .//*[normalize-space()='$label']]")
            )
        )
        button.click()
    }

    private fun findGiftCodeInput(driver: WebDriver): WebElement =
        driver.findElements(By.cssSelector("input[placeholder='Enter gift code'], input[name='giftCode'], input[id*='gift' i]"))
            .firstOrNull { it.isDisplayed }
            ?: throw NoSuchElementException("Gift code input was not found")

    private fun findPlayerName(driver: WebDriver, beforeLookupLines: Set<String>, playerId: Long, kingdom: Long): String {
        val scriptResult = (driver as JavascriptExecutor).executeScript(
            """
            const playerId = arguments[0];
            const kingdom = arguments[1];
            const beforeLookupLines = new Set(arguments[2]);
            const values = [...document.querySelectorAll('body *')]
              .map(e => (e.innerText || e.textContent || '').trim())
              .filter(Boolean)
              .flatMap(t => t.split('\n').map(line => line.trim()))
              .filter(Boolean)
              .filter(t => t.length <= 80)
              .filter(t => !beforeLookupLines.has(t))
              .filter(t => !['Player ID', 'Kingdom', 'Gift Code', 'Continue', 'Redeem Gift Code'].includes(t))
              .filter(t => t !== playerId && t !== kingdom);
            return values[0] || '';
            """.trimIndent(),
            playerId.toString(),
            kingdom.toString(),
            beforeLookupLines.toList(),
        ) as? String

        return scriptResult?.takeIf { it.isNotBlank() } ?: playerId.toString()
    }

    private fun bodyLines(driver: WebDriver): Set<String> =
        driver.findElement(By.tagName("body")).text
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()

    private fun String.isGiftCodeLine(): Boolean =
        matches(GIFT_CODE_PATTERN) && this !in NON_CODE_LINES && !startsWith("Expires:")

    private fun playerIdInputLocators(): List<By> = listOf(
        By.cssSelector("input[placeholder='Enter your Player ID']"),
        By.cssSelector("input[name='playerId']"),
        By.cssSelector("input[id*='player' i]"),
    )

    private fun kingdomInputLocators(): List<By> = listOf(
        By.cssSelector("input[placeholder='Your kingdom number (e.g. 23)']"),
        By.cssSelector("input[name='kingdom']"),
        By.cssSelector("input[id*='kingdom' i]"),
    )

    private companion object {
        val GIFT_CODE_PATTERN = Regex("^[A-Za-z0-9][A-Za-z0-9_-]{2,}$")
        val NON_CODE_LINES = setOf("Active", "Copy", "Code", "Copy Code", "Sign", "In", "Redeem", "Share", "Link")
    }
}
