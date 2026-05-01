package com.example.recipeBook.ui

import com.example.recipeBook.ui.pages.*
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID

class DishUiTest : UiTestBase() {

    private fun page() = RecipeBookPage(driver)
    private fun dishModal() = DishModalPage(driver)
    private fun productModal() = ProductModalPage(driver)

    private fun uniqueName(prefix: String = "Блюдо") =
        "${prefix}_${UUID.randomUUID().toString().take(8)}"

    private fun createProductViaUi(
        name: String = uniqueName("Прод"),
        glutenFree: Boolean = true,
        vegan: Boolean = true
    ): String {
        val p = page()

        p.clickProductsTab()
        p.clickAddProduct()

        val modal = productModal()
        modal.waitUntilOpened()
        modal.fillValidProduct(name)

        if (!glutenFree) modal.setGlutenFree(false)
        if (vegan) modal.setVegan(true)

        modal.save()

        p.searchProduct(name)
        p.waitProductPresent(name)

        return name
    }

    private fun createDishViaUi(
        dishName: String,
        productName: String,
        quantity: Double = 100.0,
        servingSize: Double = 200.0,
        category: String = "Первое"
    ): String {
        val p = page()

        p.clickDishesTab()
        p.clickAddDish()

        val modal = dishModal()
        modal.waitForModal()
        modal.setName(dishName)
        modal.setServingSize(servingSize)
        modal.selectCategory(category)

        modal.clickAddIngredient().apply {
            selectProduct(productName)
            setQuantity(quantity)
            clickAdd()
        }

        modal.clickSave()

        p.searchDish(dishName)
        p.waitDishPresent(dishName)

        return dishName
    }

    @Test
    @DisplayName("Создание блюда с одним ингредиентом")
    fun `create dish with one ingredient`() {
        openMainPage()

        val product = createProductViaUi()
        val dish = uniqueName("Одно")

        createDishViaUi(dish, product)

        val p = page()
        p.clickDishesTab()
        p.searchDish(dish)
        p.waitDishPresent(dish)

        p.getDishCardNames() shouldContain dish
    }

    @Test
    @DisplayName("Просмотр деталей блюда")
    fun `view dish details`() {
        openMainPage()

        val product = createProductViaUi()
        val dish = createDishViaUi(uniqueName("Просмотр"), product)

        val p = page()
        p.clickDishesTab()
        p.searchDish(dish)
        p.waitDishPresent(dish)
        p.clickDishCardByName(dish)

        val modal = dishModal()
        modal.waitForModal()

        modal.getName() shouldBe dish
        modal.getIngredientCount() shouldBe 1
    }

    @Test
    @DisplayName("Редактирование блюда")
    fun `edit dish name`() {
        openMainPage()

        val product = createProductViaUi()
        val original = createDishViaUi(uniqueName("ДоРедакт"), product)

        val p = page()
        p.clickDishesTab()
        p.searchDish(original)
        p.waitDishPresent(original)
        p.clickDishCardByName(original)

        val modal = dishModal()
        modal.waitForModal()

        val updated = uniqueName("ПослеРедакт")
        modal.setName(updated)
        modal.clickSave()

        p.searchDish(updated)
        p.waitDishPresent(updated)

        p.getDishCardNames() shouldContain updated
    }

    @Test
    @DisplayName("Удаление блюда")
    fun `delete dish via ui`() {
        openMainPage()

        val product = createProductViaUi()
        val dish = createDishViaUi(uniqueName("Удал"), product)

        val p = page()
        p.clickDishesTab()
        p.searchDish(dish)
        p.waitDishPresent(dish)
        p.clickDishCardByName(dish)

        val modal = dishModal()
        modal.waitForModal()
        modal.clickDelete()

        p.searchDish(dish)
        p.waitDishAbsent(dish)

        p.getDishCards().size shouldBe 0
    }

    @Test
    @DisplayName("Закрытие без сохранения")
    fun `close dish modal without save`() {
        openMainPage()

        val dish = uniqueName("НеСохранять")

        val p = page()
        p.clickDishesTab()
        p.clickAddDish()

        val modal = dishModal()
        modal.waitForModal()
        modal.setName(dish)
        modal.clickClose()

        p.searchDish(dish)
        p.waitDishAbsent(dish)

        p.getDishCards().size shouldBe 0
    }

    @Test
    @DisplayName("Несколько ингредиентов")
    fun `add multiple ingredients`() {
        openMainPage()

        val p1 = createProductViaUi(uniqueName("Ингр1"))
        val p2 = createProductViaUi(uniqueName("Ингр2"))

        val p = page()
        p.clickDishesTab()
        p.clickAddDish()

        val modal = dishModal()
        modal.waitForModal()
        modal.setName(uniqueName("Много"))
        modal.setServingSize(300.0)
        modal.selectCategory("Второе")

        listOf(
            p1 to 100.0,
            p2 to 50.0
        ).forEach { (prod, qty) ->
            modal.clickAddIngredient().apply {
                selectProduct(prod)
                setQuantity(qty)
                clickAdd()
            }
        }

        modal.getIngredientCount() shouldBe 2
        modal.clickSave()
    }

    @Test
    @DisplayName("Авто КБЖУ")
    fun `nutrition auto calculated`() {
        openMainPage()

        val product = createProductViaUi()

        val p = page()
        p.clickDishesTab()
        p.clickAddDish()

        val modal = dishModal()
        modal.waitForModal()
        modal.setName(uniqueName())
        modal.setServingSize(200.0)
        modal.selectCategory("Первое")

        modal.clickAddIngredient().apply {
            selectProduct(product)
            setQuantity(100.0)
            clickAdd()
        }

        modal.getCalories().isNotBlank() shouldBe true
        modal.getProteins().isNotBlank() shouldBe true
    }
}