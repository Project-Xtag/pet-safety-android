package com.petsafety.app.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import java.io.ByteArrayOutputStream
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCaptureSheet(
    onDismiss: () -> Unit,
    onPhotoSelected: (ByteArray) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted && tempPhotoUri != null) {
            // Permission granted, but we need to re-trigger camera
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bytes = compressImage(context, it)
            if (bytes != null) {
                onPhotoSelected(bytes)
            }
        }
        onDismiss()
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null) {
            val bytes = compressImage(context, tempPhotoUri!!)
            if (bytes != null) {
                onPhotoSelected(bytes)
            }
        }
        onDismiss()
    }

    fun launchCamera() {
        if (hasCameraPermission) {
            val file = createTempImageFile(context)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.add_photo),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.take_photo)) },
                leadingContent = {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                },
                supportingContent = { Text(stringResource(R.string.use_camera)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { launchCamera() }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.choose_from_gallery)) },
                leadingContent = {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                },
                supportingContent = { Text(stringResource(R.string.select_existing_photo)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { galleryLauncher.launch("image/*") }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun createTempImageFile(context: Context): File {
    val cacheDir = File(context.cacheDir, "images")
    cacheDir.mkdirs()
    return File.createTempFile("photo_", ".jpg", cacheDir)
}

private fun compressImage(context: Context, uri: Uri, maxDimension: Int = 1200, quality: Int = 80): ByteArray? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        if (originalBitmap == null) return null

        // Calculate scaling factor
        val scaleFactor = if (originalBitmap.width > originalBitmap.height) {
            maxDimension.toFloat() / originalBitmap.width
        } else {
            maxDimension.toFloat() / originalBitmap.height
        }

        val scaledBitmap = if (scaleFactor < 1f) {
            Bitmap.createScaledBitmap(
                originalBitmap,
                (originalBitmap.width * scaleFactor).toInt(),
                (originalBitmap.height * scaleFactor).toInt(),
                true
            )
        } else {
            originalBitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

        if (scaledBitmap != originalBitmap) {
            scaledBitmap.recycle()
        }
        originalBitmap.recycle()

        outputStream.toByteArray()
    } catch (e: Exception) {
        null
    }
}
