# Firebase Setup for Pet Safety Android

## Required: google-services.json

The `google-services.json` file should be placed in the `app/` folder. This file is required for Firebase Cloud Messaging (FCM) to work.

## Setup Steps

### 1. Firebase Project Setup (if not already done)

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Create a new project or use existing "Pet Safety EU" project

### 2. Add Android App to Firebase

1. Click "Add app" and select Android
2. Enter package name: `com.petsafety.app`
3. Download the `google-services.json` file
4. Place it in the `app/` folder

### 3. Backend Configuration

The backend needs these Firebase credentials (set in environment variables):

```
FIREBASE_PROJECT_ID=tagme-ae074
FIREBASE_CLIENT_EMAIL=firebase-adminsdk-xxx@tagme-ae074.iam.gserviceaccount.com
FIREBASE_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n"
```

Get these from Firebase Console > Project Settings > Service Accounts > Generate new private key.

## FCM Token Flow

1. **App Launch**: Firebase automatically generates FCM token
2. **User Login**: Token is registered with backend via `POST /users/fcm-tokens`
3. **User Logout**: Token is removed from backend via `DELETE /users/fcm-tokens/{token}`
4. **Token Refresh**: New token is automatically registered via `PetSafetyFirebaseMessagingService`

## Notification Channels

The app creates these notification channels:

| Channel ID | Name | Priority | Description |
|------------|------|----------|-------------|
| `tag_scans` | Tag Scans | High | Pet tag scanned notifications |
| `missing_alerts` | Missing Pet Alerts | High | Missing pets in area |
| `sightings` | Sighting Reports | High | Pet sighting notifications |
| `pet_safety_general` | General | Default | Other notifications |

## Notification Types

The app handles these FCM notification types:

| Type | Description | Location Data |
|------|-------------|---------------|
| `PET_SCANNED` | Pet's tag was scanned | Optional (3-tier consent) |
| `MISSING_PET_ALERT` | Pet reported missing nearby | No |
| `PET_FOUND` | Missing pet has been found | No |
| `SIGHTING_REPORTED` | New sighting for user's pet | Yes |

## 3-Tier Location Consent (GDPR Compliant)

When someone scans a pet's tag, they can choose:

1. **Precise Location**: Exact GPS coordinates shared
2. **Approximate Location**: Rounded to ~500m accuracy (3 decimal places)
3. **No Location**: Only notification that tag was scanned

## Testing Push Notifications

### Using Firebase Console

1. Go to Firebase Console > Cloud Messaging
2. Click "Send your first message"
3. Enter test message
4. Target by FCM token (get from logcat)

### Using Backend API

```bash
# Get your FCM token from logcat (look for "FCM token:")

# Test via backend (requires auth token)
curl -X POST https://senra.pet/api/test/fcm \
  -H "Authorization: Bearer YOUR_AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "PET_SCANNED",
    "pet_name": "Buddy",
    "pet_id": "test-123"
  }'
```

## Files Changed for FCM

### New Files
- `app/src/main/java/com/petsafety/app/data/fcm/PetSafetyFirebaseMessagingService.kt`
- `app/src/main/java/com/petsafety/app/data/fcm/FCMRepository.kt`
- `app/src/main/java/com/petsafety/app/ui/components/MapAppPickerDialog.kt`

### Modified Files
- `app/build.gradle.kts` - Firebase dependencies
- `build.gradle.kts` - Google services plugin
- `gradle/libs.versions.toml` - Firebase versions
- `app/src/main/AndroidManifest.xml` - FCM service
- `app/src/main/java/com/petsafety/app/data/notifications/NotificationHelper.kt` - Rich notifications
- `app/src/main/java/com/petsafety/app/data/repository/AuthRepository.kt` - FCM token lifecycle
- `app/src/main/java/com/petsafety/app/data/repository/QrRepository.kt` - 3-tier location
- `app/src/main/java/com/petsafety/app/ui/screens/QrScannerScreen.kt` - Location consent UI
- `app/src/main/java/com/petsafety/app/ui/viewmodel/QrScannerViewModel.kt` - Location consent
- `app/src/main/java/com/petsafety/app/ui/MainActivity.kt` - Notification handling
- `app/src/main/java/com/petsafety/app/ui/PetSafetyApp.kt` - Map picker dialog
- `app/src/main/java/com/petsafety/app/PetSafetyApplication.kt` - Firebase init
- `app/src/main/java/com/petsafety/app/di/AppModule.kt` - FCM DI
- `app/src/main/res/values/strings.xml` - Notification channel strings

## Troubleshooting

### No FCM token received
- Check if `google-services.json` is in `app/` folder
- Check if Firebase is initialized in `PetSafetyApplication`
- Check logcat for errors: `adb logcat -s FCMService PetSafetyApp`

### Notifications not showing
- Check notification permissions (Android 13+)
- Check notification channel is not blocked in settings
- Check if app is in foreground (foreground notifications show differently)

### Token not registering with backend
- Check auth token is valid
- Check backend `/users/fcm-tokens` endpoint
- Check network connectivity
