package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * BuildDownloadManager - Manages APK download from Firebase App Distribution.
 * 
 * This class provides a clean interface to download the latest APK build from Firebase
 * and handles caching to avoid re-downloading the same version.
 * 
 * Environment Variables:
 * - FIREBASE_APP_ID: Your Firebase App ID (format: 1:PROJECT_NUMBER:android:HASH)
 * - FIREBASE_SERVICE_ACCOUNT_PATH: Path to service account JSON (optional, defaults to src/test/resources/firebase-service-account.json)
 * 
 * Usage:
 * ```
 * BuildDownloadManager manager = new BuildDownloadManager();
 * String apkPath = manager.downloadLatestBuild();
 * System.out.println("Downloaded APK: " + apkPath);
 * ```
 */
public class BuildDownloadManager {

    private static final String BUILDS_DIR = "app";
    
    /**
     * Download the latest APK from Firebase App Distribution.
     * If the exact version is already cached locally, returns the cached path without re-downloading.
     * 
     * @return Absolute path to the downloaded or cached APK
     * @throws IllegalStateException if FIREBASE_APP_ID is not set
     * @throws Exception if download fails
     */
    public String downloadLatestBuild() throws Exception {
        String firebaseAppId = System.getenv("FIREBASE_APP_ID");
        if (firebaseAppId == null || firebaseAppId.isEmpty()) {
            throw new IllegalStateException(
                "FIREBASE_APP_ID environment variable is not set.\n" +
                "Set it to your Firebase App ID (format: 1:PROJECT_NUMBER:android:HASH)\n" +
                "Find it in Firebase Console → Project Settings → Your apps."
            );
        }

        System.out.println("================================");
        System.out.println("Firebase Build Download Manager");
        System.out.println("================================");
        System.out.println("App ID: " + firebaseAppId);
        
        // Use existing FirebaseAppDistributionManager to download
        String apkPath = FirebaseAppDistributionManager.downloadLatestApk();
        
        System.out.println("✅ Build ready for testing: " + apkPath);
        System.out.println();
        
        return apkPath;
    }

    /**
     * Check if we have a local APK already cached.
     * Useful for quick test runs without re-downloading.
     * 
     * @return Path to cached APK if exists, null otherwise
     */
    public String getLocalApk() {
        try {
            Path appDir = Paths.get(System.getProperty("user.dir"), BUILDS_DIR);
            if (!Files.exists(appDir)) {
                return null;
            }

            // Find the latest APK in the app directory
            return Files.walk(appDir, 1)
                    .filter(p -> p.toString().endsWith(".apk"))
                    .reduce((first, second) -> second) // Get the last (most recent) APK
                    .map(Path::toString)
                    .orElse(null);
        } catch (IOException e) {
            System.err.println("Error checking for local APK: " + e.getMessage());
            return null;
        }
    }

    /**
     * Clear old cached APKs, keeping only the latest version.
     * Call this periodically to free up disk space.
     */
    public void cleanupOldBuilds() {
        try {
            Path appDir = Paths.get(System.getProperty("user.dir"), BUILDS_DIR);
            if (!Files.exists(appDir)) {
                return;
            }

            // Keep only the latest APK by modification time
            Path latestApk = Files.walk(appDir, 1)
                    .filter(p -> p.toString().endsWith(".apk"))
                    .reduce((first, second) -> {
                        try {
                            return Files.getLastModifiedTime(first).compareTo(
                                    Files.getLastModifiedTime(second)) > 0 ? first : second;
                        } catch (IOException e) {
                            return first;
                        }
                    })
                    .orElse(null);

            // Delete all other APKs
            Files.walk(appDir, 1)
                    .filter(p -> p.toString().endsWith(".apk"))
                    .filter(p -> !p.equals(latestApk))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                            System.out.println("Deleted old build: " + p.getFileName());
                        } catch (IOException e) {
                            System.err.println("Failed to delete: " + p + " - " + e.getMessage());
                        }
                    });
        } catch (IOException e) {
            System.err.println("Error cleaning up old builds: " + e.getMessage());
        }
    }

    /**
     * Get information about the latest downloaded build.
     * 
     * @return String with build info or null if no builds found
     */
    public String getLatestBuildInfo() {
        try {
            Path appDir = Paths.get(System.getProperty("user.dir"), BUILDS_DIR);
            if (!Files.exists(appDir)) {
                return "No builds directory found";
            }

            Path latestApk = Files.walk(appDir, 1)
                    .filter(p -> p.toString().endsWith(".apk"))
                    .reduce((first, second) -> second)
                    .orElse(null);

            if (latestApk == null) {
                return "No APKs found";
            }

            long fileSize = Files.size(latestApk);
            String sizeStr = fileSize > 1024 * 1024 
                ? String.format("%.2f MB", fileSize / (1024.0 * 1024.0))
                : String.format("%.2f KB", fileSize / 1024.0);

            return "Latest Build: " + latestApk.getFileName() + " (" + sizeStr + ")";
        } catch (IOException e) {
            return "Error getting build info: " + e.getMessage();
        }
    }
}
