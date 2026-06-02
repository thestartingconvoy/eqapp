$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$apkSource = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
$apkOutDir = Join-Path $root "dist"
$apkOut = Join-Path $apkOutDir "CarEqualizerTest-debug.apk"

Set-Location $root
Remove-Item -Force $apkOut -ErrorAction SilentlyContinue

if (-not $env:JAVA_HOME) {
    $androidStudioJbr = "C:\Program Files\Android\Android Studio\jbr"
    if (Test-Path (Join-Path $androidStudioJbr "bin\java.exe")) {
        $env:JAVA_HOME = $androidStudioJbr
        $env:PATH = (Join-Path $androidStudioJbr "bin") + ";" + $env:PATH
    }
}

if (Test-Path ".\gradlew.bat") {
    .\gradlew.bat assembleDebug
} else {
    gradle assembleDebug
}
if ($LASTEXITCODE -ne 0) {
    throw "Gradle build failed with exit code $LASTEXITCODE"
}

New-Item -ItemType Directory -Force -Path $apkOutDir | Out-Null
Copy-Item -Force $apkSource $apkOut
Write-Host "Debug APK copied to: $apkOut"
