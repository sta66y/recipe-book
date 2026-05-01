package com.example.recipeBook.ui.pages

import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

abstract class BasePage(protected val driver: WebDriver) {

    protected val wait = WebDriverWait(driver, Duration.ofSeconds(10))

    protected fun click(by: By) {
        wait.until(ExpectedConditions.elementToBeClickable(by)).click()
    }

    protected fun type(by: By, text: String) {
        val el = wait.until(ExpectedConditions.visibilityOfElementLocated(by))
        el.clear()
        el.sendKeys(text)
    }

    protected fun visible(by: By): WebElement =
        wait.until(ExpectedConditions.visibilityOfElementLocated(by))

    protected fun invisible(by: By) {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(by))
    }

    protected fun scrollTo(el: WebElement) {
        (driver as JavascriptExecutor)
            .executeScript("arguments[0].scrollIntoView(true);", el)
    }

    fun waitToastGone() {
        wait.until {
            driver.findElements(By.id("toast"))
                .none { it.isDisplayed }
        }
    }
}