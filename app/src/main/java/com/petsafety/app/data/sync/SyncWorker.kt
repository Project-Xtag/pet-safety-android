package com.petsafety.app.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.SerializationException
import timber.log.Timber

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                SyncWorkerEntryPoint::class.java
            )
            entryPoint.syncService().performFullSync()
            Result.success()
        } catch (e: Exception) {
            // Distinguish transient (retryable) from permanent (non-retryable)
            // failures. A SerializationException / IllegalArgumentException
            // is NOT going to resolve on its own — retrying would loop with
            // exponential backoff forever, eating battery and still losing
            // the queued actions silently. Mark permanent, let WorkManager
            // stop rescheduling; the individual action's retryCount in
            // failAction() already tracks per-action retries for transient
            // API-side failures.
            if (isNonRetryable(e)) {
                Timber.e(e, "SyncWorker permanent failure, not rescheduling")
                Result.failure()
            } else {
                Timber.w(e, "SyncWorker transient failure, scheduling retry")
                Result.retry()
            }
        }
    }

    private fun isNonRetryable(e: Throwable): Boolean =
        e is SerializationException || e is IllegalArgumentException
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncWorkerEntryPoint {
    fun syncService(): SyncService
}
