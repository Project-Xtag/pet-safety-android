# ============================================
# Pet Safety Android - ProGuard/R8 Rules
# ============================================

# Enable optimization
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
}

# Remove debug-only code
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNullParameter(...);
    public static void checkNotNullExpressionValue(...);
    public static void checkReturnedValueIsNotNull(...);
    public static void checkFieldIsNotNull(...);
}

# ============================================
# KOTLIN SERIALIZATION
# ============================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep serializers for classes marked @Serializable
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep serializable data classes
-keep,includedescriptorclasses class com.petsafety.app.data.model.**$$serializer { *; }
-keepclassmembers class com.petsafety.app.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.petsafety.app.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep network model classes
-keep,includedescriptorclasses class com.petsafety.app.data.network.model.**$$serializer { *; }
-keepclassmembers class com.petsafety.app.data.network.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.petsafety.app.data.network.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ============================================
# RETROFIT / OKHTTP
# ============================================
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# Keep Retrofit interface methods
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep annotation default values
-keepattributes AnnotationDefault

# Keep exception messages for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ============================================
# ROOM DATABASE
# ============================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.* <fields>;
}

# ============================================
# HILT / DAGGER
# ============================================
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.android.lifecycle.HiltViewModel class *
-keep @dagger.Module class *
-keep @dagger.Binds class *
-keepclassmembers class * {
    @dagger.* <methods>;
    @javax.inject.* <fields>;
}

# ============================================
# COIL IMAGE LOADING
# ============================================
-dontwarn coil.**

# ============================================
# GOOGLE PLAY SERVICES / MAPS
# ============================================
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ============================================
# ML KIT (QR SCANNING)
# ============================================
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ============================================
# CAMERAX
# ============================================
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ============================================
# SQLCIPHER DATABASE ENCRYPTION
# ============================================
-keep class net.zetetic.database.sqlcipher.** { *; }
-dontwarn net.zetetic.database.sqlcipher.**

# ============================================
# SECURITY - ENCRYPTED SHARED PREFERENCES
# ============================================
-keep class androidx.security.crypto.** { *; }
-keepclassmembers class * extends com.google.crypto.tink.shaded.protobuf.GeneratedMessageLite {
    <fields>;
}

# ============================================
# BIOMETRIC
# ============================================
-keep class androidx.biometric.** { *; }

# ============================================
# WORK MANAGER
# ============================================
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}

# ============================================
# COMPOSE
# ============================================
-dontwarn androidx.compose.**

# ============================================
# SENTRY CRASH REPORTING
# ============================================
-keep class io.sentry.** { *; }
-dontwarn io.sentry.**

# ============================================
# FIREBASE CLOUD MESSAGING
# ============================================
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep Firebase Messaging Service
-keep class com.petsafety.app.data.fcm.PetSafetyFirebaseMessagingService { *; }

# Keep FCM-related classes
-keep class com.google.firebase.messaging.** { *; }
-keep class com.google.firebase.iid.** { *; }

# Keep notification data classes
-keep class com.petsafety.app.data.fcm.NotificationLocation { *; }
-keep class com.petsafety.app.ui.NotificationData { *; }

# ============================================
# SECURITY HARDENING
# ============================================

# Obfuscate class names aggressively
-repackageclasses 'o'

# Remove unused code
-dontwarn javax.annotation.**

# Protect sensitive classes from easy identification
-keep class com.petsafety.app.data.local.AuthTokenStore { *; }
-keep class com.petsafety.app.data.local.BiometricHelper { *; }
-keep class com.petsafety.app.data.local.DatabaseKeyManager { *; }
-keep class com.petsafety.app.data.network.TokenAuthenticator { *; }

# Keep BuildConfig for debug checks
-keep class com.petsafety.app.BuildConfig { *; }

# ============================================
# COROUTINES
# ============================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ServiceLoader support
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ============================================
# ENUM CLASSES
# ============================================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ============================================
# PARCELABLE
# ============================================
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}
