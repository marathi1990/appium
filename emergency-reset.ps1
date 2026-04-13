# Emergency Emulator Reset Script
# This is a last-resort fix for persistent "Broken pipe" errors

Write-Host "==== EMERGENCY APPIUM RECOVERY ===" -ForegroundColor Red
Write-Host "This will perform a complete reset of ADB and the emulator" -ForegroundColor Yellow

$adbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

Write-Host "`n[1/6] Killing all adb processes..." -ForegroundColor Yellow
taskkill /F /IM adb.exe 2>$null
Start-Sleep -Seconds 2

Write-Host "[2/6] Killing emulator..." -ForegroundColor Yellow
taskkill /F /IM emulator.exe 2>$null
taskkill /F /IM emulator64-arm.exe 2>$null
Start-Sleep -Seconds 3

Write-Host "[3/6] Clearing ADB cache and config..." -ForegroundColor Yellow
$adbDataPath = "$env:USERPROFILE\.android"
if (Test-Path "$adbDataPath\adb_keys") {
    Remove-Item "$adbDataPath\adb_keys" -Force -ErrorAction SilentlyContinue
}
if (Test-Path "$adbDataPath\adbkey.pub") {
    Remove-Item "$adbDataPath\adbkey.pub" -Force -ErrorAction SilentlyContinue
}

Write-Host "[4/6] Starting fresh ADB daemon..." -ForegroundColor Yellow
& $adbPath start-server
Start-Sleep -Seconds 3

Write-Host "[5/6] Checking device status..." -ForegroundColor Yellow
$devices = & $adbPath devices
Write-Host $devices -ForegroundColor Cyan

if ($devices -match "emulator-5554") {
    Write-Host "`n[6/6] Clearing emulator data (UIAutomator2)..." -ForegroundColor Yellow
    & $adbPath -s emulator-5554 shell "pm clear io.appium.settings" 2>$null
    & $adbPath -s emulator-5554 shell "pm clear io.appium.uiautomator2.server" 2>$null
    & $adbPath -s emulator-5554 shell "pm clear io.appium.uiautomator2.server.test" 2>$null
    & $adbPath -s emulator-5554 shell "am kill-all" 2>$null
    Write-Host "✓ Recovery complete" -ForegroundColor Green
} else {
    Write-Host "`n✗ Emulator is not running!" -ForegroundColor Red
    Write-Host "  Please start the emulator in Android Studio before running tests"
}

Write-Host "`nNext steps:" -ForegroundColor Cyan
Write-Host "  1. Restart Appium: appium --loglevel debug"
Write-Host "  2. Run your tests"
