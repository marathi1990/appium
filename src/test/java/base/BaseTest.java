package base;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import utils.DriverManager;
import utils.FirebaseManager;

/**
 * BaseTest - Base class for all Appium tests with Firebase integration.
 *
 * Setup:1
 * 1. Initializes Firebase Admin SDK once per test class (BeforeAll)
 * 2. Initializes Appium driver before each test (BeforeEach)
 * 3. Cleans up driver after each test (AfterEach)
 * 4. Shuts down Firebase after all tests (AfterAll)
 *
 * Firebase Configuration:
 * - Set FIREBASE_SERVICE_ACCOUNT_PATH environment variable to your service account JSON
 * - Or place firebase-service-account.json at src/test/resources/
 *
 * Firebase App Distribution Configuration:
 * - Set FIREBASE_APP_ID environment variable to your app ID
 * - Format: 1:PROJECT_NUMBER:android:HASH
 * - If not set, uses local APK from app/FluentHealth.apk
 */
public class BaseTest {

    private static boolean firebaseInitialized = false;

    /**
     * Initialize Firebase once per test class.
     * Only runs once even if multiple test methods exist.
     */
    @BeforeAll
    public static void initializeFirebase() throws Exception {
        if (!firebaseInitialized) {
            try {
                System.out.println("Initializing Firebase Admin SDK...");
                FirebaseManager.initFirebase();
                firebaseInitialized = true;
                System.out.println("✅ Firebase initialized successfully");
            } catch (Exception e) {
                System.out.println("⚠️  Firebase initialization failed - continuing without Firebase");
                System.out.println("   Error: " + e.getMessage());
                firebaseInitialized = false;
                // Don't throw - let tests continue with Appium only
            }
        }
    }

    /**
     * Initialize Appium driver before each test.
     * Downloads latest APK from Firebase if FIREBASE_APP_ID is set.
     */
    @BeforeEach
    public void setUp() throws Exception {
        System.out.println();
        System.out.println("Starting test - Initializing Appium driver...");
        DriverManager.initDriver();
        System.out.println("✅ Appium driver ready");
    }

    /**
     * Clean up Appium driver after each test.
     */
    @AfterEach
    public void tearDown() {
        try {
            if (DriverManager.driver != null) {
                DriverManager.driver.quit();
                System.out.println("✅ Appium driver closed");
            }
        } catch (Exception e) {
            System.err.println("Error closing driver: " + e.getMessage());
        }
    }

    /**
     * Shut down Firebase after all tests complete.
     */
    @AfterAll
    public static void shutdownFirebase() {
        try {
            if (firebaseInitialized) {
                FirebaseManager.shutdownFirebase();
                System.out.println("✅ Firebase shut down");
            }
        } catch (Exception e) {
            System.err.println("Error shutting down Firebase: " + e.getMessage());
        }
    }
}
