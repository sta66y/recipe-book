package com.example.recipeBook.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "dish_ingredients")
class DishIngredient(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dish_id", nullable = false)
    @JsonIgnore
    var dish: Dish,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product,

    @Column(name = "quantity", nullable = false)
    var quantity: Double
) {
    /**
     * Валидация: количество продукта должно быть > 0.
     */
    fun validate() {
        require(quantity > 0) { "Количество продукта должно быть больше 0" }
    }

    /**
     * Рассчитывает вклад данного ингредиента в калорийность порции.
     */
    fun calcCalories(): Double = product.calories * quantity / 100.0

    /**
     * Рассчитывает вклад данного ингредиента в белки порции.
     */
    fun calcProteins(): Double = product.proteins * quantity / 100.0

    /**
     * Рассчитывает вклад данного ингредиента в жиры порции.
     */
    fun calcFats(): Double = product.fats * quantity / 100.0

    /**
     * Рассчитывает вклад данного ингредиента в углеводы порции.
     */
    fun calcCarbohydrates(): Double = product.carbohydrates * quantity / 100.0
}