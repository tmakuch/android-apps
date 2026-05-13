# homelab-share

An Android app that appears in the system share menu and forwards shared content to a homelab server via HTTP. It has no launcher icon and does not appear in the app drawer.

## What it does

- **Text** — POSTs `{"type": "text", "data": "..."}` as JSON
- **URLs** — POSTs `{"type": "url", "data": "..."}` as JSON (auto-detected from `text/plain` shares)
- **Images** — POSTs raw bytes with the image's MIME type as `Content-Type`

The server URL is configured in `ShareActivity.kt`:

```kotlin
private const val SERVER_URL = "https://your-server/endpoint"
```

## Requirements

- Android SDK (set `sdk.dir` in `local.properties`)
- JDK 8+
- Gradle (or use the included wrapper)

## Build

```bash
./gradlew assembleDebug    # debug APK
./gradlew assembleRelease  # release APK (requires signing config, see below)
```

Output: `app/build/outputs/apk/debug/` or `.../release/`

## Deploy

### Debug

Enable **Developer Options** on your device (tap *Build Number* 7 times in Settings → About), then enable **USB Debugging**. Connect via USB, then:

```bash
./gradlew installDebug
```

Stream logs in real time:

```bash
adb logcat --pid=$(adb shell pidof com.homelab.share)
```

### Release

Generate a keystore (one-time):

```bash
keytool -genkey -v -keystore homelab-share.jks -alias homelab -keyalg RSA -keysize 2048 -validity 10000
```

Add a signing config to `app/build.gradle`:

```groovy
android {
    signingConfigs {
        release {
            storeFile file("../../homelab-share.jks")
            storePassword "your-store-password"
            keyAlias "homelab"
            keyPassword "your-key-password"
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
        }
    }
}
```

> **Do not commit the keystore or passwords to version control.** Store credentials in `local.properties` and read them in the Gradle file instead.

Build and install:

```bash
./gradlew assembleRelease
adb install app/build/outputs/apk/release/app-release.apk
```
