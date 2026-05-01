package com.example.recipeBook.ui.pages

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.Select

class DishModalPage(driver: WebDriver) : BasePage(driver) {

    private val modal = By.cssSelector("#dish-modal.active")

    private val name = By.id("dish-name")
    private val serving = By.id("dish-serving-size")
    private val category = By.id("dish-category")
    private val vegan = By.id("dish-vegan")

    private val calories = By.id("dish-calories")
    private val proteins = By.id("dish-proteins")

    private val addIngredient = By.id("add-ingredient-btn")
    private val save = By.id("dish-save-btn")
    private val delete = By.id("dish-delete-btn")
    private val close = By.id("dish-close-btn")

    private val ingredientItems =
        By.cssSelector("#dish-ingredients-container .ingredient-item")

    fun waitForModal() {
        visible(modal)
        visible(name)
    }

    fun waitUntilClosed() {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(modal))
    }

    fun setName(v: String) = type(name, v)

    fun setServingSize(v: Double) =
        type(serving, v.toString())

    fun selectCategory(v: String) {
        Select(visible(category)).selectByVisibleText(v)
    }

    fun getName(): String =
        visible(name).getAttribute("value") ?: ""

    fun getCalories(): String =
        visible(calories).getAttribute("value") ?: ""

    fun getProteins(): String =
        visible(proteins).getAttribute("value") ?: ""

    fun getIngredientCount(): Int =
        driver.findElements(ingredientItems).size

    fun clickAddIngredient(): IngredientModalPage {
        click(addIngredient)
        val modal = IngredientModalPage(driver)
        modal.waitForModal()
        return modal
    }

    fun clickSave() {
        click(save)
        waitUntilClosed()
    }

    fun clickDelete() {
        click(delete)
        waitUntilClosed()
    }

    fun clickClose() {
        click(close)
        waitUntilClosed()
    }
}