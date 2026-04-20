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
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
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
    @Column(name = "photo_url", columnDefinition = "TEXT")
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

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
) {

    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }

    // ==================== Enums ====================

    enum class Category(val displayName: String) {
        DESSERT("Десерт"),
        FIRST_COURSE("Первое"),
        SECOND_COURSE("Второе"),
        BEVERAGE("Напиток"),
        SALAD("Салат"),
        SOUP("Суп"),
        SNACK("Перекус");

        companion object {
            fun fromDisplayName(name: String) = entries.find { it.displayName == name }
        }
    }

    enum class DishFlag(val displayName: String) {
        VEGAN("Веган"),
        GLUTEN_FREE("Без глютена"),
        SUGAR_FREE("Без сахара");

        companion object {
            fun fromDisplayName(name: String) = entries.find { it.displayName == name }
        }
    }

    // ==================== Макросы (п. 2.3) ====================

    companion object {
        private val MACRO_MAP = linkedMapOf(
            "!десерт" to Category.DESSERT,
            "!первое" to Category.FIRST_COURSE,
            "!второе" to Category.SECOND_COURSE,
            "!напиток" to Category.BEVERAGE,
            "!салат" to Category.SALAD,
            "!суп" to Category.SOUP,
            "!перекус" to Category.SNACK
        )

        /**
         * Парсит макросы из названия блюда.
         * Возвращает пару: (очищенное название, категория из первого найденного макроса или null).
         * Все макросы удаляются из названия, но применяется только первый.
         */
        fun parseMacro(rawName: String): Pair<String, Category?> {
            var firstCategory: Category? = null

            var cleanName = rawName
            for ((macro, category) in MACRO_MAP) {
                if (cleanName.contains(macro, ignoreCase = true)) {
                    if (firstCategory == null) {
                        firstCategory = category
                    }
                    cleanName = cleanName.replace(macro, "", ignoreCase = true)
                }
            }

            return cleanName.trim() to firstCategory
        }
    }

    // ==================== Фото (макс. 5) ====================

    fun addPhoto(photoUrl: String) {
        if (photos.size >= 5) {
            throw IllegalStateException("Максимальное количество фотографий — 5")
        }
        photos.add(photoUrl)
    }

    fun removePhoto(photoUrl: String) {
        photos.remove(photoUrl)
    }

    // ==================== Ингредиенты ====================

    fun addIngredient(product: Product, quantity: Double) {
        require(quantity > 0) { "Количество продукта должно быть больше 0" }
        val ingredient = DishIngredient(
            dish = this,
            product = product,
            quantity = quantity
        )
        ingredients.add(ingredient)
        recalculateNutrition()
        recalculateFlags()
    }

    fun removeIngredient(productId: Long) {
        ingredients.removeAll { it.product.id == productId }
        recalculateNutrition()
        recalculateFlags()
    }

    fun updateIngredientQuantity(productId: Long, newQuantity: Double) {
        require(newQuantity > 0) { "Количество продукта должно быть больше 0" }
        ingredients.find { it.product.id == productId }?.let {
            it.quantity = newQuantity
            recalculateNutrition()
            recalculateFlags()
        }
    }

    // ==================== Расчёт КБЖУ (п. 2.2) ====================

    /**
     * Автоматический расчёт КБЖУ на порцию.
     * Формула: Σ (значение продукта на 100г × количество продукта в порции / 100)
     */
    fun recalculateNutrition() {
        var totalCalories = 0.0
        var totalProteins = 0.0
        var totalFats = 0.0
        var totalCarbohydrates = 0.0

        ingredients.forEach { ingredient ->
            val product = ingredient.product
            val factor = ingredient.quantity / 100.0

            totalCalories += product.calories * factor
            totalProteins += product.proteins * factor
            totalFats += product.fats * factor
            totalCarbohydrates += product.carbohydrates * factor
        }

        this.calories = totalCalories
        this.proteins = totalProteins
        this.fats = totalFats
        this.carbohydrates = totalCarbohydrates
    }

    /**
     * Ручная корректировка КБЖУ пользователем.
     */
    fun adjustNutrition(calories: Double, proteins: Double, fats: Double, carbohydrates: Double) {
        require(calories >= 0) { "Калорийность не может быть отрицательной" }
        require(proteins >= 0) { "Белки не могут быть отрицательными" }
        require(fats >= 0) { "Жиры не могут быть отрицательными" }
        require(carbohydrates >= 0) { "Углеводы не могут быть отрицательными" }

        this.calories = calories
        this.proteins = proteins
        this.fats = fats
        this.carbohydrates = carbohydrates
    }

    fun resetToAutoCalculated() {
        recalculateNutrition()
    }

    // ==================== Флаги (п. 2.4) ====================

    /**
     * Определяет набор доступных флагов на основе состава.
     * Флаг доступен только если ВСЕ продукты в составе имеют соответствующий флаг.
     */
    fun getAvailableFlags(): Set<DishFlag> {
        if (ingredients.isEmpty()) {
            return emptySet()
        }

        val allProducts = ingredients.map { it.product }
        val available = mutableSetOf<DishFlag>()

        if (allProducts.all { it.hasFlag(Product.ProductFlag.VEGAN) }) {
            available.add(DishFlag.VEGAN)
        }
        if (allProducts.all { it.hasFlag(Product.ProductFlag.GLUTEN_FREE) }) {
            available.add(DishFlag.GLUTEN_FREE)
        }
        if (allProducts.all { it.hasFlag(Product.ProductFlag.SUGAR_FREE) }) {
            available.add(DishFlag.SUGAR_FREE)
        }

        return available
    }

    /**
     * Пересчитывает флаги: снимает те, которые более не доступны по составу.
     */
    fun recalculateFlags() {
        val available = getAvailableFlags()
        flags.retainAll(available)
    }

    // ==================== Валидация ====================

    /**
     * Валидация: название >= 2 символа.
     */
    fun validateName() {
        require(name.length >= 2) { "Название блюда должно содержать минимум 2 символа" }
    }

    /**
     * Валидация: servingSize > 0.
     */
    fun validateServingSize() {
        require(servingSize > 0) { "Размер порции должен быть больше 0" }
    }

    /**
     * Валидация: минимум 1 ингредиент.
     */
    fun validateIngredients() {
        require(ingredients.isNotEmpty()) { "Блюдо должно содержать хотя бы 1 ингредиент" }
    }

    /**
     * Валидация: сумма весов ингредиентов не должна превышать размер порции.
     */
    fun validateIngredientsWeight() {
        val totalIngredientsWeight = ingredients.sumOf { it.quantity }
        require(totalIngredientsWeight <= servingSize) {
            "Сумма весов ингредиентов (%.2f г) не может превышать размер порции (%.2f г)".format(totalIngredientsWeight, servingSize)
        }
    }

    /**
     * Валидация: КБЖУ >= 0.
     */
    fun validateNutritionNonNegative() {
        require(calories >= 0) { "Калорийность не может быть отрицательной" }
        require(proteins >= 0) { "Белки не могут быть отрицательными" }
        require(fats >= 0) { "Жиры не могут быть отрицательными" }
        require(carbohydrates >= 0) { "Углеводы не могут быть отрицательными" }
    }

    /**
     * Валидация: сумма БЖУ на 100г порции <= 100.
     */
    fun validateNutritionPer100g() {
        if (servingSize > 0) {
            val proteinsPer100 = proteins * 100.0 / servingSize
            val fatsPer100 = fats * 100.0 / servingSize
            val carbsPer100 = carbohydrates * 100.0 / servingSize
            val sum = proteinsPer100 + fatsPer100 + carbsPer100
            require(sum <= 100) {
                "Сумма БЖУ на 100 грамм порции не может превышать 100 (текущая: %.2f)".format(sum)
            }
        }
    }

    /**
     * Валидация: фото <= 5.
     */
    fun validatePhotos() {
        require(photos.size <= 5) { "Максимальное количество фотографий — 5" }
    }

    /**
     * Полная валидация сущности.
     */
    fun validate() {
        validateName()
        validateServingSize()
        validateIngredients()
        validateNutritionNonNegative()
        validateNutritionPer100g()
        validatePhotos()
    }
}
