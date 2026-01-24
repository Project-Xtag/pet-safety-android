# Keep Kotlin serialization metadata
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}

# Retrofit/OkHttp
-dontwarn okhttp3.**
-dontwarn retrofit2.**
