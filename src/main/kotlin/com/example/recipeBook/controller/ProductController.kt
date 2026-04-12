package com.example.recipeBook.controller

import com.example.recipeBook.dto.ProductRequest
import com.example.recipeBook.dto.ProductResponse
import com.example.recipeBook.service.ProductService
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
@RequestMapping("/api/products")
class ProductController(
    private val productService: ProductService
) {

    // ==================== CRUD ====================

    @PostMapping
    fun createProduct(@Valid @RequestBody request: ProductRequest): ResponseEntity<ProductResponse> {
        val response = productService.createProduct(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PutMapping("/{id}")
    fun updateProduct(
        @PathVariable id: Long,
        @Valid @RequestBody request: ProductRequest
    ): ResponseEntity<ProductResponse> {
        val response = productService.updateProduct(id, request)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}")
    fun getProduct(@PathVariable id: Long): ResponseEntity<ProductResponse> {
        val response = productService.getProduct(id)
        return ResponseEntity.ok(response)
    }

    /**
     * Поиск, фильтрация и сортировка продуктов (п. 1.2).
     * Без параметров — возвращает все продукты.
     *
     * @param sortBy — критерий сортировки: name, calories, proteins, fats, carbohydrates
     */
    @GetMapping
    fun searchProducts(
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) cookingRequirement: String?,
        @RequestParam(required = false, defaultValue = "false") vegan: Boolean,
        @RequestParam(required = false, defaultValue = "false") glutenFree: Boolean,
        @RequestParam(required = false, defaultValue = "false") sugarFree: Boolean,
        @RequestParam(required = false) sortBy: String?
    ): ResponseEntity<List<ProductResponse>> {
        val products = productService.searchProducts(
            name, category, cookingRequirement,
            vegan, glutenFree, sugarFree, sortBy
        )
        return ResponseEntity.ok(products)
    }

    // ==================== Удаление (п. 1.5) ====================

    /**
     * Удаляет продукт.
     * Если продукт используется в блюдах — возвращает 409 Conflict с перечнем блюд.
     */
    @DeleteMapping("/{id}")
    fun deleteProduct(@PathVariable id: Long): ResponseEntity<Any> {
        productService.deleteProduct(id)
        return ResponseEntity.noContent().build()
    }

    /**
     * Проверяет, можно ли удалить продукт (для UI — предупреждение перед удалением).
     */
    @GetMapping("/{id}/can-delete")
    fun canDeleteProduct(@PathVariable id: Long): ResponseEntity<CanDeleteResponse> {
        val (canDelete, dishNames) = productService.canDeleteProduct(id)
        return ResponseEntity.ok(CanDeleteResponse(canDelete, dishNames))
    }

    // ==================== Response DTOs ====================

    data class CanDeleteResponse(
        val canDelete: Boolean,
        val dishes: List<String>
    )
}