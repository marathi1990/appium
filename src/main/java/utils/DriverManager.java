package utils;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class DriverManager {
    public static AndroidDriver driver;

    public static void initDriver() throws Exception {
        String appPath     = resolveApkPath();
        String deviceName  = env("APPIUM_DEVICE_NAME",     "Redmi Note 12");
        String platformVer = env("APPIUM_PLATFORM_VERSION", "17");
        String appiumUrl   = env("APPIUM_SERVER_URL",       "http://127.0.0.1:4723");

        UiAutomator2Options options = new UiAutomator2Options()
                .setPlatformVersion(platformVer)
                .setDeviceName(deviceName)
                .setApp(appPath)
                .setAppPackage("io.appium.android.apis")
                .setAutoGrantPermissions(true)
                .setNoReset(false)
                .setNewCommandTimeout(java.time.Duration.ofSeconds(3600));

        driver = new AndroidDriver(URI.create(appiumUrl).toURL(), options);
    }

    /**
     * Resolves the APK path with the following priority:
     *
     * 1. APP_PATH env var  — set by CI (GitHub Actions) pointing to the LFS APK.
     * 2. Local app/ dir    — any APK already on disk (Git LFS pull or manual copy).
     * 3. Firebase fallback — downloads the latest build from Firebase App Distribution.
     *                        (Firebase config is kept intact for future use.)
     */
    private static String resolveApkPath() throws Exception {
        // 1. Explicit path from environment (CI sets this to the LFS-pulled APK)
        String envPath = System.getenv("APP_PATH");
        if (envPath != null && !envPath.isEmpty() && Files.exists(Paths.get(envPath))) {
            System.out.println("[DriverManager] Using APK from APP_PATH: " + envPath);
            return envPath;
        }

        // 2. First APK found in the project's app/ directory
        Path appDir = Paths.get(System.getProperty("user.dir"), "app");
        if (Files.exists(appDir)) {
            Optional<Path> localApk = Files.walk(appDir, 1)
                    .filter(p -> p.toString().endsWith(".apk"))
                    .findFirst();
            if (localApk.isPresent()) {
                String path = localApk.get().toAbsolutePath().toString();
                System.out.println("[DriverManager] Using local APK: " + path);
                return path;
            }
        }

        // 3. Firebase App Distribution (fallback — config preserved for future use)
        System.out.println("[DriverManager] No local APK found — downloading from Firebase App Distribution...");
        return FirebaseAppDistributionManager.downloadLatestApk(
                FirebaseAppDistributionManager.APP_ID_IO_APPIUM_APIS);
    }

    private static String env(String key, String defaultValue) {
        String val = System.getenv(key);
        return (val != null && !val.isEmpty()) ? val : defaultValue;
    }
}
