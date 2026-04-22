package com.example.recipeBook.data

import com.example.recipeBook.dto.DishRequest
import com.example.recipeBook.dto.DishResponse

data class DishCreateDataSet(
    val request: DishRequest,
    val expectedResponse: DishResponse? = null,
    val expectedStatusCode: Int,
    val expectedErrorMessage: String? = null
)

data class DishUpdateDataSet(
    val isExisting: Boolean = true,
    val request: DishRequest,
    val expectedResponse: DishResponse? = null,
    val expectedStatusCode: Int,
    val expectedErrorMessage: String? = null
)

data class DishGetDataSet(
    val isExisting: Boolean = true,
    val expectedStatusCode: Int,
    val expectedResponse: DishResponse? = null,
    val expectedErrorMessage: String? = null
)

data class DishSearchDataSet(
    val name: String? = null,
    val category: String? = null,
    val vegan: Boolean = false,
    val glutenFree: Boolean = false,
    val sugarFree: Boolean = false,
    val sortBy: String? = null,
    val expectedStatusCode: Int,
    val expectedProductCount: Int? = null,
    val expectedFirstProductName: String? = null,
    val expectedProductNames: List<String>? = null,
    val expectedErrorMessage: String? = null
)

data class DishDeleteDataSet(
    val isExisting: Boolean = true,
    val expectedStatusCode: Int,
    val expectedErrorMessage: String? = null
)
