package com.example.recipeBook.integration

import com.example.recipeBook.TestcontainersConfiguration
import com.example.recipeBook.data.DishCreateDataSet
import com.example.recipeBook.data.DishDeleteDataSet
import com.example.recipeBook.data.DishGetDataSet
import com.example.recipeBook.data.DishSearchDataSet
import com.example.recipeBook.data.DishUpdateDataSet
import com.example.recipeBook.dto.DishIngredientRequest
import com.example.recipeBook.dto.DishRequest
import com.example.recipeBook.dto.ProductRequest
import com.example.recipeBook.repository.DishRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.BeforeEach
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
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
@Transactional
class DishIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var dishRepository: DishRepository

    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    @BeforeEach
    fun setup() {
        dishRepository.deleteAll()
    }

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

        val actualRequest = testCase.request.copy(
            ingredients = testCase.request.ingredients.map { it.copy(productId = productId) }
        )

        mockMvc.perform(
            post("/api/dishes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(actualRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value(testCase.expectedResponse!!.name))
            .andExpect(jsonPath("$.servingSize").value(testCase.expectedResponse.servingSize))
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

        val actualRequest = when {
            testCase.request.ingredients.isEmpty() -> testCase.request
            else -> testCase.request.copy(
                ingredients = testCase.request.ingredients.map { it.copy(productId = productId) }
            )
        }

        val errorMsg = mockMvc.perform(
            post("/api/dishes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(actualRequest))
        )
            .andExpect(status().`is`(testCase.expectedStatusCode))
            .andReturn()
            .response
            .contentAsString

        errorMsg shouldContain testCase.expectedErrorMessage!!
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

        val actualRequest = testCase.request.copy(
            ingredients = testCase.request.ingredients.map { it.copy(productId = productId) }
        )

        mockMvc.perform(
            put("/api/dishes/$dishId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(actualRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value(testCase.expectedResponse!!.name))
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
        val dishId = 999L

        val actualRequest = testCase.request.copy(
            ingredients = testCase.request.ingredients.map { it.copy(productId = productId) }
        )

        val errorMsg = mockMvc.perform(
            put("/api/dishes/$dishId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(actualRequest))
        )
            .andExpect(status().`is`(testCase.expectedStatusCode))
            .andReturn()
            .response
            .contentAsString

        errorMsg shouldContain testCase.expectedErrorMessage!!
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

        mockMvc.perform(
            get("/api/dishes/$dishId")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(dishId))
            .andExpect(jsonPath("$.name").exists())
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
        val dishId = 999L

        val errorMsg = mockMvc.perform(
            get("/api/dishes/$dishId")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().`is`(testCase.expectedStatusCode))
            .andReturn()
            .response
            .contentAsString

        errorMsg shouldContain testCase.expectedErrorMessage!!
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

        val urlBuilder = StringBuilder("/api/dishes?")
        testCase.name?.let { urlBuilder.append("name=$it&") }
        testCase.category?.let { urlBuilder.append("category=$it&") }
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

    class SearchDishErrorProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = Stream.of(
            arguments("ошибка при неверной категории", searchDishesInvalidCategory())
        )
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(SearchDishErrorProvider::class)
    @DisplayName("Ошибки при поиске блюд")
    fun `test search dishes errors`(caseName: String, testCase: DishSearchDataSet) {
        createTestDishesForSearch()

        val urlBuilder = StringBuilder("/api/dishes?")
        testCase.name?.let { urlBuilder.append("name=$it&") }
        testCase.category?.let { urlBuilder.append("category=$it&") }
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

        mockMvc.perform(
            delete("/api/dishes/$dishId")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNoContent)

        val exists = dishRepository.existsById(dishId)
        exists shouldBe false
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
        val dishId = 999L

        val errorMsg = mockMvc.perform(
            delete("/api/dishes/$dishId")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().`is`(testCase.expectedStatusCode))
            .andReturn()
            .response
            .contentAsString

        errorMsg shouldContain testCase.expectedErrorMessage!!
    }

    private fun createTestProduct(): Long {
        val request = ProductRequest(
            name = "TestProduct",
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

    private fun createDish(productId: Long): Long {
        val request = DishRequest(
            name = "TestDish",
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
        val result = mockMvc.perform(
            post("/api/dishes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()
        return objectMapper.readTree(result.response.contentAsString)
            .get("id").asLong()
    }

    private fun createTestDishesForSearch() {
        val productId = createTestProduct()
        val dishes = listOf(
            DishRequest("Dish1", emptyList(), null, null, null, null, listOf(DishIngredientRequest(productId, 100.0)), 200.0, "Первое", listOf("Веган")),
            DishRequest("Dish2", emptyList(), null, null, null, null, listOf(DishIngredientRequest(productId, 80.0)), 200.0, "Первое", listOf("Веган")),
            DishRequest("Dish3", emptyList(), null, null, null, null, listOf(DishIngredientRequest(productId, 50.0)), 200.0, "Первое", listOf("Веган")),
            DishRequest("Dish4", emptyList(), null, null, null, null, listOf(DishIngredientRequest(productId, 120.0)), 200.0, "Второе", listOf("Веган")),
            DishRequest("Dish5", emptyList(), null, null, null, null, listOf(DishIngredientRequest(productId, 150.0)), 200.0, "Салат", emptyList())
        )
        dishes.forEach { request ->
            mockMvc.perform(
                post("/api/dishes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
        }
    }
}
