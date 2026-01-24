# Pet Safety Android App

Native Android application for the Pet Safety QR tag system.

## Features
- OTP-based authentication
- Pet management with photo uploads and gallery
- QR code scanning and tag activation
- Missing pet alerts and community sightings
- Offline mode with action queue + auto sync
- Real-time SSE notifications + local notifications
- Orders and replacement tags
- Profile, address, and notification preferences

## Requirements
- Android Studio (latest stable)
- Android SDK 34
- JDK 17

## Setup
1. Open project in Android Studio.
2. Sync Gradle files.
3. Update base URLs if needed in `ApiClient` and `SseService`.
4. Run on emulator or device.

## Permissions
- Camera (QR scanning, pet photos)
- Location (alerts and share location)
- Notifications (SSE local notifications)

## Testing
Run unit tests:
```
./gradlew test
```

Run instrumentation tests:
```
./gradlew connectedAndroidTest
```

## Offline Mode Tests
See `OFFLINE_MODE_TEST_PLAN_ANDROID.md`.
