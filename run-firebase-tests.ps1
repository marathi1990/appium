#!/usr/bin/env pwsh

################################################################################
# Firebase Build Download & Test Automation Script
# 
# Purpose:
# - Downloads latest APK from Firebase App Distribution
# - Runs Appium tests against the downloaded build
# - Generates Allure test reports
# - Can run on schedule or manually
#
# Prerequisites:
# - Maven installed and in PATH
# - Appium server running (on port 4723)
# - Android emulator running
# - Firebase service account JSON configured
# - Environment variables set (see Setup section)
#
# Usage:
# .\run-firebase-tests.ps1 -TestSuite smoke
# .\run-firebase-tests.ps1 -TestSuite regression
# .\run-firebase-tests.ps1 -All
# .\run-firebase-tests.ps1 -DownloadOnly
#
################################################################################

param(
    [Parameter(Mandatory = $false)]
    [ValidateSet('smoke', 'regression', 'all')]
    [string]$TestSuite = 'smoke',
    
    [Parameter(Mandatory = $false)]
    [switch]$DownloadOnly = $false,
    
    [Parameter(Mandatory = $false)]
    [switch]$All = $false,
    
    [Parameter(Mandatory = $false)]
    [switch]$GenerateReport = $false,
    
    [Parameter(Mandatory = $false)]
    [switch]$Cleanup = $false
)

# ─────────────────────────────────────────────
# Configuration
# ─────────────────────────────────────────────

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$ProjectRoot = $ScriptDir
$AppiumPort = 4723
$AppiumUrl = "http://127.0.0.1:$AppiumPort"

# Colors for output
$Colors = @{
    Success = 'Green'
    Error   = 'Red'
    Warning = 'Yellow'
    Info    = 'Cyan'
    Section = 'Magenta'
}

# ─────────────────────────────────────────────
# Functions
# ─────────────────────────────────────────────

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "════════════════════════════════════════════════════════" -ForegroundColor $Colors.Section
    Write-Host "  $Title" -ForegroundColor $Colors.Section
    Write-Host "════════════════════════════════════════════════════════" -ForegroundColor $Colors.Section
}

function Write-Success {
    param([string]$Message)
    Write-Host "✅ $Message" -ForegroundColor $Colors.Success
}

function Write-Error {
    param([string]$Message)
    Write-Host "❌ $Message" -ForegroundColor $Colors.Error
}

function Write-Warning {
    param([string]$Message)
    Write-Host "⚠️  $Message" -ForegroundColor $Colors.Warning
}

function Write-Info {
    param([string]$Message)
    Write-Host "ℹ️  $Message" -ForegroundColor $Colors.Info
}

function Test-Requirements {
    Write-Section "Checking Requirements"
    
    $requirements = @{
        'Maven' = { mvn -v }
        'Java'  = { java -version }
        'Git'   = { git --version }
    }
    
    foreach ($tool in $requirements.Keys) {
        try {
            & $requirements[$tool] | Out-Null
            Write-Success "$tool is installed"
        } catch {
            Write-Error "$tool is not installed or not in PATH"
            exit 1
        }
    }
}

function Test-Appium {
    Write-Info "Testing Appium connection at $AppiumUrl..."
    
    try {
        $response = Invoke-WebRequest -Uri "$AppiumUrl/status" -Method GET -TimeoutSec 5 -ErrorAction Stop
        if ($response.StatusCode -eq 200) {
            Write-Success "Appium server is running and responding"
            return $true
        }
    } catch {
        Write-Warning "Appium server at $AppiumUrl is not responding"
        Write-Info "Make sure Appium server is running: appium --loglevel debug"
        return $false
    }
}

function Test-Emulator {
    Write-Info "Testing Android emulator..."
    
    try {
        $devices = adb devices | Select-Object -Skip 1 | Where-Object { $_ -match 'device$' }
        if ($devices) {
            Write-Success "Android emulator is connected"
            return $true
        } else {
            Write-Warning "No Android devices found"
            return $false
        }
    } catch {
        Write-Warning "Could not check Android devices"
        return $false
    }
}

function Check-Environment {
    Write-Section "Checking Environment Variables"
    
    $vars = @{
        'FIREBASE_APP_ID' = 'Firebase App ID'
        'FIREBASE_SERVICE_ACCOUNT_PATH' = 'Firebase Service Account Path (optional)'
    }
    
    foreach ($var in $vars.Keys) {
        $value = [Environment]::GetEnvironmentVariable($var)
        if ($value) {
            Write-Success "$var is set"
        } else {
            if ($var -eq 'FIREBASE_SERVICE_ACCOUNT_PATH') {
                Write-Info "$var is not set (will use default: src/test/resources/firebase-service-account.json)"
            } else {
                Write-Warning "$var is not set - Downloads will use local APK instead"
            }
        }
    }
}

function Download-Build {
    Write-Section "Download Latest APK from Firebase"
    
    try {
        Push-Location $ProjectRoot
        
        Write-Info "Running build download..."
        mvn clean compile -q
        
        Write-Info "Downloading from Firebase App Distribution..."
        java -cp "target/classes:$([string]::Join(';', (Get-ChildItem -Path "$ProjectRoot\.m2\repository" -Recurse -Include "*.jar" | ForEach-Object { $_.FullName })))" `
            utils.FirebaseTestRunner
        
        $buildInfo = mvn exec:java -Dexec.mainClass="utils.FirebaseTestRunner" 2>&1 | Select-String "Downloaded APK"
        
        if ($buildInfo) {
            Write-Success "Build downloaded successfully"
            Write-Info $buildInfo
            return $true
        } else {
            Write-Error "Build download may have failed - check output above"
            return $false
        }
    } catch {
        Write-Error "Build download failed: $_"
        return $false
    } finally {
        Pop-Location
    }
}

function Run-Tests {
    param([string]$Suite)
    
    Write-Section "Run $Suite.ToUpper() Tests"
    
    try {
        Push-Location $ProjectRoot
        
        Write-Info "Starting test execution..."
        Write-Info "Timestamp: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
        Write-Info ""
        
        $suiteClass = switch ($Suite.ToLower()) {
            'smoke' { 'tests.SmokeSuite' }
            'regression' { 'tests.RegressionSuite' }
            default { "tests.$Suite" }
        }
        
        $command = "mvn -Dtest=$suiteClass test -DskipUnitTests=true"
        Write-Info "Running: $command"
        Write-Info ""
        
        mvn -Dtest=$suiteClass test -DskipUnitTests=true
        
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Tests completed successfully"
            return $true
        } else {
            Write-Error "Tests failed with exit code $LASTEXITCODE"
            return $false
        }
    } catch {
        Write-Error "Test execution failed: $_"
        return $false
    } finally {
        Pop-Location
    }
}

function Generate-Report {
    Write-Section "Generate Allure Test Report"
    
    try {
        Push-Location $ProjectRoot
        
        Write-Info "Generating Allure report..."
        mvn allure:report -DskipTests=true -q
        
        Write-Success "Report generated successfully"
        Write-Info "Open report: target/allure-report/index.html"
        Write-Info ""
        
        # Try to open in browser on Windows
        if ($PSVersionTable.Platform -eq 'Win32NT' -or -not $PSVersionTable.Platform) {
            $reportPath = Join-Path $ProjectRoot "target\allure-report\index.html"
            if (Test-Path $reportPath) {
                Write-Info "Opening report in default browser..."
                & $reportPath
            }
        }
        
        return $true
    } catch {
        Write-Warning "Report generation had issues: $_"
        return $false
    } finally {
        Pop-Location
    }
}

function Cleanup-Builds {
    Write-Section "Cleanup: Remove Old Builds"
    
    try {
        $appDir = Join-Path $ProjectRoot "app"
        if (Test-Path $appDir) {
            Get-ChildItem -Path $appDir -Filter "*.apk" -File | Sort-Object LastWriteTime -Descending | Select-Object -Skip 1 | Remove-Item -Force
            Write-Success "Old builds cleaned up"
        }
    } catch {
        Write-Warning "Cleanup failed: $_"
    }
}

function Show-Summary {
    param(
        [hashtable]$Results
    )
    
    Write-Section "Test Execution Summary"
    
    Write-Info "Download: $(if ($Results.Download) { '✅ PASSED' } else { '❌ FAILED' })"
    Write-Info "Tests: $(if ($Results.Tests) { '✅ PASSED' } else { 'SKIPPED or ❌ FAILED' })"
    Write-Info "Report: $(if ($Results.Report) { '✅ GENERATED' } else { 'SKIPPED' })"
    
    Write-Info ""
    if ($Results.Download -and $Results.Tests) {
        Write-Success "All checks passed! ✨"
    } else {
        Write-Error "Some checks failed - review output above"
    }
}

# ─────────────────────────────────────────────
# Main Execution
# ─────────────────────────────────────────────

Clear-Host
Write-Host "╔════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║    Firebase Build Download & Test Automation Script     ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# Step 1: Check requirements
Test-Requirements

# Step 2: Check environment
Check-Environment

# Step 3: Check prerequisites
$appiumReady = Test-Appium
$emulatorReady = Test-Emulator

if (-not $appiumReady -or -not $emulatorReady) {
    Write-Warning "Some prerequisites are missing - tests may fail"
    Read-Host "Press Enter to continue anyway, or Ctrl+C to exit"
}

# Step 4: Execute based on parameters
$results = @{
    Download = $false
    Tests    = $false
    Report   = $false
}

# Download build
if (-not $DownloadOnly) {
    $results.Download = Download-Build
} else {
    $results.Download = Download-Build
    Write-Success "Download complete - exiting"
    exit 0
}

# Run tests
if ($results.Download) {
    if ($All) {
        $results.Tests = (Run-Tests -Suite 'smoke') -and (Run-Tests -Suite 'regression')
    } else {
        $results.Tests = Run-Tests -Suite $TestSuite
    }
}

# Generate report
if ($results.Tests -and $GenerateReport) {
    $results.Report = Generate-Report
}

# Cleanup
if ($Cleanup) {
    Cleanup-Builds
}

# Show summary
Show-Summary $results

Write-Host ""
Write-Info "Script completed at $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
Write-Host ""

exit (if ($results.Download -and $results.Tests) { 0 } else { 1 })
