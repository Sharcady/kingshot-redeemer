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
            val activeGiftCodes = waitUntil(driver, wait, "gift codes") {
                findGiftCodes(it).takeIf { codes -> codes.isNotEmpty() }
            }

            if (activeGiftCodes.isEmpty()) {
                throw GiftCodesRetrievalException()
            }

            activeGiftCodes
        } catch (exception: GiftCodesRetrievalException) {
            logger.error("Unable to retrieve active gift codes.", exception)
            throw exception
        } catch (exception: RuntimeException) {
            logger.error("Unexpected error while retrieving active gift codes.", exception)
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
            logger.error("Unable to redeem gift codes for player {} in kingdom {}.", player.id, player.kingdom, exception)
            throw exception
        } catch (exception: RuntimeException) {
            logger.error("Unexpected error while redeeming gift codes for player {} in kingdom {}.", player.id, player.kingdom, exception)
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
        options.addArguments("--disable-gpu", "--disable-dev-shm-usage", "--no-sandbox", "--window-size=1440,1200")
        return ChromeDriver(options)
    }

    private fun openRedeemPageAndFillPlayer(driver: WebDriver, player: Player) {
        val wait = WebDriverWait(driver, Duration.ofSeconds(applicationConfiguration.kingshot.browser.timeoutSeconds))
        driver.get(applicationConfiguration.kingshot.redeemUrl)

        fillFirstVisibleInput(driver, wait, "Player ID input", player.id, playerIdInputLocators())
        fillFirstVisibleInput(driver, wait, "Kingdom input", player.kingdom.toString(), kingdomInputLocators())
    }

    private fun redeemGiftCode(driver: WebDriver, player: Player, giftCode: String): GiftCodeRedemptionResult {
        val wait = WebDriverWait(driver, Duration.ofSeconds(applicationConfiguration.kingshot.browser.timeoutSeconds))
        val giftCodeInput = waitUntil(driver, wait, "Gift code input") {
            findGiftCodeInputOrNull(it)
        }
        fillInput(driver, giftCodeInput, giftCode)
        clickRedeemGiftCodeButton(driver, wait)
        Thread.sleep(applicationConfiguration.kingshot.browser.redemptionResultSettleMillis)
        val message = waitForRedeemCompletion(driver, wait)
        val status = message.toRedemptionStatus()
        dismissRedemptionResultModal(driver, wait)
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

    private fun fillFirstVisibleInput(
        driver: WebDriver,
        wait: WebDriverWait,
        description: String,
        value: String,
        locators: List<By>,
    ) {
        val input = waitUntil(driver, wait, description) {
            locators.asSequence()
                .flatMap { locator -> it.findElements(locator).asSequence() }
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

    private fun clickRedeemGiftCodeButton(driver: WebDriver, wait: WebDriverWait) {
        val button = waitUntil(driver, wait, "redemption confirmation button") {
            it.findElements(By.cssSelector(".exchange_btn:not(.disabled)"))
                .firstOrNull { element -> element.isDisplayed && element.isEnabled }
                ?: findClickableButtonByText(it, "Redeem Gift Code")
                ?: findRedeemButtonNearGiftCodeInput(it)
        }
        clickElement(driver, button)
    }

    private fun dismissRedemptionResultModal(driver: WebDriver, wait: WebDriverWait) {
        val confirmationButton = waitUntil(driver, wait, "redemption result modal confirmation button") {
            it.findElements(By.cssSelector(".modal_content .confirm_btn"))
                .firstOrNull { element -> element.isDisplayed && element.isEnabled }
        }
        clickElement(driver, confirmationButton)
    }

    private fun <T : Any> waitUntil(
        driver: WebDriver,
        wait: WebDriverWait,
        description: String,
        condition: (WebDriver) -> T?,
    ): T =
        try {
            wait.until { condition(it) }
        } catch (exception: TimeoutException) {
            logger.error("Timed out waiting for {}.", description, exception)
            throw GiftCodeRedeemingException(
                message = "Timed out waiting for $description. Current URL: ${driver.currentUrl}. Page title: ${driver.title}. Page text: ${pageText(driver)}",
                cause = exception,
            )
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
                logger.debug("Ignoring a stale button while searching for {}.", label, exception)
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
            logger.warn("Element click was intercepted; retrying with JavaScript.", exception)
            driver.executeScript("arguments[0].click();", element)
        } catch (exception: StaleElementReferenceException) {
            logger.error("Element became stale while clicking it.", exception)
            throw exception
        }
    }

    private fun findGiftCodeInput(driver: WebDriver): WebElement =
        findGiftCodeInputOrNull(driver)
            ?: throw NoSuchElementException("Gift code input was not found")

    private fun findGiftCodeInputOrNull(driver: WebDriver): WebElement? =
        driver.findElements(By.cssSelector("input#giftCode, input[name='giftCode'], input[placeholder='Enter gift code'], .code_con input, input[id*='gift' i]"))
            .firstOrNull { it.isDisplayed }

    private fun waitForRedeemCompletion(driver: WebDriver, wait: WebDriverWait): String =
        try {
            wait.until {
                val message = findRedeemResultMessage(driver)
                message?.takeIf { it.isTerminalRedeemMessage() }
            }
        } catch (exception: TimeoutException) {
            logger.error("Timed out waiting for the gift-code redemption result.", exception)
            throw GiftCodeRedeemingException(
                message = "Timed out waiting for gift code redemption result. Page text: ${driver.findElement(By.tagName("body")).text.normalized()}",
                cause = exception,
            )
        }

    private fun pageText(driver: WebDriver): String =
        try {
            driver.findElement(By.tagName("body")).text.normalized()
        } catch (exception: RuntimeException) {
            logger.warn("Unable to read page text while handling a browser error.", exception)
            "<unavailable: ${exception.message}>"
        }

    private fun findRedeemResultMessage(driver: WebDriver): String? {
        val scriptResult = (driver as JavascriptExecutor).executeScript(
            """
            const modal = [...document.querySelectorAll('.modal_content')]
              .find(element => element.offsetParent !== null);
            if (!modal) return null;

            return modal.innerText
              .split('\n')
              .map(line => line.trim())
              .find(line =>
                /gift code redeemed successfully/i.test(line) ||
                /redeemed successfully\. please check your mail for rewards!/i.test(line) ||
                /already redeemed this gift code/i.test(line) ||
                /gift has already been claimed!/i.test(line) ||
                /same gift code type can only be redeemed once!/i.test(line) ||
                /gift code not found or invalid/i.test(line) ||
                /gift code not found!/i.test(line) ||
                /redemption code error/i.test(line)
              ) || null;
            """.trimIndent(),
        ) as? String

        return scriptResult?.takeIf { it.isNotBlank() }
    }

    private fun findGiftCodes(driver: WebDriver): List<String> =
        driver.findElements(By.cssSelector(".code"))
            .asSequence()
            .map { it.text.trim() }
            .plus(findConciergeGiftCodes(driver).asSequence())
            .filter { it.isGiftCodeLine() }
            .distinct()
            .toList()

    private fun findConciergeGiftCodes(driver: WebDriver): List<String> =
        ((driver as JavascriptExecutor).executeScript(
            """
            const conciergeHeading = [...document.querySelectorAll('p')]
              .find(element => /\bconcierge\b/i.test(element.innerText));
            const conciergeList = conciergeHeading?.nextElementSibling;
            if (conciergeList?.tagName !== 'UL') return [];

            return [...conciergeList.querySelectorAll(':scope > li')]
              .map(item => (item.querySelector('.code')?.innerText || item.innerText).trim())
              .filter(Boolean);
            """.trimIndent(),
        ) as? List<*>)
            ?.filterIsInstance<String>()
            ?: emptyList()

    private fun String.isGiftCodeLine(): Boolean =
        matches(GIFT_CODE_PATTERN) && this !in NON_CODE_LINES && !startsWith("Expires:")

    private fun String.normalized(): String =
        replace(Regex("\\s+"), " ").trim()

    private fun String.isTerminalRedeemMessage(): Boolean {
        val normalizedMessage = normalized()
        return normalizedMessage.contains(GIFT_CODE_REDEEMED_SUCCESSFULLY, ignoreCase = true) ||
            normalizedMessage.contains(REDEEMED_SUCCESSFULLY_CHECK_MAIL, ignoreCase = true) ||
            normalizedMessage.contains(GIFT_CODE_ALREADY_REDEEMED, ignoreCase = true) ||
            normalizedMessage.contains(GIFT_HAS_ALREADY_BEEN_CLAIMED, ignoreCase = true) ||
            normalizedMessage.contains(GIFT_CODE_ALREADY_REDEEMED_ONCE, ignoreCase = true) ||
            normalizedMessage.contains(GIFT_CODE_NOT_FOUND_OR_INVALID, ignoreCase = true) ||
            normalizedMessage.contains(GIFT_CODE_NOT_FOUND, ignoreCase = true) ||
            normalizedMessage.contains(REDEMPTION_CODE_ERROR, ignoreCase = true)
    }

    private fun String.toRedemptionStatus(): GiftCodeRedemptionStatus {
        val normalizedMessage = normalized()
        return when {
            normalizedMessage.contains(GIFT_CODE_REDEEMED_SUCCESSFULLY, ignoreCase = true) ->
                GiftCodeRedemptionStatus.REDEEMED
            normalizedMessage.contains(REDEEMED_SUCCESSFULLY_CHECK_MAIL, ignoreCase = true) ->
                GiftCodeRedemptionStatus.REDEEMED
            normalizedMessage.contains(GIFT_CODE_ALREADY_REDEEMED, ignoreCase = true) ||
                normalizedMessage.contains(GIFT_HAS_ALREADY_BEEN_CLAIMED, ignoreCase = true) ||
                normalizedMessage.contains(GIFT_CODE_ALREADY_REDEEMED_ONCE, ignoreCase = true) ->
                GiftCodeRedemptionStatus.ALREADY_REDEEMED
            else ->
                GiftCodeRedemptionStatus.INVALID
        }
    }

    private fun playerIdInputLocators(): List<By> = listOf(
        By.cssSelector("input[placeholder='Player ID']"),
        By.cssSelector("input[placeholder='Enter your Player ID']"),
        By.cssSelector("input[name='playerId']"),
        By.cssSelector("input[id*='player' i]"),
    )

    private fun kingdomInputLocators(): List<By> = listOf(
        By.cssSelector("input[placeholder='Kingdom']"),
        By.cssSelector("input[placeholder='Your kingdom number (e.g. 23)']"),
        By.cssSelector("input[name='kingdom']"),
        By.cssSelector("input[id*='kingdom' i]"),
    )

    private companion object {
        val logger = LoggerFactory.getLogger(GiftCodeAdapter::class.java)
        val GIFT_CODE_PATTERN = Regex("^[A-Za-z0-9][A-Za-z0-9_-]{2,}$")
        val NON_CODE_LINES = setOf("Active", "Copy", "Code", "Copy Code", "Sign", "In", "Redeem", "Share", "Link")
        const val GIFT_CODE_REDEEMED_SUCCESSFULLY = "Gift code redeemed successfully"
        const val REDEEMED_SUCCESSFULLY_CHECK_MAIL = "Redeemed successfully. Please check your mail for rewards!"
        const val GIFT_CODE_ALREADY_REDEEMED = "already redeemed this gift code"
        const val GIFT_HAS_ALREADY_BEEN_CLAIMED = "Gift has already been claimed!"
        const val GIFT_CODE_ALREADY_REDEEMED_ONCE = "The same Gift Code type can only be redeemed once!"
        const val GIFT_CODE_NOT_FOUND_OR_INVALID = "gift code not found or invalid"
        const val GIFT_CODE_NOT_FOUND = "Gift Code not found!"
        const val REDEMPTION_CODE_ERROR = "Redemption Code Error"
    }
}
