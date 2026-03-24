package com.example.recipeBook.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "dishes")
@EntityListeners(AuditingEntityListener::class)
class Dish(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "name", nullable = false, length = 255)
    var name: String,

    @ElementCollection
    @CollectionTable(
        name = "dish_photos",
        joinColumns = [JoinColumn(name = "dish_id")]
    )
    @Column(name = "photo_url")
    var photos: MutableList<String> = mutableListOf(),

    @Column(name = "calories", nullable = false)
    var calories: Double = 0.0,

    @Column(name = "proteins", nullable = false)
    var proteins: Double = 0.0,

    @Column(name = "fats", nullable = false)
    var fats: Double = 0.0,

    @Column(name = "carbohydrates", nullable = false)
    var carbohydrates: Double = 0.0,

    @Column(name = "serving_size", nullable = false)
    var servingSize: Double,

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    var category: Category,

    @ElementCollection
    @CollectionTable(
        name = "dish_flags",
        joinColumns = [JoinColumn(name = "dish_id")]
    )
    @Column(name = "flag", length = 20)
    @Enumerated(EnumType.STRING)
    var flags: MutableSet<DishFlag> = mutableSetOf(),

    @OneToMany(
        mappedBy = "dish",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    var ingredients: MutableList<DishIngredient> = mutableListOf(),

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
) {
    enum class Category(val displayName: String) {
        DESSERT("Десерт"),
        FIRST_COURSE("Первое"),
        SECOND_COURSE("Второе"),
        BEVERAGE("Напиток"),
        SALAD("Салат"),
        SOUP("Суп"),
        SNACK("Перекус")
        ;

        companion object {
            fun fromDisplayName(name: String) = entries.find { it.displayName == name }
        }
    }

    enum class DishFlag(val displayName: String) {
        VEGAN("Веган"),
        GLUTEN_FREE("Без глютена"),
        SUGAR_FREE("Без сахара")
        ;

        companion object {
            fun fromDisplayName(name: String) = entries.find { it.displayName == name }
        }
    }

    fun addIngredient(product: Product, quantity: Double) {
        val ingredient = DishIngredient(
            dish = this,
            product = product,
            quantity = quantity
        )
        ingredients.add(ingredient)
        recalculateNutrition()
    }

    fun removeIngredient(productId: Long) {
        ingredients.removeAll { it.product.id == productId }
        recalculateNutrition()
    }

    fun updateIngredientQuantity(productId: Long, newQuantity: Double) {
        ingredients.find { it.product.id == productId }?.let {
            it.quantity = newQuantity
            recalculateNutrition()
        }
    }

    fun recalculateNutrition() {
        var totalCalories = 0.0
        var totalProteins = 0.0
        var totalFats = 0.0
        var totalCarbohydrates = 0.0
        var totalWeight = 0.0

        ingredients.forEach { ingredient ->
            val product = ingredient.product
            val qty = ingredient.quantity
            val factor = qty / 100.0

            totalCalories += product.calories * factor
            totalProteins += product.proteins * factor
            totalFats += product.fats * factor
            totalCarbohydrates += product.carbohydrates * factor
            totalWeight += qty
        }

        if (totalWeight > 0 && servingSize > 0) {
            val portionRatio = servingSize / totalWeight
            this.calories = totalCalories * portionRatio
            this.proteins = totalProteins * portionRatio
            this.fats = totalFats * portionRatio
            this.carbohydrates = totalCarbohydrates * portionRatio
        } else {
            this.calories = totalCalories
            this.proteins = totalProteins
            this.fats = totalFats
            this.carbohydrates = totalCarbohydrates
        }
    }


    fun autoDetectFlags() {
        val newFlags = mutableSetOf<DishFlag>()

        var isVegan = true
        var isGlutenFree = true
        var isSugarFree = true

        ingredients.forEach { ingredient ->
            val product = ingredient.product

            if (product.category == Product.Category.MEAT ||
                product.category == Product.Category.FROZEN && product.name.contains("мясо", ignoreCase = true)) {
                isVegan = false
            }

            if (product.category == Product.Category.GROATS &&
                !product.flags.contains(Product.ProductFlag.GLUTEN_FREE)) {
                isGlutenFree = false
            }

            if (product.name.contains("сахар", ignoreCase = true) ||
                product.category == Product.Category.SWEETS) {
                isSugarFree = false
            }
        }

        if (isVegan) newFlags.add(DishFlag.VEGAN)
        if (isGlutenFree) newFlags.add(DishFlag.GLUTEN_FREE)
        if (isSugarFree) newFlags.add(DishFlag.SUGAR_FREE)

        this.flags = newFlags
    }

    fun adjustNutrition(calories: Double, proteins: Double, fats: Double, carbohydrates: Double) {
        this.calories = calories
        this.proteins = proteins
        this.fats = fats
        this.carbohydrates = carbohydrates
    }

    fun resetToAutoCalculated() {
        recalculateNutrition()
    }
}
