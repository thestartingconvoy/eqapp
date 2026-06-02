# Car Equalizer Test

A small native Android test app for car-style landscape screens. It uses a plain Java `Activity`, Android SDK views, and a real `android.media.audiofx.Equalizer` initialized against audio session `0` / the global output mix.

The app detects the device's actual EQ band count, center frequencies, and gain range at runtime. Slider changes call `equalizer.setBandLevel()`. If the platform refuses to initialize the global equalizer, the screen shows a clear error and disables EQ controls.

The `Sync Presets` button fetches active presets from:

```text
https://eqapp-admin.vercel.app/api/presets
```

The app caches the API response locally for offline use, maps each JSON `hz` value to the nearest detected Android EQ band, averages gains when multiple JSON bands map to one Android band, converts `gainDb` to Android millibels, clamps to the device range, and reapplies the last selected preset on restart.

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
