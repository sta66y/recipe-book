package com.example.recipeBook.controller

import com.example.recipeBook.dto.DishIngredientRequest
import com.example.recipeBook.dto.DishRequest
import com.example.recipeBook.dto.DishResponse
import com.example.recipeBook.service.DishService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/dishes")
class DishController(
    private val dishService: DishService
) {

    // ==================== CRUD ====================

    @PostMapping
    fun createDish(@Valid @RequestBody request: DishRequest): ResponseEntity<DishResponse> {
        val response = dishService.createDish(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PutMapping("/{id}")
    fun updateDish(
        @PathVariable id: Long,
        @Valid @RequestBody request: DishRequest
    ): ResponseEntity<DishResponse> {
        val response = dishService.updateDish(id, request)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}")
    fun getDish(@PathVariable id: Long): ResponseEntity<DishResponse> {
        val response = dishService.getDish(id)
        return ResponseEntity.ok(response)
    }

    /**
     * Поиск и фильтрация блюд (п. 2.5).
     * Без параметров — возвращает все блюда.
     */
    @GetMapping
    fun searchDishes(
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false, defaultValue = "false") vegan: Boolean,
        @RequestParam(required = false, defaultValue = "false") glutenFree: Boolean,
        @RequestParam(required = false, defaultValue = "false") sugarFree: Boolean
    ): ResponseEntity<List<DishResponse>> {
        val dishes = dishService.searchDishes(name, category, vegan, glutenFree, sugarFree)
        return ResponseEntity.ok(dishes)
    }

    @DeleteMapping("/{id}")
    fun deleteDish(@PathVariable id: Long): ResponseEntity<Unit> {
        dishService.deleteDish(id)
        return ResponseEntity.noContent().build()
    }

    // ==================== Вспомогательные эндпоинты ====================

    /**
     * Возвращает доступные флаги для заданного состава (п. 2.4).
     * Используется фронтендом для динамического отображения чекбоксов.
     */
    @PostMapping("/available-flags")
    fun getAvailableFlags(
        @Valid @RequestBody ingredients: List<DishIngredientRequest>
    ): ResponseEntity<Set<String>> {
        val availableFlags = dishService.getAvailableFlagsForIngredients(ingredients)
        return ResponseEntity.ok(availableFlags)
    }
}