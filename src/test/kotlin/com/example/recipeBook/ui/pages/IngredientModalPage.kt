package com.example.recipeBook.ui.pages

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.Select

class IngredientModalPage(driver: WebDriver) : BasePage(driver) {

    private val modal = By.cssSelector("#ingredient-modal.active")
    private val select = By.id("ingredient-product-select")
    private val quantity = By.id("ingredient-quantity")
    private val add = By.id("ingredient-add-btn")

    fun waitForModal() {
        visible(modal)
        wait.until(ExpectedConditions.elementToBeClickable(select))
    }

    fun selectProduct(name: String) {
        val dropdown = Select(visible(select))
        val option = dropdown.options.firstOrNull { it.text.startsWith(name) }
            ?: error("No product: $name")

        dropdown.selectByVisibleText(option.text)
    }

    fun setQuantity(value: Double) {
        type(quantity, value.toString())
    }

    fun clickAdd() {
        click(add)
        invisible(modal)
    }
}