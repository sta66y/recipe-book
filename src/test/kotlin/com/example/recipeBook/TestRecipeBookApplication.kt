package com.example.recipeBook

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
	fromApplication<RecipeBookApplication>().with(TestcontainersConfiguration::class).run(*args)
}
