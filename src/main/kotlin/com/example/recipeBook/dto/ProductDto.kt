package com.example.recipeBook.dto

import com.example.recipeBook.entity.Product
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

// ==================== Request DTO ====================

data class ProductRequest(
    @field:NotBlank(message = "Название обязательно")
    @field:Size(min = 2, message = "Название должно содержать минимум 2 символа")
    val name: String,

    @field:Size(max = 5, message = "Максимальное количество фотографий — 5")
    val photos: List<String> = emptyList(),

    @field:DecimalMin(value = "0.0", message = "Калорийность не может быть отрицательной")
    val calories: Double,

    @field:DecimalMin(value = "0.0", message = "Белки не могут быть отрицательными")
    @field:DecimalMax(value = "100.0", message = "Белки не могут превышать 100г")
    val proteins: Double,

    @field:DecimalMin(value = "0.0", message = "Жиры не могут быть отрицательными")
    @field:DecimalMax(value = "100.0", message = "Жиры не могут превышать 100г")
    val fats: Double,

    @field:DecimalMin(value = "0.0", message = "Углеводы не могут быть отрицательными")
    @field:DecimalMax(value = "100.0", message = "Углеводы не могут превышать 100г")
    val carbohydrates: Double,

    val composition: String? = null,

    @field:NotBlank(message = "Категория обязательна")
    val category: String,

    @field:NotBlank(message = "Требование готовки обязательно")
    val cookingRequirement: String,

    val flags: List<String> = emptyList()
)

// ==================== Response DTO ====================

data class ProductResponse(
    val id: Long,
    val name: String,
    val photos: List<String>,
    val calories: Double,
    val proteins: Double,
    val fats: Double,
    val carbohydrates: Double,
    val composition: String?,
    val category: String,
    val cookingRequirement: String,
    val flags: List<String>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun fromEntity(product: Product): ProductResponse {
            return ProductResponse(
                id = product.id,
                name = product.name,
                photos = product.photos.toList(),
                calories = product.calories,
                proteins = product.proteins,
                fats = product.fats,
                carbohydrates = product.carbohydrates,
                composition = product.composition,
                category = product.category.displayName,
                cookingRequirement = product.cookingRequirement.displayName,
                flags = product.flags.map { it.displayName },
                createdAt = product.createdAt,
                updatedAt = product.updatedAt
            )
        }
    }
}
