# Firebase Integration Setup Guide

Complete guide to integrate Firebase App Distribution with your Appium automation tests.

## Overview

This framework includes automated Firebase integration that:
- ✅ Downloads the latest APK from Firebase App Distribution
- ✅ Runs Appium tests against the downloaded build
- ✅ Generates Allure test reports
- ✅ Caches builds to avoid re-downloading
- ✅ Supports smoke and regression test suites

---

## Prerequisites

Before starting, ensure you have:

### Software Requirements
- [Java 17+](https://www.oracle.com/java/technologies/downloads/) installed
- [Maven 3.9+](https://maven.apache.org/download.cgi) installed
- [Android Studio](https://developer.android.com/studio) with emulator configured
- [Appium Server](https://appium.io/docs/en/latest/quickstart/install/) installed
- [Node.js](https://nodejs.org/) (required for Appium)

### Android Configuration
- Android emulator running with **4GB+ RAM allocated**
  - Device Manager → Edit Device → Advanced → RAM = 4096 MB
- Android SDK Platform API 31+ installed
- Google Play Services installed on emulator (for Firebase)

### Appium Server Running
```powershell
# Start Appium server in a new terminal
appium --loglevel debug
# Should show: "Appium REST server listening on http://127.0.0.1:4723"
```

---

## Step 1: Firebase Project Setup

### 1.1 Find Your Firebase App ID

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Click **Project Settings** (gear icon)
4. Go to **Your apps** tab
5. Find your Android app and click it
6. Copy the **App ID** - Format: `1:PROJECT_NUMBER:android:HASH`

Example: `1:123456789012:android:abcdef1234567890`

### 1.2 Create Service Account

1. In Firebase Console → **Project Settings**
2. Go to **Service Accounts** tab
3. Click **Generate New Private Key**
4. Save the JSON file as: `src/test/resources/firebase-service-account.json`

**Important:** Keep this file secure and never commit to version control!

Add to `.gitignore`:
```
src/test/resources/firebase-service-account.json
src/test/resources/google-services.json
```

### 1.3 Grant Necessary Permissions

The service account needs these roles in Firebase:
- **Firebase App Distribution Admin** (automatic, but verify)
- **Service Account User**

Check in [Google Cloud Console](https://console.cloud.google.com/):
1. Go to **IAM & Admin → Service Accounts**
2. Select your service account
3. Click **Grant Access**
4. Add role: **Firebase App Distribution Admin**

---

## Step 2: Environment Variables

Set these environment variables on your system:

### Windows (PowerShell)

```powershell
# Set FIREBASE_APP_ID (required for Firebase download)
[Environment]::SetEnvironmentVariable("FIREBASE_APP_ID", "1:YOUR_PROJECT_NUMBER:android:YOUR_HASH", "User")

# Optional: Set service account path (if not at default location)
[Environment]::SetEnvironmentVariable("FIREBASE_SERVICE_ACCOUNT_PATH", "C:\path\to\firebase-service-account.json", "User")

# Restart your terminal for changes to take effect
```

**For current session only:**
```powershell
$env:FIREBASE_APP_ID = "1:YOUR_PROJECT_NUMBER:android:YOUR_HASH"
$env:FIREBASE_SERVICE_ACCOUNT_PATH = "src/test/resources/firebase-service-account.json"
```

### Windows (Command Prompt)

```cmd
setx FIREBASE_APP_ID "1:YOUR_PROJECT_NUMBER:android:YOUR_HASH"
setx FIREBASE_SERVICE_ACCOUNT_PATH "C:\path\to\firebase-service-account.json"
```

### Verify Environment Variables

```powershell
# Check if set correctly
$env:FIREBASE_APP_ID
$env:FIREBASE_SERVICE_ACCOUNT_PATH
```

---

## Step 3: Upload APK to Firebase App Distribution

1. In Firebase Console, go to **App Distribution**
2. Click **Upload your first build**
3. Select your APK file
4. Add release notes (optional)
5. Click **Distribute**

The APK will now be available for download via the API.

---

## Step 4: Update Configuration

### Update DriverManager (Already Done)

The `DriverManager.initDriver()` automatically:
1. Checks for `FIREBASE_APP_ID` environment variable
2. If set, downloads latest APK from Firebase
3. If not set, uses local `app/FluentHealth.apk`

### Update API Client Capabilities (Optional)

Edit `src/main/java/utils/DriverManager.java` to match your app:
```java
.amend("appium:appPackage", "com.fluenthealth.app")  // Your app package
.amend("appium:appActivity", ".MainActivity")         // Your main activity (optional)
.amend("appium:platformVersion", "17")                // Your emulator API level
.amend("appium:deviceName", "Redmi Note 12")          // Your device name
```

---

## Step 5: Run Tests

### Quick Start

```powershell
# Download latest build and run smoke tests
cd C:\Users\ashish.taralkar\Downloads\Appium-JAVA-Firebase
.\run-firebase-tests.ps1 -TestSuite smoke
```

### All Options

```powershell
# Run smoke tests
.\run-firebase-tests.ps1 -TestSuite smoke

# Run regression tests
.\run-firebase-tests.ps1 -TestSuite regression

# Run both smoke and regression
.\run-firebase-tests.ps1 -All

# Download only (no tests)
.\run-firebase-tests.ps1 -DownloadOnly

# Download, run tests, and generate report
.\run-firebase-tests.ps1 -TestSuite regression -GenerateReport

# Download, run all tests, generate report, and cleanup old builds
.\run-firebase-tests.ps1 -All -GenerateReport -Cleanup
```

### Manual Execution (Without Script)

```powershell
# Build project
mvn clean install

# Download build and run tests manually
mvn -Dtest=tests.SmokeSuite test

# Generate Allure report
mvn allure:report
```

---

## Step 6: View Test Reports

After tests complete, view the Allure report:

```powershell
# Open report in browser
& "target\allure-report\index.html"

# Or manually navigate to
C:\Users\ashish.taralkar\Downloads\Appium-JAVA-Firebase\target\allure-report\index.html
```

---

## Troubleshooting

### Issue: "FIREBASE_APP_ID not set"

**Solution:** Set the environment variable:
```powershell
[Environment]::SetEnvironmentVariable("FIREBASE_APP_ID", "1:YOUR_ID", "User")
```

### Issue: "Service account JSON not found"

**Solution:** Ensure file exists at:
```
src/test/resources/firebase-service-account.json
```

Or set environment variable:
```powershell
$env:FIREBASE_SERVICE_ACCOUNT_PATH = "C:\path\to\firebase-service-account.json"
```

### Issue: "Connected Device not found"

**Solution:** Start Android emulator:
```powershell
# List available emulators
emulator -list-avds

# Start emulator
emulator -avd Pixel_5_API_31  # Replace with your emulator name
```

### Issue: "Appium server is not responding"

**Solution:** Start Appium server:
```powershell
appium --loglevel debug
```

### Issue: "Broken pipe (32)" Error During APK Installation

See [Broken Pipe Fix Guide](../BROKEN_PIPE_FIX.md) for detailed troubleshooting.

Quick fixes:
```powershell
# Restart ADB
adb kill-server
adb start-server

# Clear UIAutomator cache
adb -s emulator-5554 shell "pm clear io.appium.settings"

# Increase emulator RAM to 4GB
# Android Studio → Device Manager → Edit Device → RAM = 4096
```

---

## File Structure

```
Appium-JAVA-Firebase/
├── src/
│   ├── main/java/utils/
│   │   ├── DriverManager.java              # Appium driver initialization
│   │   ├── BuildDownloadManager.java       # Firebase download manager ✨ NEW
│   │   ├── FirebaseTestRunner.java         # Test orchestrator ✨ NEW
│   │   ├── FirebaseAppDistributionManager.java
│   │   └── FirebaseManager.java
│   │
│   └── test/
│       ├── java/base/
│       │   └── BaseTest.java               # Updated with Firebase init
│       ├── java/tests/
│       │   ├── SmokeSuite.java
│       │   └── RegressionSuite.java
│       └── resources/
│           ├── firebase-service-account.json
│           └── testdata/
│
├── app/                                    # Downloaded APKs cached here
├── target/
│   └── allure-report/                      # Test reports
├── run-firebase-tests.ps1                  # Automation script ✨ NEW
├── FIREBASE_SETUP.md                       # This file ✨ NEW
└── pom.xml

✨ = New or updated files for Firebase integration
```

---

## Best Practices

### 1. Appium Server Management
- Always start Appium before running tests
- Use `--loglevel debug` for detailed logs during troubleshooting
- Run in a separate terminal to keep logs accessible

### 2. Emulator Configuration
- Allocate 4GB+ RAM to emulator for stable operation
- Keep 5GB+ free disk space on host machine
- Restart emulator weekly to clear memory

### 3. Firebase Builds
- Upload new builds to Firebase App Distribution regularly
- Add meaningful release notes to each build
- Use semantic versioning (v1.0, v1.1, etc.)

### 4. Test Automation
- Run smoke tests first to catch critical issues early
- Run regression tests in CI/CD pipeline for complete coverage
- Generate reports for each test run to track regression

### 5. Caching
- The framework automatically caches APKs to avoid re-downloading
- Run cleanup periodically: `.\run-firebase-tests.ps1 -Cleanup`
- Clear `app/` directory manually if needed

---

## CI/CD Integration

### GitHub Actions Example

Create `.github/workflows/firebase-tests.yml`:

```yaml
name: Firebase Build Tests

on:
  schedule:
    - cron: '0 9 * * MON-FRI'  # 9 AM on weekdays
  workflow_dispatch:

jobs:
  test:
    runs-on: windows-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Set Firebase Env Vars
        run: |
          echo "FIREBASE_APP_ID=${{ secrets.FIREBASE_APP_ID }}" | Out-File -FilePath $env:GITHUB_ENV -Encoding utf8 -Append
          echo "FIREBASE_SERVICE_ACCOUNT_PATH=${{ secrets.FIREBASE_SERVICE_ACCOUNT_PATH }}" | Out-File -FilePath $env:GITHUB_ENV -Encoding utf8 -Append
      
      - name: Run Smoke Tests
        run: mvn -Dtest=tests.SmokeSuite test
      
      - name: Generate Report
        if: always()
        run: mvn allure:report
      
      - name: Upload Report
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: allure-report
          path: target/allure-report/
```

---

## Support & Troubleshooting

For issues:

1. **Check logs:**
   ```powershell
   # Test execution logs
   cat target/logs/test.log
   ```

2. **Run with verbose output:**
   ```powershell
   mvn -X -Dtest=tests.SmokeSuite test
   ```

3. **Check Firebase API:**
   ```powershell
   # Verify service account has correct permissions
   # Firebase Console → Project Settings → Service Accounts
   ```

4. **Verify emulator connection:**
   ```powershell
   adb devices
   adb -s emulator-5554 shell getprop ro.build.version.sdk
   ```

---

## Next Steps

✅ **Setup complete!** Now you can:

1. **Download and test manually:**
   ```powershell
   .\run-firebase-tests.ps1 -TestSuite smoke
   ```

2. **View test reports:**
   ```powershell
   Open-Item target\allure-report\index.html
   ```

3. **Integrate with CI/CD** using the GitHub Actions example above

4. **Monitor tests** on Firebase App Distribution dashboard

---

## Additional Resources

- [Firebase App Distribution Documentation](https://firebase.google.com/docs/app-distribution)
- [Appium Documentation](https://appium.io/docs/en/latest/)
- [Allure Reporting](https://docs.qameta.io/allure/)
- [Android Emulator Configuration](https://developer.android.com/studio/run/managing-avds)

---

**Last Updated:** April 2025
**Framework Version:** 1.0
**Status:** ✅ Production Ready
