# List It (Android)

Kotlin + Jetpack Compose port of the List It iOS app, with the list cards, tab bar, Settings,
Progress and task/note views taken from the web app's Capacitor (Android) design. Same Supabase
backend as the iOS and web apps.

- Package / application id: `com.list_it.app`
- Min SDK 26 (Android 8), target SDK 36, compile SDK 37
- Kotlin 2.4, Android Gradle Plugin 9.5, Gradle 9.8, Jetpack Compose (Material 3)

## Run

1. Open the `ListIt` folder in Android Studio (Narwhal or newer) and let Gradle sync.
2. Run the `app` configuration on a device or emulator.

Supabase URL and anon key live in `gradle.properties` (`SUPABASE_URL`, `SUPABASE_KEY`).

## Tests

```
./gradlew testDebugUnitTest lintDebug
```

- `SupabaseTest`: every Supabase call, sign-up/sign-in flows, validation and dates, against an
  in-memory fake Supabase server (no network needed).
- `AppUiTest`: Robolectric UI tests that drive the real screens (sign-up, login, lists, tasks,
  notes, settings, all default lists) and save screenshots to `app/build/screenshots/`.

## Release build (Google Play)

1. Create an upload key once (keep it and its passwords safe):
   ```
   keytool -genkeypair -v -keystore upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Copy `keystore.properties.example` to `keystore.properties` and fill it in.
   Both files are git-ignored.
3. Build the bundle to upload to Play Console:
   ```
   ./gradlew bundleRelease
   ```
   Output: `app/build/outputs/bundle/release/app-release.aab`

   Without `keystore.properties` this fails with an explicit error instead of producing an
   unsigned bundle that Play rejects on upload. Once an app is live on Play the upload key can
   never change, so use the same keystore every release.
4. In Play Console, enable Play App Signing, upload the `.aab`, and add a privacy policy URL
   (required because the app has accounts) plus the Data safety form (email, name, user content).
5. Upload `app/build/outputs/mapping/release/mapping.txt` alongside the bundle, or release
   crash reports stay obfuscated.

## Minimum SDK

`minSdk` is 24 (Android 7.0), which constrains three things:

- `java.time` only entered the platform at API 26, so core library desugaring is enabled in
  `app/build.gradle.kts`. Do not turn it off.
- ML Kit's GenAI Prompt API declares `minSdk 26`. It is force-enabled via `tools:overrideLibrary`
  in the manifest, and `OnDeviceModel` refuses to touch the library below API 26 so the class is
  never loaded on a device that cannot support it.
- Adaptive icons live in `mipmap-anydpi-v26/`; API 24–25 falls back to the plain PNGs in the
  density buckets.

Run `./gradlew :app:lintDebug` after touching any of these — lint's `NewApi` check is what proves
the app still runs on API 24.

## Store assets

- `docs/play_store_icon_512.png`: the 512x512 Google Play listing icon (same artwork as the launcher icon).
- `docs/screenshots/`: screenshots of every screen, produced by the UI tests.

## Project structure

```
app/src/main/java/com/list_it/app/
├── MainActivity.kt, ListItApplication.kt
├── model/          Data classes and enums (List, Collection, Task, Note, …)
├── data/           Supabase (state + all API calls), Prefs, reminders, on-device AI
└── ui/
    ├── theme/      iOS colours and type scale
    ├── components/ Shared UI (sheets, alerts, menus, text fields, colour picker, AI views)
    └── screens/    Intro, AllLists, ListDetail, auth/, creating/, detail/,
                    defaultlists/, settings/, ai/
```

## iOS → Android equivalents

| iOS | Android |
| --- | --- |
| SwiftUI | Jetpack Compose |
| `@AppStorage` | `Prefs` (SharedPreferences) |
| supabase-swift | supabase-kt |
| Local notifications | AlarmManager + notification (reminders survive reboot) |
| EventKit "Add to Calendar" | Calendar app insert intent |
| ShareLink | Android share sheet |
| Apple Foundation Models | Gemini Nano via ML Kit Prompt API (on-device; AI buttons hide on unsupported devices) |
| MarkdownUI | multiplatform-markdown-renderer |
| Lottie | lottie-compose |
