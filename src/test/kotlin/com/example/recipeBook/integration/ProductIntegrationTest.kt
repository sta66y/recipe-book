package com.example.recipeBook.integration

import com.example.recipeBook.TestcontainersConfiguration
import com.example.recipeBook.data.ProductCreateDataSet
import com.example.recipeBook.data.ProductDeleteDataSet
import com.example.recipeBook.data.ProductGetDataSet
import com.example.recipeBook.data.ProductSearchDataSet
import com.example.recipeBook.data.ProductUpdateDataSet
import com.example.recipeBook.dto.DishIngredientRequest
import com.example.recipeBook.dto.DishRequest
import com.example.recipeBook.dto.ProductRequest
import com.example.recipeBook.dto.ProductResponse
import com.example.recipeBook.repository.ProductRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.util.stream.Stream

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
@Transactional
class ProductIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var productRepository: ProductRepository

    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    class CreateProductArgumentsProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = ofCases(
            "создание продукта с корректными данными".case(::successfulCreate),
            "ошибка при коротком названии".case(::nameTooShort),
            "ошибка при сумме БЖУ больше 100".case(::macrosSumOver100),
            "ошибка при неверной категории".case(::invalidCategory)
        )

        private fun <T> String.case(block: () -> T) = arguments(this, block())
        private fun ofCases(vararg values: Arguments): Stream<out Arguments> = Stream.of(*values)
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(CreateProductArgumentsProvider::class)
    @DisplayName("Тестирование создания продукта")
    fun `test create product`(caseName: String, testCase: ProductCreateDataSet) {
        val perform = mockMvc.perform(
            post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testCase.request))
        )

        perform.andExpect(status().`is`(testCase.expectedStatusCode))

        if (testCase.expectedStatusCode == 201) {
            testCase.expectedResponse?.let { expected ->
                perform
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.name").value(expected.name))
                    .andExpect(jsonPath("$.calories").value(expected.calories))
            }
        } else {
            testCase.expectedErrorMessage?.let { expectedMsg ->
                val errorMsg = perform.andReturn().response.contentAsString
                errorMsg shouldContain expectedMsg
            }
        }
    }

    class UpdateProductArgumentsProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = ofCases(
            "обновление существующего продукта".case(::successfulUpdate),
            "ошибка при обновлении несуществующего продукта".case(::productNotFound),
            "ошибка при коротком названии".case(::updateNameTooShort)
        )

        private fun <T> String.case(block: () -> T) = arguments(this, block())
        private fun ofCases(vararg values: Arguments): Stream<out Arguments> = Stream.of(*values)
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(UpdateProductArgumentsProvider::class)
    @DisplayName("Тестирование обновления продукта")
    fun `test update product`(caseName: String, testCase: ProductUpdateDataSet) {
        val productId = if (testCase.isExisting) createProduct() else 999L

        val perform = mockMvc.perform(
            put("/api/products/$productId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testCase.request))
        )

        perform.andExpect(status().`is`(testCase.expectedStatusCode))

        when (testCase.expectedStatusCode) {
            200 -> {
                testCase.expectedResponse?.let { expected ->
                    perform.andExpect(jsonPath("$.name").value(expected.name))
                }
            }
            404, 400 -> {
                testCase.expectedErrorMessage?.let { expectedMsg ->
                    val errorMsg = perform.andReturn().response.contentAsString
                    errorMsg shouldContain expectedMsg
                }
            }
        }
    }

    class GetProductArgumentsProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = ofCases(
            "получение существующего продукта".case(::successfulGet),
            "ошибка при получении несуществующего продукта".case(::productNotFoundGet)
        )

        private fun <T> String.case(block: () -> T) = arguments(this, block())
        private fun ofCases(vararg values: Arguments): Stream<out Arguments> = Stream.of(*values)
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(GetProductArgumentsProvider::class)
    @DisplayName("Тестирование получения продукта")
    fun `test get product`(caseName: String, testCase: ProductGetDataSet) {
        val productId = if (testCase.isExisting) createProduct(testCase.expectedResponse!!) else 999L

        val perform = mockMvc.perform(
            get("/api/products/$productId")
                .contentType(MediaType.APPLICATION_JSON)
        )

        perform.andExpect(status().`is`(testCase.expectedStatusCode))

        if (testCase.expectedStatusCode == 200) {
            testCase.expectedResponse?.let { expected ->
                perform
                    .andExpect(jsonPath("$.id").value(productId))
                    .andExpect(jsonPath("$.name").value(expected.name))
            }
        } else {
            testCase.expectedErrorMessage?.let { expectedMsg ->
                val errorMsg = perform.andReturn().response.contentAsString
                errorMsg shouldContain expectedMsg
            }
        }
    }

    class SearchProductArgumentsProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = ofCases(
            "поиск всех продуктов".case(::searchAllProducts),
            "поиск по части названия".case(::searchByNamePartial),
            "поиск по категории".case(::searchByCategory),
            "поиск по флагу".case(::searchByVeganFlag),
            "сортировка по калориям".case(::sortByCalories),
            "ошибка при неверной категории".case(::searchInvalidCategory)
        )

        private fun <T> String.case(block: () -> T) = arguments(this, block())
        private fun ofCases(vararg values: Arguments): Stream<out Arguments> = Stream.of(*values)
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(SearchProductArgumentsProvider::class)
    @DisplayName("Тестирование поиска продуктов")
    fun `test search products`(caseName: String, testCase: ProductSearchDataSet) {
        createTestProductsForSearch()

        val urlBuilder = StringBuilder("/api/products?")
        testCase.name?.let { urlBuilder.append("name=$it&") }
        testCase.category?.let { urlBuilder.append("category=$it&") }
        testCase.cookingRequirement?.let { urlBuilder.append("cookingRequirement=$it&") }
        if (testCase.vegan) urlBuilder.append("vegan=true&")
        if (testCase.glutenFree) urlBuilder.append("glutenFree=true&")
        if (testCase.sugarFree) urlBuilder.append("sugarFree=true&")
        testCase.sortBy?.let { urlBuilder.append("sortBy=$it&") }

        val url = urlBuilder.toString().trimEnd('&', '?')

        val perform = mockMvc.perform(
            get(url).contentType(MediaType.APPLICATION_JSON)
        )

        perform.andExpect(status().`is`(testCase.expectedStatusCode))

        when (testCase.expectedStatusCode) {
            200 -> {
                testCase.expectedProductCount?.let { count ->
                    perform.andExpect(jsonPath("$.length()").value(count))
                }
                testCase.expectedProductNames?.let { names ->
                    names.forEachIndexed { index, name ->
                        perform.andExpect(jsonPath("$[$index].name").value(name))
                    }
                }
            }
            400 -> {
                testCase.expectedErrorMessage?.let { expectedMsg ->
                    val errorMsg = perform.andReturn().response.contentAsString
                    errorMsg shouldContain expectedMsg
                }
            }
        }
    }

    class DeleteProductArgumentsProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = ofCases(
            "успешное удаление продукта".case(::successfulDelete),
            "ошибка при удалении несуществующего продукта".case(::deleteProductNotFound),
            "ошибка при удалении продукта используемого в блюде".case(::deleteProductUsedInDish)
        )

        private fun <T> String.case(block: () -> T) = arguments(this, block())
        private fun ofCases(vararg values: Arguments): Stream<out Arguments> = Stream.of(*values)
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(DeleteProductArgumentsProvider::class)
    @DisplayName("Тестирование удаления продукта")
    fun `test delete product`(caseName: String, testCase: ProductDeleteDataSet) {
        val productId = if (testCase.isExisting) {
            val id = createProduct()
            if (testCase.isUsedInDish) {
                createDishWithProduct(id)
            }
            id
        } else {
            999L
        }

        val perform = mockMvc.perform(
            delete("/api/products/$productId")
                .contentType(MediaType.APPLICATION_JSON)
        )

        perform.andExpect(status().`is`(testCase.expectedStatusCode))

        when (testCase.expectedStatusCode) {
            204 -> {
                val exists = productRepository.existsById(productId)
                exists shouldBe false
            }
            404, 409 -> {
                testCase.expectedErrorMessage?.let { expectedMsg ->
                    val errorMsg = perform.andReturn().response.contentAsString
                    errorMsg shouldContain expectedMsg
                }
            }
        }
    }

    private fun createDishWithProduct(productId: Long) {
        val dishRequest = DishRequest(
            name = "TestDish",
            photos = emptyList(),
            calories = null,
            proteins = null,
            fats = null,
            carbohydrates = null,
            ingredients = listOf(
                DishIngredientRequest(
                    productId = productId,
                    quantity = 100.0
                )
            ),
            servingSize = 200.0,
            category = "Второе",
            flags = emptyList()
        )

        mockMvc.perform(
            post("/api/dishes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dishRequest))
        ).andExpect(status().isCreated)
    }

    private fun createTestProductsForSearch() {
        val testProducts = listOf(
            ProductRequest("Product1", emptyList(), 100.0, 10.0, 5.0, 15.0, "Composition1", "Овощи", "Готовый к употреблению", listOf("Веган", "Без глютена")),
            ProductRequest("Product2", emptyList(), 80.0, 8.0, 3.0, 12.0, "Composition2", "Овощи", "Готовый к употреблению", listOf("Веган", "Без глютена")),
            ProductRequest("Product3", emptyList(), 50.0, 5.0, 2.0, 8.0, "Composition3", "Овощи", "Готовый к употреблению", listOf("Веган", "Без глютена", "Без сахара")),
            ProductRequest("Product4", emptyList(), 120.0, 12.0, 6.0, 20.0, "Composition4", "Овощи", "Готовый к употреблению", listOf("Веган")),
            ProductRequest("Product5", emptyList(), 150.0, 15.0, 7.0, 25.0, "Composition5", "Крупы", "Требует приготовления", listOf("Без глютена", "Без сахара"))
        )

        testProducts.forEach { request ->
            mockMvc.perform(
                post("/api/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            ).andExpect(status().isCreated)
        }
    }

    private fun createProduct(expectedResponse: ProductResponse): Long {
        val request = ProductRequest(
            name = expectedResponse.name,
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

        val result = mockMvc.perform(
            post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()

        return objectMapper.readTree(result.response.contentAsString)
            .get("id").asLong()
    }

    private fun createProduct(): Long {
        val request = ProductRequest(
            name = "TestProduct",
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

        val result = mockMvc.perform(
            post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()

        return objectMapper.readTree(result.response.contentAsString)
            .get("id").asLong()
    }
}
