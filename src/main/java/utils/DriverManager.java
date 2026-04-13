package utils;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.options.BaseOptions;
import org.openqa.selenium.Capabilities;
import java.net.URL;

public class DriverManager {
    public static AndroidDriver driver;

    public static void initDriver() throws Exception {
        String appPath = FirebaseAppDistributionManager.downloadLatestApk(FirebaseAppDistributionManager.APP_ID_IO_APPIUM_APIS);

        Capabilities options = new BaseOptions()
                .amend("platformName", "Android")
                .amend("appium:platformVersion", "17")
                .amend("appium:deviceName", "Redmi Note 12")
                .amend("appium:app", appPath)
                .amend("appium:appPackage", "io.appium.android.apis")
                .amend("appium:automationName", "UiAutomator2")
                .amend("appium:autoGrantPermissions", true)
                .amend("appium:noReset", false)
                .amend("appium:newCommandTimeout", 3600);

        driver = new AndroidDriver(new URL("http://127.0.0.1:4723"), options);
    }
}
