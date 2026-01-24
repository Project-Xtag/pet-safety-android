package com.petsafety.app

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import com.petsafety.app.data.local.AuthTokenStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class AuthTokenStoreTests {
    @Test
    fun saveAndRetrieveToken() = runBlocking {
        val file = File.createTempFile("prefs", "test")
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { file }
        )
        val store = AuthTokenStore(dataStore)
        store.saveAuthToken("token_123")
        val token = store.authToken.first()
        assertEquals("token_123", token)
    }
}
