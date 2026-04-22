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
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var productRepository: ProductRepository

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
        val perform = mockMvc.perform(
            post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testCase.request))
        )

        perform
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value(testCase.expectedResponse!!.name))
            .andExpect(jsonPath("$.calories").value(testCase.expectedResponse.calories))
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
        val errorMsg = mockMvc.perform(
            post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testCase.request))
        )
            .andExpect(status().`is`(testCase.expectedStatusCode))
            .andReturn()
            .response
            .contentAsString

        errorMsg shouldContain testCase.expectedErrorMessage!!
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

        mockMvc.perform(
            put("/api/products/$productId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testCase.request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value(testCase.expectedResponse!!.name))
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
        val productId = 999L

        val errorMsg = mockMvc.perform(
            put("/api/products/$productId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testCase.request))
        )
            .andExpect(status().`is`(testCase.expectedStatusCode))
            .andReturn()
            .response
            .contentAsString

        errorMsg shouldContain testCase.expectedErrorMessage!!
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

        mockMvc.perform(
            get("/api/products/$productId")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(productId))
            .andExpect(jsonPath("$.name").value(testCase.expectedResponse.name))
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
        val productId = 999L

        val errorMsg = mockMvc.perform(
            get("/api/products/$productId")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().`is`(testCase.expectedStatusCode))
            .andReturn()
            .response
            .contentAsString

        errorMsg shouldContain testCase.expectedErrorMessage!!
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

        val urlBuilder = StringBuilder("/api/products?")
        testCase.name?.let { urlBuilder.append("name=$it&") }
        testCase.category?.let { urlBuilder.append("category=$it&") }
        testCase.cookingRequirement?.let { urlBuilder.append("cookingRequirement=$it&") }
        testCase.vegan.takeIf { it }?.let { urlBuilder.append("vegan=true&") }
        testCase.glutenFree.takeIf { it }?.let { urlBuilder.append("glutenFree=true&") }
        testCase.sugarFree.takeIf { it }?.let { urlBuilder.append("sugarFree=true&") }
        testCase.sortBy?.let { urlBuilder.append("sortBy=$it&") }
        val url = urlBuilder.toString().trimEnd('&', '?')

        val perform = mockMvc.perform(
            get(url).contentType(MediaType.APPLICATION_JSON)
        )

        perform.andExpect(status().isOk)

        testCase.expectedProductCount?.let { count ->
            perform.andExpect(jsonPath("$.length()").value(count))
        }

        testCase.expectedProductNames?.let { names ->
            names.forEachIndexed { index, name ->
                perform.andExpect(jsonPath("$[$index].name").value(name))
            }
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
        createTestProductsForSearch()

        val urlBuilder = StringBuilder("/api/products?")
        testCase.name?.let { urlBuilder.append("name=$it&") }
        testCase.category?.let { urlBuilder.append("category=$it&") }
        testCase.cookingRequirement?.let { urlBuilder.append("cookingRequirement=$it&") }
        testCase.vegan.takeIf { it }?.let { urlBuilder.append("vegan=true&") }
        testCase.glutenFree.takeIf { it }?.let { urlBuilder.append("glutenFree=true&") }
        testCase.sugarFree.takeIf { it }?.let { urlBuilder.append("sugarFree=true&") }
        testCase.sortBy?.let { urlBuilder.append("sortBy=$it&") }
        val url = urlBuilder.toString().trimEnd('&', '?')

        val errorMsg = mockMvc.perform(
            get(url).contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().`is`(testCase.expectedStatusCode))
            .andReturn()
            .response
            .contentAsString

        errorMsg shouldContain testCase.expectedErrorMessage!!
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

        mockMvc.perform(
            delete("/api/products/$productId")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNoContent)

        val exists = productRepository.existsById(productId)
        exists shouldBe false
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
            else -> 999L
        }

        val errorMsg = mockMvc.perform(
            delete("/api/products/$productId")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().`is`(testCase.expectedStatusCode))
            .andReturn()
            .response
            .contentAsString

        errorMsg shouldContain testCase.expectedErrorMessage!!
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
