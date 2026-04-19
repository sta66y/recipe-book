package com.example.recipeBook.unit.entity

fun singleIngredientQuantityIsAHundred() = DishTest.TestCase(
    productCalories = 10.0,
    productProteins = 10.0,
    productFats = 10.0,
    productCarbs = 10.0,
    quantity = 100.0,
    expectedCalories = 10.0,
    expectedProteins = 10.0,
    expectedFats = 10.0,
    expectedCarbs = 10.0,
)

fun singleIngredientQuantityIsZero() = DishTest.TestCase(
    productCalories = 10.0,
    productProteins = 10.0,
    productFats = 10.0,
    productCarbs = 10.0,
    quantity = 0.0,
    expectedCalories = 0.0,
    expectedProteins = 0.0,
    expectedFats = 0.0,
    expectedCarbs = 0.0,
)

fun singleIngredientWithBigQuantity() = DishTest.TestCase(
    productCalories = 10.0,
    productProteins = 10.0,
    productFats = 10.0,
    productCarbs = 10.0,
    quantity = 999999999.0,
    expectedCalories = 99999999.9,
    expectedProteins = 99999999.9,
    expectedFats = 99999999.9,
    expectedCarbs = 99999999.9,
)

fun singleIngredientQuantityIsMinimum() = DishTest.TestCase(
    productCalories = 10.0,
    productProteins = 10.0,
    productFats = 10.0,
    productCarbs = 10.0,
    quantity = 0.0000000001,
    expectedCalories = 0.00000000001,
    expectedProteins = 0.00000000001,
    expectedFats = 0.00000000001,
    expectedCarbs = 0.00000000001,
)

fun singleIngredientNutritionalValuesAreZero() = DishTest.TestCase(
    productCalories = 0.0,
    productProteins = 0.0,
    productFats = 0.0,
    productCarbs = 0.0,
    quantity = 100.0,
    expectedCalories = 0.0,
    expectedProteins = 0.0,
    expectedFats = 0.0,
    expectedCarbs = 0.0,
)

fun singleIngredientNutritionalValuesAreMinimum() = DishTest.TestCase(
    productCalories = 0.1,
    productProteins = 0.1,
    productFats = 0.1,
    productCarbs = 0.1,
    quantity = 100.0,
    expectedCalories = 0.1,
    expectedProteins = 0.1,
    expectedFats = 0.1,
    expectedCarbs = 0.1,
)

fun singleIngredientNutritionalValuesAreNormal() = DishTest.TestCase(
    productCalories = 500.0,
    productProteins = 20.0,
    productFats = 32.0,
    productCarbs = 10.0,
    quantity = 100.0,
    expectedCalories = 500.0,
    expectedProteins = 20.0,
    expectedFats = 32.0,
    expectedCarbs = 10.0,
)

fun singleIngredientNutritionalValuesAreMaximum() = DishTest.TestCase(
    productCalories = 500.0,
    productProteins = 33.0,
    productFats = 33.0,
    productCarbs = 34.0,
    quantity = 100.0,
    expectedCalories = 500.0,
    expectedProteins = 33.0,
    expectedFats = 33.0,
    expectedCarbs = 34.0,
)

fun twoIngredientsEach100g() = DishTest.MultipleTestCase(
    ingredients = listOf(
        DishTest.IngredientData(
            productId = 1,
            calories = 100.0,
            proteins = 10.0,
            fats = 5.0,
            carbs = 20.0,
            quantity = 100.0,
        ),
        DishTest.IngredientData(
            productId = 2,
            calories = 200.0,
            proteins = 5.0,
            fats = 15.0,
            carbs = 10.0,
            quantity = 100.0,
        )
    ),
    expectedCalories = 300.0,
    expectedProteins = 15.0,
    expectedFats = 20.0,
    expectedCarbs = 30.0,
)

fun fourIngredientsDifferentQuantities() = DishTest.MultipleTestCase(
    ingredients = listOf(
        DishTest.IngredientData(
            productId = 1,
            calories = 1000.0,
            proteins = 100.0,
            fats = 0.0,
            carbs = 0.0,
            quantity = 1210.0,
        ),
        DishTest.IngredientData(
            productId = 2,
            calories = 1000.0,
            proteins = 80.0,
            fats = 20.0,
            carbs = 0.0,
            quantity = 235.0,
        ),
        DishTest.IngredientData(
            productId = 3,
            calories = 1000.0,
            proteins = 0.0,
            fats = 0.0,
            carbs = 100.0,
            quantity = 20.0,
        ),
        DishTest.IngredientData(
            productId = 4,
            calories = 1000.0,
            proteins = 0.0,
            fats = 75.0,
            carbs = 25.0,
            quantity = 15.0,
        )
    ),
    expectedCalories = 14800.0, // 1000*1210/100 + 1000*235/100 + 1000*20/100 + 1000*15/100= 14800
    expectedProteins = 1398.0, // 100*1210/100 + 80*235/100 = 1398
    expectedFats = 58.25,// 20*235/100 + 75*15/100 = 58.25
    expectedCarbs = 23.75 // 100*20/100 + 25*15/100 = 23.75
)


