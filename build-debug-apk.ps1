$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$apkSource = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
$apkOutDir = Join-Path $root "dist"
$apkOut = Join-Path $apkOutDir "CarEqualizerTest-debug.apk"

Set-Location $root

if (Test-Path ".\gradlew.bat") {
    .\gradlew.bat assembleDebug
} else {
    gradle assembleDebug
}

New-Item -ItemType Directory -Force -Path $apkOutDir | Out-Null
Copy-Item -Force $apkSource $apkOut
Write-Host "Debug APK copied to: $apkOut"
