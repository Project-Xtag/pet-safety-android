# Pet Safety Android App (pet-safety-android)

## Project Context

Building a native Android app for the Pet Safety QR tag platform. This app must achieve feature parity with the existing iOS app (pet-safety-ios) while following Android-native patterns and conventions.

**Reference:** The iOS app uses SwiftUI, Core Data, and follows MVVM. Mirror the same user flows and features, but implement using idiomatic Android/Kotlin patterns.

**Backend API:** https://senra.pet/api (production), https://staging.senra.pet/api (staging)
**API Docs:** Refer to backend repo `/docs/api.md` or Postman collection
**iOS Reference:** `/Users/viktorszasz/pet-safety-ios/PetSafety/`

---

## Tech Stack (Required)

| Layer | Technology | Notes |
|-------|------------|-------|
| Language | Kotlin | No Java except where absolutely necessary |
| UI | Jetpack Compose | Material 3 design system |
| Min SDK | 26 (Android 8.0) | ~95% device coverage |
| Target SDK | 34 (Android 14) | Latest stable |
| Architecture | MVVM + Clean Architecture | Match iOS patterns |
| DI | Hilt | Standard for Android |
| Networking | Retrofit + OkHttp | With interceptors for auth |
| Local DB | Room | Equivalent to iOS Core Data |
| Image Loading | Coil | Compose-native |
| Camera/QR | CameraX + ML Kit | For QR scanning |
| Maps | Google Maps SDK | Alert locations, sightings (display only) |
| Navigation | Intent to native apps | Google Maps, Waze, etc. via chooser |
| Payments | Stripe Android SDK | When implemented |
| Push | Firebase Cloud Messaging | Equivalent to APNs |
| Analytics | Firebase Analytics | Optional, match iOS |
| Crash Reporting | Sentry | Match iOS implementation |
| Secure Storage | EncryptedSharedPreferences | Equivalent to iOS Keychain |

---

## Project Structure

```
app/
├── src/main/
│   ├── java/com/petsafety/app/
│   │   ├── di/                     # Hilt modules
│   │   ├── data/
│   │   │   ├── local/              # Room database, DAOs, entities
│   │   │   ├── remote/             # Retrofit API, DTOs
│   │   │   └── repository/         # Repository implementations
│   │   ├── domain/
│   │   │   ├── model/              # Domain models
│   │   │   ├── repository/         # Repository interfaces
│   │   │   └── usecase/            # Business logic use cases
│   │   ├── ui/
│   │   │   ├── theme/              # Material 3 theme, colors, typography
│   │   │   ├── components/         # Reusable composables
│   │   │   ├── navigation/         # Nav graph, routes
│   │   │   ├── home/               # Home screen + ViewModel
│   │   │   ├── pets/               # Pet list, detail, edit
│   │   │   ├── alerts/             # Missing alerts, sightings
│   │   │   ├── scanner/            # QR scanner screen
│   │   │   ├── activation/         # Tag activation flow
│   │   │   ├── profile/            # User settings, subscription
│   │   │   ├── vetfinder/          # Google Maps vet/shelter lookup
│   │   │   └── auth/               # Login, register, 2FA
│   │   ├── util/                   # Extensions, helpers
│   │   └── PetSafetyApp.kt         # Application class
│   ├── res/
│   └── AndroidManifest.xml
├── build.gradle.kts
└── proguard-rules.pro
```

---

## Critical Rules (ALWAYS FOLLOW)

### Code Quality
- **No hardcoded strings** - All user-facing text in `strings.xml` for future localization
- **No hardcoded colors** - Use theme colors only (`MaterialTheme.colorScheme.*`)
- **No hardcoded dimensions** - Use `dimens.xml` or Compose `dp` constants
- **All API calls go through repositories** - Never call Retrofit directly from ViewModels
- **All ViewModels use StateFlow** - No LiveData in new code (Compose-native approach)
- **Handle configuration changes** - ViewModels must survive rotation
- **Null safety** - Never use `!!` operator; use safe calls and proper null handling

### Architecture
- **Single Activity architecture** - Use Jetpack Navigation Compose
- **Unidirectional data flow** - State flows down, events flow up
- **Repository pattern** - ViewModels never know about Room or Retrofit
- **Use cases for complex logic** - Keep ViewModels thin
- **Sealed classes for UI state** - `Loading`, `Success`, `Error` pattern

### Security
- **Tokens in EncryptedSharedPreferences** - Never plain SharedPreferences
- **Certificate pinning** - For production API calls
- **ProGuard/R8 obfuscation** - Enabled for release builds
- **No sensitive data in logs** - Strip in release builds

### Offline Support
- **Room as single source of truth** - Cache all fetched data
- **Sync queue for offline actions** - Match iOS offline behavior (see `OfflineDataManager.swift`)
- **Optimistic UI updates** - Update local first, sync in background
- **Conflict resolution** - Server wins, notify user of conflicts
- **Network monitoring** - Show offline indicator banner when disconnected
- **Sync error handling** - Retry with exponential backoff, surface persistent failures

### Image Handling
- **Compression before upload** - Max 1200px dimension, 80% JPEG quality
- **Max file size** - 5MB per image (enforced by backend)
- **Supported formats** - JPEG, PNG (no HEIC on Android)
- **Gallery max photos** - 10 photos per pet
- **Primary photo** - One photo marked as primary, shown on pet card

---

## Feature Parity Checklist (iOS Reference)

### Authentication
- [ ] Email/password login
- [ ] Registration with email verification
- [ ] 2FA via SMS/email OTP
- [ ] Password reset flow
- [ ] Biometric login (fingerprint/face)
- [ ] Session management (token refresh)
- [ ] Logout with local data cleanup

### Home Screen
- [ ] Pet cards grid/list
- [ ] Quick "Mark as Lost" action
- [ ] Quick "Mark as Found" action
- [ ] Pull-to-refresh
- [ ] Empty state for no pets

### Pet Management
- [ ] Add new pet (multi-step form)
- [ ] Edit pet details
- [ ] Pet photo gallery (multi-image)
- [ ] Camera capture for photos
- [ ] Gallery picker for photos
- [ ] Image cropping
- [ ] Delete pet (with confirmation)
- [ ] Species/breed selection (autocomplete)
- [ ] Medical notes field
- [ ] Unique features field

### QR Scanner
- [ ] Native camera QR scanning
- [ ] Flashlight toggle
- [ ] Manual code entry fallback
- [ ] Handle deep links: `senra://tag/{code}`
- [ ] Handle universal links: `https://senra.pet/qr/{code}`

### Tag Activation
- [ ] Link scanned tag to existing pet
- [ ] Link scanned tag to new pet
- [ ] Activation confirmation
- [ ] Already-activated tag handling

### Alerts (Lost Pets)
- [ ] Create missing pet alert
- [ ] Set last seen location (current location or text input)
- [ ] Add reward amount
- [ ] View active alerts (list + map view toggle)
- [ ] Alert detail with map showing last seen location
- [ ] View sighting reports on map with markers
- [ ] "Get Directions" to any sighting location (launches native map chooser)
- [ ] Resolve alert (pet found)

### Sightings & Found Pet Flow
- [ ] Report sighting for scanned lost pet
- [ ] Capture sighting photo
- [ ] Location auto-capture (share finder's location)
- [ ] Add description
- [ ] Owner receives push notification with finder's location
- [ ] Owner views finder location on in-app map preview
- [ ] "Get Directions" button launches native map app chooser
- [ ] User navigates with their preferred app (Google Maps, Waze, etc.)

### Profile & Settings
- [ ] Edit user profile (personal information)
- [ ] Manage addresses
- [ ] Manage emergency contacts
- [ ] Notification preferences (push, email, SMS toggles)
- [ ] Subscription status display
- [ ] Manage subscription (Stripe portal)
- [ ] Privacy mode settings
- [ ] Help & support view
- [ ] Delete account

### Success Stories
- [ ] View success stories feed (reunited pets)
- [ ] Success story detail view
- [ ] Submit success story prompt (after marking pet found)
- [ ] Share success story

### Notifications
- [ ] FCM push notification setup
- [ ] Pet scanned notifications
- [ ] Sighting report notifications
- [ ] Alert status notifications
- [ ] In-app notification center
- [ ] SSE real-time updates (background)

### Orders & Tags
- [ ] Stripe payment intent creation
- [ ] Order additional tags
- [ ] Order replacement tags (for damaged/lost)
- [ ] Check replacement eligibility
- [ ] Order history view
- [ ] Order detail view
- [ ] Pending tag registrations

### Vet/Shelter Network (Alert Recipients)
- [ ] Add user's vet practice (crowdsourced)
  - Name required, address optional
  - Phone OR email required (for notifications)
  - Geocoded from address or uses user's location
  - Immediately active after submission
- [ ] View user's submitted vets (`/my-suggestions`)
- [ ] Shelter/Vet self-registration flow (web link)
  - Email verification required before active
- [ ] Display which providers will be notified (within 10km radius)

**Note:** This is NOT a vet finder with maps. The purpose is to build a network of vets/shelters who receive automatic email notifications when a pet is marked lost within 10km of their location.

---

## API Integration

### Base Configuration
```kotlin
// Base URL by build type (note: path includes /api)
const val BASE_URL_PROD = "https://senra.pet/api/"
const val BASE_URL_STAGING = "https://staging.senra.pet/api/"

// Auth header interceptor pattern
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    override fun intercept(chain: Chain): Response {
        val token = tokenManager.accessToken
        val request = if (token != null) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
```

### Key Endpoints (Reference)

**Authentication**
| Endpoint | Method | Auth | Purpose |
|----------|--------|------|---------|
| `/auth/register` | POST | No | Register new user |
| `/auth/login` | POST | No | Login |
| `/auth/logout` | POST | Yes | Logout (invalidate token) |
| `/auth/forgot-password` | POST | No | Request password reset |
| `/auth/reset-password` | POST | No | Reset password with token |
| `/auth/change-password` | POST | Yes | Change password (logged in) |
| `/auth/send-otp` | POST | No | Send OTP for 2FA |
| `/auth/verify-otp` | POST | No | Verify OTP code |
| `/auth/session` | GET | Yes | Get current session info |
| `/auth/me` | GET | Yes | Get current user |

**Users**
| Endpoint | Method | Auth | Purpose |
|----------|--------|------|---------|
| `/users/me` | GET | Yes | Get user profile |
| `/users/me` | PATCH | Yes | Update user profile |
| `/users/me/notification-preferences` | GET | Yes | Get notification settings |
| `/users/me/notification-preferences` | PUT | Yes | Update notification settings |

**Pets**
| Endpoint | Method | Auth | Purpose |
|----------|--------|------|---------|
| `/pets` | GET | Yes | List user's pets |
| `/pets` | POST | Yes | Create pet |
| `/pets/{id}` | GET | Yes | Pet details |
| `/pets/{id}` | PUT | Yes | Update pet |
| `/pets/{id}` | DELETE | Yes | Delete pet |
| `/pets/{id}/mark-missing` | POST | Yes | Mark pet as missing |
| `/pets/{id}/image` | POST | Yes | Upload main pet image |
| `/pets/{id}/image` | DELETE | Yes | Delete main pet image |
| `/pets/{id}/photos` | GET | Yes | Get photo gallery |
| `/pets/{id}/photos` | POST | Yes | Upload gallery photo |
| `/pets/{id}/photos/{photoId}/primary` | PUT | Yes | Set primary photo |
| `/pets/{id}/photos/{photoId}` | DELETE | Yes | Delete photo |
| `/pets/{id}/photos/reorder` | PUT | Yes | Reorder photos |
| `/pets/public/{qrCode}` | GET | **No** | Public pet profile |
| `/pets/scan/{qrCode}` | POST | **No** | Log scan event |
| `/pets/share-location/{qrCode}` | POST | **No** | Share finder's location |

**Alerts**
| Endpoint | Method | Auth | Purpose |
|----------|--------|------|---------|
| `/alerts` | GET | Yes | User's alerts |
| `/alerts/missing` | POST | Yes | Create missing pet alert |
| `/alerts/nearby` | GET | Yes | Get alerts near location |
| `/alerts/{id}/sightings` | POST | Yes | Report sighting |
| `/alerts/{id}/found` | POST | Yes | Mark pet as found |
| `/alerts/users/location` | PUT | Yes | Update user location |
| `/alerts/users/notification-preferences` | PUT | Yes | Alert notification prefs |

**QR Tags**
| Endpoint | Method | Auth | Purpose |
|----------|--------|------|---------|
| `/qr-tags/activate` | POST | Yes | Activate tag to pet |
| `/qr-tags/scan/{code}` | GET | **No** | Get tag info (public) |
| `/qr-tags/pet/{petId}` | GET | Yes | Get tags for pet |
| `/qr-tags/pet/{petId}/history` | GET | Yes | Get scan history |
| `/qr-tags/deactivate` | POST | Yes | Deactivate a tag |
| `/qr-tags/share-location` | POST | **No** | Share finder location |

**Subscriptions**
| Endpoint | Method | Auth | Purpose |
|----------|--------|------|---------|
| `/subscriptions/plans` | GET | No | List subscription plans |
| `/subscriptions/my-subscription` | GET | Yes | Get user's subscription |
| `/subscriptions/checkout` | POST | Yes | Create checkout session |
| `/subscriptions/upgrade` | POST | Yes | Upgrade plan |
| `/subscriptions/cancel` | POST | Yes | Cancel subscription |
| `/subscriptions/reactivate` | POST | Yes | Reactivate cancelled |
| `/subscriptions/features` | GET | Yes | Get plan features |
| `/subscriptions/check-eligibility` | GET | Yes | Check upgrade eligibility |

**Orders**
| Endpoint | Method | Auth | Purpose |
|----------|--------|------|---------|
| `/orders` | GET | Yes | List orders |
| `/orders` | POST | Yes | Create order |
| `/orders/{id}` | GET | Yes | Order details |
| `/orders/{id}/items` | GET | Yes | Order line items |
| `/orders/pending-registrations` | GET | Yes | Unactivated tags |
| `/orders/replacement/check-eligibility` | GET | Yes | Check if eligible |
| `/orders/replacement/{petId}` | POST | Yes | Order replacement tag |

**Payments**
| Endpoint | Method | Auth | Purpose |
|----------|--------|------|---------|
| `/payments/intent` | POST | Yes | Create payment intent |
| `/payments/intent/{id}` | GET | Yes | Get payment intent |
| `/payments/confirm/{id}` | POST | Yes | Confirm payment |

**Service Providers (Vet/Shelter Notification Network)**
| Endpoint | Method | Auth | Purpose |
|----------|--------|------|---------|
| `/service-providers/user-vet` | POST | Yes | Add user's vet (crowdsourced) |
| `/service-providers/my-suggestions` | GET | Yes | List vets user has added |
| `/service-providers/register` | POST | No | Vet/shelter self-registration |
| `/service-providers/verify` | GET | No | Email verification for providers |
| `/service-providers/nearby` | GET | No | Internal: find providers to notify |

**Note:** The `/nearby` endpoint is used internally when a pet is marked lost to find vets/shelters within 10km to send email alerts. It's not intended as a user-facing vet finder feature.

**Success Stories**
| Endpoint | Method | Auth | Purpose |
|----------|--------|------|---------|
| `/success-stories` | GET | No | List public stories |
| `/success-stories/{id}` | GET | No | Story details |
| `/success-stories/pet/{petId}` | GET | Yes | Story for pet |
| `/success-stories` | POST | Yes | Create story |
| `/success-stories/{id}` | PATCH | Yes | Update story |
| `/success-stories/{id}` | DELETE | Yes | Delete story |

**Breeds**
| Endpoint | Method | Auth | Purpose |
|----------|--------|------|---------|
| `/breeds` | GET | No | All breeds (searchable) |
| `/breeds/dog` | GET | No | Dog breeds only |
| `/breeds/cat` | GET | No | Cat breeds only |

**Real-time (SSE)**
| Endpoint | Method | Auth | Purpose |
|----------|--------|------|---------|
| `/sse/events` | GET | Yes | SSE event stream |

### Error Handling Pattern
```kotlin
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int, val message: String) : ApiResult<Nothing>()
    object NetworkError : ApiResult<Nothing>()
}

// Standard error codes from backend
// 401 - Unauthorized (trigger token refresh or logout)
// 403 - Forbidden
// 404 - Not found
// 422 - Validation error (parse field errors)
// 429 - Rate limited (show retry message)
// 500 - Server error
```

### SSE Event Types
The backend sends these event types via Server-Sent Events:

```kotlin
// Event types to handle
sealed class SSEEvent {
    data class TagScanned(
        val petId: String,
        val petName: String,
        val scannerLocation: Location?,
        val timestamp: Instant
    ) : SSEEvent()

    data class SightingReported(
        val alertId: String,
        val petName: String,
        val sightingLocation: Location,
        val description: String?
    ) : SSEEvent()

    data class PetFound(
        val petId: String,
        val petName: String,
        val alertId: String
    ) : SSEEvent()

    data class AlertCreated(
        val alertId: String,
        val petName: String,
        val lastSeenLocation: Location?
    ) : SSEEvent()

    data class AlertUpdated(
        val alertId: String,
        val status: String
    ) : SSEEvent()

    object Connected : SSEEvent()
    object KeepAlive : SSEEvent()
}
```

### API Response Envelope
All API responses follow this structure:
```kotlin
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: String?,
    val details: Map<String, Any>?  // Validation errors
)
```

---

## Deep Links Configuration

### AndroidManifest.xml
```xml
<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="https" android:host="senra.pet" android:pathPrefix="/qr/" />
</intent-filter>

<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="petsafety" android:host="tag" />
</intent-filter>
```

### Handling in Navigation
```kotlin
// Parse incoming intents in MainActivity
// Route to: TagActivationScreen if logged in
// Route to: LoginScreen -> TagActivationScreen if not logged in (preserve deep link)
```

### Navigation Intent (Get Directions)
Launch user's preferred map app for navigation to a location:
```kotlin
fun launchNavigation(context: Context, lat: Double, lng: Double, label: String? = null) {
    // Generic geo URI works with most map apps
    val geoUri = if (label != null) {
        Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
    } else {
        Uri.parse("geo:$lat,$lng?q=$lat,$lng")
    }

    val intent = Intent(Intent.ACTION_VIEW, geoUri)

    // Show app chooser so user picks their preferred map app
    val chooser = Intent.createChooser(intent, "Navigate with...")

    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(chooser)
    } else {
        // Fallback: open in browser with Google Maps
        val browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
        context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
    }
}

// Usage in Composable
Button(onClick = { launchNavigation(context, sighting.lat, sighting.lng, "Pet sighting") }) {
    Icon(Icons.Default.Directions, contentDescription = null)
    Text("Get Directions")
}
```

---

### App Links Verification (assetlinks.json)
Host this file at `https://senra.pet/.well-known/assetlinks.json`:
```json
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "com.petsafety.app",
    "sha256_cert_fingerprints": [
      "YOUR_RELEASE_SIGNING_KEY_SHA256"
    ]
  }
}]
```

Get your SHA256 fingerprint:
```bash
keytool -list -v -keystore your-release-key.keystore -alias your-alias
```

---

## Testing Requirements

- **Unit tests** for all ViewModels and Use Cases
- **Repository tests** with fake data sources
- **UI tests** for critical flows (login, add pet, scan QR)
- **Use Turbine** for testing StateFlows
- **Use MockK** for mocking

---

## Build Variants

| Variant | API | Features |
|---------|-----|----------|
| `debug` | Staging | Logging, debug menu |
| `staging` | Staging | Release config, staging API |
| `release` | Production | Minified, signed |

---

## Implementation Phases

### Phase 1: Foundation 
1. Project setup with all dependencies
2. Hilt DI configuration
3. Retrofit + OkHttp setup with auth interceptor
4. Room database with core entities (User, Pet, Alert)
5. Navigation graph skeleton
6. Theme setup (Material 3, match iOS colors)
7. Auth flow (login, register, token management)

### Phase 2: Core Features
1. Home screen with pet list
2. Pet CRUD operations
3. Image capture and upload
4. Offline caching with Room
5. Pull-to-refresh, loading states

### Phase 3: QR & Alerts
1. CameraX + ML Kit QR scanner
2. Deep link handling
3. Tag activation flow
4. Missing pet alerts
5. Sighting reports
6. SSE real-time notifications

### Phase 4: Maps & Polish
1. Google Maps integration
2. Vet/shelter finder
3. Location services
4. Push notifications (FCM)
5. Biometric authentication
6. Error handling polish
7. UI/UX polish, animations

### Phase 5: Payments & Launch 
1. Stripe SDK integration
2. Order flow
3. Play Store assets
4. Beta testing
5. Production release

---

## Common Pitfalls to Avoid

1. **Don't block the main thread** - All API/DB calls via coroutines on IO dispatcher
2. **Don't ignore lifecycle** - Collect flows with `repeatOnLifecycle`
3. **Don't skip loading states** - Always show loading, error, empty, success states
4. **Don't forget permissions** - Runtime permissions for camera, location
5. **Don't hardcode API URLs** - Use BuildConfig
6. **Don't leak ViewModels** - Use Hilt's `@HiltViewModel`
7. **Don't skip ProGuard rules** - Add keep rules for Retrofit models

---

## Commands Reference

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Run connected tests
./gradlew connectedDebugAndroidTest

# Check lint issues
./gradlew lintDebug

# Generate signed release
./gradlew assembleRelease
```

---

## iOS to Android Mapping

| iOS | Android | Notes |
|-----|---------|-------|
| SwiftUI | Jetpack Compose | Both declarative UI |
| Combine | Kotlin Flow/StateFlow | Reactive streams |
| Core Data | Room | Local database |
| Keychain | EncryptedSharedPreferences | Secure token storage |
| URLSession | OkHttp/Retrofit | Networking |
| APNs | FCM | Push notifications |
| Sentry iOS SDK | Sentry Android SDK | Crash reporting |
| async/await | Coroutines | Concurrency |
| @Published | StateFlow | Observable state |
| @StateObject | viewModel() | ViewModel scoping |
| NavigationStack | NavHost | Navigation |
| UserDefaults | SharedPreferences | Simple key-value |

---

## Questions to Resolve with Backend Team

- [x] SSE endpoint authentication method → Bearer token in header
- [x] Image upload size limits → 5MB max per image
- [ ] Rate limiting thresholds (login: 5/15min, register: 3/hour, scan: 10/min)
- [ ] FCM server key setup
- [x] Deep link verification file → assetlinks.json at /.well-known/

---

## Token Refresh Pattern

```kotlin
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val authApi: AuthApi  // Separate API without auth interceptor
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        // Prevent infinite retry loops
        if (response.request.header("Retry-Auth") != null) {
            return null
        }

        synchronized(this) {
            // Check if token was already refreshed by another thread
            val currentToken = tokenManager.accessToken
            val requestToken = response.request.header("Authorization")
                ?.removePrefix("Bearer ")

            if (currentToken != requestToken && currentToken != null) {
                // Token was refreshed, retry with new token
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .header("Retry-Auth", "true")
                    .build()
            }

            // Need to refresh token
            val refreshToken = tokenManager.refreshToken ?: return null

            return try {
                val newTokens = runBlocking {
                    authApi.refreshToken(RefreshRequest(refreshToken))
                }
                tokenManager.saveTokens(newTokens)
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${newTokens.accessToken}")
                    .header("Retry-Auth", "true")
                    .build()
            } catch (e: Exception) {
                tokenManager.clearTokens()
                null  // Will trigger logout
            }
        }
    }
}
```

---

## Sentry Integration

```kotlin
// In Application.onCreate()
SentryAndroid.init(this) { options ->
    options.dsn = BuildConfig.SENTRY_DSN
    options.environment = if (BuildConfig.DEBUG) "development" else "production"
    options.isDebug = BuildConfig.DEBUG
    options.tracesSampleRate = 0.2  // 20% of transactions
    options.setBeforeSend { event, hint ->
        // Strip sensitive data
        event.user?.email = null
        event
    }
}

// Add breadcrumbs for API calls (in OkHttp interceptor)
Sentry.addBreadcrumb(Breadcrumb().apply {
    type = "http"
    category = "api"
    message = "${request.method} ${request.url.encodedPath}"
    level = SentryLevel.INFO
})
```

---

## Notes

- When in doubt, check how iOS implements it and adapt to Android idioms
- Keep feature branches small and focused
- Test on physical devices, not just emulator (especially camera/QR)
- Monitor Sentry for crashes during development
- The iOS app uses `https://senra.pet/api` as base URL (not api.senra.pet)
