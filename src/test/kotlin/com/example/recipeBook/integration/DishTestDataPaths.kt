package com.example.recipeBook.integration

import com.example.recipeBook.data.DishCreateDataSet
import com.example.recipeBook.data.DishDeleteDataSet
import com.example.recipeBook.data.DishGetDataSet
import com.example.recipeBook.data.DishSearchDataSet
import com.example.recipeBook.data.DishUpdateDataSet
import com.example.recipeBook.dto.DishIngredientRequest
import com.example.recipeBook.dto.DishRequest
import com.example.recipeBook.dto.DishResponse
import com.example.recipeBook.dto.DishIngredientResponse
import java.time.LocalDateTime

private fun validRequest(productId: Long) = DishRequest(
    name = "Dish1",
    photos = emptyList(),
    calories = null,
    proteins = null,
    fats = null,
    carbohydrates = null,
    ingredients = listOf(DishIngredientRequest(productId, 100.0)),
    servingSize = 200.0,
    category = "Первое",
    flags = emptyList()
)

private fun validUpdateRequest(productId: Long) = DishRequest(
    name = "UpdatedDish",
    photos = emptyList(),
    calories = null,
    proteins = null,
    fats = null,
    carbohydrates = null,
    ingredients = listOf(DishIngredientRequest(productId, 150.0)),
    servingSize = 250.0,
    category = "Второе",
    flags = emptyList()
)

fun successfulDishCreate(productId: Long) = DishCreateDataSet(
    request = DishRequest(
        name = "Dish1",
        photos = emptyList(),
        calories = null,
        proteins = null,
        fats = null,
        carbohydrates = null,
        ingredients = listOf(DishIngredientRequest(productId, 100.0)),
        servingSize = 200.0,
        category = "Первое",
        flags = listOf("Веган")
    ),
    expectedStatusCode = 201,
    expectedResponse = DishResponse(
        id = 0,
        name = "Dish1",
        photos = emptyList(),
        calories = 50.0,
        proteins = 5.0,
        fats = 2.5,
        carbohydrates = 7.5,
        servingSize = 200.0,
        category = "Первое",
        flags = listOf("Веган"),
        availableFlags = listOf("Веган", "Без глютена"),
        ingredients = listOf(
            DishIngredientResponse(productId, "TestProduct", 100.0, 50.0, 5.0, 2.5, 7.5)
        ),
        createdAt = LocalDateTime.now(),
        updatedAt = null
    )
)

fun dishNameTooShort(productId: Long) = DishCreateDataSet(
    request = validRequest(productId).copy(name = "D"),
    expectedStatusCode = 400,
    expectedErrorMessage = "2 символа"
)

fun dishMacrosSumOver100(productId: Long) = DishCreateDataSet(
    request = validRequest(productId).copy(
        calories = 400.0,
        proteins = 50.0,
        fats = 40.0,
        carbohydrates = 30.0,
        servingSize = 100.0
    ),
    expectedStatusCode = 400,
    expectedErrorMessage = "100"
)

fun dishInvalidCategory(productId: Long) = DishCreateDataSet(
    request = validRequest(productId).copy(category = "InvalidCategory"),
    expectedStatusCode = 400,
    expectedErrorMessage = "категория"
)

fun dishEmptyIngredients() = DishCreateDataSet(
    request = DishRequest(
        name = "EmptyDish",
        photos = emptyList(),
        calories = null,
        proteins = null,
        fats = null,
        carbohydrates = null,
        ingredients = emptyList(),
        servingSize = 200.0,
        category = "Первое",
        flags = emptyList()
    ),
    expectedStatusCode = 400,
    expectedErrorMessage = "продукт"
)

fun successfulDishUpdate(productId: Long) = DishUpdateDataSet(
    request = validUpdateRequest(productId),
    expectedStatusCode = 200,
    expectedResponse = DishResponse(
        id = 1L,
        name = "UpdatedDish",
        photos = emptyList(),
        calories = 75.0,
        proteins = 7.5,
        fats = 3.75,
        carbohydrates = 11.25,
        servingSize = 250.0,
        category = "Второе",
        flags = emptyList(),
        availableFlags = listOf("Веган", "Без глютена"),
        ingredients = listOf(
            DishIngredientResponse(productId, "TestProduct", 150.0, 75.0, 7.5, 3.75, 11.25)
        ),
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
)

fun dishNotFound(productId: Long) = DishUpdateDataSet(
    isExisting = false,
    request = validUpdateRequest(productId),
    expectedStatusCode = 404,
    expectedErrorMessage = "не найдено"
)

fun updateDishNameTooShort(productId: Long) = DishUpdateDataSet(
    request = validUpdateRequest(productId).copy(name = "U"),
    expectedStatusCode = 400,
    expectedErrorMessage = "2 символа"
)

fun successfulDishGet() = DishGetDataSet(
    expectedStatusCode = 200,
    expectedResponse = DishResponse(
        id = 1L,
        name = "Dish1",
        photos = emptyList(),
        calories = 50.0,
        proteins = 5.0,
        fats = 2.5,
        carbohydrates = 7.5,
        servingSize = 200.0,
        category = "Первое",
        flags = emptyList(),
        availableFlags = listOf("Веган", "Без глютена"),
        ingredients = emptyList(),
        createdAt = LocalDateTime.now(),
        updatedAt = null
    )
)

fun dishNotFoundGet() = DishGetDataSet(
    isExisting = false,
    expectedStatusCode = 404,
    expectedErrorMessage = "не найдено"
)

fun searchAllDishes() = DishSearchDataSet(
    expectedStatusCode = 200,
    expectedProductCount = 5
)

fun searchDishesByNamePartial() = DishSearchDataSet(
    name = "dish",
    expectedStatusCode = 200,
    expectedProductCount = 5
)

fun searchDishesByCategory() = DishSearchDataSet(
    category = "Первое",
    expectedStatusCode = 200,
    expectedProductCount = 3
)

fun searchDishesByVeganFlag() = DishSearchDataSet(
    vegan = true,
    expectedStatusCode = 200,
    expectedProductCount = 4
)

fun sortDishesByCalories() = DishSearchDataSet(
    sortBy = "calories",
    expectedStatusCode = 200,
    expectedProductNames = listOf("Dish3", "Dish2", "Dish1", "Dish4", "Dish5")
)

fun searchDishesInvalidCategory() = DishSearchDataSet(
    category = "InvalidCategory",
    expectedStatusCode = 400,
    expectedErrorMessage = "категория"
)

fun successfulDishDelete() = DishDeleteDataSet(
    isExisting = true,
    expectedStatusCode = 204
)

fun deleteDishNotFound() = DishDeleteDataSet(
    isExisting = false,
    expectedStatusCode = 404,
    expectedErrorMessage = "не найдено"
)
