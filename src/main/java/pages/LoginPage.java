package pages;

import utils.DriverManager;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;

public class LoginPage {

    WebDriverWait wait = new WebDriverWait(DriverManager.driver, Duration.ofSeconds(20));

    // Landing Screen Login Text
    private By landingLoginText = AppiumBy.xpath("//*[contains(@text,'Log')]");

    // Mobile Screen
    private By mobileField = AppiumBy.id("com.fluenthealth.app:id/phoneET");

    private By continueBtn = AppiumBy.id("com.fluenthealth.app:id/button");

    // Pin Screen
    private By pinField = AppiumBy.id("com.fluenthealth.app:id/pinCode");

    private By pinBoxes = AppiumBy.className("android.widget.EditText");

    private By loginBtn = AppiumBy.id("com.fluenthealth.app:id/loginBtn");

    public void clickLoginFromLanding() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(landingLoginText)).click();
    }

    public void enterMobileNumber(String number) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(mobileField)).sendKeys(number);
    }

    public void clickContinue() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(continueBtn)).click();
    }

    public void enterPin(String pin) {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(pinField)).sendKeys(pin);
        } catch (Exception e) {
            wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(pinBoxes));
            for (int i = 0; i < pin.length(); i++) {
                DriverManager.driver.findElements(pinBoxes)
                        .get(i)
                        .sendKeys(String.valueOf(pin.charAt(i)));
            }
        }
    }

    public void clickFinalLogin() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(loginBtn)).click();
    }

    public void loginWithPin(String mobile, String pin) {
        clickLoginFromLanding();

        enterMobileNumber(mobile);
        clickContinue();
        enterPin(pin);
        clickFinalLogin();
    }
}
