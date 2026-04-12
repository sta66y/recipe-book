package com.example.recipeBook.service

import com.example.recipeBook.dto.DishIngredientRequest
import com.example.recipeBook.dto.DishRequest
import com.example.recipeBook.dto.DishResponse
import com.example.recipeBook.entity.Dish
import com.example.recipeBook.entity.DishIngredient
import com.example.recipeBook.entity.Product
import com.example.recipeBook.repository.DishRepository
import com.example.recipeBook.repository.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class DishService(
    private val dishRepository: DishRepository,
    private val productRepository: ProductRepository
) {

    // ==================== CRUD ====================

    fun createDish(request: DishRequest): DishResponse {
        // Валидация входных данных
        validateRequest(request)

        // Определяем название и категорию (макросы + явная категория)
        val (finalName, resolvedCategory) = resolveNameAndCategory(request.name, request.category)

        // Создаём блюдо
        val dish = Dish(
            name = finalName,
            photos = request.photos.toMutableList(),
            servingSize = request.servingSize,
            category = resolvedCategory,
            flags = mutableSetOf(),
            ingredients = mutableListOf()
        )

        // Добавляем ингредиенты
        addIngredientsToDish(dish, request.ingredients)

        // Авторасчёт КБЖУ
        dish.recalculateNutrition()

        // Ручная корректировка КБЖУ (если пользователь указал)
        applyNutritionOverrides(dish, request)

        // Флаги: определяем доступные и применяем запрошенные
        applyFlags(dish, request.flags)

        // Валидация сущности перед сохранением
        dish.validate()

        val saved = dishRepository.save(dish)
        return DishResponse.fromEntity(saved)
    }

    fun updateDish(id: Long, request: DishRequest): DishResponse {
        validateRequest(request)

        val dish = dishRepository.findById(id)
            .orElseThrow { NoSuchElementException("Блюдо с ID $id не найдено") }

        val (finalName, resolvedCategory) = resolveNameAndCategory(request.name, request.category)

        dish.name = finalName
        dish.photos = request.photos.toMutableList()
        dish.servingSize = request.servingSize
        dish.category = resolvedCategory

        // Пересобираем ингредиенты
        dish.ingredients.clear()
        addIngredientsToDish(dish, request.ingredients)

        // Авторасчёт КБЖУ
        dish.recalculateNutrition()

        // Ручная корректировка КБЖУ
        applyNutritionOverrides(dish, request)

        // Флаги
        applyFlags(dish, request.flags)

        // Валидация сущности перед сохранением
        dish.validate()

        val saved = dishRepository.save(dish)
        return DishResponse.fromEntity(saved)
    }

    @Transactional(readOnly = true)
    fun getDish(id: Long): DishResponse {
        val dish = dishRepository.findById(id)
            .orElseThrow { NoSuchElementException("Блюдо с ID $id не найдено") }
        return DishResponse.fromEntity(dish)
    }

    @Transactional(readOnly = true)
    fun getAllDishes(): List<DishResponse> {
        return dishRepository.findAll().map { DishResponse.fromEntity(it) }
    }

    @Transactional(readOnly = true)
    fun searchDishes(
        name: String?,
        category: String?,
        vegan: Boolean?,
        glutenFree: Boolean?,
        sugarFree: Boolean?
    ): List<DishResponse> {
        val categoryEnum = category?.let {
            Dish.Category.fromDisplayName(it)
                ?: throw IllegalArgumentException("Неверная категория блюда: $it")
        }

        val dishes = dishRepository.findWithFilters(
            name = name?.lowercase(),
            category = categoryEnum,
            vegan = vegan ?: false,
            glutenFree = glutenFree ?: false,
            sugarFree = sugarFree ?: false
        )
        return dishes.map { DishResponse.fromEntity(it) }
    }

    fun deleteDish(id: Long) {
        val dish = dishRepository.findById(id)
            .orElseThrow { NoSuchElementException("Блюдо с ID $id не найдено") }
        dishRepository.delete(dish)
    }

    // ==================== Проверка удаления продукта (п. 1.5) ====================

    /**
     * Проверяет, можно ли удалить продукт.
     * Возвращает (true, emptyList) если можно, или (false, список названий блюд) если нельзя.
     */
    @Transactional(readOnly = true)
    fun canDeleteProduct(productId: Long): Pair<Boolean, List<String>> {
        if (!dishRepository.existsByProductInIngredients(productId)) {
            return Pair(true, emptyList())
        }
        val dishNames = dishRepository.findByProductInIngredients(productId).map { it.name }
        return Pair(false, dishNames)
    }

    // ==================== Доступные флаги для фронтенда ====================

    /**
     * Возвращает набор доступных флагов для заданного состава (для UI).
     */
    @Transactional(readOnly = true)
    fun getAvailableFlagsForIngredients(ingredients: List<DishIngredientRequest>): Set<String> {
        if (ingredients.isEmpty()) {
            return emptySet()
        }

        val products = ingredients.map { req ->
            productRepository.findById(req.productId)
                .orElseThrow { NoSuchElementException("Продукт с ID ${req.productId} не найден") }
        }

        val available = mutableSetOf<String>()

        if (products.all { it.hasFlag(Product.ProductFlag.VEGAN) }) {
            available.add(Dish.DishFlag.VEGAN.displayName)
        }
        if (products.all { it.hasFlag(Product.ProductFlag.GLUTEN_FREE) }) {
            available.add(Dish.DishFlag.GLUTEN_FREE.displayName)
        }
        if (products.all { it.hasFlag(Product.ProductFlag.SUGAR_FREE) }) {
            available.add(Dish.DishFlag.SUGAR_FREE.displayName)
        }

        return available
    }

    // ==================== Приватные методы ====================

    /**
     * Валидация входного запроса.
     */
    private fun validateRequest(request: DishRequest) {
        require(request.name.length >= 2) {
            "Название блюда должно содержать минимум 2 символа"
        }
        require(request.photos.size <= 5) {
            "Максимальное количество фотографий — 5"
        }
        require(request.servingSize > 0) {
            "Размер порции должен быть больше 0"
        }
        require(request.ingredients.isNotEmpty()) {
            "Блюдо должно содержать хотя бы 1 ингредиент"
        }
        request.ingredients.forEach { ingredient ->
            require(ingredient.quantity > 0) {
                "Количество продукта (ID ${ingredient.productId}) должно быть больше 0"
            }
        }
        
        // Валидация суммы БЖУ на 100г порции (п. 2.1, 2.7)
        // Если значения указаны, проверяем сумму БЖУ на 100г
        val calories = request.calories
        val proteins = request.proteins
        val fats = request.fats
        val carbs = request.carbohydrates
        
        // Проверяем только если все значения указаны (авторасчёт или ручное заполнение)
        if (calories != null && proteins != null && fats != null && carbs != null) {
            val sumPer100 = (proteins + fats + carbs) * 100.0 / request.servingSize
            require(sumPer100 <= 100) {
                "Сумма БЖУ на 100 грамм порции не может превышать 100 (текущая: %.2f)".format(sumPer100)
            }
        }
    }

    /**
     * Определяет итоговое название и категорию блюда.
     *
     * Логика (п. 2.3):
     * - Если категория указана явно в поле формы — используется она, макросы из названия удаляются
     * - Если категория НЕ указана — берётся из первого макроса в названии, макросы удаляются
     * - Если ни то ни другое — ошибка
     */
    private fun resolveNameAndCategory(rawName: String, categoryInput: String?): Pair<String, Dish.Category> {
        val (cleanName, macroCategory) = Dish.parseMacro(rawName)

        // Если категория указана явно — она приоритетнее
        if (!categoryInput.isNullOrBlank()) {
            val explicitCategory = Dish.Category.fromDisplayName(categoryInput)
                ?: throw IllegalArgumentException("Неверная категория блюда: $categoryInput")
            // Название очищаем от макросов в любом случае
            return cleanName to explicitCategory
        }

        // Если категория из макроса
        if (macroCategory != null) {
            return cleanName to macroCategory
        }

        throw IllegalArgumentException("Категория не указана и макросы в названии не найдены")
    }

    /**
     * Добавляет ингредиенты к блюду, загружая продукты из БД.
     */
    private fun addIngredientsToDish(dish: Dish, ingredientRequests: List<DishIngredientRequest>) {
        ingredientRequests.forEach { req ->
            val product = productRepository.findById(req.productId)
                .orElseThrow { NoSuchElementException("Продукт с ID ${req.productId} не найден") }
            val ingredient = DishIngredient(
                dish = dish,
                product = product,
                quantity = req.quantity
            )
            ingredient.validate()
            dish.ingredients.add(ingredient)
        }
    }

    /**
     * Применяет ручную корректировку КБЖУ, если пользователь указал значения.
     */
    private fun applyNutritionOverrides(dish: Dish, request: DishRequest) {
        val overrideCalories = request.calories ?: dish.calories
        val overrideProteins = request.proteins ?: dish.proteins
        val overrideFats = request.fats ?: dish.fats
        val overrideCarbs = request.carbohydrates ?: dish.carbohydrates

        // Применяем только если хотя бы одно значение было указано пользователем
        if (request.calories != null || request.proteins != null ||
            request.fats != null || request.carbohydrates != null
        ) {
            dish.adjustNutrition(overrideCalories, overrideProteins, overrideFats, overrideCarbs)
        }
    }

    /**
     * Определяет доступные флаги по составу и применяет запрошенные пользователем.
     *
     * Логика (п. 2.4):
     * - Определяем доступные флаги (все продукты имеют соответствующий флаг)
     * - Из запрошенных пользователем оставляем только доступные
     * - Если запрошенный флаг недоступен — он игнорируется (не применяется)
     * - Автоматически снимаются флаги, которые более не доступны по составу
     */
    private fun applyFlags(dish: Dish, requestedFlags: List<String>) {
        val availableFlags = dish.getAvailableFlags()

        // Сначала снимаем все флаги, которые более не доступны (автоснятие)
        dish.flags.retainAll(availableFlags)

        if (requestedFlags.isEmpty()) {
            return
        }

        val resolvedFlags = mutableSetOf<Dish.DishFlag>()

        requestedFlags.forEach { flagName ->
            val flag = Dish.DishFlag.fromDisplayName(flagName)
                ?: throw IllegalArgumentException("Неизвестный флаг: '$flagName'")

            // Если флаг доступен — применяем, иначе игнорируем
            if (flag in availableFlags) {
                resolvedFlags.add(flag)
            }
        }

        dish.flags = resolvedFlags
    }
}
