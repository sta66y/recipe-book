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

    class CreateDishArgumentsProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = ofCases(
            "создание блюда с корректными данными".case { successfulDishCreate(1L) },
            "ошибка при коротком названии".case { dishNameTooShort(1L) },
            "ошибка при сумме БЖУ больше 100".case { dishMacrosSumOver100(1L) },
            "ошибка при неверной категории".case { dishInvalidCategory(1L) },
            "ошибка при пустом списке ингредиентов".case(::dishEmptyIngredients)
        )

        private fun <T> String.case(block: () -> T) = arguments(this, block())
        private fun ofCases(vararg values: Arguments): Stream<out Arguments> = Stream.of(*values)
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(CreateDishArgumentsProvider::class)
    @DisplayName("Тестирование создания блюда")
    fun `test create dish`(caseName: String, testCase: DishCreateDataSet) {
        val productId = if (testCase.request.ingredients.isNotEmpty()) {
            createTestProduct()
        } else null

        val actualRequest = if (productId != null && testCase.request.ingredients.isNotEmpty()) {
            testCase.request.copy(
                ingredients = testCase.request.ingredients.map { it.copy(productId = productId) }
            )
        } else {
            testCase.request
        }

        val perform = mockMvc.perform(
            post("/api/dishes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(actualRequest))
        )

        perform.andExpect(status().`is`(testCase.expectedStatusCode))

        if (testCase.expectedStatusCode == 201) {
            testCase.expectedResponse?.let { expected ->
                perform
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.name").value(expected.name))
                    .andExpect(jsonPath("$.servingSize").value(expected.servingSize))
            }
        } else {
            testCase.expectedErrorMessage?.let { expectedMsg ->
                val errorMsg = perform.andReturn().response.contentAsString
                errorMsg shouldContain expectedMsg
            }
        }
    }

    class UpdateDishArgumentsProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = ofCases(
            "обновление существующего блюда".case { successfulDishUpdate(1L) },
            "ошибка при обновлении несуществующего блюда".case { dishNotFound(1L) },
            "ошибка при коротком названии".case { updateDishNameTooShort(1L) }
        )

        private fun <T> String.case(block: () -> T) = arguments(this, block())
        private fun ofCases(vararg values: Arguments): Stream<out Arguments> = Stream.of(*values)
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(UpdateDishArgumentsProvider::class)
    @DisplayName("Тестирование обновления блюда")
    fun `test update dish`(caseName: String, testCase: DishUpdateDataSet) {
        val productId = createTestProduct()
        val dishId = if (testCase.isExisting) createDish(productId) else 999L

        val actualRequest = testCase.request.copy(
            ingredients = testCase.request.ingredients.map { it.copy(productId = productId) }
        )

        val perform = mockMvc.perform(
            put("/api/dishes/$dishId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(actualRequest))
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

    class GetDishArgumentsProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = ofCases(
            "получение существующего блюда".case(::successfulDishGet),
            "ошибка при получении несуществующего блюда".case(::dishNotFoundGet)
        )

        private fun <T> String.case(block: () -> T) = arguments(this, block())
        private fun ofCases(vararg values: Arguments): Stream<out Arguments> = Stream.of(*values)
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(GetDishArgumentsProvider::class)
    @DisplayName("Тестирование получения блюда")
    fun `test get dish`(caseName: String, testCase: DishGetDataSet) {
        val productId = createTestProduct()
        val dishId = if (testCase.isExisting) createDish(productId) else 999L

        val perform = mockMvc.perform(
            get("/api/dishes/$dishId")
                .contentType(MediaType.APPLICATION_JSON)
        )

        perform.andExpect(status().`is`(testCase.expectedStatusCode))

        if (testCase.expectedStatusCode == 200) {
            testCase.expectedResponse?.let {
                perform
                    .andExpect(jsonPath("$.id").value(dishId))
                    .andExpect(jsonPath("$.name").exists())
            }
        } else {
            testCase.expectedErrorMessage?.let { expectedMsg ->
                val errorMsg = perform.andReturn().response.contentAsString
                errorMsg shouldContain expectedMsg
            }
        }
    }

    class SearchDishArgumentsProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = ofCases(
            "поиск всех блюд".case(::searchAllDishes),
            "поиск по части названия".case(::searchDishesByNamePartial),
            "поиск по категории".case(::searchDishesByCategory),
            "поиск по флагу веган".case(::searchDishesByVeganFlag),
            "сортировка по калориям".case(::sortDishesByCalories),
            "ошибка при неверной категории".case(::searchDishesInvalidCategory)
        )

        private fun <T> String.case(block: () -> T) = arguments(this, block())
        private fun ofCases(vararg values: Arguments): Stream<out Arguments> = Stream.of(*values)
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(SearchDishArgumentsProvider::class)
    @DisplayName("Тестирование поиска блюд")
    fun `test search dishes`(caseName: String, testCase: DishSearchDataSet) {
        createTestDishesForSearch()

        val urlBuilder = StringBuilder("/api/dishes?")
        testCase.name?.let { urlBuilder.append("name=$it&") }
        testCase.category?.let { urlBuilder.append("category=$it&") }
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

    class DeleteDishArgumentsProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext) = ofCases(
            "успешное удаление блюда".case(::successfulDishDelete),
            "ошибка при удалении несуществующего блюда".case(::deleteDishNotFound)
        )

        private fun <T> String.case(block: () -> T) = arguments(this, block())
        private fun ofCases(vararg values: Arguments): Stream<out Arguments> = Stream.of(*values)
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(DeleteDishArgumentsProvider::class)
    @DisplayName("Тестирование удаления блюда")
    fun `test delete dish`(caseName: String, testCase: DishDeleteDataSet) {
        val productId = createTestProduct()
        val dishId = if (testCase.isExisting) createDish(productId) else 999L

        val perform = mockMvc.perform(
            delete("/api/dishes/$dishId")
                .contentType(MediaType.APPLICATION_JSON)
        )

        perform.andExpect(status().`is`(testCase.expectedStatusCode))

        when (testCase.expectedStatusCode) {
            204 -> {
                val exists = dishRepository.existsById(dishId)
                exists shouldBe false
            }
            404 -> {
                testCase.expectedErrorMessage?.let { expectedMsg ->
                    val errorMsg = perform.andReturn().response.contentAsString
                    errorMsg shouldContain expectedMsg
                }
            }
        }
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
