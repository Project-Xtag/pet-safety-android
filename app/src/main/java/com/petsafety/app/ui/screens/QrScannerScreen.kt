package com.petsafety.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.petsafety.app.ui.components.SecureScreen
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.petsafety.app.R
import com.petsafety.app.ui.a11y.markAsHeading
import com.petsafety.app.data.model.TagLookupResponse
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.theme.TealAccent
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import com.petsafety.app.ui.viewmodel.QrScannerViewModel
import com.petsafety.app.ui.viewmodel.TagLookupState
import com.petsafety.app.util.QrCodeParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
    appStateViewModel: AppStateViewModel,
    pendingQrCode: String?,
    onQrCodeHandled: () -> Unit,
    onNavigateToActivation: (String) -> Unit = {},
    onNavigateToPromoClaim: (String, String, Int) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    // Note: SecureScreen (FLAG_SECURE) intentionally NOT used here —
    // it interferes with CameraX ImageAnalysis on some devices (e.g. MediaTek)

    val viewModel: QrScannerViewModel = hiltViewModel()
    val scanResult by viewModel.scanResult.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val lookupState by viewModel.lookupState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    var hasPermission by remember { mutableStateOf(false) }
    // Increment to force CameraPreview recreation (e.g., after returning from activation)
    var cameraKey by remember { mutableIntStateOf(0) }
    var isTorchOn by remember { mutableStateOf(false) }
    var cameraRef by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    val qrScannedMessage = stringResource(R.string.qr_scanned)
    val locationSharedMessage = stringResource(R.string.location_shared)
    val shareLocationFailedMessage = stringResource(R.string.share_location_failed)

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(pendingQrCode) {
        if (!pendingQrCode.isNullOrBlank()) {
            viewModel.lookupAndRoute(QrCodeParser.extractTagCode(pendingQrCode))
            onQrCodeHandled()
        }
    }

    // Handle lookup state transitions
    LaunchedEffect(lookupState) {
        when (val state = lookupState) {
            is TagLookupState.PromoClaimAvailable -> {
                onNavigateToPromoClaim(state.code, state.shelterName, state.promoDurationMonths)
                viewModel.reset()
                cameraKey++
            }
            is TagLookupState.NeedsActivation -> {
                onNavigateToActivation(state.qrCode)
                viewModel.reset()
                cameraKey++
            }
            is TagLookupState.NotActivated -> {
                appStateViewModel.showError(state.message)
                viewModel.reset()
            }
            is TagLookupState.NotFound -> {
                appStateViewModel.showError(state.message)
                viewModel.reset()
            }
            is TagLookupState.Error -> {
                appStateViewModel.showError(state.message)
                viewModel.reset()
            }
            else -> {}
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (hasPermission) {
            // Camera Preview — key forces recreation after returning from activation
            key(cameraKey) {
            CameraPreview(
                lifecycleOwner = lifecycleOwner,
                onQrCodeScanned = { rawCode ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val code = QrCodeParser.extractTagCode(rawCode)
                    viewModel.lookupAndRoute(code)
                    appStateViewModel.showSuccess(qrScannedMessage)
                },
                onCameraReady = { camera -> cameraRef = camera }
            )
            } // end key(cameraKey)

            // Flashlight Toggle
            if (cameraRef?.cameraInfo?.hasFlashUnit() == true) {
                IconButton(
                    onClick = {
                        isTorchOn = !isTorchOn
                        cameraRef?.cameraControl?.enableTorch(isTorchOn)
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(48.dp)
                        .background(
                            if (isTorchOn) Color.White else Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = if (isTorchOn) stringResource(R.string.turn_off_flashlight) else stringResource(R.string.turn_on_flashlight),
                        tint = if (isTorchOn) Color.Black else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Scanning Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = stringResource(R.string.qr_scanner_icon),
                        modifier = Modifier.size(40.dp),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.scan_qr_code),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    modifier = Modifier.markAsHeading()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.scan_qr_subtitle),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

        } else {
            // Camera Permission Required
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = stringResource(R.string.qr_scanner_icon),
                            modifier = Modifier.size(44.dp),
                            tint = TealAccent
                        )
                    }

                    Text(
                        text = stringResource(R.string.camera_access_required),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.markAsHeading()
                    )

                    Text(
                        text = stringResource(R.string.camera_access_message),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 40.dp)
                    )

                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier
                            .padding(horizontal = 60.dp)
                            .height(52.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = BrandOrange.copy(alpha = 0.3f),
                                spotColor = BrandOrange.copy(alpha = 0.3f)
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                    ) {
                        Text(
                            text = stringResource(R.string.enable_camera),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }

    // Scanned Pet Bottom Sheet
    scanResult?.let { result ->
        ScannedPetSheet(
            scanResult = result,
            onDismiss = { viewModel.reset() },
            onShareGpsLocation = { qrCode, lat, lng, accuracy ->
                viewModel.shareLocation(
                    qrCode = qrCode,
                    latitude = lat,
                    longitude = lng,
                    accuracyMeters = accuracy
                ) { success, message ->
                    if (success) {
                        appStateViewModel.showSuccess(locationSharedMessage)
                        viewModel.reset()
                    } else {
                        appStateViewModel.showError(message ?: shareLocationFailedMessage)
                    }
                }
            },
            onShareManualAddress = { qrCode, address ->
                viewModel.shareManualAddress(qrCode = qrCode, manualAddress = address) { success, message ->
                    if (success) {
                        appStateViewModel.showSuccess(locationSharedMessage)
                        viewModel.reset()
                    } else {
                        appStateViewModel.showError(message ?: shareLocationFailedMessage)
                    }
                }
            }
        )
    }

    errorMessage?.let { message ->
        if (message.isNotBlank()) {
            appStateViewModel.showError(message)
            viewModel.reset()
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
private fun CameraPreview(
    lifecycleOwner: LifecycleOwner,
    onQrCodeScanned: (String) -> Unit,
    onCameraReady: (androidx.camera.core.Camera) -> Unit = {}
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            // QR-only format filter — faster and more accurate than scanning all formats
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
            val scanner = BarcodeScanning.getClient(options)

            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(android.util.Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { imageAnalysis ->
                    imageAnalysis.setAnalyzer(
                        ContextCompat.getMainExecutor(ctx)
                    ) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    val code = barcodes.firstOrNull()?.rawValue
                                    if (!code.isNullOrBlank()) {
                                        onQrCodeScanned(code)
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }
                }

                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )

                // Enable auto-focus continuous mode for better QR detection
                camera.cameraControl.cancelFocusAndMetering()

                onCameraReady(camera)
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScannedPetSheet(
    scanResult: TagLookupResponse,
    onDismiss: () -> Unit,
    onShareGpsLocation: (qrCode: String, lat: Double, lng: Double, accuracyMeters: Double?) -> Unit,
    onShareManualAddress: (qrCode: String, address: String) -> Unit
) {
    val context = LocalContext.current
    val locationProvider = LocationServices.getFusedLocationProviderClient(context)
    val pet = scanResult.pet ?: return

    var currentLocation by remember { mutableStateOf<android.location.Location?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isCapturingLocation by remember { mutableStateOf(false) }
    var showManualAddressDialog by remember { mutableStateOf(false) }
    var manualAddressInput by remember { mutableStateOf("") }

    // Location permission
    var hasLocationPermission by remember { mutableStateOf(false) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (hasLocationPermission) {
            // Get current location after permission granted
            locationProvider.lastLocation.addOnSuccessListener { location ->
                currentLocation = location
            }
        }
    }

    // Get location when sheet opens (if permission already granted)
    LaunchedEffect(Unit) {
        try {
            locationProvider.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = location
                    hasLocationPermission = true
                }
            }
        } catch (_: SecurityException) {
            // Location permission not granted yet
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pet Photo
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .shadow(10.dp, CircleShape)
                    .clip(CircleShape)
                    .background(TealAccent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (!pet.profileImage.isNullOrBlank()) {
                    AsyncImage(
                        model = pet.profileImage,
                        contentDescription = pet.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Pets,
                        contentDescription = stringResource(R.string.pet_photo),
                        modifier = Modifier.size(50.dp),
                        tint = TealAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pet Name & Info
            Text(
                text = stringResource(R.string.hello_pet_name, pet.name),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.markAsHeading()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.scanned_tag_thanks),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Pet Details
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                pet.breed?.let {
                    Text(
                        text = stringResource(R.string.breed_label, it),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                pet.age?.let {
                    Text(
                        text = stringResource(R.string.age_label, it),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                pet.color?.let {
                    Text(
                        text = stringResource(R.string.color_label, it),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Location Sharing Section.
            //
            // Tier-gated: hidden when the backend reports the owner can't
            // receive notifications (Starter tier — surfacing the button
            // would silently store an unactionable scan). Direct-contact
            // rows below stay live regardless. The precise/approximate
            // toggle and the 3-decimal client-side rounding are gone as
            // of the 2026-05-02 missing-pet flow overhaul.
            if (pet.qrCode != null && pet.ownerCanReceiveNotifications != false) {
                Text(
                    text = stringResource(R.string.step_share_location),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.share_location_help, pet.name),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Primary GPS share button.
                Button(
                    onClick = {
                        if (!isSubmitting) {
                            if (!hasLocationPermission) {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                                return@Button
                            }

                            val qrCode = pet.qrCode

                            if (currentLocation != null) {
                                isSubmitting = true
                                onShareGpsLocation(
                                    qrCode,
                                    currentLocation!!.latitude,
                                    currentLocation!!.longitude,
                                    currentLocation!!.accuracy.toDouble()
                                )
                            } else {
                                isCapturingLocation = true
                                locationProvider.lastLocation.addOnSuccessListener { location ->
                                    isCapturingLocation = false
                                    if (location != null) {
                                        isSubmitting = true
                                        onShareGpsLocation(
                                            qrCode,
                                            location.latitude,
                                            location.longitude,
                                            location.accuracy.toDouble()
                                        )
                                    }
                                    // No location available: surface the manual-address
                                    // dialog so the finder still has a path forward.
                                    else {
                                        showManualAddressDialog = true
                                    }
                                }.addOnFailureListener {
                                    isCapturingLocation = false
                                    showManualAddressDialog = true
                                }
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = BrandOrange.copy(alpha = 0.3f),
                            spotColor = BrandOrange.copy(alpha = 0.3f)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandOrange,
                        disabledContainerColor = BrandOrange.copy(alpha = 0.5f)
                    )
                ) {
                    if (isSubmitting || isCapturingLocation) {
                        Text(
                            text = stringResource(R.string.sending),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.share_location),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Manual-address fallback — always available, not just on
                // GPS denial. Stressed finders without GPS or with denied
                // permission have a one-click escape hatch.
                TextButton(
                    onClick = { showManualAddressDialog = true },
                    enabled = !isSubmitting
                ) {
                    Text(
                        text = stringResource(R.string.share_address_instead),
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.owner_notified_message, pet.name),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Starter-tier no-contact fallback. Owner can't be reached via
            // any channel — surface an empathetic empty-state rather than
            // a dead-end blank section.
            if (pet.ownerCanReceiveNotifications == false
                && pet.ownerPhone == null && pet.ownerEmail == null
                && pet.ownerSecondaryPhone == null && pet.ownerSecondaryEmail == null
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.owner_communication_disabled),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }

            // Manual-address input dialog. Server geocodes server-side; on
            // failure the owner gets the typed text with a "no map
            // coordinates" note (transparent to the finder — same toast).
            if (showManualAddressDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showManualAddressDialog = false
                        manualAddressInput = ""
                    },
                    title = {
                        Text(stringResource(R.string.share_manual_address_title))
                    },
                    text = {
                        Column {
                            Text(
                                text = stringResource(R.string.share_manual_address_desc, pet.name),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            OutlinedTextField(
                                value = manualAddressInput,
                                onValueChange = { manualAddressInput = it },
                                label = { Text(stringResource(R.string.share_manual_address_label)) },
                                placeholder = {
                                    Text(stringResource(R.string.share_manual_address_placeholder))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val trimmed = manualAddressInput.trim()
                                if (trimmed.isNotEmpty() && pet.qrCode != null) {
                                    showManualAddressDialog = false
                                    isSubmitting = true
                                    onShareManualAddress(pet.qrCode, trimmed)
                                    manualAddressInput = ""
                                }
                            },
                            enabled = manualAddressInput.trim().isNotEmpty()
                        ) { Text(stringResource(R.string.share_location)) }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showManualAddressDialog = false
                            manualAddressInput = ""
                        }) { Text(stringResource(R.string.cancel)) }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Contact Owner Section
            if (pet.ownerPhone != null || pet.ownerEmail != null) {
                Text(
                    text = stringResource(R.string.contact_owner),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                pet.ownerPhone?.let { phone ->
                    ContactRow(
                        icon = Icons.Default.Call,
                        label = stringResource(R.string.call_phone, phone),
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${phone.replace(" ", "")}")
                            }
                            context.startActivity(intent)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                pet.ownerEmail?.let { email ->
                    ContactRow(
                        icon = Icons.Default.Email,
                        label = stringResource(R.string.email_contact, email),
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:$email")
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // Medical Information
            pet.medicalNotes?.let { medical ->
                if (medical.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    InfoCard(
                        icon = Icons.Default.LocalHospital,
                        title = stringResource(R.string.medical_information),
                        content = medical,
                        backgroundColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        iconColor = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Notes
            pet.notes?.let { notes ->
                if (notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoCard(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.notes),
                        content = notes,
                        backgroundColor = Color.Blue.copy(alpha = 0.1f),
                        iconColor = Color.Blue
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Privacy Notice
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = stringResource(R.string.privacy_notice_icon),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.your_privacy_notice, pet.name),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ContactRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = TealAccent
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoCard(
    icon: ImageVector,
    title: String,
    content: String,
    backgroundColor: Color,
    iconColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = iconColor
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = iconColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// Extraction logic moved to com.petsafety.app.util.QrCodeParser so that
// this screen, MainActivity deep-link handling, and any future caller
// all agree on what counts as a valid tag scan.

