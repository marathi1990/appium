package tests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.NoSuchElementException;

import base.BaseTest;
import io.appium.java_client.AppiumBy;
import utils.DriverManager;

public class AccessibilityTest extends BaseTest {

    @Test
    public void verifyAccessibilityTextExistsTest() {
        boolean isAccessibilityPresent;
        try {
            isAccessibilityPresent = DriverManager.driver
                    .findElement(AppiumBy.androidUIAutomator("new UiSelector().text(\"Accessibility\")"))
                    .isDisplayed();
            System.out.println("Test is completed");
        } catch (NoSuchElementException e) {
            isAccessibilityPresent = false;
        }

        Assertions.assertTrue(isAccessibilityPresent,
                "1Element with text 'Accessibility' was not found on the screen");
    }
}
