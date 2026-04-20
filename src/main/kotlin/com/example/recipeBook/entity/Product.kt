package com.example.recipeBook.entity

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "products")
@EntityListeners(AuditingEntityListener::class)
class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "name", nullable = false, length = 255)
    var name: String,

    @ElementCollection
    @CollectionTable(
        name = "product_photos",
        joinColumns = [JoinColumn(name = "product_id")]
    )
    @Column(name = "photo_url", columnDefinition = "TEXT")
    var photos: MutableList<String> = mutableListOf(),

    @Column(name = "calories", nullable = false)
    var calories: Double,

    @Column(name = "proteins", nullable = false)
    var proteins: Double,

    @Column(name = "fats", nullable = false)
    var fats: Double,

    @Column(name = "carbohydrates", nullable = false)
    var carbohydrates: Double,

    @Column(name = "composition", columnDefinition = "TEXT")
    var composition: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    var category: Category,

    @Enumerated(EnumType.STRING)
    @Column(name = "cooking_requirement", nullable = false, length = 30)
    var cookingRequirement: CookingRequirement,

    @ElementCollection
    @CollectionTable(
        name = "product_flags",
        joinColumns = [JoinColumn(name = "product_id")]
    )
    @Column(name = "flag", length = 20)
    @Enumerated(EnumType.STRING)
    var flags: MutableSet<ProductFlag> = mutableSetOf(),

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
        FROZEN("Замороженный"),
        MEAT("Мясной"),
        VEGETABLES("Овощи"),
        GREENS("Зелень"),
        SPICES("Специи"),
        GROATS("Крупы"),
        CANNED_FOOD("Консервы"),
        LIQUID("Жидкость"),
        SWEETS("Сладости");

        companion object {
            fun fromDisplayName(name: String) = entries.find { it.displayName == name }
        }
    }

    enum class CookingRequirement(val displayName: String) {
        READY_TO_EAT("Готовый к употреблению"),
        SEMI_FINISHED("Полуфабрикат"),
        REQUIRES_COOKING("Требует приготовления");

        companion object {
            fun fromDisplayName(name: String) = entries.find { it.displayName == name }
        }
    }

    enum class ProductFlag(val displayName: String) {
        VEGAN("Веган"),
        GLUTEN_FREE("Без глютена"),
        SUGAR_FREE("Без сахара");

        companion object {
            fun fromDisplayName(name: String) = entries.find { it.displayName == name }
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

    // ==================== Флаги ====================

    fun addFlag(flag: ProductFlag) {
        flags.add(flag)
    }

    fun removeFlag(flag: ProductFlag) {
        flags.remove(flag)
    }

    fun hasFlag(flag: ProductFlag): Boolean = flags.contains(flag)

    fun isVegan(): Boolean = hasFlag(ProductFlag.VEGAN)
    fun isGlutenFree(): Boolean = hasFlag(ProductFlag.GLUTEN_FREE)
    fun isSugarFree(): Boolean = hasFlag(ProductFlag.SUGAR_FREE)

    // ==================== Валидация ====================

    /**
     * Валидация: название >= 2 символа.
     */
    fun validateName() {
        require(name.length >= 2) { "Название продукта должно содержать минимум 2 символа" }
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
     * Валидация: каждое значение БЖУ <= 100.
     */
    fun validateNutritionMax() {
        require(proteins <= 100) { "Белки на 100г не могут превышать 100" }
        require(fats <= 100) { "Жиры на 100г не могут превышать 100" }
        require(carbohydrates <= 100) { "Углеводы на 100г не могут превышать 100" }
    }

    /**
     * Валидация: сумма БЖУ на 100г <= 100.
     */
    fun validateNutritionSum() {
        val sum = proteins + fats + carbohydrates
        require(sum <= 100) {
            "Сумма БЖУ на 100 грамм не может превышать 100 (текущая: %.2f)".format(sum)
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
        validateNutritionNonNegative()
        validateNutritionMax()
        validateNutritionSum()
        validatePhotos()
    }
}
