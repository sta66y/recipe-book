package com.example.recipeBook.service

import com.example.recipeBook.dto.ProductRequest
import com.example.recipeBook.dto.ProductResponse
import com.example.recipeBook.entity.Product
import com.example.recipeBook.repository.DishRepository
import com.example.recipeBook.repository.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ProductService(
    private val productRepository: ProductRepository,
    private val dishRepository: DishRepository
) {

    // ==================== CRUD ====================

    fun createProduct(request: ProductRequest): ProductResponse {
        validateRequest(request)

        val product = Product(
            name = request.name,
            photos = request.photos.toMutableList(),
            calories = request.calories,
            proteins = request.proteins,
            fats = request.fats,
            carbohydrates = request.carbohydrates,
            composition = request.composition,
            category = resolveCategory(request.category),
            cookingRequirement = resolveCookingRequirement(request.cookingRequirement),
            flags = resolveFlags(request.flags)
        )

        // Валидация сущности
        product.validate()

        val saved = productRepository.save(product)
        return ProductResponse.fromEntity(saved)
    }

    fun updateProduct(id: Long, request: ProductRequest): ProductResponse {
        val product = productRepository.findById(id)
            .orElseThrow { NoSuchElementException("Продукт с ID $id не найден") }

        validateRequest(request)

        product.name = request.name
        product.photos = request.photos.toMutableList()
        product.calories = request.calories
        product.proteins = request.proteins
        product.fats = request.fats
        product.carbohydrates = request.carbohydrates
        product.composition = request.composition
        product.category = resolveCategory(request.category)
        product.cookingRequirement = resolveCookingRequirement(request.cookingRequirement)
        product.flags = resolveFlags(request.flags)

        // Валидация сущности
        product.validate()

        val saved = productRepository.save(product)
        return ProductResponse.fromEntity(saved)
    }

    @Transactional(readOnly = true)
    fun getProduct(id: Long): ProductResponse {
        val product = productRepository.findById(id)
            .orElseThrow { NoSuchElementException("Продукт с ID $id не найден") }
        return ProductResponse.fromEntity(product)
    }

    @Transactional(readOnly = true)
    fun getAllProducts(): List<ProductResponse> {
        return productRepository.findAll().map { ProductResponse.fromEntity(it) }
    }

    @Transactional(readOnly = true)
    fun searchProducts(
        name: String?,
        category: String?,
        cookingRequirement: String?,
        vegan: Boolean?,
        glutenFree: Boolean?,
        sugarFree: Boolean?,
        sortBy: String?
    ): List<ProductResponse> {
        val lowercaseName = name?.lowercase()
        val categoryEnum = category?.let {
            Product.Category.fromDisplayName(it)
                ?: throw IllegalArgumentException("Неверная категория продукта: $it")
        }
        val cookingReqEnum = cookingRequirement?.let {
            Product.CookingRequirement.fromDisplayName(it)
                ?: throw IllegalArgumentException("Неверное требование готовки: $it")
        }

        val products = when (sortBy?.lowercase()) {
            "name" -> productRepository.findWithFiltersSortByName(
                lowercaseName, categoryEnum, cookingReqEnum,
                vegan ?: false, glutenFree ?: false, sugarFree ?: false
            )
            "calories" -> productRepository.findWithFiltersSortByCalories(
                lowercaseName, categoryEnum, cookingReqEnum,
                vegan ?: false, glutenFree ?: false, sugarFree ?: false
            )
            "proteins" -> productRepository.findWithFiltersSortByProteins(
                lowercaseName, categoryEnum, cookingReqEnum,
                vegan ?: false, glutenFree ?: false, sugarFree ?: false
            )
            "fats" -> productRepository.findWithFiltersSortByFats(
                lowercaseName, categoryEnum, cookingReqEnum,
                vegan ?: false, glutenFree ?: false, sugarFree ?: false
            )
            "carbohydrates" -> productRepository.findWithFiltersSortByCarbohydrates(
                lowercaseName, categoryEnum, cookingReqEnum,
                vegan ?: false, glutenFree ?: false, sugarFree ?: false
            )
            else -> productRepository.findWithFilters(
                lowercaseName, categoryEnum, cookingReqEnum,
                vegan ?: false, glutenFree ?: false, sugarFree ?: false
            )
        }

        return products.map { ProductResponse.fromEntity(it) }
    }

    // ==================== Удаление (п. 1.5) ====================

    /**
     * Удаляет продукт. Если продукт используется в блюдах — выбрасывает исключение
     * с перечнем блюд, в которых он используется.
     */
    fun deleteProduct(id: Long) {
        if (!productRepository.existsById(id)) {
            throw NoSuchElementException("Продукт с ID $id не найден")
        }

        val dishesUsingProduct = dishRepository.findByProductInIngredients(id)
        if (dishesUsingProduct.isNotEmpty()) {
            val dishNames = dishesUsingProduct.joinToString(", ") { "\"${it.name}\"" }
            throw IllegalStateException(
                "Невозможно удалить продукт: он используется в блюдах: $dishNames"
            )
        }

        productRepository.deleteById(id)
    }

    /**
     * Проверяет, можно ли удалить продукт (для UI — показать предупреждение).
     * Возвращает (canDelete, список названий блюд где используется).
     */
    @Transactional(readOnly = true)
    fun canDeleteProduct(id: Long): Pair<Boolean, List<String>> {
        if (!productRepository.existsById(id)) {
            throw NoSuchElementException("Продукт с ID $id не найден")
        }

        val dishesUsingProduct = dishRepository.findByProductInIngredients(id)
        if (dishesUsingProduct.isNotEmpty()) {
            return Pair(false, dishesUsingProduct.map { it.name })
        }
        return Pair(true, emptyList())
    }

    // ==================== Приватные методы ====================

    /**
     * Валидация входного запроса.
     */
    private fun validateRequest(request: ProductRequest) {
        require(request.name.length >= 2) {
            "Название продукта должно содержать минимум 2 символа"
        }
        require(request.photos.size <= 5) {
            "Максимальное количество фотографий — 5"
        }
        require(request.calories >= 0) {
            "Калорийность не может быть отрицательной"
        }
        require(request.proteins >= 0) {
            "Белки не могут быть отрицательными"
        }
        require(request.fats >= 0) {
            "Жиры не могут быть отрицательными"
        }
        require(request.carbohydrates >= 0) {
            "Углеводы не могут быть отрицательными"
        }
        require(request.proteins <= 100) {
            "Белки на 100г не могут превышать 100"
        }
        require(request.fats <= 100) {
            "Жиры на 100г не могут превышать 100"
        }
        require(request.carbohydrates <= 100) {
            "Углеводы на 100г не могут превышать 100"
        }
        require(request.proteins + request.fats + request.carbohydrates <= 100) {
            "Сумма БЖУ на 100 грамм не может превышать 100"
        }
    }

    private fun resolveCategory(categoryName: String): Product.Category {
        return Product.Category.fromDisplayName(categoryName)
            ?: throw IllegalArgumentException("Неверная категория продукта: $categoryName")
    }

    private fun resolveCookingRequirement(requirementName: String): Product.CookingRequirement {
        return Product.CookingRequirement.fromDisplayName(requirementName)
            ?: throw IllegalArgumentException("Неверное требование готовки: $requirementName")
    }

    private fun resolveFlags(flagNames: List<String>): MutableSet<Product.ProductFlag> {
        return flagNames.map { flagName ->
            Product.ProductFlag.fromDisplayName(flagName)
                ?: throw IllegalArgumentException("Неизвестный флаг продукта: $flagName")
        }.toMutableSet()
    }
}
