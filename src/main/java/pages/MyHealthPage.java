package pages;

import utils.DriverManager;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.Point;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.Arrays;
import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class MyHealthPage {

    WebDriverWait wait = new WebDriverWait(DriverManager.driver, Duration.ofSeconds(20));

    // Navigation elements
    private By basicInfoSection = AppiumBy.xpath("//*[contains(@text,'Basic Info') or contains(@text,'Profile') or contains(@text,'Info') or @resource-id='*basic*' or @resource-id='*info*']");

    // UiSelector-based locators
    private By uiSelectorProfileAddPhoto = AppiumBy.androidUIAutomator("new UiSelector().resourceId(\"com.fluenthealth.app:id/profileAddPhotoIV\")");
    private By uiSelectorChooseFromGallery = AppiumBy.androidUIAutomator("new UiSelector().text(\"Choose from Gallery\")");
    private By uiSelectorButtonOnce = AppiumBy.androidUIAutomator("new UiSelector().resourceId(\"android:id/button_once\")");
    private By photosTextViewXpath = AppiumBy.xpath("//android.widget.TextView[@resource-id=\"android:id/text1\" and @text=\"Photos\"]");
    private By uiSelectorPhotosText = AppiumBy.androidUIAutomator("new UiSelector().text(\"Photos\")");
    private By uiSelectorSpecificPhoto = AppiumBy.androidUIAutomator("new UiSelector().className(\"android.widget.ImageView\").instance(0)");

    public void navigateToMyHealth() {
        // First try exact match for "My Health"
        By exactMyHealth = AppiumBy.xpath("//*[@text='My Health']");
        wait.until(ExpectedConditions.elementToBeClickable(exactMyHealth)).click();
        System.out.println("Successfully clicked exact 'My Health' tab");
    }

    public void navigateToBasicInfo() {
        wait.until(ExpectedConditions.elementToBeClickable(basicInfoSection)).click();
        System.out.println("Successfully navigated to Basic Info");
    }

    public void clickPhotosTextView() {
        wait.until(ExpectedConditions.elementToBeClickable(photosTextViewXpath)).click();
        System.out.println("Successfully clicked Photos TextView");
    }

    public void clickPhotosUiSelector() {
        wait.until(ExpectedConditions.elementToBeClickable(uiSelectorPhotosText)).click();
        System.out.println("Successfully clicked Photos via UiSelector");
    }

    public void clickSpecificPhoto() {
        wait.until(ExpectedConditions.elementToBeClickable(uiSelectorSpecificPhoto)).click();
        System.out.println("Successfully clicked specific photo via UiSelector");
    }

    public void performTouchAction(int x, int y) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Point tapPoint = new Point(x, y);
        Sequence tap = new Sequence(finger, 1);
        tap.addAction(finger.createPointerMove(Duration.ofMillis(0),
                PointerInput.Origin.viewport(), tapPoint.x, tapPoint.y));
        tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        tap.addAction(new Pause(finger, Duration.ofMillis(50)));
        tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        DriverManager.driver.perform(Arrays.asList(tap));
        System.out.println("Successfully performed touch action at coordinates: " + x + ", " + y);
    }

    public void uploadImageFromGallery() {
        System.out.println("Starting image upload process...");

        navigateToMyHealth();
        navigateToBasicInfo();

        wait.until(ExpectedConditions.elementToBeClickable(uiSelectorProfileAddPhoto)).click();
        System.out.println("Clicked profileAddPhotoIV via UiSelector");

        wait.until(ExpectedConditions.elementToBeClickable(uiSelectorChooseFromGallery)).click();
        System.out.println("Clicked Choose from Gallery text via UiSelector");

        clickPhotosTextView();
        System.out.println("Clicked Photos TextView");

        wait.until(ExpectedConditions.elementToBeClickable(uiSelectorButtonOnce)).click();
        System.out.println("Clicked button_once via UiSelector");

        clickPhotosUiSelector();
        System.out.println("Clicked Photos via UiSelector");

        clickSpecificPhoto();
        System.out.println("Clicked specific photo with timestamp description");

        System.out.println("Image upload workflow completed.");
    }

    public boolean isImageUploaded() {
        By profilePictureSelector = AppiumBy.androidUIAutomator("new UiSelector().resourceId(\"com.fluenthealth.app:id/profilePictureSiv\")");
        boolean isDisplayed = wait.until(ExpectedConditions.visibilityOfElementLocated(profilePictureSelector)).isDisplayed();
        System.out.println("Profile picture verified - image uploaded successfully");
        return isDisplayed;
    }

    /**
     * Capture screenshot of the profile picture element
     * @return File containing the screenshot
     */
    public File captureProfilePictureScreenshot() {
        By profilePictureSelector = AppiumBy.androidUIAutomator("new UiSelector().resourceId(\"com.fluenthealth.app:id/profilePictureSiv\")");
        WebElement profileImage = wait.until(ExpectedConditions.visibilityOfElementLocated(profilePictureSelector));

        File screenshot = profileImage.getScreenshotAs(OutputType.FILE);
        System.out.println("Profile picture screenshot captured: " + screenshot.getAbsolutePath());
        return screenshot;
    }

    /**
     * Save screenshot to a specific location for comparison
     * @param sourceFile Source screenshot file
     * @param destinationPath Destination path
     * @return true if saved successfully
     */
    public boolean saveScreenshot(File sourceFile, String destinationPath) throws IOException {
        if (sourceFile == null || !sourceFile.exists()) {
            System.out.println("Source file does not exist");
            return false;
        }

        File destinationFile = new File(destinationPath);
        org.apache.commons.io.FileUtils.copyFile(sourceFile, destinationFile);
        System.out.println("Screenshot saved to: " + destinationPath);
        return true;
    }

    /**
     * Resize BufferedImage to specified dimensions
     * @param img Source image
     * @param width Target width
     * @param height Target height
     * @return Resized image
     */
    private BufferedImage resizeImage(BufferedImage img, int width, int height) {
        java.awt.Image scaledImage = img.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(scaledImage, 0, 0, null);
        g2d.dispose();
        return resized;
    }

    /**
     * Compare two images with tolerance for pixel differences
     * Handles resolution differences, cropping, and resizing
     * @param expectedImagePath Path to expected image
     * @param actualImage BufferedImage of actual screenshot
     * @param tolerancePercent Tolerance percentage (0-100, default 5%)
     * @return true if images match within tolerance
     */
    public boolean compareImages(String expectedImagePath, BufferedImage actualImage, double tolerancePercent) throws IOException {
        BufferedImage expectedImage = ImageIO.read(new File(expectedImagePath));

        if (expectedImage == null || actualImage == null) {
            System.out.println("Expected or actual image is null");
            return false;
        }

        // Resize expected image to match actual image dimensions if different
        if (expectedImage.getWidth() != actualImage.getWidth() || expectedImage.getHeight() != actualImage.getHeight()) {
            System.out.println("Image dimensions differ. Expected: " + expectedImage.getWidth() + "x" + expectedImage.getHeight() +
                    ", Actual: " + actualImage.getWidth() + "x" + actualImage.getHeight());
            expectedImage = resizeImage(expectedImage, actualImage.getWidth(), actualImage.getHeight());
            System.out.println("Expected image resized to match actual dimensions");
        }

        // Compare images pixel by pixel with tolerance
        int diffPixels = 0;
        int totalPixels = actualImage.getWidth() * actualImage.getHeight();
        double maxDiffPixels = totalPixels * (tolerancePercent / 100.0);

        for (int y = 0; y < actualImage.getHeight(); y++) {
            for (int x = 0; x < actualImage.getWidth(); x++) {
                int expectedRGB = expectedImage.getRGB(x, y);
                int actualRGB = actualImage.getRGB(x, y);

                if (expectedRGB != actualRGB) {
                    diffPixels++;
                }
            }
        }

        double diffPercentage = (diffPixels / (double) totalPixels) * 100.0;

        System.out.println("Image Comparison Results:");
        System.out.println("  Total Pixels: " + totalPixels);
        System.out.println("  Different Pixels: " + diffPixels);
        System.out.println("  Difference Percentage: " + String.format("%.2f", diffPercentage) + "%");
        System.out.println("  Tolerance: " + tolerancePercent + "%");

        if (diffPercentage <= tolerancePercent) {
            System.out.println("✅ Images MATCH within tolerance!");
            return true;
        } else {
            System.out.println("❌ Images DIFFER beyond tolerance!");
            return false;
        }
    }

    /**
     * Compare images with default 5% tolerance
     * @param expectedImagePath Path to expected image
     * @param actualImage Actual screenshot
     * @return true if images match within 5% tolerance
     */
    public boolean compareImages(String expectedImagePath, BufferedImage actualImage) throws IOException {
        return compareImages(expectedImagePath, actualImage, 5.0);
    }

    /**
     * Full image comparison workflow - capture and compare
     * @param expectedImagePath Path to expected profile image
     * @param savePath Optional path to save actual screenshot (null to skip saving)
     * @param tolerancePercent Tolerance percentage
     * @return true if images match within tolerance
     */
    public boolean verifyProfileImageUpload(String expectedImagePath, String savePath, double tolerancePercent) throws IOException {
        File actualScreenshot = captureProfilePictureScreenshot();

        if (actualScreenshot == null) {
            System.out.println("Failed to capture profile picture screenshot");
            return false;
        }

        // Save actual screenshot if path provided
        if (savePath != null) {
            saveScreenshot(actualScreenshot, savePath);
        }

        // Read actual image
        BufferedImage actualImage = ImageIO.read(actualScreenshot);
        return compareImages(expectedImagePath, actualImage, tolerancePercent);
    }

    /**
     * Click on the edit profile button in basic info section
     */
    public void clickEditProfileButton() {

        By editButton = AppiumBy.id("com.fluenthealth.app:id/editProfileTv");
        //By editButton = AppiumBy.androidUIAutomator("new UiSelector().resourceId(\"com.fluenthealth.app:id/editProfileTv\")");
        try {
            WebElement button = wait.until(ExpectedConditions.elementToBeClickable(editButton));
            button.click();
            System.out.println("Clicked on edit profile button");
        } catch (Exception e) {
            System.out.println("Edit Profile button not clickable directly: " + e.getMessage());
            System.out.println("Trying fallback scroll and retry...");
            try {
                // fallback swipe and retry click
                performTouchAction(540, 1700);
                WebElement button = wait.until(ExpectedConditions.elementToBeClickable(editButton));
                button.click();
                System.out.println("Clicked on edit profile button after fallback scroll");
            } catch (Exception ex) {
                System.out.println("Fallback failed: " + ex.getMessage());
                throw new RuntimeException("Unable to click edit profile button", ex);
            }
        }
    }
}
