# Music Player Pro — Android 2.3 (Gingerbread) Build

A minimal, dependency-free music player targeting **API 9–10 (Android 2.3 Gingerbread)**.
Scans on-device music via `MediaStore`, plays via a background `Service` + `MediaPlayer`,
and shows a plain `ListView` + transport controls (Prev / Play-Pause / Next / SeekBar).

No AndroidX, no support libraries, no ConstraintLayout — all of that postdates 2.3 and
won't compile against this target anyway.

## Why you can't just hit "Run" in current Android Studio

Modern Android Studio (Flamingo/Koala/etc.) ships an AGP (Android Gradle Plugin) version
that **refuses `minSdkVersion`/`compileSdkVersion` below ~14–16** in practice, and current
build-tools no longer ship binaries compatible with API 10 targeting. You have two realistic
options:

### Option A — Old Android Studio (recommended, easiest)
1. Download **Android Studio 2.3.3** (the last version comfortable with this era) or
   **Eclipse ADT** if you still have it archived.
2. Install **SDK Platform API 10** and **SDK Build-Tools 17.0.0** via the SDK Manager
   (old versions — you may need to check "show obsolete packages").
3. Open this project folder directly (`File > Open`) — it already has `build.gradle` set to
   AGP `1.5.0` and Gradle `2.8`, both compatible with API 10.
4. Build > Build APK.

### Option B — Command line with old Gradle
```
cd MusicPlayerPro
./gradlew assembleDebug
```
This requires `ANDROID_HOME` pointing at an SDK that has `platforms/android-10` and
`build-tools/17.0.0` installed. Gradle wrapper is pinned to 2.8 in
`gradle/wrapper/gradle-wrapper.properties` — do not let it auto-upgrade.

The output APK will be at:
```
app/build/outputs/apk/app-debug.apk
```

## Getting it onto your device
Since you're transferring by file copy rather than `adb`:
1. Copy `app-debug.apk` onto the device's SD card / internal storage (USB mass storage,
   MTP, or however your Gingerbread device mounts).
2. On the device, enable **Settings > Applications > Unknown sources** (this is where
   that toggle lives pre-Android 8).
3. Use a file manager app on the device to browse to the APK and tap it to install.

## Notes / limitations on real Gingerbread hardware
- `MediaStore` audio scanning can be slow on old/slow storage — this is normal for the era.
- No runtime permission prompts (API 10 predates that model — permissions are granted at
  install time from the manifest, which is why `READ_EXTERNAL_STORAGE` is declared there).
- If your device's `MediaStore` index is empty (never scanned), you may need to trigger a
  media scan via the stock Gallery/Music app first, or the song list will show empty.
- No album art in this version — art loading involves either bitmap decoding you'll want to
  size-limit carefully on a 2010-era device's limited RAM, or embedded-tag extraction, which
  I left out to keep this a clean starting point. Happy to add either as a next step.

## Project layout
```
MusicPlayerPro/
├── build.gradle                 (root, pins AGP 1.5.0)
├── settings.gradle
├── gradle/wrapper/gradle-wrapper.properties   (pins Gradle 2.8)
└── app/
    ├── build.gradle              (minSdk 9, targetSdk 10)
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/taha/musicplayerpro/
        │   ├── MainActivity.java
        │   ├── PlaybackService.java
        │   ├── MusicLibrary.java
        │   └── Song.java
        └── res/
            ├── layout/activity_main.xml
            ├── values/strings.xml
            └── drawable/ic_launcher.png
```
