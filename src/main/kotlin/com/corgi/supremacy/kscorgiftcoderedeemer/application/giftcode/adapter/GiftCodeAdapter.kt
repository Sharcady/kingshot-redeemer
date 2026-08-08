package com.corgi.supremacy.kscorgiftcoderedeemer.application.giftcode.adapter

import com.corgi.supremacy.kscorgiftcoderedeemer.domain.GiftCodeRedeemingException
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.GiftCodeRedemptionResult
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.GiftCodeRedemptionStatus
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.GiftCodesRetrievalException
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.Player
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.port.GiftCodePort
import com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.configuration.ApplicationConfiguration
import org.openqa.selenium.By
import org.openqa.selenium.ElementClickInterceptedException
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.WebDriverWait
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class GiftCodeAdapter(
    private val applicationConfiguration: ApplicationConfiguration,
) : GiftCodePort {

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
            throw GiftCodesRetrievalException(exception)
        } finally {
            driver.quit()
        }
    }

    override fun redeemGiftCodes(player: Player, giftCodes: List<String>): List<GiftCodeRedemptionResult> {
        val driver = createDriver()
        return try {
            openRedeemPageAndFillPlayer(driver, player)
            giftCodes.filter { it.isNotBlank() }.map { giftCode ->
                redeemGiftCode(driver, player, giftCode.trim())
            }
        } catch (exception: GiftCodeRedeemingException) {
            throw exception
        } catch (exception: RuntimeException) {
            throw GiftCodeRedeemingException(cause = exception)
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

    private fun openRedeemPageAndFillPlayer(driver: WebDriver, player: Player) {
        val wait = WebDriverWait(driver, Duration.ofSeconds(applicationConfiguration.kingshot.browser.timeoutSeconds))
        driver.get(applicationConfiguration.kingshot.redeemUrl)

        fillFirstVisibleInput(driver, wait, player.id, playerIdInputLocators())
        fillFirstVisibleInput(driver, wait, player.kingdom.toString(), kingdomInputLocators())
        clickButton(driver, wait, "Continue")
    }

    private fun redeemGiftCode(driver: WebDriver, player: Player, giftCode: String): GiftCodeRedemptionResult {
        val wait = WebDriverWait(driver, Duration.ofSeconds(applicationConfiguration.kingshot.browser.timeoutSeconds))
        val giftCodeInput = wait.until { findGiftCodeInput(driver) }
        fillInput(driver, giftCodeInput, giftCode)
        clickRedeemGiftCodeButton(driver, wait)
        Thread.sleep(applicationConfiguration.kingshot.browser.redemptionResultSettleMillis)
        val message = waitForRedeemCompletion(driver, wait)
        val status = message.toRedemptionStatus()
        logger.info(
            "Gift code {} redemption result for {} ({}) in kingdom {}: {} - {}",
            giftCode,
            player.name,
            player.id,
            player.kingdom,
            status,
            message,
        )

        return GiftCodeRedemptionResult(
            giftCode = giftCode,
            status = status,
            message = message,
        )
    }

    private fun fillFirstVisibleInput(driver: WebDriver, wait: WebDriverWait, value: String, locators: List<By>) {
        val input = wait.until {
            locators.asSequence()
                .flatMap { locator -> driver.findElements(locator).asSequence() }
                .firstOrNull { it.isDisplayed && it.isEnabled }
        }
        fillInput(driver, input, value)
    }

    private fun fillInput(driver: WebDriver, input: WebElement, value: String) {
        (driver as JavascriptExecutor).executeScript(
            """
            const input = arguments[0];
            const value = arguments[1];
            const nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;
            nativeInputValueSetter.call(input, '');
            input.dispatchEvent(new Event('input', { bubbles: true }));
            nativeInputValueSetter.call(input, value);
            input.dispatchEvent(new Event('input', { bubbles: true }));
            input.dispatchEvent(new Event('change', { bubbles: true }));
            """.trimIndent(),
            input,
            value,
        )
    }

    private fun clickButton(driver: WebDriver, wait: WebDriverWait, label: String) {
        val button = wait.until { findClickableButtonByText(driver, label) }
        clickElement(driver, button)
    }

    private fun clickRedeemGiftCodeButton(driver: WebDriver, wait: WebDriverWait) {
        val button = wait.until {
            findClickableButtonByText(driver, "Redeem Gift Code")
                ?: findRedeemButtonNearGiftCodeInput(driver)
        }
        clickElement(driver, button)
    }

    private fun findClickableButtonByText(driver: WebDriver, label: String): WebElement? {
        driver.findElements(By.tagName("button")).forEach { button ->
            try {
                if (
                    button.isDisplayed &&
                    button.isEnabled &&
                    button.text.normalized().contains(label.normalized(), ignoreCase = true)
                ) {
                    return button
                }
            } catch (exception: StaleElementReferenceException) {
                return@forEach
            }
        }
        return null
    }

    private fun findRedeemButtonNearGiftCodeInput(driver: WebDriver): WebElement? {
        val giftCodeInput = findGiftCodeInput(driver)
        val scriptResult = (driver as JavascriptExecutor).executeScript(
            """
            const input = arguments[0];
            const container = input.closest('form') || input.parentElement;
            if (!container) return null;
            return [...container.querySelectorAll('button')]
              .find(button => !button.disabled && button.offsetParent !== null)
              || null;
            """.trimIndent(),
            giftCodeInput,
        )

        return scriptResult as? WebElement
    }

    private fun clickElement(driver: WebDriver, element: WebElement) {
        (driver as JavascriptExecutor).executeScript("arguments[0].scrollIntoView({block: 'center'});", element)
        try {
            element.click()
        } catch (exception: ElementClickInterceptedException) {
            driver.executeScript("arguments[0].click();", element)
        } catch (exception: StaleElementReferenceException) {
            throw exception
        }
    }

    private fun findGiftCodeInput(driver: WebDriver): WebElement =
        driver.findElements(By.cssSelector("input#giftCode, input[name='giftCode'], input[placeholder='Enter gift code'], input[id*='gift' i]"))
            .firstOrNull { it.isDisplayed }
            ?: throw NoSuchElementException("Gift code input was not found")

    private fun waitForRedeemCompletion(driver: WebDriver, wait: WebDriverWait): String =
        try {
            wait.until {
                val message = findRedeemResultMessage(driver)
                message?.takeIf { it.isTerminalRedeemMessage() }
            }
        } catch (exception: TimeoutException) {
            throw GiftCodeRedeemingException(
                message = "Timed out waiting for gift code redemption result. Page text: ${driver.findElement(By.tagName("body")).text.normalized()}",
                cause = exception,
            )
        }

    private fun findRedeemResultMessage(driver: WebDriver): String? {
        val scriptResult = (driver as JavascriptExecutor).executeScript(
            """
            const buttons = [...document.querySelectorAll('button')];
            const redeemButton = buttons.find(button => button.innerText.trim() === 'Redeem Gift Code');
            if (!redeemButton) return null;

            const messages = [];
            let element = redeemButton;
            for (let i = 0; i < 8 && element; i++) {
              element = element.nextElementSibling;
              if (element && element.innerText && element.innerText.trim()) {
                messages.push(...element.innerText.split('\n').map(line => line.trim()).filter(Boolean));
              }
            }

            const parentText = redeemButton.parentElement?.innerText || '';
            messages.push(...parentText.split('\n').map(line => line.trim()).filter(Boolean));

            return messages.find(line =>
              /gift code redeemed successfully/i.test(line) ||
              /already redeemed this gift code/i.test(line) ||
              /gift code not found or invalid/i.test(line)
            ) || null;
            """.trimIndent(),
        ) as? String

        return scriptResult?.takeIf { it.isNotBlank() }
    }

    private fun bodyLines(driver: WebDriver): Set<String> =
        driver.findElement(By.tagName("body")).text
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()

    private fun String.isGiftCodeLine(): Boolean =
        matches(GIFT_CODE_PATTERN) && this !in NON_CODE_LINES && !startsWith("Expires:")

    private fun String.normalized(): String =
        replace(Regex("\\s+"), " ").trim()

    private fun String.isTerminalRedeemMessage(): Boolean {
        val normalizedMessage = normalized()
        return normalizedMessage.contains(GIFT_CODE_REDEEMED_SUCCESSFULLY, ignoreCase = true) ||
            normalizedMessage.contains(GIFT_CODE_ALREADY_REDEEMED, ignoreCase = true) ||
            normalizedMessage.contains(GIFT_CODE_NOT_FOUND_OR_INVALID, ignoreCase = true)
    }

    private fun String.toRedemptionStatus(): GiftCodeRedemptionStatus {
        val normalizedMessage = normalized()
        return when {
            normalizedMessage.contains(GIFT_CODE_REDEEMED_SUCCESSFULLY, ignoreCase = true) ->
                GiftCodeRedemptionStatus.REDEEMED
            normalizedMessage.contains(GIFT_CODE_ALREADY_REDEEMED, ignoreCase = true) ->
                GiftCodeRedemptionStatus.ALREADY_REDEEMED
            else ->
                GiftCodeRedemptionStatus.INVALID
        }
    }

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
        val logger = LoggerFactory.getLogger(GiftCodeAdapter::class.java)
        val GIFT_CODE_PATTERN = Regex("^[A-Za-z0-9][A-Za-z0-9_-]{2,}$")
        val NON_CODE_LINES = setOf("Active", "Copy", "Code", "Copy Code", "Sign", "In", "Redeem", "Share", "Link")
        const val GIFT_CODE_REDEEMED_SUCCESSFULLY = "Gift code redeemed successfully"
        const val GIFT_CODE_ALREADY_REDEEMED = "already redeemed this gift code"
        const val GIFT_CODE_NOT_FOUND_OR_INVALID = "gift code not found or invalid"
    }
}
