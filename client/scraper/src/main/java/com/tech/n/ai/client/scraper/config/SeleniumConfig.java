package com.tech.n.ai.client.scraper.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selenium WebDriver 설정
 * SPA/Cloudflare 보호 페이지 크롤링용
 */
@Configuration
@ConditionalOnProperty(name = "scraper.selenium.enabled", havingValue = "true")
@Slf4j
public class SeleniumConfig {

    @Value("${scraper.selenium.driver-path:}")
    private String driverPath;

    @Value("${scraper.selenium.headless:true}")
    private boolean headless;

    private WebDriver webDriver;

    @Bean
    public WebDriver seleniumWebDriver() {
        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage");
        options.addArguments("--user-agent=ShrimpTM-Demo/1.0");

        if (driverPath != null && !driverPath.isEmpty()) {
            System.setProperty("webdriver.chrome.driver", driverPath);
        }

        webDriver = new ChromeDriver(options);
        log.info("Selenium WebDriver initialized (headless={})", headless);
        return webDriver;
    }

    @PreDestroy
    public void cleanup() {
        if (webDriver != null) {
            try {
                webDriver.quit();
                log.info("Selenium WebDriver closed");
            } catch (Exception e) {
                log.warn("Failed to close Selenium WebDriver: {}", e.getMessage());
            }
        }
    }
}
