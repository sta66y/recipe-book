package com.example.recipeBook.unit.entity

import com.example.recipeBook.util.buildDish
import com.example.recipeBook.util.buildDishIngredient
import com.example.recipeBook.util.buildProduct
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class DishTest {

    companion object {
        @JvmStatic
        fun prepareData() = listOf(
            "Один ингредиент с нормальным quantity".case(::singleIngredientQuantityIsAHundred),
            "Один ингредиент с quantity = 0".case(::singleIngredientQuantityIsZero),
            "Один ингредиент с большим quantity".case(::singleIngredientWithBigQuantity),
            "Один ингредиент с маленьким quantity".case(::singleIngredientQuantityIsMinimum),
            "Нулевые КБЖУ".case(::singleIngredientNutritionalValuesAreZero),
            "Дробные минимальные значения КБЖУ".case(::singleIngredientNutritionalValuesAreMinimum),
            "Обычные значения КБЖУ".case(::singleIngredientNutritionalValuesAreNormal),
            "Максимальная сумма БЖУ = 100г".case(::singleIngredientNutritionalValuesAreMaximum)
        )

        @JvmStatic
        fun prepareMultipleData() = listOf(
            "Два ингредиента по 100г".multiCase(::twoIngredientsEach100g),
            "Четыре ингредиента разных количеств".multiCase(::fourIngredientsDifferentQuantities),
        )

        private fun String.case(block: () -> TestCase) =
            Arguments.arguments(this, block())

        private fun String.multiCase(block: () -> MultipleTestCase) =
            Arguments.arguments(this, block())
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("prepareData")
    @DisplayName("Расчёт КБЖУ для одиночного ингредиента")
    fun `test recalculateNutrition with single ingredient`(
        caseName: String,
        testCase: TestCase
    ) {
        val product = buildProduct(
            calories = testCase.productCalories,
            proteins = testCase.productProteins,
            fats = testCase.productFats,
            carbohydrates = testCase.productCarbs,
        )

        val dish = buildDish()
        val ingredient = buildDishIngredient(
            dish = dish,
            product = product,
            quantity = testCase.quantity,
        )

        dish.ingredients.add(ingredient)
        dish.recalculateNutrition()

        dish.calories shouldBe testCase.expectedCalories
        dish.proteins shouldBe testCase.expectedProteins
        dish.fats shouldBe testCase.expectedFats
        dish.carbohydrates shouldBe testCase.expectedCarbs
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("prepareMultipleData")
    @DisplayName("Расчёт КБЖУ для множественных ингредиентов")
    fun `test recalculateNutrition with multiple ingredients`(
        caseName: String,
        testCase: MultipleTestCase
    ) {
        val dish = buildDish()

        testCase.ingredients.forEach { ingredientData ->
            val product = buildProduct(
                id = ingredientData.productId,
                calories = ingredientData.calories,
                proteins = ingredientData.proteins,
                fats = ingredientData.fats,
                carbohydrates = ingredientData.carbs,
            )

            dish.ingredients.add(
                buildDishIngredient(
                    id = ingredientData.productId,
                    dish = dish,
                    product = product,
                    quantity = ingredientData.quantity
                )
            )
        }

        dish.recalculateNutrition()

        dish.calories shouldBe testCase.expectedCalories
        dish.proteins shouldBe testCase.expectedProteins
        dish.fats shouldBe testCase.expectedFats
        dish.carbohydrates shouldBe testCase.expectedCarbs
    }

    @Test
    @DisplayName("Пустой список ингредиентов -> КБЖУ = 0")
    fun `recalculateNutrition should return zero for empty ingredients`() {
        val dish = buildDish(ingredients = mutableListOf())

        dish.recalculateNutrition()

        dish.calories shouldBe 0.0
        dish.proteins shouldBe 0.0
        dish.fats shouldBe 0.0
        dish.carbohydrates shouldBe 0.0
    }

    @Test
    @DisplayName("Повторный вызов recalculateNutrition() не меняет результат")
    fun `recalculateNutrition should not change result on repeated calls`() {
        val product = buildProduct(
            calories = 123.0,
            proteins = 12.0,
            fats = 12.12,
            carbohydrates = 0.0
        )

        val dish = buildDish()
        dish.ingredients.add(
            buildDishIngredient(dish = dish, product = product, quantity = 150.0)
        )

        dish.recalculateNutrition()
        val firstCalories = dish.calories
        val firstProteins = dish.proteins
        val firstFats = dish.fats
        val firstCarbs = dish.carbohydrates

        dish.recalculateNutrition()

        dish.calories shouldBe firstCalories
        dish.proteins shouldBe firstProteins
        dish.fats shouldBe firstFats
        dish.carbohydrates shouldBe firstCarbs
    }

    @Test
    @DisplayName("После удаления ингредиента КБЖУ пересчитываются корректно")
    fun `recalculateNutrition after removing ingredient should update nutrition correctly`() {
        val product1 = buildProduct(
            id = 1,
            calories = 123.0,
            proteins = 12.0,
            fats = 12.12,
            carbohydrates = 0.0
        )

        val product2 = buildProduct(
            id = 2,
            calories = 321.0,
            proteins = 32.32,
            fats = 32.32,
            carbohydrates = 0.0
        )

        val dish = buildDish()
        val ingredient1 = buildDishIngredient(id = 1, dish = dish, product = product1, quantity = 150.0)
        val ingredient2 = buildDishIngredient(id = 2, dish = dish, product = product2, quantity = 100.0)

        dish.ingredients.add(ingredient1)
        dish.ingredients.add(ingredient2)
        dish.recalculateNutrition()

        dish.calories shouldBe 505.5 //123*150/100 + 321

        dish.ingredients.remove(ingredient2)
        dish.recalculateNutrition()

        dish.calories shouldBe 184.5
        dish.proteins shouldBe 18.0
        dish.fats shouldBe 18.18
        dish.carbohydrates shouldBe 0.0
    }

    data class TestCase(
        val productCalories: Double,
        val productProteins: Double,
        val productFats: Double,
        val productCarbs: Double,
        val quantity: Double,
        val expectedCalories: Double,
        val expectedProteins: Double,
        val expectedFats: Double,
        val expectedCarbs: Double,
    )

    data class MultipleTestCase(
        val ingredients: List<IngredientData>,
        val expectedCalories: Double,
        val expectedProteins: Double,
        val expectedFats: Double,
        val expectedCarbs: Double,
    )

    data class IngredientData(
        val productId: Long,
        val calories: Double,
        val proteins: Double,
        val fats: Double,
        val carbs: Double,
        val quantity: Double
    )
}
