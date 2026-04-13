package utils;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.FirestoreClient;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.DocumentReference;
import com.google.api.core.ApiFuture;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * FirebaseManager - Utility class for Firebase Admin SDK operations.
 *
 * Provides methods to:
 * - Initialize Firebase app using a service account
 * - Verify users in Firebase Authentication (by phone/UID)
 * - Read and write documents in Firestore
 * - Validate data synced from the app to Firebase backend
 *
 * Setup:
 *   1. Download your Firebase service account JSON from:
 *      Firebase Console → Project Settings → Service Accounts → Generate new private key
 *   2. Place it at: src/test/resources/firebase-service-account.json
 *      OR set the environment variable FIREBASE_SERVICE_ACCOUNT_PATH to the full path.
 *   3. Set FIREBASE_DATABASE_URL to your project's Realtime Database URL (if used).
 */
public class FirebaseManager {

    private static FirebaseApp firebaseApp;
    private static final String SERVICE_ACCOUNT_ENV = "FIREBASE_SERVICE_ACCOUNT_PATH";
    private static final String DATABASE_URL_ENV = "FIREBASE_DATABASE_URL";
    private static final String DEFAULT_SERVICE_ACCOUNT_PATH = "src/test/resources/firebase-service-account.json";

    /**
     * Initialize Firebase Admin SDK.
     * Reads service account credentials from environment variable or default path.
     * Safe to call multiple times - skips if already initialized.
     */
    public static void initFirebase() throws IOException {
        if (firebaseApp != null) {
            System.out.println("Firebase already initialized, skipping.");
            return;
        }

        String serviceAccountPath = System.getenv(SERVICE_ACCOUNT_ENV);
        if (serviceAccountPath == null || serviceAccountPath.isEmpty()) {
            serviceAccountPath = Paths.get(System.getProperty("user.dir"), DEFAULT_SERVICE_ACCOUNT_PATH).toString();
        }

        String databaseUrl = System.getenv(DATABASE_URL_ENV);

        System.out.println("Initializing Firebase with service account: " + serviceAccountPath);

        try (InputStream serviceAccount = new FileInputStream(serviceAccountPath)) {
            FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount));

            if (databaseUrl != null && !databaseUrl.isEmpty()) {
                optionsBuilder.setDatabaseUrl(databaseUrl);
            }

            FirebaseOptions options = optionsBuilder.build();
            firebaseApp = FirebaseApp.initializeApp(options);
            System.out.println("✅ Firebase initialized successfully. Project: " + firebaseApp.getName());
        }
    }

    /**
     * Shut down Firebase app and release resources.
     * Call this in @AfterAll or test teardown.
     */
    public static void shutdownFirebase() {
        if (firebaseApp != null) {
            firebaseApp.delete();
            firebaseApp = null;
            System.out.println("Firebase app shut down.");
        }
    }

    // ─────────────────────────────────────────────
    //  Firebase Authentication helpers
    // ─────────────────────────────────────────────

    /**
     * Fetch a Firebase Auth user record by phone number.
     * Phone number must be in E.164 format e.g. "+919594260325".
     *
     * @param phoneNumber Phone in E.164 format
     * @return UserRecord if found
     * @throws FirebaseAuthException if user is not found or auth fails
     */
    public static UserRecord getUserByPhone(String phoneNumber) throws FirebaseAuthException {
        UserRecord user = FirebaseAuth.getInstance().getUserByPhoneNumber(phoneNumber);
        System.out.println("✅ Firebase Auth user found - UID: " + user.getUid() + ", Phone: " + user.getPhoneNumber());
        return user;
    }

    /**
     * Fetch a Firebase Auth user record by UID.
     *
     * @param uid Firebase UID
     * @return UserRecord
     * @throws FirebaseAuthException if not found
     */
    public static UserRecord getUserByUid(String uid) throws FirebaseAuthException {
        UserRecord user = FirebaseAuth.getInstance().getUser(uid);
        System.out.println("✅ Firebase Auth user found - UID: " + user.getUid());
        return user;
    }

    /**
     * Verify that a user with the given phone number exists in Firebase Auth.
     *
     * @param phoneNumber E.164 phone number e.g. "+919594260325"
     * @return true if user exists
     */
    public static boolean verifyUserExistsInFirebaseAuth(String phoneNumber) {
        try {
            UserRecord user = getUserByPhone(phoneNumber);
            return user != null && !user.isDisabled();
        } catch (FirebaseAuthException e) {
            System.out.println("❌ User not found in Firebase Auth for phone: " + phoneNumber + " | Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if a Firebase Auth user account is enabled (not disabled).
     *
     * @param phoneNumber E.164 phone number
     * @return true if account is active and enabled
     */
    public static boolean isUserAccountEnabled(String phoneNumber) {
        try {
            UserRecord user = getUserByPhone(phoneNumber);
            boolean enabled = !user.isDisabled();
            System.out.println("User account enabled: " + enabled);
            return enabled;
        } catch (FirebaseAuthException e) {
            System.out.println("❌ Could not check account status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Create a custom token for a given UID (useful for test auth flows).
     *
     * @param uid Firebase UID
     * @return Custom token string
     * @throws FirebaseAuthException on error
     */
    public static String createCustomToken(String uid) throws FirebaseAuthException {
        String token = FirebaseAuth.getInstance().createCustomToken(uid);
        System.out.println("✅ Custom token created for UID: " + uid);
        return token;
    }

    // ─────────────────────────────────────────────
    //  Firestore helpers
    // ─────────────────────────────────────────────

    /**
     * Get the Firestore instance.
     *
     * @return Firestore client
     */
    public static Firestore getFirestore() {
        return FirestoreClient.getFirestore();
    }

    /**
     * Read a Firestore document and return its data as a Map.
     *
     * @param collection Firestore collection name e.g. "users"
     * @param documentId Document ID (typically the Firebase UID)
     * @return Map of field → value, or null if document does not exist
     */
    public static Map<String, Object> readDocument(String collection, String documentId)
            throws ExecutionException, InterruptedException {
        Firestore db = getFirestore();
        DocumentReference docRef = db.collection(collection).document(documentId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot snapshot = future.get();

        if (snapshot.exists()) {
            System.out.println("✅ Document found: " + collection + "/" + documentId);
            return snapshot.getData();
        } else {
            System.out.println("❌ Document not found: " + collection + "/" + documentId);
            return null;
        }
    }

    /**
     * Get a single field value from a Firestore document.
     *
     * @param collection   Firestore collection name
     * @param documentId   Document ID
     * @param fieldName    Field to retrieve
     * @return Field value as Object, or null if not found
     */
    public static Object getFieldValue(String collection, String documentId, String fieldName)
            throws ExecutionException, InterruptedException {
        Map<String, Object> data = readDocument(collection, documentId);
        if (data == null) return null;
        Object value = data.get(fieldName);
        System.out.println("Field '" + fieldName + "': " + value);
        return value;
    }

    /**
     * Write / update fields in a Firestore document.
     * Uses merge=true so it won't overwrite unrelated fields.
     *
     * @param collection Firestore collection name
     * @param documentId Document ID
     * @param data       Map of field → value to write
     */
    public static void writeDocument(String collection, String documentId, Map<String, Object> data)
            throws ExecutionException, InterruptedException {
        Firestore db = getFirestore();
        ApiFuture<com.google.cloud.firestore.WriteResult> future =
                db.collection(collection).document(documentId).set(data,
                        com.google.cloud.firestore.SetOptions.merge());
        com.google.cloud.firestore.WriteResult result = future.get();
        System.out.println("✅ Document written at: " + result.getUpdateTime());
    }

    /**
     * Verify that a Firestore document exists for the given user UID.
     *
     * @param collection Firestore collection name
     * @param uid        Firebase UID
     * @return true if document exists
     */
    public static boolean documentExists(String collection, String uid)
            throws ExecutionException, InterruptedException {
        Map<String, Object> data = readDocument(collection, uid);
        return data != null;
    }

    /**
     * Verify a specific Firestore field matches the expected value.
     *
     * @param collection    Collection name
     * @param documentId    Document ID
     * @param fieldName     Field to check
     * @param expectedValue Expected value
     * @return true if field value equals expected
     */
    public static boolean verifyFieldValue(String collection, String documentId,
                                           String fieldName, Object expectedValue)
            throws ExecutionException, InterruptedException {
        Object actualValue = getFieldValue(collection, documentId, fieldName);
        if (actualValue == null) {
            System.out.println("❌ Field '" + fieldName + "' is null/missing in document.");
            return false;
        }
        boolean matches = actualValue.equals(expectedValue);
        if (matches) {
            System.out.println("✅ Field '" + fieldName + "' matches expected value: " + expectedValue);
        } else {
            System.out.println("❌ Field '" + fieldName + "' mismatch. Expected: " + expectedValue + ", Actual: " + actualValue);
        }
        return matches;
    }
}
