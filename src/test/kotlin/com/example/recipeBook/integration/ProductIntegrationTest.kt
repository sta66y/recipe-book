package com.example.recipeBook.integration

import com.example.recipeBook.data.ProductCreateDataSet
import com.example.recipeBook.data.ProductDeleteDataSet
import com.example.recipeBook.data.ProductGetDataSet
import com.example.recipeBook.data.ProductSearchDataSet
import com.example.recipeBook.data.ProductUpdateDataSet
import com.example.recipeBook.dto.DishIngredientRequest
import com.example.recipeBook.dto.DishRequest
import com.example.recipeBook.dto.ProductRequest
import com.example.recipeBook.dto.ProductResponse
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

class ProductIntegrationTest {

    private companion object {
        private const val BASE_URL = "http://localhost:8080"
    }

    private val webTestClient: WebTestClient = WebTestClient
        .bindToServer()
        .baseUrl(BASE_URL)
        .responseTimeout(Duration.ofSeconds(30))
        .build()

    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    class CreateProductSuccessProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = Stream.of(
            arguments("создание продукта с корректными данными", successfulCreate())
        )
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(CreateProductSuccessProvider::class)
    @DisplayName("Успешное создание продукта")
    fun `test create product success`(caseName: String, testCase: ProductCreateDataSet) {
        val request = testCase.request.withUniqueName()

        webTestClient.post()
            .uri("/api/products")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.id").exists()
            .jsonPath("$.name").isEqualTo(request.name)
            .jsonPath("$.calories").isEqualTo(testCase.expectedResponse!!.calories)
    }

    class CreateProductErrorProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = Stream.of(
            arguments("ошибка при коротком названии", nameTooShort()),
            arguments("ошибка при сумме БЖУ больше 100", macrosSumOver100()),
            arguments("ошибка при неверной категории", invalidCategory())
        )
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(CreateProductErrorProvider::class)
    @DisplayName("Ошибки при создании продукта")
    fun `test create product errors`(caseName: String, testCase: ProductCreateDataSet) {
        val body = webTestClient.post()
            .uri("/api/products")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(testCase.request)
            .exchange()
            .expectStatus().isEqualTo(testCase.expectedStatusCode)
            .expectBody(String::class.java)
            .returnResult()
            .responseBody ?: ""

        body shouldContain testCase.expectedErrorMessage!!
    }

    class UpdateProductSuccessProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = Stream.of(
            arguments("обновление существующего продукта", successfulUpdate())
        )
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(UpdateProductSuccessProvider::class)
    @DisplayName("Успешное обновление продукта")
    fun `test update product success`(caseName: String, testCase: ProductUpdateDataSet) {
        val productId = createProduct()
        val request = testCase.request.withUniqueName()

        webTestClient.put()
            .uri("/api/products/$productId")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.name").isEqualTo(request.name)
    }

    class UpdateProductErrorProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = Stream.of(
            arguments("ошибка при обновлении несуществующего продукта", productNotFound()),
            arguments("ошибка при коротком названии", updateNameTooShort())
        )
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(UpdateProductErrorProvider::class)
    @DisplayName("Ошибки при обновлении продукта")
    fun `test update product errors`(caseName: String, testCase: ProductUpdateDataSet) {
        val productId = 999_999_999L

        val body = webTestClient.put()
            .uri("/api/products/$productId")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(testCase.request)
            .exchange()
            .expectStatus().isEqualTo(testCase.expectedStatusCode)
            .expectBody(String::class.java)
            .returnResult()
            .responseBody ?: ""

        body shouldContain testCase.expectedErrorMessage!!
    }

    class GetProductSuccessProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = Stream.of(
            arguments("получение существующего продукта", successfulGet())
        )
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(GetProductSuccessProvider::class)
    @DisplayName("Успешное получение продукта")
    fun `test get product success`(caseName: String, testCase: ProductGetDataSet) {
        val productId = createProduct(testCase.expectedResponse!!)

        webTestClient.get()
            .uri("/api/products/$productId")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(productId)
    }

    class GetProductErrorProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = Stream.of(
            arguments("ошибка при получении несуществующего продукта", productNotFoundGet())
        )
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(GetProductErrorProvider::class)
    @DisplayName("Ошибки при получении продукта")
    fun `test get product errors`(caseName: String, testCase: ProductGetDataSet) {
        val productId = 999_999_999L

        val body = webTestClient.get()
            .uri("/api/products/$productId")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(testCase.expectedStatusCode)
            .expectBody(String::class.java)
            .returnResult()
            .responseBody ?: ""

        body shouldContain testCase.expectedErrorMessage!!
    }

    class SearchProductSuccessProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = Stream.of(
            arguments("поиск всех продуктов", searchAllProducts()),
            arguments("поиск по части названия", searchByNamePartial()),
            arguments("поиск по категории", searchByCategory()),
            arguments("поиск по флагу", searchByVeganFlag()),
            arguments("сортировка по калориям", sortByCalories())
        )
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(SearchProductSuccessProvider::class)
    @DisplayName("Успешный поиск продуктов")
    fun `test search products success`(caseName: String, testCase: ProductSearchDataSet) {
        createTestProductsForSearch()

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

    class SearchProductErrorProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = Stream.of(
            arguments("ошибка при неверной категории", searchInvalidCategory())
        )
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(SearchProductErrorProvider::class)
    @DisplayName("Ошибки при поиске продуктов")
    fun `test search products errors`(caseName: String, testCase: ProductSearchDataSet) {
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

    class DeleteProductSuccessProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = Stream.of(
            arguments("успешное удаление продукта", successfulDelete())
        )
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(DeleteProductSuccessProvider::class)
    @DisplayName("Успешное удаление продукта")
    fun `test delete product success`(caseName: String, testCase: ProductDeleteDataSet) {
        val productId = createProduct()

        webTestClient.delete()
            .uri("/api/products/$productId")
            .exchange()
            .expectStatus().isNoContent

        webTestClient.get()
            .uri("/api/products/$productId")
            .exchange()
            .expectStatus().isNotFound
    }

    class DeleteProductErrorProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = Stream.of(
            arguments("ошибка при удалении несуществующего продукта", deleteProductNotFound()),
            arguments("ошибка при удалении продукта используемого в блюде", deleteProductUsedInDish())
        )
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(DeleteProductErrorProvider::class)
    @DisplayName("Ошибки при удалении продукта")
    fun `test delete product errors`(caseName: String, testCase: ProductDeleteDataSet) {
        val productId = when {
            testCase.isExisting -> {
                val id = createProduct()
                createDishWithProduct(id)
                id
            }
            else -> 999_999_999L
        }

        val body = webTestClient.delete()
            .uri("/api/products/$productId")
            .exchange()
            .expectStatus().isEqualTo(testCase.expectedStatusCode)
            .expectBody(String::class.java)
            .returnResult()
            .responseBody ?: ""

        body shouldContain testCase.expectedErrorMessage!!
    }

    private fun buildSearchUrl(testCase: ProductSearchDataSet): String {
        val sb = StringBuilder("/api/products?")
        testCase.name?.let { sb.append("name=$it&") }
        testCase.category?.let { sb.append("category=$it&") }
        testCase.cookingRequirement?.let { sb.append("cookingRequirement=$it&") }
        if (testCase.vegan) sb.append("vegan=true&")
        if (testCase.glutenFree) sb.append("glutenFree=true&")
        if (testCase.sugarFree) sb.append("sugarFree=true&")
        testCase.sortBy?.let { sb.append("sortBy=$it&") }
        return sb.toString().trimEnd('&', '?')
    }

    private fun ProductRequest.withUniqueName(): ProductRequest =
        copy(name = "${name}_${UUID.randomUUID().toString().take(8)}")

    private fun createDishWithProduct(productId: Long) {
        val dishRequest = DishRequest(
            name = "TestDish_${UUID.randomUUID().toString().take(8)}",
            photos = emptyList(),
            calories = null,
            proteins = null,
            fats = null,
            carbohydrates = null,
            ingredients = listOf(DishIngredientRequest(productId = productId, quantity = 100.0)),
            servingSize = 200.0,
            category = "Второе",
            flags = emptyList()
        )

        webTestClient.post()
            .uri("/api/dishes")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(dishRequest)
            .exchange()
            .expectStatus().isCreated
    }

    private fun createTestProductsForSearch() {
        val suffix = UUID.randomUUID().toString().take(8)
        val testProducts = listOf(
            ProductRequest("Product1_$suffix", emptyList(), 100.0, 10.0, 5.0, 15.0, "Composition1", "Овощи", "Готовый к употреблению", listOf("Веган", "Без глютена")),
            ProductRequest("Product2_$suffix", emptyList(), 80.0, 8.0, 3.0, 12.0, "Composition2", "Овощи", "Готовый к употреблению", listOf("Веган", "Без глютена")),
            ProductRequest("Product3_$suffix", emptyList(), 50.0, 5.0, 2.0, 8.0, "Composition3", "Овощи", "Готовый к употреблению", listOf("Веган", "Без глютена", "Без сахара")),
            ProductRequest("Product4_$suffix", emptyList(), 120.0, 12.0, 6.0, 20.0, "Composition4", "Овощи", "Готовый к употреблению", listOf("Веган")),
            ProductRequest("Product5_$suffix", emptyList(), 150.0, 15.0, 7.0, 25.0, "Composition5", "Крупы", "Требует приготовления", listOf("Без глютена", "Без сахара"))
        )

        testProducts.forEach { request ->
            webTestClient.post()
                .uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated
        }
    }

    private fun createProduct(expectedResponse: ProductResponse): Long {
        val request = ProductRequest(
            name = "${expectedResponse.name}_${UUID.randomUUID().toString().take(8)}",
            photos = emptyList(),
            calories = expectedResponse.calories,
            proteins = expectedResponse.proteins,
            fats = expectedResponse.fats,
            carbohydrates = expectedResponse.carbohydrates,
            composition = expectedResponse.composition,
            category = expectedResponse.category,
            cookingRequirement = expectedResponse.cookingRequirement,
            flags = emptyList()
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

    private fun createProduct(): Long {
        val request = ProductRequest(
            name = "TestProduct_${UUID.randomUUID().toString().take(8)}",
            photos = emptyList(),
            calories = 100.0,
            proteins = 10.0,
            fats = 5.0,
            carbohydrates = 15.0,
            composition = "TestComposition",
            category = "Сладости",
            cookingRequirement = "Готовый к употреблению",
            flags = emptyList()
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
}
