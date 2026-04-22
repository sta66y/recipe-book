package com.example.recipeBook.data

import com.example.recipeBook.dto.ProductRequest
import com.example.recipeBook.dto.ProductResponse

data class ProductCreateDataSet(
    val request: ProductRequest,
    val expectedResponse: ProductResponse? = null,
    val expectedStatusCode: Int,
    val expectedErrorMessage: String? = null
)

data class ProductUpdateDataSet(
    val isExisting: Boolean = true,
    val request: ProductRequest,
    val expectedResponse: ProductResponse? = null,
    val expectedStatusCode: Int,
    val expectedErrorMessage: String? = null
)

data class ProductGetDataSet(
    val isExisting: Boolean = true,
    val expectedStatusCode: Int,
    val expectedResponse: ProductResponse? = null,
    val expectedErrorMessage: String? = null
)

data class ProductSearchDataSet(
    val name: String? = null,
    val category: String? = null,
    val cookingRequirement: String? = null,
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

data class ProductDeleteDataSet(
    val isExisting: Boolean = true,
    val isUsedInDish: Boolean = false, // ✅ Флаг для проверки связи с блюдом
    val expectedStatusCode: Int,
    val expectedErrorMessage: String? = null
)
