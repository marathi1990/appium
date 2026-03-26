package tests;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;
import java.io.File;
import java.io.IOException;

import base.BaseTest;
import org.junit.jupiter.api.Assertions;
import pages.LoginPage;
import pages.MyHealthPage;
import utils.DriverManager;
import io.appium.java_client.AppiumBy;

public class LoginTest extends BaseTest {

    @Test
    public void validLoginWithPinTest() {
        LoginPage loginPage = new LoginPage();

        loginPage.loginWithPin("9594260325", "111111");

        // Validate successful login (home/dashboard)
        boolean isHomeDisplayed = DriverManager.driver
                .findElement(AppiumBy.xpath("//*[contains(@text,'Home')]"))
                .isDisplayed();

        Assertions.assertTrue(isHomeDisplayed);
    }

    @Test
    public void uploadImageFromGalleryInBasicInfoTest() throws InterruptedException {
        LoginPage loginPage = new LoginPage();
        MyHealthPage myHealthPage = new MyHealthPage();

        // First login to the app/
        loginPage.loginWithPin("9594260325", "111111");

        // Wait for home screen to load
        WebDriverWait wait = new WebDriverWait(DriverManager.driver, Duration.ofSeconds(30));
        wait.until(ExpectedConditions.presenceOfElementLocated(AppiumBy.xpath("//*[contains(@text,'Home')]")));

        // Navigate to My Health and upload image
        myHealthPage.uploadImageFromGallery();

        // Add a small wait for the upload to complete
        Thread.sleep(5000);

        // Verify image is uploaded using profilePictureSiv resource ID
        boolean isImageUploaded = myHealthPage.isImageUploaded();
        
        Assertions.assertTrue(isImageUploaded,
                "Image upload verified - profile picture is displayed after upload");
    }

    @Test
    public void verifyProfilePictureImageComparisonTest() throws InterruptedException, IOException {
        LoginPage loginPage = new LoginPage();
        MyHealthPage myHealthPage = new MyHealthPage();

        // First login to the app
        loginPage.loginWithPin("9594260325", "111111");

        // Wait for home screen to load
        WebDriverWait wait = new WebDriverWait(DriverManager.driver, Duration.ofSeconds(30));
        wait.until(ExpectedConditions.presenceOfElementLocated(AppiumBy.xpath("//*[contains(@text,'Home')]")));

        // Navigate to My Health and upload image
        myHealthPage.uploadImageFromGallery();

        // Add a small wait for the upload to complete
        Thread.sleep(5000);

        // Get paths
        String expectedImagePath = "src/test/resources/expect.png";
        String actualImagePath = "src/test/resources/actual_profile.png";
        
        File expectedFile = new File(expectedImagePath);
        
        // Capture actual profile picture screenshot
        File actualScreenshot = myHealthPage.captureProfilePictureScreenshot();
        Assertions.assertNotNull(actualScreenshot, "Failed to capture profile picture screenshot");
        
        // Save the actual screenshot for debugging
        myHealthPage.saveScreenshot(actualScreenshot, actualImagePath);

        // If expected image doesn't exist, create it from the actual captured image
        if (!expectedFile.exists()) {
            System.out.println("⚠️ Expected image not found. Creating baseline image...");
            myHealthPage.saveScreenshot(actualScreenshot, expectedImagePath);
            System.out.println("✅ Baseline image created at: " + expectedImagePath);
            System.out.println("Run the test again to compare against the baseline.");
        } else {
            // Compare images with 10% tolerance to handle device/resolution differences
            boolean imagesMatch = myHealthPage.verifyProfileImageUpload(expectedImagePath, actualImagePath, 10.0);
            
            Assertions.assertTrue(imagesMatch,
                    "Profile picture image comparison failed - uploaded image differs from expected");
        }
    }
}