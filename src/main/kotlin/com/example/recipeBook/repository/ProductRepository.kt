package com.example.recipeBook.repository

import com.example.recipeBook.entity.Product
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository : JpaRepository<Product, Long> {

    fun findByNameContainingIgnoreCase(name: String): List<Product>

    fun findByCategory(category: Product.Category): List<Product>

    /**
     * Комбинированный поиск/фильтрация продуктов.
     * - name: поиск по подстроке (case-insensitive), null = без фильтра
     * - category: фильтр по категории, null = без фильтра
     * - cookingRequirement: фильтр по необходимости готовки, null = без фильтра
     * - vegan/glutenFree/sugarFree: true = требуется флаг, false = без фильтра
     *
     * Сортировка задаётся параметром sortBy:
     *   name, calories, proteins, fats, carbohydrates
     */
    @Query(
        """
        SELECT DISTINCT p FROM Product p
        WHERE (:name IS NULL OR LOWER(p.name) LIKE %:name%)
          AND (:category IS NULL OR p.category = :category)
          AND (:cookingRequirement IS NULL OR p.cookingRequirement = :cookingRequirement)
          AND (:vegan = false OR 'VEGAN' IN (SELECT f1 FROM Product p1 JOIN p1.flags f1 WHERE p1 = p))
          AND (:glutenFree = false OR 'GLUTEN_FREE' IN (SELECT f2 FROM Product p2 JOIN p2.flags f2 WHERE p2 = p))
          AND (:sugarFree = false OR 'SUGAR_FREE' IN (SELECT f3 FROM Product p3 JOIN p3.flags f3 WHERE p3 = p))
        """
    )
    fun findWithFilters(
        @Param("name") name: String?,
        @Param("category") category: Product.Category?,
        @Param("cookingRequirement") cookingRequirement: Product.CookingRequirement?,
        @Param("vegan") vegan: Boolean = false,
        @Param("glutenFree") glutenFree: Boolean = false,
        @Param("sugarFree") sugarFree: Boolean = false
    ): List<Product>

    // Сортированные варианты (п. 1.2 — сортировка по названию, калорийности, БЖУ)

    @Query(
        """
        SELECT DISTINCT p FROM Product p
        WHERE (:name IS NULL OR LOWER(p.name) LIKE %:name%)
          AND (:category IS NULL OR p.category = :category)
          AND (:cookingRequirement IS NULL OR p.cookingRequirement = :cookingRequirement)
          AND (:vegan = false OR 'VEGAN' IN (SELECT f1 FROM Product p1 JOIN p1.flags f1 WHERE p1 = p))
          AND (:glutenFree = false OR 'GLUTEN_FREE' IN (SELECT f2 FROM Product p2 JOIN p2.flags f2 WHERE p2 = p))
          AND (:sugarFree = false OR 'SUGAR_FREE' IN (SELECT f3 FROM Product p3 JOIN p3.flags f3 WHERE p3 = p))
        ORDER BY p.name ASC
        """
    )
    fun findWithFiltersSortByName(
        @Param("name") name: String?,
        @Param("category") category: Product.Category?,
        @Param("cookingRequirement") cookingRequirement: Product.CookingRequirement?,
        @Param("vegan") vegan: Boolean = false,
        @Param("glutenFree") glutenFree: Boolean = false,
        @Param("sugarFree") sugarFree: Boolean = false
    ): List<Product>

    @Query(
        """
        SELECT DISTINCT p FROM Product p
        WHERE (:name IS NULL OR LOWER(p.name) LIKE %:name%)
          AND (:category IS NULL OR p.category = :category)
          AND (:cookingRequirement IS NULL OR p.cookingRequirement = :cookingRequirement)
          AND (:vegan = false OR 'VEGAN' IN (SELECT f1 FROM Product p1 JOIN p1.flags f1 WHERE p1 = p))
          AND (:glutenFree = false OR 'GLUTEN_FREE' IN (SELECT f2 FROM Product p2 JOIN p2.flags f2 WHERE p2 = p))
          AND (:sugarFree = false OR 'SUGAR_FREE' IN (SELECT f3 FROM Product p3 JOIN p3.flags f3 WHERE p3 = p))
        ORDER BY p.calories ASC
        """
    )
    fun findWithFiltersSortByCalories(
        @Param("name") name: String?,
        @Param("category") category: Product.Category?,
        @Param("cookingRequirement") cookingRequirement: Product.CookingRequirement?,
        @Param("vegan") vegan: Boolean = false,
        @Param("glutenFree") glutenFree: Boolean = false,
        @Param("sugarFree") sugarFree: Boolean = false
    ): List<Product>

    @Query(
        """
        SELECT DISTINCT p FROM Product p
        WHERE (:name IS NULL OR LOWER(p.name) LIKE %:name%)
          AND (:category IS NULL OR p.category = :category)
          AND (:cookingRequirement IS NULL OR p.cookingRequirement = :cookingRequirement)
          AND (:vegan = false OR 'VEGAN' IN (SELECT f1 FROM Product p1 JOIN p1.flags f1 WHERE p1 = p))
          AND (:glutenFree = false OR 'GLUTEN_FREE' IN (SELECT f2 FROM Product p2 JOIN p2.flags f2 WHERE p2 = p))
          AND (:sugarFree = false OR 'SUGAR_FREE' IN (SELECT f3 FROM Product p3 JOIN p3.flags f3 WHERE p3 = p))
        ORDER BY p.proteins ASC
        """
    )
    fun findWithFiltersSortByProteins(
        @Param("name") name: String?,
        @Param("category") category: Product.Category?,
        @Param("cookingRequirement") cookingRequirement: Product.CookingRequirement?,
        @Param("vegan") vegan: Boolean = false,
        @Param("glutenFree") glutenFree: Boolean = false,
        @Param("sugarFree") sugarFree: Boolean = false
    ): List<Product>

    @Query(
        """
        SELECT DISTINCT p FROM Product p
        WHERE (:name IS NULL OR LOWER(p.name) LIKE %:name%)
          AND (:category IS NULL OR p.category = :category)
          AND (:cookingRequirement IS NULL OR p.cookingRequirement = :cookingRequirement)
          AND (:vegan = false OR 'VEGAN' IN (SELECT f1 FROM Product p1 JOIN p1.flags f1 WHERE p1 = p))
          AND (:glutenFree = false OR 'GLUTEN_FREE' IN (SELECT f2 FROM Product p2 JOIN p2.flags f2 WHERE p2 = p))
          AND (:sugarFree = false OR 'SUGAR_FREE' IN (SELECT f3 FROM Product p3 JOIN p3.flags f3 WHERE p3 = p))
        ORDER BY p.fats ASC
        """
    )
    fun findWithFiltersSortByFats(
        @Param("name") name: String?,
        @Param("category") category: Product.Category?,
        @Param("cookingRequirement") cookingRequirement: Product.CookingRequirement?,
        @Param("vegan") vegan: Boolean = false,
        @Param("glutenFree") glutenFree: Boolean = false,
        @Param("sugarFree") sugarFree: Boolean = false
    ): List<Product>

    @Query(
        """
        SELECT DISTINCT p FROM Product p
        WHERE (:name IS NULL OR LOWER(p.name) LIKE %:name%)
          AND (:category IS NULL OR p.category = :category)
          AND (:cookingRequirement IS NULL OR p.cookingRequirement = :cookingRequirement)
          AND (:vegan = false OR 'VEGAN' IN (SELECT f1 FROM Product p1 JOIN p1.flags f1 WHERE p1 = p))
          AND (:glutenFree = false OR 'GLUTEN_FREE' IN (SELECT f2 FROM Product p2 JOIN p2.flags f2 WHERE p2 = p))
          AND (:sugarFree = false OR 'SUGAR_FREE' IN (SELECT f3 FROM Product p3 JOIN p3.flags f3 WHERE p3 = p))
        ORDER BY p.carbohydrates ASC
        """
    )
    fun findWithFiltersSortByCarbohydrates(
        @Param("name") name: String?,
        @Param("category") category: Product.Category?,
        @Param("cookingRequirement") cookingRequirement: Product.CookingRequirement?,
        @Param("vegan") vegan: Boolean = false,
        @Param("glutenFree") glutenFree: Boolean = false,
        @Param("sugarFree") sugarFree: Boolean = false
    ): List<Product>
}
