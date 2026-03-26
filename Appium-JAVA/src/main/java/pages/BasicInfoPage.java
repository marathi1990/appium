package pages;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.DriverManager;

import java.time.Duration;
import java.util.List;

public class BasicInfoPage {

    private final WebDriverWait wait = new WebDriverWait(DriverManager.driver, Duration.ofSeconds(20));

    // Basic info shared locators
    private final By editButton = AppiumBy.androidUIAutomator("new UiSelector().resourceId(\"com.fluenthealth.app:id/editProfileTv\")");
    private final By mobileInput = AppiumBy.xpath("(//android.widget.EditText)[1]");
    private final By emailInput = AppiumBy.xpath("(//android.widget.EditText)[2]");
    private final By doneButton = AppiumBy.xpath("//*[contains(@text,'Done') or contains(@text,'done') or contains(@content-desc,'Done')]");
    private final By editableFields = AppiumBy.xpath("//android.widget.EditText[@text='-']");
    private final By errorHelperText = AppiumBy.androidUIAutomator("new UiSelector().resourceId(\"com.fluenthealth.app:id/textinput_helper_text\")");

    public void clickEdit() {
        wait.until(ExpectedConditions.elementToBeClickable(editButton)).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(mobileInput));
    }

    public void enterMobile(String mobile) {
        WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(mobileInput));
        element.clear();
        element.sendKeys(mobile);
    }

    public void enterEmail(String email) {
        WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(emailInput));
        element.clear();
        element.sendKeys(email);
    }

    public void clearemail()
    {
        WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(emailInput));
        element.clear();
    }

    public void clickDone() {
        wait.until(ExpectedConditions.elementToBeClickable(doneButton)).click();
    }

    public void verifyErrorMessageDisplayed() {
        List<WebElement> errors = DriverManager.driver.findElements(errorHelperText);
        if (errors.isEmpty()) {
            throw new AssertionError("Expected error helper text to be displayed but it was not.");
        }
    }

    public void verifyNoErrorMessage() {
        List<WebElement> errors = DriverManager.driver.findElements(errorHelperText);
        if (!errors.isEmpty()) {
            throw new AssertionError("Expected no error helper text, but some are present.");
        }
    }

    public void submitBasicInfo(String mobile, String email) {
        clickEdit();
        enterMobile(mobile);
        enterEmail(email);
        clickDone();
    }

    public void submitBasicInfoWithoutSave(String mobile, String email) {
        clickEdit();
        enterMobile(mobile);
        enterEmail(email);
    }

    public void verifyBasicInfoDataDriven(BasicInfoPayload data) {
        for (BasicInfoEntry entry : data.validData) {
            submitBasicInfo(entry.mobile, entry.email);
            verifyNoErrorMessage();
        }

        for (BasicInfoEntry entry : data.invalidData) {
            submitBasicInfoWithoutSave(entry.mobile, entry.email);
            verifyErrorMessageDisplayed();
            clickDone();
        }
    }

    public static class BasicInfoPayload {
        public List<BasicInfoEntry> validData;
        public List<BasicInfoEntry> invalidData;
    }

    public static class BasicInfoEntry {
        public String mobile;
        public String email;
        public String reason;
    }

    public void verifyFieldNotEditable(String locatorXpath, String fieldName) {
        By fieldLocator = AppiumBy.xpath(locatorXpath);
        WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(fieldLocator));
        String originalText = element.getText();

        element.click();
        try {
            element.sendKeys("TEST");
            Thread.sleep(500);
            String textAfter = element.getText();
            if (!originalText.equals(textAfter)) {
                throw new AssertionError(fieldName + " should not accept text input (non-editable field)");
            }
        } catch (Exception e) {
            // expected for non-editable fields
        }
    }

    public void verifyFieldEditable(WebElement element, String fieldName) {
        String originalText = element.getText();
        element.click();
        element.clear();
        element.sendKeys("EDITABLE_TEST");
        String afterText = element.getText();
        if (originalText.equals(afterText) || afterText.length() == 0) {
            throw new AssertionError(fieldName + " should accept text input and change value");
        }
    }

    public void verifyEditableFields() {
        List<WebElement> fields = DriverManager.driver.findElements(editableFields);
        if (fields.isEmpty()) {
            throw new AssertionError("No editable fields found in Basic Info");
        }
        for (int i = 0; i < fields.size(); i++) {
            verifyFieldEditable(fields.get(i), "Editable field " + (i + 1));
        }
    }

    public void checkElementExists(String locatorXpath, String fieldName) {
        By fieldLocator = AppiumBy.xpath(locatorXpath);
        WebElement element = DriverManager.driver.findElement(fieldLocator);
        boolean isDisplayed = element.isDisplayed();
        String text = element.getText();
        String tagName = element.getTagName();
        System.out.println("✅ " + fieldName + " found - Tag: " + tagName + ", Text: '" + text + "', Displayed: " + isDisplayed);
    }

    public void scrollDown() {
        JavascriptExecutor js = (JavascriptExecutor) DriverManager.driver;
        js.executeScript("mobile: scrollGesture", java.util.Map.of(
                "left", 100,
                "top", 800,
                "width", 200,
                "height", 400,
                "direction", "down",
                "percent", 1.0
        ));
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void verifyBasicInfoFieldsEditability() {
        clickEdit();
        verifyFieldNotEditable("//android.widget.TextView[@resource-id=\"com.fluenthealth.app:id/infoValueTv\" and @text=\"Shubham\"]", "Name field");
        verifyFieldNotEditable("//android.widget.TextView[@resource-id=\"com.fluenthealth.app:id/infoValueTv\" and @text=\"Haryan\"]", "State field");
        verifyFieldNotEditable("//android.widget.TextView[@resource-id=\"com.fluenthealth.app:id/infoValueTv\" and @text=\"02 Oct 1998\"]", "DOB field");
        verifyFieldNotEditable("//android.widget.TextView[@resource-id=\"com.fluenthealth.app:id/infoValueTv\" and @text=\"91-9594260325\"]", "Phone field");
        verifyFieldNotEditable("//android.widget.TextView[@resource-id=\"com.fluenthealth.app:id/infoValueTv\" and @text=\"haryanshubham98@gmail.com\"]", "Email field");

        verifyEditableFields();
    }
}
