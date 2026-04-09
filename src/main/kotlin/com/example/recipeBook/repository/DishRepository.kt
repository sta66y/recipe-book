package com.example.recipeBook.repository

import com.example.recipeBook.entity.Dish
import com.example.recipeBook.entity.Product
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface DishRepository : JpaRepository<Dish, Long> {

    fun findByNameContainingIgnoreCase(name: String): List<Dish>

    fun findByCategory(category: Dish.Category): List<Dish>

    /**
     * Комбинированный поиск/фильтрация блюд.
     * - name: поиск по подстроке (case-insensitive), null = без фильтра
     * - category: фильтр по категории, null = без фильтра
     * - vegan/glutenFree/sugarFree: true = требуется флаг, null/false = без фильтра
     */
    @Query(
        """
        SELECT DISTINCT d FROM Dish d
        LEFT JOIN d.flags f
        WHERE (:name IS NULL OR d.name LIKE %:name%)
          AND (:category IS NULL OR d.category = :category)
          AND (:vegan = false OR 'VEGAN' IN (SELECT f1 FROM Dish d1 JOIN d1.flags f1 WHERE d1 = d))
          AND (:glutenFree = false OR 'GLUTEN_FREE' IN (SELECT f2 FROM Dish d2 JOIN d2.flags f2 WHERE d2 = d))
          AND (:sugarFree = false OR 'SUGAR_FREE' IN (SELECT f3 FROM Dish d3 JOIN d3.flags f3 WHERE d3 = d))
        """
    )
    fun findWithFilters(
        @Param("name") name: String?,
        @Param("category") category: Dish.Category?,
        @Param("vegan") vegan: Boolean = false,
        @Param("glutenFree") glutenFree: Boolean = false,
        @Param("sugarFree") sugarFree: Boolean = false
    ): List<Dish>

    /**
     * Проверяет, используется ли продукт хотя бы в одном блюде (п. 1.5).
     */
    @Query("SELECT COUNT(d) > 0 FROM Dish d JOIN d.ingredients i WHERE i.product.id = :productId")
    fun existsByProductInIngredients(@Param("productId") productId: Long): Boolean

    /**
     * Возвращает список блюд, в которых используется данный продукт (п. 1.5).
     * Нужно для информирования пользователя при попытке удаления продукта.
     */
    @Query("SELECT d FROM Dish d JOIN d.ingredients i WHERE i.product.id = :productId")
    fun findByProductInIngredients(@Param("productId") productId: Long): List<Dish>
}
