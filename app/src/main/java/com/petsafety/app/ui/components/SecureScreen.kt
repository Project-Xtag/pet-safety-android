package com.petsafety.app.ui.components

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.petsafety.app.BuildConfig

/**
 * Composable that sets FLAG_SECURE on the current window while in composition.
 * Prevents screenshots and screen recording on sensitive screens.
 * The flag is cleared when the composable leaves composition.
 *
 * Disabled in DEBUG builds so product demos can be recorded; release/staging
 * builds keep FLAG_SECURE.
 */
@Composable
fun SecureScreen() {
    if (BuildConfig.DEBUG) return
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
