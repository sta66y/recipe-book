package com.example.recipeBook.ui.pages

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement

class RecipeBookPage(driver: WebDriver) : BasePage(driver) {

    private val productsTab = By.id("products-tab-btn")
    private val dishesTab = By.id("dishes-tab-btn")

    private val productSearch = By.id("product-search")
    private val dishSearch = By.id("dish-search")

    fun clickProductsTab() = click(productsTab)
    fun clickDishesTab() = click(dishesTab)

    fun searchProduct(q: String) = type(productSearch, q)
    fun searchDish(q: String) = type(dishSearch, q)

    fun getProductCardNames(): List<String> =
        driver.findElements(By.cssSelector("#products-list .card-title"))
            .map { it.text }

    fun getDishCardNames(): List<String> =
        driver.findElements(By.cssSelector("#dishes-list .card-title"))
            .map { it.text }

    fun getProductCards(): List<WebElement> =
        driver.findElements(By.cssSelector("#products-list .card"))

    fun getDishCards(): List<WebElement> =
        driver.findElements(By.cssSelector("#dishes-list .card"))

    fun clickAddProduct() {
        click(By.id("add-product-btn"))
        visible(By.cssSelector("#product-modal.active"))
    }

    fun clickAddDish() {
        click(By.id("add-dish-btn"))
        visible(By.cssSelector("#dish-modal.active"))
    }

    fun clickProductCardByName(name: String) {
        wait.until { getProductCardNames().any { it.contains(name) } }
        val card = getProductCards().first { it.text.contains(name) }
        scrollTo(card)
        card.click()
    }

    fun clickDishCardByName(name: String) {
        wait.until { getDishCardNames().any { it.contains(name) } }
        val card = getDishCards().first { it.text.contains(name) }
        scrollTo(card)
        card.click()
    }

    fun waitProductPresent(name: String) {
        wait.until { getProductCardNames().any { it.contains(name) } }
    }

    fun waitProductAbsent(name: String) {
        wait.until { getProductCardNames().none { it.contains(name) } }
    }

    fun waitDishPresent(name: String) {
        wait.until { getDishCardNames().any { it.contains(name) } }
    }

    fun waitDishAbsent(name: String) {
        wait.until { getDishCardNames().none { it.contains(name) } }
    }
}