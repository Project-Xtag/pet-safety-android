package com.petsafety.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import com.petsafety.app.ui.theme.PetSafetyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val deepLinkCodeState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepLinkCodeState.value = extractQrCode(intent)
        setContent {
            PetSafetyTheme {
                PetSafetyApp(
                    pendingQrCode = deepLinkCodeState.value,
                    onQrCodeHandled = { deepLinkCodeState.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deepLinkCodeState.value = extractQrCode(intent)
    }

    private fun extractQrCode(intent: Intent?): String? {
        val data = intent?.data ?: return null
        return when (data.scheme) {
            "petsafety" -> data.lastPathSegment
            "https" -> extractQrFromHttps(data)
            else -> null
        }
    }

    private fun extractQrFromHttps(uri: Uri): String? {
        if (uri.host != "pet-er.app") return null
        val segments = uri.pathSegments
        if (segments.size >= 2 && segments[0] == "qr") {
            return segments[1]
        }
        return null
    }
}
