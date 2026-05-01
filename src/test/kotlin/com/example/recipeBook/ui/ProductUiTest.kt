package com.example.recipeBook.ui

import com.example.recipeBook.ui.pages.ProductModalPage
import com.example.recipeBook.ui.pages.RecipeBookPage
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID

class ProductUiTest : UiTestBase() {

    private fun page() = RecipeBookPage(driver)
    private fun productModal() = ProductModalPage(driver)

    private fun uniqueName(prefix: String = "Тест") =
        "${prefix}_${UUID.randomUUID().toString().take(8)}"

    private fun createProductViaUi(
        name: String = uniqueName(),
        glutenFree: Boolean = true
    ): String {
        val p = page()

        p.clickProductsTab()
        p.clickAddProduct()

        val modal = productModal()
        modal.waitUntilOpened()
        modal.fillValidProduct(name)

        if (!glutenFree) modal.setGlutenFree(false)

        modal.save()
        modal.waitUntilClosed()

        p.searchProduct(name)
        p.waitProductPresent(name)

        return name
    }

    @Test
    @DisplayName("Создание продукта через UI")
    fun `create product via ui`() {
        openMainPage()

        val name = createProductViaUi(uniqueName("Создание"))

        val p = page()
        p.searchProduct(name)
        p.waitProductPresent(name)

        p.getProductCardNames() shouldContain name
    }

    @Test
    @DisplayName("Просмотр продукта")
    fun `view product details opens modal`() {
        openMainPage()

        val name = createProductViaUi(uniqueName("Детали"))

        val p = page()
        p.searchProduct(name)
        p.waitProductPresent(name)
        p.clickProductCardByName(name)

        val modal = productModal()
        modal.waitUntilOpened()

        modal.getName() shouldBe name
    }

    @Test
    @DisplayName("Редактирование продукта")
    fun `edit product name`() {
        openMainPage()

        val original = createProductViaUi(uniqueName("Оригинал"))

        val p = page()
        p.searchProduct(original)
        p.waitProductPresent(original)
        p.clickProductCardByName(original)

        val modal = productModal()
        modal.waitUntilOpened()

        val updated = uniqueName("Изменен")
        modal.setName(updated)
        modal.save()
        modal.waitUntilClosed()

        p.searchProduct(updated)
        p.waitProductPresent(updated)

        p.getProductCardNames() shouldContain updated
    }

    @Test
    @DisplayName("Удаление продукта")
    fun `delete product via ui`() {
        openMainPage()

        val name = createProductViaUi(uniqueName("Удал"))

        val p = page()
        p.searchProduct(name)
        p.waitProductPresent(name)
        p.clickProductCardByName(name)

        val modal = productModal()
        modal.waitUntilOpened()

        p.waitToastGone()
        modal.clickDelete()
        modal.waitUntilClosed()

        p.searchProduct(name)
        p.waitProductAbsent(name)

        p.getProductCards().size shouldBe 0
    }

    @Test
    @DisplayName("Закрытие без сохранения")
    fun `close modal without save does not create product`() {
        openMainPage()

        val name = uniqueName("НеСохранять")

        val p = page()
        p.clickProductsTab()
        p.clickAddProduct()

        val modal = productModal()
        modal.waitUntilOpened()
        modal.setName(name)
        modal.clickClose()
        modal.waitUntilClosed()

        p.searchProduct(name)
        p.waitProductAbsent(name)

        p.getProductCards().size shouldBe 0
    }
}