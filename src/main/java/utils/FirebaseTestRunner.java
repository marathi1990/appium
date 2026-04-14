package utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * FirebaseTestRunner - Orchestrates APK download from Firebase and test execution..
 *
 * This class manages the complete workflow:
 * 1. Download latest APK from Firebase App Distribution
 * 2. Initialize Appium driver with the downloaded APK
 * 3. Execute tests
 * 4. Generate reports
 *
 * Environment Variables Required:
 * - FIREBASE_APP_ID: Firebase App ID (format: 1:PROJECT_NUMBER:android:HASH)
 * - FIREBASE_SERVICE_ACCOUNT_PATH (optional): Path to service account JSON
 * - APPIUM_SERVER: Appium server URL (default: http://127.0.0.1:4723)
 *
 * Usage:
 * ```
 * FirebaseTestRunner runner = new FirebaseTestRunner();
 * runner.downloadBuild();
 * runner.runTestSuite("smoke");  // Run smoke tests
 * runner.generateReport();
 * ```
 */
public class FirebaseTestRunner {

    private BuildDownloadManager downloadManager;
    private String downloadedBuildPath;
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT);

    public FirebaseTestRunner() {
        this.downloadManager = new BuildDownloadManager();
    }

    // ─────────────────────────────────────────────
    //  Main Workflow Methods
    // ─────────────────────────────────────────────

    /**
     * Download the latest APK from Firebase App Distribution.
     *
     * @return Path to downloaded APK
     * @throws Exception if download fails
     */
    public String downloadBuild() throws Exception {
        logSection("STEP 1: Download Latest APK from Firebase");
        
        try {
            this.downloadedBuildPath = downloadManager.downloadLatestBuild();
            logSuccess("✅ Build downloaded successfully");
            return this.downloadedBuildPath;
        } catch (IllegalStateException e) {
            logError("Configuration Error: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logError("Download Failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Run a specific test suite (smoke, regression, etc.)
     *
     * @param suiteName Name of the test suite to run (e.g., "smoke", "regression")
     * @return true if all tests passed, false otherwise
     * @throws Exception if test execution fails
     */
    public boolean runTestSuite(String suiteName) throws Exception {
        logSection("STEP 2: Run Test Suite - " + suiteName.toUpperCase());

        String command = buildMavenCommand(suiteName);
        logInfo("Executing: " + command);
        logInfo("Timestamp: " + LocalDateTime.now().format(formatter));
        logInfo("");

        boolean success = executeCommand(command);
        
        if (success) {
            logSuccess("✅ Test suite completed");
        } else {
            logError("❌ Test suite failed");
        }

        return success;
    }

    /**
     * Run all tests (both smoke and regression).
     *
     * @return true if all tests passed
     * @throws Exception if test execution fails
     */
    public boolean runAllTests() throws Exception {
        logSection("STEP 2: Run All Tests (Smoke + Regression)");

        // Run smoke tests first
        boolean smokeSuccess = runTestSuite("smoke");
        
        if (!smokeSuccess) {
            logWarning("⚠️  Smoke tests failed - skipping regression tests");
            return false;
        }

        // If smoke tests pass, run regression
        boolean regressionSuccess = runTestSuite("regression");
        
        if (regressionSuccess) {
            logSuccess("✅ All tests passed!");
        }

        return regressionSuccess;
    }

    /**
     * Generate Allure test report.
     * Report will be available at: target/allure-report/
     */
    public void generateReport() throws Exception {
        logSection("STEP 3: Generate Test Report");

        String command = "mvn allure:report -DskipTests=true";
        logInfo("Generating Allure report...");

        boolean success = executeCommand(command);
        
        if (success) {
            logSuccess("✅ Report generated at: target/allure-report/");
            logInfo("Open in browser: target/allure-report/index.html");
        } else {
            logWarning("⚠️  Report generation had issues - check output above");
        }
    }

    /**
     * Clean up build cache (remove old APKs).
     */
    public void cleanupBuilds() {
        logSection("Cleanup: Remove Old Builds");
        downloadManager.cleanupOldBuilds();
        logSuccess("✅ Cleanup complete");
    }

    // ─────────────────────────────────────────────
    //  Build Information
    // ─────────────────────────────────────────────

    /**
     * Display information about the current build.
     */
    public void displayBuildInfo() {
        logSection("Current Build Information");
        logInfo(downloadManager.getLatestBuildInfo());
        if (downloadedBuildPath != null) {
            logInfo("Downloaded APK: " + downloadedBuildPath);
        }
    }

    // ─────────────────────────────────────────────
    //  Private Helpers
    // ─────────────────────────────────────────────

    private String buildMavenCommand(String suiteName) {
        String suiteClass = switch (suiteName.toLowerCase()) {
            case "smoke" -> "tests.SmokeSuite";
            case "regression" -> "tests.RegressionSuite";
            default -> "tests." + suiteName;
        };

        return String.format(
            "mvn -Dtest=%s test -DskipUnitTests=true",
            suiteClass
        );
    }

    private boolean executeCommand(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
            pb.inheritIO();
            pb.redirectErrorStream(true);

            Process process = pb.start();
            int exitCode = process.waitFor();

            return exitCode == 0;
        } catch (Exception e) {
            logError("Command execution failed: " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────
    //  Logging Utilities
    // ─────────────────────────────────────────────

    private void logSection(String title) {
        System.out.println();
        System.out.println("════════════════════════════════════════════════════════");
        System.out.println("  " + title);
        System.out.println("════════════════════════════════════════════════════════");
    }

    private void logInfo(String message) {
        System.out.println("ℹ️  " + message);
    }

    private void logSuccess(String message) {
        System.out.println("✅ " + message);
    }

    private void logWarning(String message) {
        System.out.println("⚠️  " + message);
    }

    private void logError(String message) {
        System.err.println("❌ " + message);
    }

    // ─────────────────────────────────────────────
    //  Main Entry Point (for testing)
    // ─────────────────────────────────────────────

    /**
     * Quick test of the download functionality.
     * Run with: java -cp target/classes:... utils.FirebaseTestRunner
     */
    public static void main(String[] args) {
        try {
            FirebaseTestRunner runner = new FirebaseTestRunner();
            
            // Just test the download
            runner.downloadBuild();
            runner.displayBuildInfo();
            
            System.out.println();
            System.out.println("✅ Firebase integration is working!");
            System.out.println("Now run your tests with: mvn test");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
