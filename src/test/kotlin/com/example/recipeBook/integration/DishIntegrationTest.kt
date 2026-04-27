package com.example.recipeBook.integration

import com.example.recipeBook.data.DishCreateDataSet
import com.example.recipeBook.data.DishDeleteDataSet
import com.example.recipeBook.data.DishGetDataSet
import com.example.recipeBook.data.DishSearchDataSet
import com.example.recipeBook.data.DishUpdateDataSet
import com.example.recipeBook.dto.DishIngredientRequest
import com.example.recipeBook.dto.DishRequest
import com.example.recipeBook.dto.ProductRequest
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Duration
import java.util.UUID
import java.util.stream.Stream

class DishIntegrationTest {

    private companion object {
        private const val BASE_URL = "http://localhost:8080"
    }

    private val webTestClient: WebTestClient = WebTestClient
        .bindToServer()
        .baseUrl(BASE_URL)
        .responseTimeout(Duration.ofSeconds(30))
        .build()

    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    class CreateDishSuccessProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = Stream.of(
            arguments("создание блюда с корректными данными", successfulDishCreate(1L))
        )
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(CreateDishSuccessProvider::class)
    @DisplayName("Успешное создание блюда")
    fun `test create dish success`(caseName: String, testCase: DishCreateDataSet) {
        val productId = createTestProduct()
        val request = testCase.request
            .copy(name = "${testCase.request.name}_${UUID.randomUUID().toString().take(8)}")
            .withProductId(productId)

        webTestClient.post()
            .uri("/api/dishes")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.id").exists()
            .jsonPath("$.name").isEqualTo(request.name)
            .jsonPath("$.servingSize").isEqualTo(testCase.expectedResponse!!.servingSize)
    }

    class CreateDishErrorProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = Stream.of(
            arguments("ошибка при коротком названии", dishNameTooShort(1L)),
            arguments("ошибка при сумме БЖУ больше 100", dishMacrosSumOver100(1L)),
            arguments("ошибка при неверной категории", dishInvalidCategory(1L)),
            arguments("ошибка при пустом списке ингредиентов", dishEmptyIngredients())
        )
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(CreateDishErrorProvider::class)
    @DisplayName("Ошибки при создании блюда")
    fun `test create dish errors`(caseName: String, testCase: DishCreateDataSet) {
        val productId = createTestProduct()
        val request = if (testCase.request.ingredients.isEmpty()) {
            testCase.request
        } else {
            testCase.request.withProductId(productId)
        }

        val body = webTestClient.post()
            .uri("/api/dishes")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isEqualTo(testCase.expectedStatusCode)
            .expectBody(String::class.java)
            .returnResult()
            .responseBody ?: ""

        body shouldContain testCase.expectedErrorMessage!!
    }

    class UpdateDishSuccessProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = Stream.of(
            arguments("обновление существующего блюда", successfulDishUpdate(1L))
        )
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(UpdateDishSuccessProvider::class)
    @DisplayName("Успешное обновление блюда")
    fun `test update dish success`(caseName: String, testCase: DishUpdateDataSet) {
        val productId = createTestProduct()
        val dishId = createDish(productId)
        val request = testCase.request
            .copy(name = "${testCase.request.name}_${UUID.randomUUID().toString().take(8)}")
            .withProductId(productId)

        webTestClient.put()
            .uri("/api/dishes/$dishId")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.name").isEqualTo(request.name)
    }

    class UpdateDishErrorProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = Stream.of(
            arguments("ошибка при обновлении несуществующего блюда", dishNotFound(1L)),
            arguments("ошибка при коротком названии", updateDishNameTooShort(1L))
        )
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(UpdateDishErrorProvider::class)
    @DisplayName("Ошибки при обновлении блюда")
    fun `test update dish errors`(caseName: String, testCase: DishUpdateDataSet) {
        val productId = createTestProduct()
        val dishId = 999_999_999L
        val request = testCase.request.withProductId(productId)

        val body = webTestClient.put()
            .uri("/api/dishes/$dishId")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isEqualTo(testCase.expectedStatusCode)
            .expectBody(String::class.java)
            .returnResult()
            .responseBody ?: ""

        body shouldContain testCase.expectedErrorMessage!!
    }

    class GetDishSuccessProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = Stream.of(
            arguments("получение существующего блюда", successfulDishGet())
        )
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(GetDishSuccessProvider::class)
    @DisplayName("Успешное получение блюда")
    fun `test get dish success`(caseName: String, testCase: DishGetDataSet) {
        val productId = createTestProduct()
        val dishId = createDish(productId)

        webTestClient.get()
            .uri("/api/dishes/$dishId")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(dishId)
            .jsonPath("$.name").exists()
    }

    class GetDishErrorProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = Stream.of(
            arguments("ошибка при получении несуществующего блюда", dishNotFoundGet())
        )
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(GetDishErrorProvider::class)
    @DisplayName("Ошибки при получении блюда")
    fun `test get dish errors`(caseName: String, testCase: DishGetDataSet) {
        val dishId = 999_999_999L

        val body = webTestClient.get()
            .uri("/api/dishes/$dishId")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(testCase.expectedStatusCode)
            .expectBody(String::class.java)
            .returnResult()
            .responseBody ?: ""

        body shouldContain testCase.expectedErrorMessage!!
    }

    class SearchDishSuccessProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = Stream.of(
            arguments("поиск всех блюд", searchAllDishes()),
            arguments("поиск по части названия", searchDishesByNamePartial()),
            arguments("поиск по категории", searchDishesByCategory()),
            arguments("поиск по флагу веган", searchDishesByVeganFlag()),
            arguments("сортировка по калориям", sortDishesByCalories())
        )
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(SearchDishSuccessProvider::class)
    @DisplayName("Успешный поиск блюд")
    fun `test search dishes success`(caseName: String, testCase: DishSearchDataSet) {
        createTestDishesForSearch()

        val spec = webTestClient.get()
            .uri(buildSearchUrl(testCase))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBody()

        testCase.expectedProductCount?.let { expected ->
            spec.jsonPath("$.length()").value<Int> { actual ->
                assert(actual >= expected) {
                    "ожидалось минимум $expected, получено $actual"
                }
            }
        }
        testCase.expectedProductNames?.forEach { expectedName ->
            spec.jsonPath("$[?(@.name =~ /.*${Regex.escape(expectedName)}.*/)]").exists()
        }
    }

    class SearchDishErrorProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = Stream.of(
            arguments("ошибка при неверной категории", searchDishesInvalidCategory())
        )
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(SearchDishErrorProvider::class)
    @DisplayName("Ошибки при поиске блюд")
    fun `test search dishes errors`(caseName: String, testCase: DishSearchDataSet) {
        val body = webTestClient.get()
            .uri(buildSearchUrl(testCase))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(testCase.expectedStatusCode)
            .expectBody(String::class.java)
            .returnResult()
            .responseBody ?: ""

        body shouldContain testCase.expectedErrorMessage!!
    }

    class DeleteDishSuccessProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = Stream.of(
            arguments("успешное удаление блюда", successfulDishDelete())
        )
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(DeleteDishSuccessProvider::class)
    @DisplayName("Успешное удаление блюда")
    fun `test delete dish success`(caseName: String, testCase: DishDeleteDataSet) {
        val productId = createTestProduct()
        val dishId = createDish(productId)

        webTestClient.delete()
            .uri("/api/dishes/$dishId")
            .exchange()
            .expectStatus().isNoContent

        webTestClient.get()
            .uri("/api/dishes/$dishId")
            .exchange()
            .expectStatus().isNotFound
    }

    class DeleteDishErrorProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = Stream.of(
            arguments("ошибка при удалении несуществующего блюда", deleteDishNotFound())
        )
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(DeleteDishErrorProvider::class)
    @DisplayName("Ошибки при удалении блюда")
    fun `test delete dish errors`(caseName: String, testCase: DishDeleteDataSet) {
        val dishId = 999_999_999L

        val body = webTestClient.delete()
            .uri("/api/dishes/$dishId")
            .exchange()
            .expectStatus().isEqualTo(testCase.expectedStatusCode)
            .expectBody(String::class.java)
            .returnResult()
            .responseBody ?: ""

        body shouldContain testCase.expectedErrorMessage!!
    }

    private fun buildSearchUrl(testCase: DishSearchDataSet): String {
        val sb = StringBuilder("/api/dishes?")
        testCase.name?.let { sb.append("name=$it&") }
        testCase.category?.let { sb.append("category=$it&") }
        if (testCase.vegan) sb.append("vegan=true&")
        if (testCase.glutenFree) sb.append("glutenFree=true&")
        if (testCase.sugarFree) sb.append("sugarFree=true&")
        testCase.sortBy?.let { sb.append("sortBy=$it&") }
        return sb.toString().trimEnd('&', '?')
    }

    private fun DishRequest.withProductId(productId: Long): DishRequest =
        copy(ingredients = ingredients.map { it.copy(productId = productId) })

    private fun createTestProduct(): Long {
        val request = ProductRequest(
            name = "TestProduct_${UUID.randomUUID().toString().take(8)}",
            photos = emptyList(),
            calories = 100.0,
            proteins = 10.0,
            fats = 5.0,
            carbohydrates = 15.0,
            composition = "TestComposition",
            category = "Овощи",
            cookingRequirement = "Готовый к употреблению",
            flags = listOf("Веган", "Без глютена")
        )

        val body = webTestClient.post()
            .uri("/api/products")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated
            .expectBody(String::class.java)
            .returnResult()
            .responseBody!!

        return objectMapper.readTree(body).get("id").asLong()
    }

    private fun createDish(productId: Long): Long {
        val request = DishRequest(
            name = "TestDish_${UUID.randomUUID().toString().take(8)}",
            photos = emptyList(),
            calories = null,
            proteins = null,
            fats = null,
            carbohydrates = null,
            ingredients = listOf(DishIngredientRequest(productId, 100.0)),
            servingSize = 200.0,
            category = "Первое",
            flags = emptyList()
        )

        val body = webTestClient.post()
            .uri("/api/dishes")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated
            .expectBody(String::class.java)
            .returnResult()
            .responseBody!!

        return objectMapper.readTree(body).get("id").asLong()
    }

    private fun createTestDishesForSearch() {
        val productId = createTestProduct()
        val suffix = UUID.randomUUID().toString().take(8)
        val dishes = listOf(
            DishRequest("Dish1_$suffix", emptyList(), null, null, null, null, listOf(DishIngredientRequest(productId, 100.0)), 200.0, "Первое", listOf("Веган")),
            DishRequest("Dish2_$suffix", emptyList(), null, null, null, null, listOf(DishIngredientRequest(productId, 80.0)), 200.0, "Первое", listOf("Веган")),
            DishRequest("Dish3_$suffix", emptyList(), null, null, null, null, listOf(DishIngredientRequest(productId, 50.0)), 200.0, "Первое", listOf("Веган")),
            DishRequest("Dish4_$suffix", emptyList(), null, null, null, null, listOf(DishIngredientRequest(productId, 120.0)), 200.0, "Второе", listOf("Веган")),
            DishRequest("Dish5_$suffix", emptyList(), null, null, null, null, listOf(DishIngredientRequest(productId, 150.0)), 200.0, "Салат", emptyList())
        )

        dishes.forEach { request ->
            webTestClient.post()
                .uri("/api/dishes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated
        }
    }
}
