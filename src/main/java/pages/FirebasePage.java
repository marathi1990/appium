package pages;

import utils.DriverManager;
import utils.FirebaseManager;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.google.firebase.auth.UserRecord;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * FirebasePage - Page Object for Firebase-related UI flows and backend validations.
 *
 * Bridges Appium UI interactions with Firebase backend checks.
 * Use this page object when a test needs to:
 *   - Perform an action in the app AND verify the result in Firebase
 *   - Validate that app actions persist correctly to Firestore/Firebase Auth
 *   - Check Firebase Auth state after a login/logout flow
 *
 * Example flow:
 *   1. User logs in via the app (Appium)
 *   2. FirebasePage verifies the user exists in Firebase Auth
 *   3. FirebasePage reads the user's Firestore profile and asserts fields
 */
public class FirebasePage {

    private final WebDriverWait wait = new WebDriverWait(DriverManager.driver, Duration.ofSeconds(30));

    // Locators for Firebase Auth-related UI elements
    private final By homeScreenIndicator = AppiumBy.xpath("//*[contains(@text,'Home')]");
    private final By userProfileName    = AppiumBy.id("com.fluenthealth.app:id/userNameTv");
    private final By logoutButton       = AppiumBy.xpath("//*[contains(@text,'Logout') or contains(@text,'Sign Out')]");

    // ─────────────────────────────────────────────
    //  UI helpers
    // ─────────────────────────────────────────────

    /**
     * Wait until the home screen is visible after login.
     * Used as a sync point before running Firebase backend checks.
     */
    public void waitForHomeScreen() {
        wait.until(ExpectedConditions.presenceOfElementLocated(homeScreenIndicator));
        System.out.println("✅ Home screen is visible.");
    }

    /**
     * Get the displayed username from the profile UI, if available.
     *
     * @return Displayed name string, or empty string if element not found
     */
    public String getDisplayedUsername() {
        try {
            WebElement nameEl = wait.until(ExpectedConditions.visibilityOfElementLocated(userProfileName));
            String name = nameEl.getText();
            System.out.println("Displayed username in app: " + name);
            return name;
        } catch (Exception e) {
            System.out.println("Username element not found on screen: " + e.getMessage());
            return "";
        }
    }

    /**
     * Tap the logout button if visible (used in auth-state tests).
     */
    public void tapLogout() {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(logoutButton)).click();
            System.out.println("✅ Tapped logout button.");
        } catch (Exception e) {
            System.out.println("Logout button not found: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    //  Firebase Auth verifications
    // ─────────────────────────────────────────────

    /**
     * After login, verify the user exists in Firebase Authentication by phone number.
     * Converts a 10-digit Indian mobile number to E.164 format (+91XXXXXXXXXX).
     *
     * @param mobileNumber 10-digit phone e.g. "9594260325"
     * @return true if Firebase Auth record found and account is active
     */
    public boolean verifyUserInFirebaseAuth(String mobileNumber) {
        String e164Phone = toE164(mobileNumber);
        System.out.println("Checking Firebase Auth for: " + e164Phone);
        return FirebaseManager.verifyUserExistsInFirebaseAuth(e164Phone);
    }

    /**
     * Verify the user's account is not disabled in Firebase Auth.
     *
     * @param mobileNumber 10-digit phone number
     * @return true if account is enabled
     */
    public boolean verifyUserAccountEnabled(String mobileNumber) {
        String e164Phone = toE164(mobileNumber);
        return FirebaseManager.isUserAccountEnabled(e164Phone);
    }

    /**
     * Get the Firebase UID for a user by phone number.
     * Returns null if user not found.
     *
     * @param mobileNumber 10-digit phone number
     * @return Firebase UID string or null
     */
    public String getFirebaseUid(String mobileNumber) {
        try {
            String e164Phone = toE164(mobileNumber);
            UserRecord user = FirebaseManager.getUserByPhone(e164Phone);
            String uid = user.getUid();
            System.out.println("Firebase UID for " + mobileNumber + ": " + uid);
            return uid;
        } catch (Exception e) {
            System.out.println("Could not retrieve Firebase UID: " + e.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────────
    //  Firestore profile verifications
    // ─────────────────────────────────────────────

    /**
     * Verify the user's Firestore profile document exists.
     *
     * @param uid Firebase UID
     * @return true if document exists in the "users" collection
     */
    public boolean verifyFirestoreProfileExists(String uid) {
        try {
            boolean exists = FirebaseManager.documentExists("users", uid);
            if (exists) {
                System.out.println("✅ Firestore profile document exists for UID: " + uid);
            } else {
                System.out.println("❌ No Firestore profile document for UID: " + uid);
            }
            return exists;
        } catch (ExecutionException | InterruptedException e) {
            System.out.println("Error checking Firestore profile: " + e.getMessage());
            return false;
        }
    }

    /**
     * Read all fields from the user's Firestore profile.
     *
     * @param uid Firebase UID
     * @return Map of Firestore document fields, or null if not found
     */
    public Map<String, Object> getFirestoreProfile(String uid) {
        try {
            return FirebaseManager.readDocument("users", uid);
        } catch (ExecutionException | InterruptedException e) {
            System.out.println("Error reading Firestore profile: " + e.getMessage());
            return null;
        }
    }

    /**
     * Verify a specific field in the user's Firestore profile matches the expected value.
     *
     * @param uid           Firebase UID
     * @param fieldName     Firestore field name e.g. "email", "name"
     * @param expectedValue Expected value
     * @return true if field value matches
     */
    public boolean verifyFirestoreField(String uid, String fieldName, Object expectedValue) {
        try {
            return FirebaseManager.verifyFieldValue("users", uid, fieldName, expectedValue);
        } catch (ExecutionException | InterruptedException e) {
            System.out.println("Error verifying Firestore field: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verify the phone number stored in Firestore matches what the user logged in with.
     *
     * @param uid           Firebase UID
     * @param mobileNumber  10-digit mobile number
     * @return true if Firestore phone matches
     */
    public boolean verifyFirestorePhoneNumber(String uid, String mobileNumber) {
        // Firestore may store phone as "9594260325" or "+919594260325" — check both
        try {
            Object storedPhone = FirebaseManager.getFieldValue("users", uid, "phone");
            if (storedPhone == null) {
                storedPhone = FirebaseManager.getFieldValue("users", uid, "phoneNumber");
            }
            if (storedPhone == null) {
                storedPhone = FirebaseManager.getFieldValue("users", uid, "mobile");
            }
            if (storedPhone == null) {
                System.out.println("❌ No phone field found in Firestore for UID: " + uid);
                return false;
            }
            String stored = storedPhone.toString();
            boolean matches = stored.contains(mobileNumber);
            System.out.println(matches
                    ? "✅ Firestore phone matches: " + stored
                    : "❌ Firestore phone mismatch. Stored: " + stored + ", Expected to contain: " + mobileNumber);
            return matches;
        } catch (ExecutionException | InterruptedException e) {
            System.out.println("Error verifying Firestore phone: " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────
    //  Health data verifications (Firestore)
    // ─────────────────────────────────────────────

    /**
     * Verify that health data for the user exists in Firestore.
     * Assumes health data is stored under a "healthData" collection with UID as document ID.
     *
     * @param uid Firebase UID
     * @return true if health data document exists
     */
    public boolean verifyHealthDataExistsInFirestore(String uid) {
        try {
            boolean exists = FirebaseManager.documentExists("healthData", uid);
            System.out.println(exists
                    ? "✅ Health data exists in Firestore for UID: " + uid
                    : "❌ No health data in Firestore for UID: " + uid);
            return exists;
        } catch (ExecutionException | InterruptedException e) {
            System.out.println("Error verifying health data: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verify a specific health field value in Firestore.
     *
     * @param uid           Firebase UID
     * @param fieldName     Health field e.g. "name", "dateOfBirth", "state"
     * @param expectedValue Expected value
     * @return true if field matches
     */
    public boolean verifyHealthDataField(String uid, String fieldName, Object expectedValue) {
        try {
            return FirebaseManager.verifyFieldValue("healthData", uid, fieldName, expectedValue);
        } catch (ExecutionException | InterruptedException e) {
            System.out.println("Error verifying health data field: " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────
    //  Utility
    // ─────────────────────────────────────────────

    /**
     * Convert a 10-digit Indian mobile number to E.164 format.
     * e.g. "9594260325" → "+919594260325"
     * If already in E.164 format, returns as-is.
     *
     * @param mobile Mobile number string
     * @return E.164 formatted phone number
     */
    private String toE164(String mobile) {
        if (mobile == null) return "";
        String cleaned = mobile.trim();
        if (cleaned.startsWith("+")) return cleaned;
        if (cleaned.startsWith("91") && cleaned.length() == 12) return "+" + cleaned;
        if (cleaned.length() == 10) return "+91" + cleaned;
        return "+" + cleaned;
    }
}
