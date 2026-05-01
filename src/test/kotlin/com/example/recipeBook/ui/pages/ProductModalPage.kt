package com.example.recipeBook.ui.pages

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.ExpectedConditions

class ProductModalPage(driver: WebDriver) : BasePage(driver) {

    private val modal = By.cssSelector("#product-modal.active")

    private val name = By.id("product-name")
    private val gluten = By.id("product-gluten-free")

    private val save = By.id("product-save-btn")
    private val delete = By.id("product-delete-btn")
    private val close = By.id("product-close-btn")
    private val vegan = By.id("product-vegan")

    fun setVegan(v: Boolean) {
        val el = visible(vegan)
        if (el.isSelected != v) el.click()
    }

    fun waitUntilOpened() {
        visible(modal)
        visible(name)
    }

    fun setName(v: String) = type(name, v)

    fun setGlutenFree(v: Boolean) {
        val el = visible(gluten)
        if (el.isSelected != v) el.click()
    }

    fun fillValidProduct(name: String) {
        waitUntilOpened()

        setName(name)
        setCalories(100.0)
        setProteins(10.0)
        setFats(5.0)
        setCarbs(15.0)

        setGlutenFree(true)
        setVegan(true)
    }

    fun getName(): String =
        visible(name).getAttribute("value") ?: ""

    fun save() {
        click(save)
        invisible(modal)
    }

    fun clickDelete() {
        wait.until(ExpectedConditions.elementToBeClickable(delete))
        click(delete)
        waitUntilClosed()
    }

    fun clickClose() {
        click(close)
        invisible(modal)
    }

    fun waitUntilClosed() {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(modal))
    }

    fun setCalories(v: Double) = type(By.id("product-calories"), v.toString())
    fun setProteins(v: Double) = type(By.id("product-proteins"), v.toString())
    fun setFats(v: Double) = type(By.id("product-fats"), v.toString())
    fun setCarbs(v: Double) = type(By.id("product-carbs"), v.toString())
}