package utils;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.options.BaseOptions;
import org.openqa.selenium.Capabilities;
import java.net.URL;
import java.nio.file.Paths;

public class DriverManager {
    public static AndroidDriver driver;

    public static void initDriver() throws Exception {
        // Get the project root directory and construct the APK path
        String appPath = Paths.get(System.getProperty("user.dir"), "app", "FluentHealth.apk").toString();
        
        Capabilities options = new BaseOptions()
                .amend("platformName", "Android")
                .amend("appium:platformVersion", "16")
                .amend("appium:deviceName", "Redmi Note 12")
                .amend("appium:app", appPath)
                .amend("appium:appPackage", "com.fluenthealth.app")
                .amend("appium:automationName", "UiAutomator2")
                .amend("appium:autoGrantPermissions", true)
                .amend("appium:noReset", false)
                .amend("appium:newCommandTimeout", 3600);

        driver = new AndroidDriver(new URL("http://127.0.0.1:4723"), options);
    }
}