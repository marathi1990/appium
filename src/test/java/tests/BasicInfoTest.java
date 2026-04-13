package tests;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;

import base.BaseTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import pages.LoginPage;
import pages.MyHealthPage;
import pages.BasicInfoPage;
import utils.DriverManager;
import io.appium.java_client.AppiumBy;

public class BasicInfoTest extends BaseTest {

    @Tag("regression")
    @Test
    public void verifyBasicInfoFieldsEditabilityTest1() throws InterruptedException {
        LoginPage loginPage = new LoginPage();
        MyHealthPage myHealthPage = new MyHealthPage();
        BasicInfoPage basicInfoPage = new BasicInfoPage();

        System.out.println("=== Starting BasicInfo Editability Test ===");
        loginPage.loginWithPin("9594260325", "111111");
        System.out.println("✅ Login completed, waiting for Home page...");
        Thread.sleep(3000);

        WebDriverWait wait = new WebDriverWait(DriverManager.driver, Duration.ofSeconds(45));
        wait.until(ExpectedConditions.presenceOfElementLocated(AppiumBy.xpath("//*[contains(@text,'Home')]")));
        System.out.println("✅ Home page loaded");
        Thread.sleep(2000);

        myHealthPage.navigateToMyHealth();
        System.out.println("✅ Navigated to MyHealth");
        Thread.sleep(2000);

        myHealthPage.navigateToBasicInfo();
        System.out.println("✅ Navigated to BasicInfo, waiting for page to render...");
        Thread.sleep(2000);

        basicInfoPage.scrollDowntilleditdone();
        System.out.println("✅ Scrolled down, waiting for content...");
        Thread.sleep(2000);
        basicInfoPage.clickEdit();
        basicInfoPage.scrollDowntilleditdone();
        Thread.sleep(2000);
        System.out.println("✅ Clicked Edit, verifying field editability...");
        Thread.sleep(2000);
        basicInfoPage.verifyBasicInfoFieldsEditability();

        System.out.println("All field editability checks completed successfully");
    }


    @Tag("regression")
    @Test
    public void verifyBasicInfoFieldsEditabilityTest2() throws InterruptedException {
        LoginPage loginPage = new LoginPage();
        MyHealthPage myHealthPage = new MyHealthPage();
        BasicInfoPage basicInfoPage = new BasicInfoPage();

        System.out.println("=== Starting BasicInfo Editability Test ===");
        loginPage.loginWithPin("9594260325", "111111");
        System.out.println("✅ Login completed, waiting for Home page...");
        Thread.sleep(3000);

        WebDriverWait wait = new WebDriverWait(DriverManager.driver, Duration.ofSeconds(45));
        wait.until(ExpectedConditions.presenceOfElementLocated(AppiumBy.xpath("//*[contains(@text,'Home')]")));
        System.out.println("✅ Home page loaded");
        Thread.sleep(2000);

        myHealthPage.navigateToMyHealth();
        System.out.println("✅ Navigated to MyHealth");
        Thread.sleep(2000);

        myHealthPage.navigateToBasicInfo();
        System.out.println("✅ Navigated to BasicInfo, waiting for page to render...");
        Thread.sleep(2000);

        basicInfoPage.scrollDowntilleditdone();
        System.out.println("✅ Scrolled down, waiting for content...");
        Thread.sleep(2000);
        basicInfoPage.clickEdit();
        basicInfoPage.scrollDowntilleditdone();
        Thread.sleep(2000);
        System.out.println("✅ Clicked Edit, verifying field editability...");
        Thread.sleep(2000);
        basicInfoPage.verifyBasicInfoFieldsEditability();

        System.out.println("All field editability checks completed successfully");
    }

    @Tag("regression")
    @Test
    public void verifyBasicInfoFieldsEditabilityTest3() throws InterruptedException {
        LoginPage loginPage = new LoginPage();
        MyHealthPage myHealthPage = new MyHealthPage();
        BasicInfoPage basicInfoPage = new BasicInfoPage();

        System.out.println("=== Starting BasicInfo Editability Test ===");
        loginPage.loginWithPin("9594260325", "111111");
        System.out.println("✅ Login completed, waiting for Home page...");
        Thread.sleep(3000);

        WebDriverWait wait = new WebDriverWait(DriverManager.driver, Duration.ofSeconds(45));
        wait.until(ExpectedConditions.presenceOfElementLocated(AppiumBy.xpath("//*[contains(@text,'Home')]")));
        System.out.println("✅ Home page loaded");
        Thread.sleep(2000);

        myHealthPage.navigateToMyHealth();
        System.out.println("✅ Navigated to MyHealth");
        Thread.sleep(2000);

        myHealthPage.navigateToBasicInfo();
        System.out.println("✅ Navigated to BasicInfo, waiting for page to render...");
        Thread.sleep(2000);

        basicInfoPage.scrollDowntilleditdone();
        System.out.println("✅ Scrolled down, waiting for content...");
        Thread.sleep(2000);
        basicInfoPage.clickEdit();
        basicInfoPage.scrollDowntilleditdone();
        Thread.sleep(2000);
        System.out.println("✅ Clicked Edit, verifying field editability...");
        Thread.sleep(2000);
        basicInfoPage.verifyBasicInfoFieldsEditability();

        System.out.println("All field editability checks completed successfully");
    }

    @Tag("regression")
    @Test
    public void verifyBasicInfoFieldsEditabilityTest4() throws InterruptedException {
        LoginPage loginPage = new LoginPage();
        MyHealthPage myHealthPage = new MyHealthPage();
        BasicInfoPage basicInfoPage = new BasicInfoPage();

        System.out.println("=== Starting BasicInfo Editability Test ===");
        loginPage.loginWithPin("9594260325", "111111");
        System.out.println("✅ Login completed, waiting for Home page...");
        Thread.sleep(3000);

        WebDriverWait wait = new WebDriverWait(DriverManager.driver, Duration.ofSeconds(45));
        wait.until(ExpectedConditions.presenceOfElementLocated(AppiumBy.xpath("//*[contains(@text,'Home')]")));
        System.out.println("✅ Home page loaded");
        Thread.sleep(2000);

        myHealthPage.navigateToMyHealth();
        System.out.println("✅ Navigated to MyHealth");
        Thread.sleep(2000);

        myHealthPage.navigateToBasicInfo();
        System.out.println("✅ Navigated to BasicInfo, waiting for page to render...");
        Thread.sleep(2000);

        basicInfoPage.scrollDowntilleditdone();
        System.out.println("✅ Scrolled down, waiting for content...");
        Thread.sleep(2000);
        basicInfoPage.clickEdit();
        basicInfoPage.scrollDowntilleditdone();
        Thread.sleep(2000);
        System.out.println("✅ Clicked Edit, verifying field editability...");
        Thread.sleep(2000);
        basicInfoPage.verifyBasicInfoFieldsEditability();

        System.out.println("All field editability checks completed successfully");
    }

    @Tag("regression")
    @Test
    public void verifyBasicInfoFieldsEditabilityTest5() throws InterruptedException {
        LoginPage loginPage = new LoginPage();
        MyHealthPage myHealthPage = new MyHealthPage();
        BasicInfoPage basicInfoPage = new BasicInfoPage();

        System.out.println("=== Starting BasicInfo Editability Test ===");
        loginPage.loginWithPin("9594260325", "111111");
        System.out.println("✅ Login completed, waiting for Home page...");
        Thread.sleep(3000);

        WebDriverWait wait = new WebDriverWait(DriverManager.driver, Duration.ofSeconds(45));
        wait.until(ExpectedConditions.presenceOfElementLocated(AppiumBy.xpath("//*[contains(@text,'Home')]")));
        System.out.println("✅ Home page loaded");
        Thread.sleep(2000);

        myHealthPage.navigateToMyHealth();
        System.out.println("✅ Navigated to MyHealth");
        Thread.sleep(2000);

        myHealthPage.navigateToBasicInfo();
        System.out.println("✅ Navigated to BasicInfo, waiting for page to render...");
        Thread.sleep(2000);

        basicInfoPage.scrollDowntilleditdone();
        System.out.println("✅ Scrolled down, waiting for content...");
        Thread.sleep(2000);
        basicInfoPage.clickEdit();
        basicInfoPage.scrollDowntilleditdone();
        Thread.sleep(2000);
        System.out.println("✅ Clicked Edit, verifying field editability...");
        Thread.sleep(2000);
        basicInfoPage.verifyBasicInfoFieldsEditability();

        System.out.println("All field editability checks completed successfully");
    }

    @Tag("smoke")
    @Test
    public void verifyBasicInfoFieldsEditabilityTest6() throws InterruptedException {
        LoginPage loginPage = new LoginPage();
        MyHealthPage myHealthPage = new MyHealthPage();
        BasicInfoPage basicInfoPage = new BasicInfoPage();

        System.out.println("=== Starting BasicInfo Editability Test ===");
        loginPage.loginWithPin("9594260325", "111111");
        System.out.println("✅ Login completed, waiting for Home page...");
        Thread.sleep(3000);

        WebDriverWait wait = new WebDriverWait(DriverManager.driver, Duration.ofSeconds(45));
        wait.until(ExpectedConditions.presenceOfElementLocated(AppiumBy.xpath("//*[contains(@text,'Home')]")));
        System.out.println("✅ Home page loaded");
        Thread.sleep(2000);

        myHealthPage.navigateToMyHealth();
        System.out.println("✅ Navigated to MyHealth");
        Thread.sleep(2000);

        myHealthPage.navigateToBasicInfo();
        System.out.println("✅ Navigated to BasicInfo, waiting for page to render...");
        Thread.sleep(2000);

        basicInfoPage.scrollDowntilleditdone();
        System.out.println("✅ Scrolled down, waiting for content...");
        Thread.sleep(2000);
        basicInfoPage.clickEdit();
        basicInfoPage.scrollDowntilleditdone();
        Thread.sleep(2000);
        System.out.println("✅ Clicked Edit, verifying field editability...");
        Thread.sleep(2000);
        basicInfoPage.verifyBasicInfoFieldsEditability();

        System.out.println("All field editability checks completed successfully");
    }

    @Tag("smoke")
    @Test
    public void verifyBasicInfoFieldsEditabilityTest7() throws InterruptedException {
        LoginPage loginPage = new LoginPage();
        MyHealthPage myHealthPage = new MyHealthPage();
        BasicInfoPage basicInfoPage = new BasicInfoPage();

        System.out.println("=== Starting BasicInfo Editability Test ===");
        loginPage.loginWithPin("9594260325", "111111");
        System.out.println("✅ Login completed, waiting for Home page...");
        Thread.sleep(3000);

        WebDriverWait wait = new WebDriverWait(DriverManager.driver, Duration.ofSeconds(45));
        wait.until(ExpectedConditions.presenceOfElementLocated(AppiumBy.xpath("//*[contains(@text,'Home')]")));
        System.out.println("✅ Home page loaded");
        Thread.sleep(2000);

        myHealthPage.navigateToMyHealth();
        System.out.println("✅ Navigated to MyHealth");
        Thread.sleep(2000);

        myHealthPage.navigateToBasicInfo();
        System.out.println("✅ Navigated to BasicInfo, waiting for page to render...");
        Thread.sleep(2000);

        basicInfoPage.scrollDowntilleditdone();
        System.out.println("✅ Scrolled down, waiting for content...");
        Thread.sleep(2000);
        basicInfoPage.clickEdit();
        basicInfoPage.scrollDowntilleditdone();
        Thread.sleep(2000);
        System.out.println("✅ Clicked Edit, verifying field editability...");
        Thread.sleep(2000);
        basicInfoPage.verifyBasicInfoFieldsEditability();

        System.out.println("All field editability checks completed successfully");
    }

    // @Test
    // public void verifyBasicInfoDataDrivenFromJson() throws Exception {
    //     LoginPage loginPage = new LoginPage();
    //     MyHealthPage myHealthPage = new MyHealthPage();
    //     BasicInfoPage basicInfoPage = new BasicInfoPage();

    //     loginPage.loginWithPin("9594260325", "111111");

    //     WebDriverWait wait = new WebDriverWait(DriverManager.driver, Duration.ofSeconds(30));
    //     wait.until(ExpectedConditions.presenceOfElementLocated(AppiumBy.xpath("//*[contains(@text,'Home')]")));
    //     //test//
    //     myHealthPage.navigateToMyHealth();
    //     myHealthPage.navigateToBasicInfo();
    //     Thread.sleep(3000);
    //     basicInfoPage.scrollDown();
    //     Thread.sleep(3000);
    //     BasicInfoPage.BasicInfoPayload testData = loadTestData();

    //     for (BasicInfoPage.BasicInfoEntry entry : testData.validData) {
    //         System.out.println("\n=== Valid case: " + entry.mobile + ", " + entry.email + " ===");
    //         basicInfoPage.clickEdit();
    //         basicInfoPage.enterMobile(entry.mobile);
    //         basicInfoPage.enterEmail(entry.email);
    //         basicInfoPage.clickDone();
    //         basicInfoPage.verifyNoErrorMessage();
    //         System.out.println("✅ Valid entry succeeded: " + entry.mobile + ", " + entry.email);
    //     }

    //     for (BasicInfoPage.BasicInfoEntry entry : testData.invalidData) {
    //         System.out.println("\n=== Invalid case: " + entry.mobile + ", " + entry.email + " reason: " + entry.reason + " ===");
    //         basicInfoPage.submitBasicInfoWithoutSave(entry.mobile, entry.email);
    //         basicInfoPage.verifyErrorMessageDisplayed();
    //         basicInfoPage.clearemail();
    //         basicInfoPage.clickDone();
    //         System.out.println("✅ Invalid entry correctly showed error: " + entry.reason);
    //     }
    // }

    // private BasicInfoPage.BasicInfoPayload loadTestData() throws IOException {
    //     try (InputStream is = getClass().getClassLoader().getResourceAsStream("testdata/basicInfoData.json")) {
    //         Assertions.assertNotNull(is, "Test data file not found: testdata/basicInfoData.json");
    //         ObjectMapper mapper = new ObjectMapper();
    //         return mapper.readValue(is, BasicInfoPage.BasicInfoPayload.class);
    //     }
    //// }

}
