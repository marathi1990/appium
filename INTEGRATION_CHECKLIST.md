# Firebase Integration - Implementation Checklist

Track your Firebase integration setup progress.

## ✅ Framework Components (Completed)

### Code Updates
- ✅ **BuildDownloadManager.java** - Manages APK download and caching
- ✅ **FirebaseTestRunner.java** - Orchestrates download + test execution
- ✅ **BaseTest.java** - Updated with Firebase initialization (BeforeAll/AfterAll)
- ✅ **DriverManager.java** - Already downloads APK from Firebase if FIREBASE_APP_ID set

### Automation Scripts
- ✅ **run-firebase-tests.ps1** - PowerShell script to automate full workflow
  - Download latest APK
  - Run smoke/regression tests
  - Generate Allure reports
  - Cleanup old builds

### Documentation
- ✅ **FIREBASE_SETUP.md** - Complete setup guide (Step-by-step instructions)
- ✅ **QUICK_START.md** - Quick reference for daily use
- ✅ **INTEGRATION_CHECKLIST.md** - This file

---

## 📋 Your Setup Checklist

### Phase 1: Firebase Project Configuration (15 min)

- [ ] **Step 1.1:** Go to [Firebase Console](https://console.firebase.google.com/)
- [ ] **Step 1.2:** Find your Firebase App ID
  - In Firebase Console → Project Settings → Your apps → Click Android app → Copy App ID
  - Format should be: `1:123456789012:android:abcdef...`
  - Save this value for next step

- [ ] **Step 1.3:** Download Service Account JSON
  - In Firebase Console → Project Settings → Service Accounts tab
  - Click "Generate New Private Key"
  - Save as: `src/test/resources/firebase-service-account.json`
  - Keep it secure - add to `.gitignore` if using git

- [ ] **Step 1.4:** Verify Permissions
  - Google Cloud Console → IAM & Admin → Service Accounts
  - Select your service account
  - Ensure it has "Firebase App Distribution Admin" role

### Phase 2: Environment Variables Setup (5 min)

**Windows PowerShell:**
```powershell
# Copy your actual Firebase App ID from Step 1.2
$appId = "1:YOUR_PROJECT_NUMBER:android:YOUR_HASH"

# Set environment variable permanently
[Environment]::SetEnvironmentVariable("FIREBASE_APP_ID", $appId, "User")

# Close and reopen PowerShell, then verify
$env:FIREBASE_APP_ID    # Should show your App ID
```

- [ ] **Step 2.1:** Set `FIREBASE_APP_ID` environment variable
- [ ] **Step 2.2:** Place service account JSON at `src/test/resources/firebase-service-account.json`
- [ ] **Step 2.3:** Verify environment variable is set
  ```powershell
  echo $env:FIREBASE_APP_ID  # Should display your App ID
  ```

### Phase 3: Appium & Android Installation (30 min if not done)

- [ ] **Step 3.1:** Install Java 17+
  ```powershell
  java -version  # Should show Java 17+
  ```

- [ ] **Step 3.2:** Install Maven
  ```powershell
  mvn -version  # Should show Maven 3.9+
  ```

- [ ] **Step 3.3:** Install Android Studio with API 31+ and emulator
  - Download [Android Studio](https://developer.android.com/studio)
  - Create or configure an emulator with 4GB+ RAM

- [ ] **Step 3.4:** Install Appium Server
  ```powershell
  npm install -g appium
  appium --version  # Should show version info
  ```

- [ ] **Step 3.5:** Verify Android emulator has 4GB+ RAM
  - Android Studio → Device Manager → Click your emulator → Edit
  - Advanced settings → RAM: 4096 MB
  - Save and test

### Phase 4: Upload First Build to Firebase (10 min)

- [ ] **Step 4.1:** Build your APK
  ```powershell
  mvn clean package
  # APK will be in target/ or available in Android Studio build output
  ```

- [ ] **Step 4.2:** Upload to Firebase App Distribution
  - Firebase Console → App Distribution
  - Click "Upload your first build"
  - Select your APK file
  - Add release notes (optional)
  - Click Distribute

---

## 🚀 First Test Run

Once all checkboxes above are done, you're ready!

### Pre-flight Checklist
- [ ] Android emulator is running
  ```powershell
  emulator -avd [your-emulator-name] &  # Start in background
  ```

- [ ] Appium server is running (in separate terminal)
  ```powershell
  appium --loglevel debug
  ```

- [ ] Navigate to project directory
  ```powershell
  cd C:\Users\ashish.taralkar\Downloads\Appium-JAVA-Firebase
  ```

### Run Your First Test
```powershell
# Download latest APK and run smoke tests
.\run-firebase-tests.ps1 -TestSuite smoke
```

Expected output:
- Shows Firebase download progress
- Shows test execution progress
- Reports test results
- Returns exit code 0 if successful

### View Results
```powershell
# Open HTML test report
& "target\allure-report\index.html"
```

---

## 📊 Different Ways to Run Tests

### Option 1: Automated Script (Recommended)
```powershell
# Smoke tests only
.\run-firebase-tests.ps1 -TestSuite smoke

# Full suite with report
.\run-firebase-tests.ps1 -All -GenerateReport
```

### Option 2: Maven Command
```powershell
# Download and run tests
mvn clean test -Dtest=tests.SmokeSuite
```

### Option 3: IDE
- Open project in IntelliJ/Eclipse
- Click Run on test class
- Right-click test class → Run

---

## 🔧 Common Issues & Fixes

### Issue: "FIREBASE_APP_ID not set"
**Fix:** Set environment variable and restart terminal
```powershell
[Environment]::SetEnvironmentVariable("FIREBASE_APP_ID", "YOUR_ID", "User")
# Close and reopen PowerShell
```

### Issue: "Service account JSON not found"
**Fix:** Ensure file at correct path
```
src/test/resources/firebase-service-account.json
```

### Issue: "Appium server is not responding"
**Fix:** Start Appium in separate terminal
```powershell
appium --loglevel debug
```

### Issue: "No emulator devices found"
**Fix:** Start Android emulator
```powershell
emulator -list-avds                    # List available
emulator -avd Pixel_5_API_31           # Start one
```

### Issue: "Insufficient emulator RAM" or "Broken pipe" error
**Fix:** Increase RAM allocation
- Android Studio → Device Manager → Click device → Edit
- Advanced → RAM: 4096 MB
- Restart emulator

See [Broken Pipe Fix Guide](../../app-mod/BROKEN_PIPE_FIX.md) for detailed troubleshooting.

---

## 📁 What's Been Added/Changed

### New Files Created
```
✨ src/main/java/utils/BuildDownloadManager.java     (APK download manager)
✨ src/main/java/utils/FirebaseTestRunner.java       (Test orchestrator)
✨ run-firebase-tests.ps1                             (Automation script)
✨ FIREBASE_SETUP.md                                  (Setup guide)
✨ QUICK_START.md                                     (Quick reference)
✨ INTEGRATION_CHECKLIST.md                           (This file)
```

### Modified Files
```
📝 src/test/java/base/BaseTest.java                  (Added Firebase init)
```

### No Changes Needed
```
→ DriverManager.java                                  (Already had Firebase download)
→ FirebaseManager.java                                (For future Firebase features)
→ FirebaseAppDistributionManager.java                (Handles API calls)
→ pom.xml                                             (All dependencies already present)
```

---

## 🎯 Next Steps After Setup

### Immediate: Test Your Setup
1. Run smoke tests: `.\run-firebase-tests.ps1 -TestSuite smoke`
2. View result report in `target/allure-report/index.html`
3. Verify all tests pass ✅

### Short-term: Add Your Tests
1. Add test cases to `SmokeSuite.java` for critical features
2. Add detailed scenarios to `RegressionSuite.java`
3. Update `src/test/resources/testdata/` with test data

### Medium-term: CI/CD Integration
1. Set up GitHub Actions workflow to run tests on schedule
2. Configure notifications for test failures
3. Archive test reports for trending analysis

### Long-term: Advanced Features
1. Add Firebase Realtime Database validation to tests
2. Implement Firebase Authentication tests
3. Add Cloud Firestore data verification
4. Set up performance benchmarking

---

## 📞 Support

For issues:

1. **Check logs:**
   ```powershell
   # View test execution logs
   cat target/logs/test.log
   
   # View Appium logs (in terminal where it's running)
   ```

2. **Run with debug output:**
   ```powershell
   mvn -X -Dtest=tests.SmokeSuite test
   ```

3. **Verify connectivity:**
   ```powershell
   # Check Appium
   curl http://127.0.0.1:4723/status
   
   # Check Android emulator
   adb devices
   ```

4. **Document issues:**
   - Note error message
   - Check [FIREBASE_SETUP.md](./FIREBASE_SETUP.md) for known solutions

---

## ✅ Completion Status

When you've done everything above:

```
Phase 1: Configuration       ✅ ___ / 4 items
Phase 2: Environment        ✅ ___ / 3 items  
Phase 3: Installation       ✅ ___ / 5 items
Phase 4: First Build        ✅ ___ / 2 items
First Test Run              ✅ ___ / 3 items
```

**Total Effort:** ~60 minutes for complete setup (one-time)

**Ongoing Effort:** ~3 minutes per test run

---

## Helpful Links

- [Firebase Console](https://console.firebase.google.com/)
- [Firebase App Distribution Docs](https://firebase.google.com/docs/app-distribution)
- [Appium Documentation](https://appium.io/docs/en/latest/)
- [Android Emulator Guide](https://developer.android.com/studio/run/managing-avds)
- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)

---

**Remember:** If you get stuck on any step, consult [FIREBASE_SETUP.md](./FIREBASE_SETUP.md) for detailed help!
