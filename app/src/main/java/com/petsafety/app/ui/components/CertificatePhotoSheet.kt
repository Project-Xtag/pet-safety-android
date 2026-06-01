package com.petsafety.app.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.petsafety.app.R
import java.io.File

/**
 * Source picker for a vaccination certificate. Mirrors PhotoCaptureSheet's
 * launcher mechanics, but emits **raw** bytes (no compress) so the cert path
 * can run them through VaccinationCertificateEncoder (passthrough accepted
 * formats untouched; transcode HEIC→JPEG). Kept separate from PhotoCaptureSheet
 * — that component decodes-from-Uri-and-compresses-to-1200px and never
 * materializes raw bytes, so reusing it would degrade cert legibility and would
 * require reshaping its decode flow (decision B; see slice 2a commit body).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertificatePhotoSheet(
    onDismiss: () -> Unit,
    onRawBytesSelected: (ByteArray) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { readRawBytes(context, it)?.let(onRawBytesSelected) }
        onDismiss()
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = tempPhotoUri
        if (success && uri != null) readRawBytes(context, uri)?.let(onRawBytesSelected)
        onDismiss()
    }

    fun launchCamera() {
        if (hasCameraPermission) {
            val file = createTempCertFile(context)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                text = stringResource(R.string.add_photo),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.take_photo)) },
                leadingContent = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                supportingContent = { Text(stringResource(R.string.use_camera)) },
                modifier = Modifier.fillMaxWidth().clickable { launchCamera() }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.choose_from_gallery)) },
                leadingContent = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                supportingContent = { Text(stringResource(R.string.select_existing_photo)) },
                modifier = Modifier.fillMaxWidth().clickable { galleryLauncher.launch("image/*") }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun readRawBytes(context: Context, uri: Uri): ByteArray? =
    runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()

private fun createTempCertFile(context: Context): File {
    val dir = File(context.cacheDir, "images").apply { mkdirs() }
    return File.createTempFile("cert_", ".jpg", dir)
}
