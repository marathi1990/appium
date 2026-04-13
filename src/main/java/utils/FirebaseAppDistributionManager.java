package utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;

/**
 * FirebaseAppDistributionManager - Downloads the latest APK from Firebase App Distribution.
 *
 * Setup:
 *   1. Service account JSON must be at src/test/resources/firebase-service-account.json
 *      OR set env var FIREBASE_SERVICE_ACCOUNT_PATH.
 *   2. Set env var FIREBASE_APP_ID to your Firebase App ID.
 *      Format: "1:PROJECT_NUMBER:android:APP_HASH"
 *      Find it: Firebase Console → Project Settings → Your apps → App ID
 *
 * The service account must have the "Firebase App Distribution Admin" role.
 */
public class FirebaseAppDistributionManager {

    private static final String SERVICE_ACCOUNT_ENV = "FIREBASE_SERVICE_ACCOUNT_PATH";
    private static final String DEFAULT_SERVICE_ACCOUNT_PATH = "src/test/resources/firebase-service-account.json";
    private static final String FIREBASE_APP_ID_ENV = "FIREBASE_APP_ID";

    // App IDs from google-services.json (project: testing-automation-5752f, number: 201489390325)
    public static final String APP_ID_IO_APPIUM_APIS  = "1:201489390325:android:f73421aeb25fd3b4c403bc"; // io.appium.android.apis
    public static final String APP_ID_IO_APPIUM_APIS1 = "1:201489390325:android:8a28dbdcae7d12cec403bc"; // io.appium.android.apis1

    // Default App ID used when FIREBASE_APP_ID env var is not set
    private static final String DEFAULT_APP_ID = APP_ID_IO_APPIUM_APIS;

    // Firebase App Distribution REST API base URL
    private static final String FAD_BASE_URL = "https://firebaseappdistribution.googleapis.com/v1";

    // OAuth2 scope required to call Firebase App Distribution API
    private static final String CLOUD_PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform";

    /**
     * Downloads the latest APK from Firebase App Distribution.
     * Saves it to the app/ directory in the project root.
     *
     * @return absolute path to the downloaded APK file
     * @throws Exception if any step fails
     */
    public static String downloadLatestApk() throws Exception {
        String firebaseAppId = System.getenv(FIREBASE_APP_ID_ENV);
        if (firebaseAppId == null || firebaseAppId.isEmpty()) {
            System.out.println("FIREBASE_APP_ID env var not set — using default App ID from google-services.json.");
            firebaseAppId = DEFAULT_APP_ID;
        }
        return downloadLatestApk(firebaseAppId);
    }

    public static String downloadLatestApk(String firebaseAppId) throws Exception {
        System.out.println("Fetching latest release from Firebase App Distribution...");
        System.out.println("App ID: " + firebaseAppId);

        // Extract project number from the App ID (format: 1:PROJECT_NUMBER:android:HASH)
        String projectNumber = extractProjectNumber(firebaseAppId);

        // Get OAuth2 access token from service account
        String accessToken = getAccessToken();

        // Call the Firebase App Distribution API to list latest release
        String releasesUrl = FAD_BASE_URL + "/projects/" + projectNumber
                + "/apps/" + firebaseAppId
                + "/releases?orderBy=createTime%20desc&pageSize=1";

        System.out.println("Calling Firebase App Distribution API...");
        String responseJson = httpGet(releasesUrl, accessToken);

        // Parse the download URL from the response
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(responseJson);
        JsonNode releases = root.get("releases");

        if (releases == null || releases.isEmpty()) {
            throw new RuntimeException(
                "No releases found in Firebase App Distribution for app: " + firebaseAppId +
                ". Make sure you have uploaded at least one APK to Firebase App Distribution."
            );
        }

        JsonNode latestRelease = releases.get(0);
        String releaseName = latestRelease.path("name").asText("unknown");
        String displayVersion = latestRelease.path("displayVersion").asText("unknown");
        String buildVersion = latestRelease.path("buildVersion").asText("unknown");
        String binaryDownloadUri = latestRelease.path("binaryDownloadUri").asText(null);

        if (binaryDownloadUri == null || binaryDownloadUri.isEmpty()) {
            throw new RuntimeException(
                "No binaryDownloadUri found in the latest release (" + releaseName + "). " +
                "Ensure the service account has the 'Firebase App Distribution Admin' role."
            );
        }

        System.out.println("Latest release found:");
        System.out.println("  Name:    " + releaseName);
        System.out.println("  Version: " + displayVersion + " (" + buildVersion + ")");

        // Download the APK
        String apkFileName = "FluentHealth-" + displayVersion + "-" + buildVersion + ".apk";
        Path apkDir = Paths.get(System.getProperty("user.dir"), "app");
        Files.createDirectories(apkDir);
        Path apkPath = apkDir.resolve(apkFileName);

        // Skip download if this exact version is already cached
        if (Files.exists(apkPath)) {
            System.out.println("APK already cached at: " + apkPath);
            return apkPath.toString();
        }

        System.out.println("Downloading APK to: " + apkPath);
        downloadFile(binaryDownloadUri, apkPath);
        System.out.println("APK downloaded successfully: " + apkPath);

        return apkPath.toString();
    }

    // ─────────────────────────────────────────────
    //  Private helpers
    // ─────────────────────────────────────────────

    private static String extractProjectNumber(String firebaseAppId) {
        // App ID format: "1:PROJECT_NUMBER:android:HASH"
        String[] parts = firebaseAppId.split(":");
        if (parts.length < 2) {
            throw new IllegalArgumentException(
                "Invalid FIREBASE_APP_ID format: '" + firebaseAppId +
                "'. Expected format: 1:PROJECT_NUMBER:android:HASH"
            );
        }
        return parts[1];
    }

    private static String getAccessToken() throws IOException {
        String serviceAccountPath = System.getenv(SERVICE_ACCOUNT_ENV);
        if (serviceAccountPath == null || serviceAccountPath.isEmpty()) {
            serviceAccountPath = Paths.get(System.getProperty("user.dir"), DEFAULT_SERVICE_ACCOUNT_PATH).toString();
        }

        try (InputStream stream = new FileInputStream(serviceAccountPath)) {
            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(stream)
                    .createScoped(Collections.singleton(CLOUD_PLATFORM_SCOPE));
            credentials.refreshIfExpired();
            return credentials.getAccessToken().getTokenValue();
        }
    }

    private static String httpGet(String urlStr, String accessToken) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(30_000);
        conn.setReadTimeout(30_000);

        int status = conn.getResponseCode();
        if (status != 200) {
            InputStream errorStream = conn.getErrorStream();
            String errorBody = errorStream != null ? new String(errorStream.readAllBytes()) : "(no body)";
            throw new IOException(
                "Firebase App Distribution API returned HTTP " + status +
                ". URL: " + urlStr + "\nResponse: " + errorBody
            );
        }

        try (InputStream in = conn.getInputStream()) {
            return new String(in.readAllBytes());
        }
    }

    private static void downloadFile(String fileUrl, Path destination) throws IOException {
        URL url = new URL(fileUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(60_000);
        conn.setReadTimeout(300_000); // 5 min for large APKs

        int status = conn.getResponseCode();
        // Follow redirects (signed URLs sometimes redirect)
        if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM) {
            String redirectUrl = conn.getHeaderField("Location");
            conn = (HttpURLConnection) new URL(redirectUrl).openConnection();
            conn.setConnectTimeout(60_000);
            conn.setReadTimeout(300_000);
        }

        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
