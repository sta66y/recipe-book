package com.example.recipeBook.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    data class ErrorResponse(
        val error: String,
        val message: String,
        val details: List<String> = emptyList()
    )

    /**
     * 404 — сущность не найдена.
     */
    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(ex: NoSuchElementException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse(
                error = "Не найдено",
                message = ex.message ?: "Ресурс не найден"
            )
        )
    }

    /**
     * 400 — ошибка валидации (require, IllegalArgumentException).
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                error = "Ошибка валидации",
                message = ex.message ?: "Некорректный запрос"
            )
        )
    }

    /**
     * 409 — конфликт (например, удаление продукта, используемого в блюдах).
     */
    @ExceptionHandler(IllegalStateException::class)
    fun handleConflict(ex: IllegalStateException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ErrorResponse(
                error = "Конфликт",
                message = ex.message ?: "Операция невозможна"
            )
        )
    }

    /**
     * 400 — ошибки валидации @Valid (Bean Validation).
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val details = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                error = "Ошибка валидации",
                message = "Некорректные данные в запросе",
                details = details
            )
        )
    }
}