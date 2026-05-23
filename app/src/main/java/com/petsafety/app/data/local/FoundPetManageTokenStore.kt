package com.petsafety.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Single-use manage tokens issued by `/community/found-pets` for anonymous
 * submitters. Persisting them on this device lets the same user later mark
 * a report as reunited or remove it without an account. Mirrors the web's
 * `senra_found_pet_tokens` localStorage entry and iOS's
 * `FoundPetManageTokenStore`.
 *
 * Stored as a JSON-encoded list under a single SharedPreferences key so the
 * format is identical to what the other platforms write.
 */
@Singleton
class FoundPetManageTokenStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    @Serializable
    data class Entry(val id: String, val token: String)

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): List<Entry> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<Entry>>(raw) }.getOrElse { emptyList() }
    }

    fun append(entry: Entry) {
        val current = load().filterNot { it.id == entry.id } + entry
        prefs.edit { putString(KEY, json.encodeToString(current)) }
    }

    fun remove(id: String) {
        val filtered = load().filterNot { it.id == id }
        prefs.edit { putString(KEY, json.encodeToString(filtered)) }
    }

    private companion object {
        const val PREFS_NAME = "senra_found_pets"
        const val KEY = "senra_found_pet_tokens"
    }
}
