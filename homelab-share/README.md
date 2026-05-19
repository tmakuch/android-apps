# homelab-share

An Android app that appears in the system share menu and forwards shared content to a homelab server via HTTP. It has no launcher icon and does not appear in the app drawer.

## What it does

- **Text** — POSTs `{"type": "text", "data": "..."}` as JSON
- **URLs** — POSTs `{"type": "url", "data": "..."}` as JSON (auto-detected from `text/plain` shares)
- **Images** — POSTs raw bytes as `application/octet-stream` with `X-Mime-Type` and `X-File-Name` headers

Shows a toast on success. On error, posts a persistent notification with the server's response body.

## Requirements

- Android SDK (`sdk.dir` in `local.properties`)
- JDK 8+
- Gradle wrapper included (`./gradlew`)
- Make sure that `values/strings.xml` points to the right url.

## local.properties

Create `local.properties` in the project root (already gitignored):

```properties
sdk.dir=/path/to/android/sdk
keystore.path=../homelab.jks
keystore.password=your-keystore-password
keystore.alias=homelab
keystore.keyPassword=your-key-password
```

## Build

```bash
./gradlew assembleDebug    # debug APK
./gradlew assembleRelease  # release APK
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
adb logcat -s HomelabShare
```

### Release

```bash
./gradlew assembleRelease
adb install app/build/outputs/apk/release/app-release.apk
```

Upgrading over an existing release build (same keystore) installs in-place. Switching from debug to release requires uninstalling first:

```bash
adb uninstall com.homelab.share
```

## Generating a keystore

One-time setup, shared across all homelab apps:

```bash
keytool -genkey -v -keystore homelab.jks -alias homelab -keyalg RSA -keysize 2048 -validity 10000
```

Keep the `.jks` file outside the repository and back it up — losing it means losing the ability to update installed apps.
