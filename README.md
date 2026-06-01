# Car Equalizer Test

A small native Android test app for car-style landscape screens. It uses a plain Java `Activity`, Android SDK views, five equalizer bands, preset cycling, reset, and a custom EQ curve preview.

## Local build commands

From this folder:

```powershell
gradle assembleDebug
New-Item -ItemType Directory -Force -Path .\dist
Copy-Item -Force .\app\build\outputs\apk\debug\app-debug.apk .\dist\CarEqualizerTest-debug.apk
```

If you open the folder in Android Studio, use:

```powershell
.\gradlew.bat assembleDebug
New-Item -ItemType Directory -Force -Path .\dist
Copy-Item -Force .\app\build\outputs\apk\debug\app-debug.apk .\dist\CarEqualizerTest-debug.apk
```

when Android Studio has generated or repaired the Gradle wrapper. You can also run:

```powershell
.\build-debug-apk.ps1
```

The default Gradle debug APK is produced at:

```text
app\build\outputs\apk\debug\app-debug.apk
```

The clearly named copy is:

```text
dist\CarEqualizerTest-debug.apk
```
