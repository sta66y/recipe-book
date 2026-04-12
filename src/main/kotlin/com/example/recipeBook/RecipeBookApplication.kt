package com.example.recipeBook

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
class RecipeBookApplication

fun main(args: Array<String>) {
	runApplication<RecipeBookApplication>(*args)
}
