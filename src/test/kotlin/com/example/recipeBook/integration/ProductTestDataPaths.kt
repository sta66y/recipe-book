package com.example.recipeBook.integration

import com.example.recipeBook.data.ProductCreateDataSet
import com.example.recipeBook.data.ProductDeleteDataSet
import com.example.recipeBook.data.ProductGetDataSet
import com.example.recipeBook.data.ProductSearchDataSet
import com.example.recipeBook.data.ProductUpdateDataSet
import com.example.recipeBook.dto.ProductRequest
import com.example.recipeBook.dto.ProductResponse
import java.time.LocalDateTime

private fun validRequest() = ProductRequest(
    name = "Product1",
    photos = emptyList(),
    calories = 100.0,
    proteins = 10.0,
    fats = 5.0,
    carbohydrates = 15.0,
    composition = "Composition1",
    category = "Овощи",
    cookingRequirement = "Готовый к употреблению",
    flags = emptyList()
)

private fun validUpdateRequest() = ProductRequest(
    name = "UpdatedProduct",
    photos = emptyList(),
    calories = 150.0,
    proteins = 15.0,
    fats = 7.0,
    carbohydrates = 20.0,
    composition = "UpdatedComposition",
    category = "Овощи",
    cookingRequirement = "Готовый к употреблению",
    flags = emptyList()
)

fun successfulCreate() = ProductCreateDataSet(
    request = ProductRequest(
        name = "Product1",
        photos = emptyList(),
        calories = 100.0,
        proteins = 10.0,
        fats = 5.0,
        carbohydrates = 15.0,
        composition = "Composition1",
        category = "Овощи",
        cookingRequirement = "Готовый к употреблению",
        flags = listOf("Веган", "Без глютена")
    ),
    expectedStatusCode = 201,
    expectedResponse = ProductResponse(
        id = 0,
        name = "Product1",
        photos = emptyList(),
        calories = 100.0,
        proteins = 10.0,
        fats = 5.0,
        carbohydrates = 15.0,
        composition = "Composition1",
        category = "Овощи",
        cookingRequirement = "Готовый к употреблению",
        flags = listOf("Веган", "Без глютена"),
        createdAt = LocalDateTime.now(),
        updatedAt = null
    )
)

fun nameTooShort() = ProductCreateDataSet(
    request = validRequest().copy(name = "P"),
    expectedStatusCode = 400,
    expectedErrorMessage = "2 символа"
)

fun macrosSumOver100() = ProductCreateDataSet(
    request = validRequest().copy(
        proteins = 50.0,
        fats = 40.0,
        carbohydrates = 30.0
    ),
    expectedStatusCode = 400,
    expectedErrorMessage = "100"
)

fun invalidCategory() = ProductCreateDataSet(
    request = validRequest().copy(category = "InvalidCategory"),
    expectedStatusCode = 400,
    expectedErrorMessage = "категория"
)

fun successfulUpdate() = ProductUpdateDataSet(
    request = validUpdateRequest(),
    expectedStatusCode = 200,
    expectedResponse = ProductResponse(
        id = 1L,
        name = "UpdatedProduct",
        photos = emptyList(),
        calories = 150.0,
        proteins = 15.0,
        fats = 7.0,
        carbohydrates = 20.0,
        composition = "UpdatedComposition",
        category = "Овощи",
        cookingRequirement = "Готовый к употреблению",
        flags = emptyList(),
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
)

fun productNotFound() = ProductUpdateDataSet(
    isExisting = false,
    request = validUpdateRequest(),
    expectedStatusCode = 404,
    expectedErrorMessage = "не найден"
)

fun updateNameTooShort() = ProductUpdateDataSet(
    request = validUpdateRequest().copy(name = "U"),
    expectedStatusCode = 400,
    expectedErrorMessage = "2 символа"
)

fun successfulGet() = ProductGetDataSet(
    expectedStatusCode = 200,
    expectedResponse = ProductResponse(
        id = 1L,
        name = "Product1",
        photos = emptyList(),
        calories = 100.0,
        proteins = 10.0,
        fats = 5.0,
        carbohydrates = 15.0,
        composition = "Composition1",
        category = "Овощи",
        cookingRequirement = "Готовый к употреблению",
        flags = emptyList(),
        createdAt = LocalDateTime.now(),
        updatedAt = null
    )
)

fun productNotFoundGet() = ProductGetDataSet(
    isExisting = false,
    expectedStatusCode = 404,
    expectedErrorMessage = "не найден"
)

fun searchAllProducts() = ProductSearchDataSet(
    expectedStatusCode = 200,
    expectedProductCount = 5
)

fun searchByNamePartial() = ProductSearchDataSet(
    name = "prod",
    expectedStatusCode = 200,
    expectedProductCount = 5
)

fun searchByCategory() = ProductSearchDataSet(
    category = "Овощи",
    expectedStatusCode = 200,
    expectedProductCount = 4
)

fun searchByVeganFlag() = ProductSearchDataSet(
    vegan = true,
    expectedStatusCode = 200,
    expectedProductCount = 4
)

fun sortByCalories() = ProductSearchDataSet(
    sortBy = "calories",
    expectedStatusCode = 200,
    expectedProductNames = listOf("Product3", "Product2", "Product1", "Product4", "Product5")
)

fun searchInvalidCategory() = ProductSearchDataSet(
    category = "InvalidCategory",
    expectedStatusCode = 400,
    expectedErrorMessage = "категория"
)

fun successfulDelete() = ProductDeleteDataSet(
    isExisting = true,
    expectedStatusCode = 204
)

fun deleteProductNotFound() = ProductDeleteDataSet(
    isExisting = false,
    expectedStatusCode = 404,
    expectedErrorMessage = "не найден"
)

fun deleteProductUsedInDish() = ProductDeleteDataSet(
    isExisting = true,
    isUsedInDish = true,
    expectedStatusCode = 409,
    expectedErrorMessage = "используется в блюдах"
)
