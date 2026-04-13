# Comprehensive Appium/Emulator Diagnostic Script
# Usage: powershell -ExecutionPolicy Bypass -File diagnose-appium.ps1

param(
    [switch]$Fix = $false
)

$adbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$emulatorPath = "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe"

function Log-Section {
    param([string]$Title)
    Write-Host "`n" + ("=" * 60) -ForegroundColor Cyan
    Write-Host $Title -ForegroundColor Cyan
    Write-Host ("=" * 60) -ForegroundColor Cyan
}

function Log-Success {
    param([string]$Message)
    Write-Host "✓ $Message" -ForegroundColor Green
}

function Log-Warning {
    param([string]$Message)
    Write-Host "⚠ $Message" -ForegroundColor Yellow
}

function Log-Error {
    param([string]$Message)
    Write-Host "✗ $Message" -ForegroundColor Red
}

# ============== DIAGNOSTICS ==============

Log-Section "SYSTEM DIAGNOSTICS"

# Check if ADB exists
if (Test-Path $adbPath) {
    Log-Success "ADB found at: $adbPath"
} else {
    Log-Error "ADB not found! Install Android SDK Platform Tools"
    exit 1
}

# Check disk space
$diskFree = (Get-Volume C).SizeRemaining / 1GB
Write-Host "Free disk space: $([math]::Round($diskFree, 2)) GB"
if ($diskFree -lt 5) {
    Log-Warning "Low disk space! UIAutomator2 needs at least 5GB free"
}

# Check running processes
Log-Section "RUNNING PROCESSES"
$adbRunning = Get-Process adb -ErrorAction SilentlyContinue
$emulatorRunning = Get-Process emulator -ErrorAction SilentlyContinue

if ($adbRunning) {
    Log-Success "ADB daemon is running (PID: $($adbRunning.Id))"
} else {
    Log-Warning "ADB daemon not running - will be auto-started"
}

if ($emulatorRunning) {
    Log-Success "Emulator is running (PID: $($emulatorRunning.Id))"
} else {
    Log-Error "Emulator is NOT running - start it before running tests!"
    exit 1
}

# Check ADB connection
Log-Section "ADB CONNECTION"
$devices = & $adbPath devices
Write-Host $devices
if ($devices -match "emulator-5554") {
    if ($devices -match "device`$") {
        Log-Success "Emulator is connected and responsive"
    } else {
        Log-Warning "Emulator found but NOT in 'device' state"
    }
} else {
    Log-Error "Emulator not found in ADB device list!"
}

# Check emulator health
Log-Section "EMULATOR HEALTH"
$uptime = & $adbPath -s emulator-5554 shell "uptime" 2>$null
if ($uptime) {
    Log-Success "Emulator shell is responsive"
    Write-Host "  Uptime: $uptime"
} else {
    Log-Warning "Emulator shell not responding"
}

# Check RAM availability on emulator
$memInfo = & $adbPath -s emulator-5554 shell "cat /proc/meminfo" 2>$null
if ($memInfo) {
    $memTotal = ($memInfo -match "MemTotal" | Select-String -Pattern "(\d+)" | ForEach-Object {$_.Matches[0].Value}) / 1024
    $memFree = ($memInfo -match "MemFree" | Select-String -Pattern "(\d+)" | ForEach-Object {$_.Matches[0].Value}) / 1024
    Log-Success "Emulator memory: Total ${memTotal}MB, Free ${memFree}MB"
    if ($memTotal -lt 2048) {
        Log-Warning "Emulator RAM is only $([math]::Round($memTotal))MB - recommend 4GB!"
    }
} else {
    Log-Warning "Could not read emulator memory info"
}

# Check disk space on emulator
$dfOutput = & $adbPath -s emulator-5554 shell "df /data" 2>$null
if ($dfOutput) {
    Write-Host "Emulator /data partition:"
    $dfOutput | Select-Object -Last 1 | Write-Host
} else {
    Log-Warning "Could not read emulator disk space"
}

# Check UIAutomator2 artifacts
Log-Section "UIAUTOMATOR2 ARTIFACTS"
$settingsApkExists = & $adbPath -s emulator-5554 shell "pm list packages | grep io.appium.settings" 2>$null
if ($settingsApkExists) {
    Log-Warning "io.appium.settings already installed - may cause conflicts"
    if ($Fix) {
        Write-Host "Clearing io.appium.settings..."
        & $adbPath -s emulator-5554 shell "pm clear io.appium.settings"
        Log-Success "Cleared"
    }
} else {
    Log-Success "io.appium.settings not yet installed (fresh)"
}

# Check for app package
$appPackageExists = & $adbPath -s emulator-5554 shell "pm list packages | grep com.fluenthealth.app" 2>$null
if ($appPackageExists) {
    Log-Success "App package (com.fluenthealth.app) is installed"
} else {
    Log-Warning "App package not yet installed"
}

# ============== RECOMMENDATIONS ==============

Log-Section "RECOMMENDATIONS"

if ($Fix) {
    Write-Host "Running fixes..." -ForegroundColor Yellow
    
    Write-Host "`n1. Stopping ADB daemon..."
    & $adbPath kill-server
    Start-Sleep -Seconds 1
    Log-Success "ADB stopped"
    
    Write-Host "`n2. Restarting ADB daemon..."
    & $adbPath start-server
    Start-Sleep -Seconds 2
    Log-Success "ADB restarted"
    
    Write-Host "`n3. Clearing UIAutomator2 packages..."
    & $adbPath -s emulator-5554 shell "pm clear io.appium.settings" 2>$null
    & $adbPath -s emulator-5554 shell "pm clear io.appium.uiautomator2.server" 2>$null
    & $adbPath -s emulator-5554 shell "pm clear io.appium.uiautomator2.server.test" 2>$null
    Start-Sleep -Seconds 1
    Log-Success "UIAutomator2 packages cleared"
    
    Write-Host "`n4. Killing running processes..."
    & $adbPath -s emulator-5554 shell "am kill-all" 2>$null
    Start-Sleep -Seconds 2
    Log-Success "Processes killed"
    
    Write-Host "`nFix completed!"
} else {
    Write-Host @"
To fix issues, run with -Fix flag:
  powershell -ExecutionPolicy Bypass -File diagnose-appium.ps1 -Fix

Critical checks:
  • Emulator RAM: Should be 4GB or higher
  • Disk space: Need at least 5GB free
  • Emulator state: Should be 'device' (not 'offline' or 'unavailable')
  • ADB responsiveness: Shell commands should execute quickly

If Appium still fails, increase Appium timeout and restart emulator completely
"@
}

Log-Section "DONE"
