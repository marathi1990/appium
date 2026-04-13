# Firebase Tests - Quick Reference Guide

Fast reference for daily use of Firebase integration.

## Quick Start (5 minutes)

### 1. Ensure Prerequisites
```powershell
# Terminal 1: Start Android emulator
emulator -avd Pixel_5_API_31

# Terminal 2: Start Appium server
appium --loglevel debug

# Terminal 3: Run tests
cd C:\Users\ashish.taralkar\Downloads\Appium-JAVA-Firebase
```

### 2. Run Tests
```powershell
# Smoke tests only (fastest)
.\run-firebase-tests.ps1 -TestSuite smoke

# Full regression suite
.\run-firebase-tests.ps1 -TestSuite regression

# Everything: smoke + regression + report + cleanup
.\run-firebase-tests.ps1 -All -GenerateReport -Cleanup
```

### 3. View Results
```powershell
# Open test report
& "target\allure-report\index.html"
```

---

## Common Commands

| Task | Command |
|------|---------|
| Smoke tests | `.\run-firebase-tests.ps1 -TestSuite smoke` |
| Regression tests | `.\run-firebase-tests.ps1 -TestSuite regression` |
| Run all tests | `.\run-firebase-tests.ps1 -All` |
| Download only | `.\run-firebase-tests.ps1 -DownloadOnly` |
| With report | `.\run-firebase-tests.ps1 -TestSuite smoke -GenerateReport` |
| Cleanup old builds | `.\run-firebase-tests.ps1 -Cleanup` |
| Check Appium | `curl http://127.0.0.1:4723/status` |

---

## Environment Setup (One-Time)

```powershell
# Set your Firebase App ID
[Environment]::SetEnvironmentVariable("FIREBASE_APP_ID", "1:YOUR_NUMBER:android:YOUR_HASH", "User")

# Verify it's set
$env:FIREBASE_APP_ID  # Should show your App ID

# Place service account JSON here
# src/test/resources/firebase-service-account.json
```

Get your Firebase App ID from:
- Firebase Console → Project Settings → Your apps → Copy App ID

---

## Troubleshooting Quick Fixes

| Issue | Fix |
|-------|-----|
| Appium not responding | `appium --loglevel debug` in new terminal |
| No emulator found | `emulator -list-avds` and `emulator -avd NAME` |
| Broken pipe error | See [BROKEN_PIPE_FIX.md](./app-mod/BROKEN_PIPE_FIX.md) |
| APK not downloading | Check `FIREBASE_APP_ID` environment variable |
| Tests fail to start | Verify Android emulator has 4GB+ RAM allocated |

---

## File Locations

```
Downloaded APKs:     app/FluentHealth-*.apk
Test Reports:        target/allure-report/index.html
Service Account:     src/test/resources/firebase-service-account.json
Test Data:           src/test/resources/testdata/
```

---

## What Gets Automated

✅ Download latest APK from Firebase  
✅ Install APK on emulator  
✅ Run test suite specified  
✅ Collect test results  
✅ Generate HTML report  
✅ Cache APK to avoid re-download  

---

## Exit Codes

| Code | Meaning |
|------|---------|
| 0 | ✅ All tests passed |
| 1 | ❌ Tests failed or script error |

---

## Performance Tips

- **First run:** ~3-5 minutes (APK download + setup)
- **Subsequent runs:** ~2-3 minutes (using cached APK)
- **With report:** Add 1-2 minutes for report generation

To keep builds cached and reuse them: don't use `-Cleanup` flag.

---

## For Developers

Test classes to modify:
- `src/test/java/tests/SmokeSuite.java` - Add/remove smoke tests
- `src/test/java/tests/RegressionSuite.java` - Add/remove regression tests
- `src/test/java/base/BaseTest.java` - Modify test setup/teardown

Utilities available:
- `BuildDownloadManager` - Download APK from Firebase
- `FirebaseTestRunner` - Orchestrate download + tests + reports
- `DriverManager` - Appium driver initialization
- `FirebaseManager` - Firebase Admin SDK operations

---

See [FIREBASE_SETUP.md](./FIREBASE_SETUP.md) for detailed instructions.
