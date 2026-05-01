package com.example.recipeBook.ui

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.Duration

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
abstract class UiTestBase {

    protected lateinit var driver: WebDriver

    @BeforeEach
    fun setup() {
//        val options = ChromeOptions()
//        options.addArguments("--headless=new")
//
//        driver = ChromeDriver(options)
        driver = ChromeDriver()
        driver.manage().window().maximize()
    }

    fun openMainPage() {
        driver.get("http://localhost:8080")
        WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.visibilityOfElementLocated(By.id("products-tab-btn")))
    }

    @AfterEach
    fun tearDown() {
        driver.quit()
    }
}