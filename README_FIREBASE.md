# Firebase Integration - COMPLETE SETUP ✅

Your Firebase integration is now ready to use! This document explains what's been set up and how to get started.

---

## 🎯 What You Now Have

A complete automated testing framework that:

1. **Downloads APK from Firebase App Distribution** 
   - Latest build every time automatically
   - Caches APKs to avoid re-downloading
   - Validates build is available before testing

2. **Installs APK on Android Emulator**
   - Automatic installation via Appium
   - Configurable device settings
   - Full cleanup between test runs

3. **Runs Appium Tests**
   - Smoke test suite (quick validation)
   - Regression test suite (comprehensive testing)
   - Both suites can run together

4. **Generates Test Reports**
   - Allure reports with detailed metrics
   - Screenshots and logs
   - HTML report for easy viewing

5. **Automates the Entire Workflow**
   - Single PowerShell command runs everything
   - Environment variable configuration
   - Clear status messages and error reporting

---

## 📁 New Files Added

### Utilities (Java Code)
```
src/main/java/utils/
├── BuildDownloadManager.java      ✨ Downloads APK from Firebase
└── FirebaseTestRunner.java        ✨ Orchestrates full workflow
```

### Automation Scripts
```
root/
└── run-firebase-tests.ps1         ✨ PowerShell automation script
```

### Documentation
```
root/
├── FIREBASE_SETUP.md              ✨ Detailed setup guide
├── QUICK_START.md                 ✨ Quick reference
├── INTEGRATION_CHECKLIST.md       ✨ Setup checklist
└── README_FIREBASE.md             ✨ This file
```

### Updated Files
```
src/test/java/base/
└── BaseTest.java                  📝 Now initializes Firebase
```

---

## 🚀 Getting Started (5 Minutes)

### Step 1: Set Environment Variable

Open PowerShell and run:

```powershell
# Get your Firebase App ID from Firebase Console
# → Project Settings → Your apps → Copy the App ID

# Example: 1:123456789012:android:abcdef1234567890

# Set it as environment variable
$appId = "1:YOUR_APP_ID_HERE"
[Environment]::SetEnvironmentVariable("FIREBASE_APP_ID", $appId, "User")

# Restart PowerShell and verify
$env:FIREBASE_APP_ID    # This should show your app ID
```

**Don't have your Firebase App ID?** See [FIREBASE_SETUP.md](./FIREBASE_SETUP.md) Step 1 for detailed instructions.

### Step 2: Place Service Account JSON

Place your Firebase service account JSON at:
```
src/test/resources/firebase-service-account.json
```

**Don't have it?** See [FIREBASE_SETUP.md](./FIREBASE_SETUP.md) Step 1.2 to download.

### Step 3: Start Prerequisites

In separate terminal windows:

```powershell
# Terminal 1: Start Android emulator
emulator -avd [YOUR_EMULATOR_NAME]

# Terminal 2: Start Appium server
appium --loglevel debug

# Terminal 3: Run tests (from project directory)
cd C:\Users\ashish.taralkar\Downloads\Appium-JAVA-Firebase
```

### Step 4: Run Tests!

```powershell
# Download latest build and run smoke tests
.\run-firebase-tests.ps1 -TestSuite smoke
```

That's it! The script will:
1. Download latest APK from Firebase
2. Install it on the emulator
3. Run all smoke tests
4. Show results in console

---

## 📊 Usage Examples

### Common Test Commands

```powershell
# Smoke tests (quick validation)
.\run-firebase-tests.ps1 -TestSuite smoke

# Regression tests (comprehensive)
.\run-firebase-tests.ps1 -TestSuite regression

# All tests (smoke + regression)
.\run-firebase-tests.ps1 -All

# Download only, don't run tests
.\run-firebase-tests.ps1 -DownloadOnly

# Run tests and generate report
.\run-firebase-tests.ps1 -TestSuite smoke -GenerateReport

# Everything: all tests, report, and cleanup
.\run-firebase-tests.ps1 -All -GenerateReport -Cleanup
```

### View Test Results

```powershell
# Open Allure test report in browser
& "target\allure-report\index.html"
```

---

## 📋 Full Setup Checklist

If you haven't done so already, complete these steps:

### Phase 1: Firebase Configuration (15 min)
- [ ] Get Firebase App ID from Firebase Console
- [ ] Download service account JSON file
- [ ] Place JSON at `src/test/resources/firebase-service-account.json`
- [ ] Verify service account has "Firebase App Distribution Admin" role

### Phase 2: Environment & Tools (5 min)
- [ ] Set `FIREBASE_APP_ID` environment variable
- [ ] Have Android Studio with emulator (4GB+ RAM) installed
- [ ] Have Appium installed: `npm install -g appium`
- [ ] Have Maven installed: `mvn -version`

### Phase 3: First Test Run (5 min)
- [ ] Start Android emulator
- [ ] Start Appium server
- [ ] Run: `.\run-firebase-tests.ps1 -TestSuite smoke`
- [ ] Verify tests pass

**See [INTEGRATION_CHECKLIST.md](./INTEGRATION_CHECKLIST.md) for detailed steps.**

---

## 🔍 System Architecture

Here's what happens when you run `.\run-firebase-tests.ps1`:

```
User Command
    ↓
PowerShell Script (run-firebase-tests.ps1)
    ├─→ Check Environment Variables
    ├─→ Verify Appium Server Running
    ├─→ Verify Android Emulator Connected
    │
    ├─→ [Step 1] Download APK from Firebase
    │   └─→ BuildDownloadManager.java
    │       ├─→ FirebaseAppDistributionManager.downloadLatestApk()
    │       ├─→ Cache in app/ directory
    │       └─→ Return path to APK
    │
    ├─→ [Step 2] Run Tests
    │   └─→ FirebaseTestRunner.runTestSuite()
    │       ├─→ Maven test command: mvn -Dtest=tests.SmokeSuite test
    │       ├─→ BaseTest.java initializes Firebase & Appium
    │       ├─→ DriverManager installs APK on emulator
    │       ├─→ Test cases execute
    │       └─→ Results in target/allure-results/
    │
    ├─→ [Step 3] Generate Report (optional)
    │   └─→ Maven allure:report
    │       └─→ HTML report in target/allure-report/
    │
    └─→ Display Results
```

---

## 📚 Documentation Files

| File | Purpose |
|------|---------|
| [FIREBASE_SETUP.md](./FIREBASE_SETUP.md) | **START HERE** - Complete step-by-step setup guide |
| [QUICK_START.md](./QUICK_START.md) | Quick reference for common commands |
| [INTEGRATION_CHECKLIST.md](./INTEGRATION_CHECKLIST.md) | Detailed checklist with checkboxes |
| [README_FIREBASE.md](./README_FIREBASE.md) | This file - overview |

---

## 🛠️ Key Components Explained

### BuildDownloadManager
Manages APK download from Firebase:
- Checks for FIREBASE_APP_ID environment variable
- Calls Firebase App Distribution API
- Downloads latest APK
- Caches APK to avoid re-download
- Provides cleanup and info methods

**Usage:**
```java
BuildDownloadManager manager = new BuildDownloadManager();
String apkPath = manager.downloadLatestBuild();  // Downloads or uses cache
```

### FirebaseTestRunner
Orchestrates the entire workflow:
- Downloads APK
- Runs test suites (smoke, regression, or both)
- Generates reports
- Handles cleanup

**Usage:**
```java
FirebaseTestRunner runner = new FirebaseTestRunner();
runner.downloadBuild();
runner.runTestSuite("smoke");
runner.generateReport();
```

### Updated BaseTest
Now handles Firebase initialization:
- `@BeforeAll`: Initializes Firebase Admin SDK once per test class
- `@BeforeEach`: Initializes Appium driver before each test
- `@AfterEach`: Cleans up driver after each test
- `@AfterAll`: Shuts down Firebase after all tests

### DriverManager
Already had Firebase integration - automatically:
- Checks for FIREBASE_APP_ID environment variable
- Downloads latest APK if set
- Uses local APK if not set
- Initializes Appium with downloaded APK

---

## ⚙️ Configuration Reference

### Environment Variables

```powershell
# Required for Firebase download
FIREBASE_APP_ID = "1:PROJECT_NUMBER:android:HASH"

# Optional - defaults to src/test/resources/firebase-service-account.json
FIREBASE_SERVICE_ACCOUNT_PATH = "C:\path\to\firebase-service-account.json"
```

### Appium Capabilities (in DriverManager.java)

Edit these to match your app:
```java
.amend("appium:appPackage", "com.fluenthealth.app")
.amend("appium:platformVersion", "17")
.amend("appium:deviceName", "Redmi Note 12")
.amend("appium:automationName", "UiAutomator2")
.amend("appium:autoGrantPermissions", true)
.amend("appium:noReset", false)
```

---

## 🐛 Troubleshooting

### "FIREBASE_APP_ID not set"
```powershell
# Set the environment variable
[Environment]::SetEnvironmentVariable("FIREBASE_APP_ID", "YOUR_ID", "User")
# Restart PowerShell
```

### "Appium server is not responding"
```powershell
# Start Appium in separate terminal
appium --loglevel debug
```

### "No emulator devices found"
```powershell
# Start Android emulator
emulator -list-avds              # List available
emulator -avd Pixel_5_API_31     # Start one
```

### Tests fail with "Broken pipe" error
See detailed troubleshooting in [BROKEN_PIPE_FIX.md](../../app-mod/BROKEN_PIPE_FIX.md)

Quick fix:
```powershell
# Restart ADB
adb kill-server && adb start-server

# Increase emulator RAM to 4GB
# Android Studio → Device Manager → Edit Device → RAM = 4096
```

**See [FIREBASE_SETUP.md](./FIREBASE_SETUP.md) Troubleshooting section for more help.**

---

## ✅ What's Automated

✅ Download latest APK from Firebase App Distribution  
✅ Verify APK is available  
✅ Skip download if already cached  
✅ Install APK on connected emulator  
✅ Initialize Appium driver  
✅ Load Firebase configuration  
✅ Run test suite specified  
✅ Collect test results and screenshots  
✅ Generate Allure HTML report  
✅ Clean up old cached APKs (optional)  

---

## 🎓 For Developers

### Add a New Test
1. Create test class in `src/test/java/tests/`
2. Extend `BaseTest` (Firebase & Appium automatically initialized)
3. Use page objects from `src/main/java/pages/`
4. Add test to SmokeSuite or RegressionSuite

### Modify Device Settings
Edit `src/main/java/utils/DriverManager.java`:
```java
.amend("appium:deviceName", "Your Device Name")
.amend("appium:platformVersion", "API_LEVEL")
```

### Use Firebase Features
1. `FirebaseManager` - Database, Auth, Firestore operations
2. `BuildDownloadManager` - Download management
3. `FirebaseTestRunner` - Test orchestration

---

## 📞 Next Steps

1. **Now:** Follow [FIREBASE_SETUP.md](./FIREBASE_SETUP.md) to complete configuration
2. **Then:** Run your first test: `.\run-firebase-tests.ps1 -TestSuite smoke`
3. **Next:** Add your test cases to SmokeSuite/RegressionSuite
4. **Finally:** Set up CI/CD pipeline for automated runs

---

## 📊 Performance Expectations

| Operation | Time |
|-----------|------|
| First APK download | 1-3 minutes |
| Smoke tests | 5-10 minutes |
| Regression tests | 15-30 minutes |
| Report generation | 1-2 minutes |
| Cached run (no download) | -2 minutes |

**Total first run:** ~5-15 minutes (depending on APK size and test count)  
**Subsequent runs:** ~3-10 minutes (reuses cached APK)

---

## 🔐 Security Notes

- **Service account JSON:** Keep secure, never commit to git version control
- **Firebase App ID:** OK to be in version control (not a secret)
- **API keys:** Use environment variables, never hardcode

Add to `.gitignore`:
```
src/test/resources/firebase-service-account.json
src/test/resources/google-services.json
*.jks
```

---

## 🎉 You're Ready!

Everything is set up. Now:

1. **Complete the configuration** - Follow [FIREBASE_SETUP.md](./FIREBASE_SETUP.md)
2. **Run your first test** - `.\run-firebase-tests.ps1 -TestSuite smoke`
3. **View the report** - Open `target/allure-report/index.html` in browser
4. **Add your tests** - Customize SmokeSuite and RegressionSuite
5. **Integrate with CI/CD** - Set up GitHub Actions or Azure Pipelines

---

**Questions?** Check the documentation files:
- Setup questions → [FIREBASE_SETUP.md](./FIREBASE_SETUP.md)
- Quick reference → [QUICK_START.md](./QUICK_START.md)
- Step-by-step checklist → [INTEGRATION_CHECKLIST.md](./INTEGRATION_CHECKLIST.md)

**Happy Testing! 🚀**
