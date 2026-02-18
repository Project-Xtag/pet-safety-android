package com.petsafety.app.di

import android.content.Context
import timber.log.Timber
import androidx.room.Room
import androidx.work.WorkManager
import com.petsafety.app.data.config.ConfigurationManager
import com.petsafety.app.data.local.AppDatabase
import com.petsafety.app.data.local.AuthTokenStore
import com.petsafety.app.data.local.DatabaseKeyManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import com.petsafety.app.data.local.OfflineDataManager
import com.petsafety.app.data.fcm.FCMRepository
import com.petsafety.app.data.network.ApiClient
import com.petsafety.app.data.network.ApiService
import com.petsafety.app.data.network.AppCheckInterceptor
import com.petsafety.app.data.network.SseService
import com.petsafety.app.data.notifications.NotificationHelper
import com.petsafety.app.data.repository.AlertsRepository
import com.petsafety.app.data.repository.AuthRepository
import com.petsafety.app.data.repository.NotificationPreferencesRepository
import com.petsafety.app.data.repository.OrdersRepository
import com.petsafety.app.data.repository.PetsRepository
import com.petsafety.app.data.repository.PhotosRepository
import com.petsafety.app.data.repository.QrRepository
import com.petsafety.app.data.repository.SubscriptionRepository
import com.petsafety.app.data.repository.SuccessStoriesRepository
import com.petsafety.app.data.sync.NetworkMonitor
import com.petsafety.app.data.sync.SyncService
import com.petsafety.app.util.AndroidStringProvider
import com.petsafety.app.util.StringProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    private const val DATABASE_NAME = "PetSafety.db"

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        val keyManager = DatabaseKeyManager(context)
        val passphrase = keyManager.getOrCreatePassphrase()
        System.loadLibrary("sqlcipher")
        val factory = SupportOpenHelperFactory(passphrase)

        fun buildDatabase(): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()

        val database = buildDatabase()
        return try {
            // Force open here so we can recover instead of crashing later on first DAO call.
            database.openHelper.writableDatabase
            database
        } catch (e: Exception) {
            val isCorruptOrIncompatible = e.message?.contains("file is not a database", ignoreCase = true) == true
            if (!isCorruptOrIncompatible) {
                throw e
            }

            Timber.w(e, "Incompatible local DB detected, recreating encrypted database")
            runCatching { database.close() }
            context.deleteDatabase(DATABASE_NAME)

            val recreated = buildDatabase()
            // If this fails, let it crash with the real root cause.
            recreated.openHelper.writableDatabase
            recreated
        }
    }

    @Provides
    @Singleton
    fun provideAppCheckInterceptor(configManager: ConfigurationManager): AppCheckInterceptor =
        AppCheckInterceptor(configManager)

    @Provides
    @Singleton
    fun provideApiService(tokenStore: AuthTokenStore, appCheckInterceptor: AppCheckInterceptor): ApiService =
        ApiClient.create(tokenStore, appCheckInterceptor)

    @Provides
    @Singleton
    fun provideSseService(tokenStore: AuthTokenStore): SseService =
        SseService(tokenStore)

    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor =
        NetworkMonitor(context)

    @Provides
    @Singleton
    fun provideOfflineDataManager(database: AppDatabase): OfflineDataManager =
        OfflineDataManager(database)

    @Provides
    @Singleton
    fun provideNotificationHelper(@ApplicationContext context: Context): NotificationHelper =
        NotificationHelper(context)

    @Provides
    @Singleton
    fun provideSyncService(
        apiService: ApiService,
        offlineDataManager: OfflineDataManager,
        networkMonitor: NetworkMonitor
    ): SyncService = SyncService(apiService, offlineDataManager, networkMonitor)

    @Provides
    @Singleton
    fun provideAuthRepository(
        apiService: ApiService,
        tokenStore: AuthTokenStore,
        fcmRepository: FCMRepository
    ): AuthRepository = AuthRepository(apiService, tokenStore, fcmRepository)

    @Provides
    @Singleton
    fun provideAuthTokenStore(@ApplicationContext context: Context): AuthTokenStore =
        AuthTokenStore(context)

    @Provides
    @Singleton
    fun providePetsRepository(
        apiService: ApiService,
        offlineDataManager: OfflineDataManager,
        networkMonitor: NetworkMonitor,
        syncService: SyncService
    ): PetsRepository = PetsRepository(apiService, offlineDataManager, networkMonitor, syncService)

    @Provides
    @Singleton
    fun provideAlertsRepository(
        apiService: ApiService,
        offlineDataManager: OfflineDataManager,
        networkMonitor: NetworkMonitor,
        syncService: SyncService
    ): AlertsRepository = AlertsRepository(apiService, offlineDataManager, networkMonitor, syncService)

    @Provides
    @Singleton
    fun provideOrdersRepository(apiService: ApiService): OrdersRepository =
        OrdersRepository(apiService)

    @Provides
    @Singleton
    fun provideQrRepository(apiService: ApiService): QrRepository =
        QrRepository(apiService)

    @Provides
    @Singleton
    fun providePhotosRepository(apiService: ApiService): PhotosRepository =
        PhotosRepository(apiService)

    @Provides
    @Singleton
    fun provideSuccessStoriesRepository(
        apiService: ApiService,
        offlineDataManager: OfflineDataManager,
        networkMonitor: NetworkMonitor
    ): SuccessStoriesRepository = SuccessStoriesRepository(apiService, offlineDataManager, networkMonitor)

    @Provides
    @Singleton
    fun provideNotificationPreferencesRepository(apiService: ApiService): NotificationPreferencesRepository =
        NotificationPreferencesRepository(apiService)

    @Provides
    @Singleton
    fun provideSubscriptionRepository(apiService: ApiService): SubscriptionRepository =
        SubscriptionRepository(apiService)

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideStringProvider(
        @ApplicationContext context: Context
    ): StringProvider = AndroidStringProvider(context)

    @Provides
    @Singleton
    fun provideFCMRepository(
        @ApplicationContext context: Context,
        apiService: ApiService
    ): FCMRepository = FCMRepository(context, apiService)
}
