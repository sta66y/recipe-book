package com.example.recipeBook.util

import com.example.recipeBook.entity.Dish
import com.example.recipeBook.entity.DishIngredient
import com.example.recipeBook.entity.Product
import java.time.LocalDateTime

fun buildDish(
    id: Long = 1,
    name: String = "Kolbasa",
    photos: MutableList<String> = mutableListOf(),
    calories: Double = 100.0,
    proteins: Double = 100.0,
    fats: Double = 100.0,
    carbohydrates: Double = 100.0,
    servingSize: Double = 100.0,
    category: Dish.Category = Dish.Category.DESSERT,
    flags: MutableSet<Dish.DishFlag> = mutableSetOf(),
    ingredients: MutableList<DishIngredient> = mutableListOf(),
    createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime? = null,
) = Dish(
    id = id,
    name = name,
    photos = photos,
    calories = calories,
    proteins = proteins,
    fats = fats,
    carbohydrates = carbohydrates,
    servingSize = servingSize,
    category = category,
    flags = flags,
    ingredients = ingredients,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun buildProduct(
    id: Long = 1,
    name: String = "Kolbasa",
    photos: MutableList<String> = mutableListOf(),
    calories: Double = 100.0,
    proteins: Double = 100.0,
    fats: Double = 100.0,
    carbohydrates: Double = 100.0,
    composition: String? = null,
    category: Product.Category = Product.Category.MEAT,
    cookingRequirement: Product.CookingRequirement = Product.CookingRequirement.READY_TO_EAT,
    flags: MutableSet<Product.ProductFlag> = mutableSetOf(),
    createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime? = null,
) = Product(
    id = id,
    name = name,
    photos = photos,
    calories = calories,
    proteins = proteins,
    fats = fats,
    carbohydrates = carbohydrates,
    composition = composition,
    category = category,
    cookingRequirement = cookingRequirement,
    flags = flags,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun buildDishIngredient(
    id: Long = 1,
    dish: Dish = buildDish(),
    product: Product = buildProduct(),
    quantity: Double = 100.0,
) = DishIngredient(
    id = id,
    dish = dish,
    product = product,
    quantity = quantity,
)
