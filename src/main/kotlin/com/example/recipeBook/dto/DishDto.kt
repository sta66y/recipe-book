package com.example.recipeBook.dto

import com.example.recipeBook.entity.Dish
import com.example.recipeBook.entity.DishIngredient
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

// ==================== Request DTOs ====================

data class DishIngredientRequest(
    val productId: Long,

    @field:DecimalMin(value = "0.0", inclusive = false, message = "Количество должно быть больше 0")
    val quantity: Double
)

data class DishRequest(
    @field:NotBlank(message = "Название обязательно")
    @field:Size(min = 2, message = "Название должно содержать минимум 2 символа")
    val name: String,

    @field:Size(max = 5, message = "Максимальное количество фотографий — 5")
    val photos: List<String> = emptyList(),

    @field:DecimalMin(value = "0.0", message = "Калорийность не может быть отрицательной")
    val calories: Double? = null,

    @field:DecimalMin(value = "0.0", message = "Белки не могут быть отрицательными")
    val proteins: Double? = null,

    @field:DecimalMin(value = "0.0", message = "Жиры не могут быть отрицательными")
    val fats: Double? = null,

    @field:DecimalMin(value = "0.0", message = "Углеводы не могут быть отрицательными")
    val carbohydrates: Double? = null,

    @field:NotEmpty(message = "Блюдо должно содержать хотя бы один продукт")
    @field:Valid
    val ingredients: List<DishIngredientRequest>,

    @field:DecimalMin(value = "0.0", inclusive = false, message = "Размер порции должен быть больше 0")
    val servingSize: Double,

    val category: String? = null,

    val flags: List<String> = emptyList()
)

// ==================== Response DTOs ====================

data class DishIngredientResponse(
    val productId: Long,
    val productName: String,
    val quantity: Double,
    val calories: Double,
    val proteins: Double,
    val fats: Double,
    val carbohydrates: Double
) {
    companion object {
        fun fromEntity(ingredient: DishIngredient): DishIngredientResponse {
            return DishIngredientResponse(
                productId = ingredient.product.id,
                productName = ingredient.product.name,
                quantity = ingredient.quantity,
                calories = ingredient.calcCalories(),
                proteins = ingredient.calcProteins(),
                fats = ingredient.calcFats(),
                carbohydrates = ingredient.calcCarbohydrates()
            )
        }
    }
}

data class DishResponse(
    val id: Long,
    val name: String,
    val photos: List<String>,
    val calories: Double,
    val proteins: Double,
    val fats: Double,
    val carbohydrates: Double,
    val servingSize: Double,
    val category: String,
    val flags: List<String>,
    val availableFlags: List<String>,
    val ingredients: List<DishIngredientResponse>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun fromEntity(dish: Dish): DishResponse {
            return DishResponse(
                id = dish.id,
                name = dish.name,
                photos = dish.photos.toList(),
                calories = dish.calories,
                proteins = dish.proteins,
                fats = dish.fats,
                carbohydrates = dish.carbohydrates,
                servingSize = dish.servingSize,
                category = dish.category.displayName,
                flags = dish.flags.map { it.displayName },
                availableFlags = dish.getAvailableFlags().map { it.displayName },
                ingredients = dish.ingredients.map { DishIngredientResponse.fromEntity(it) },
                createdAt = dish.createdAt,
                updatedAt = dish.updatedAt
            )
        }
    }
}