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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.google.mlkit.vision.common.InputImage
import com.petsafety.app.R
import com.petsafety.app.data.model.ScanResponse
import com.petsafety.app.data.repository.LocationConsent
import com.petsafety.app.ui.theme.BackgroundLight
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.theme.MutedTextLight
import com.petsafety.app.ui.theme.TealAccent
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import com.petsafety.app.ui.viewmodel.QrScannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
    appStateViewModel: AppStateViewModel,
    pendingQrCode: String?,
    onQrCodeHandled: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: QrScannerViewModel = hiltViewModel()
    val scanResult by viewModel.scanResult.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember { mutableStateOf(false) }
    var manualCode by remember { mutableStateOf("") }

    val qrScannedMessage = stringResource(R.string.qr_scanned)
    val invalidQrCodeMessage = stringResource(R.string.invalid_qr_code)
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
            viewModel.scanQr(pendingQrCode)
            onQrCodeHandled()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (hasPermission) {
            // Camera Preview
            CameraPreview(
                lifecycleOwner = lifecycleOwner,
                onQrCodeScanned = { code ->
                    viewModel.scanQr(code)
                    appStateViewModel.showSuccess(qrScannedMessage)
                }
            )

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
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Scan QR Code",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Point your camera at a pet's QR tag",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            // Manual Code Entry
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Color.Black.copy(alpha = 0.7f),
                        RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .padding(24.dp)
            ) {
                OutlinedTextField(
                    value = manualCode,
                    onValueChange = { manualCode = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.manual_code_label)) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val trimmed = manualCode.trim()
                        if (trimmed.isBlank()) {
                            appStateViewModel.showError(invalidQrCodeMessage)
                        } else {
                            viewModel.scanQr(trimmed)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                ) {
                    Text(
                        text = stringResource(R.string.submit_code),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        } else {
            // Camera Permission Required
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundLight),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color(0xFFF2F2F7), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = TealAccent
                        )
                    }

                    Text(
                        text = "Camera Access Required",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Please enable camera access to scan QR codes",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                        color = MutedTextLight,
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
            onShareLocation = { qrCode, consent, lat, lng, accuracy ->
                viewModel.shareLocation(
                    qrCode = qrCode,
                    consent = consent,
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
    onQrCodeScanned: (String) -> Unit
) {
    val scanner = BarcodeScanning.getClient()

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val analysis = ImageAnalysis.Builder().build().also { imageAnalysis ->
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
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScannedPetSheet(
    scanResult: ScanResponse,
    onDismiss: () -> Unit,
    onShareLocation: (String, LocationConsent, Double?, Double?, Double?) -> Unit
) {
    val context = LocalContext.current
    val locationProvider = LocationServices.getFusedLocationProviderClient(context)
    val pet = scanResult.pet

    // State for 3-tier location consent
    var showLocationConsentOptions by remember { mutableStateOf(false) }
    var selectedConsent by remember { mutableStateOf<LocationConsent?>(null) }
    var currentLocation by remember { mutableStateOf<android.location.Location?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

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
                        contentDescription = null,
                        modifier = Modifier.size(50.dp),
                        tint = TealAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pet Name & Info
            Text(
                text = "Hello! I'm ${pet.name}",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "You've just scanned my tag. Thank you for helping me!",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                color = MutedTextLight,
                textAlign = TextAlign.Center
            )

            // Pet Details
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                pet.breed?.let {
                    Text(
                        text = "Breed: $it",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                        color = MutedTextLight
                    )
                }
                pet.age?.let {
                    Text(
                        text = "Age: $it",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                        color = MutedTextLight
                    )
                }
                pet.color?.let {
                    Text(
                        text = "Color: $it",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                        color = MutedTextLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Location Sharing Section with 3-tier consent
            if (pet.qrCode != null) {
                if (!showLocationConsentOptions) {
                    // Initial "Share Location" button
                    Button(
                        onClick = {
                            if (!hasLocationPermission) {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                            showLocationConsentOptions = true
                        },
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
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Share My Location with Owner",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${pet.name}'s owner will receive a notification with your location",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MutedTextLight,
                        textAlign = TextAlign.Center
                    )
                } else {
                    // 3-tier Location Consent Options
                    Text(
                        text = "Share Your Location",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Help ${pet.name}'s owner find them by sharing your location",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                        color = MutedTextLight,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Option 1: Precise Location
                    LocationConsentOption(
                        title = "Precise Location",
                        description = "Share your exact GPS coordinates",
                        icon = Icons.Default.LocationOn,
                        isSelected = selectedConsent == LocationConsent.PRECISE,
                        isRecommended = true,
                        onClick = { selectedConsent = LocationConsent.PRECISE }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Option 2: Approximate Location
                    LocationConsentOption(
                        title = "Approximate Location",
                        description = "Share within ~500m accuracy (GDPR compliant)",
                        icon = Icons.Default.LocationOn,
                        isSelected = selectedConsent == LocationConsent.APPROXIMATE,
                        isRecommended = false,
                        onClick = { selectedConsent = LocationConsent.APPROXIMATE }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Option 3: No Location
                    LocationConsentOption(
                        title = "Don't Share Location",
                        description = "Just notify the owner that their pet was found",
                        icon = Icons.Default.Warning,
                        isSelected = selectedConsent == LocationConsent.DECLINE,
                        isRecommended = false,
                        onClick = { selectedConsent = LocationConsent.DECLINE }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            if (selectedConsent != null && !isSubmitting) {
                                isSubmitting = true
                                val consent = selectedConsent!!
                                val qrCode = pet.qrCode

                                when (consent) {
                                    LocationConsent.DECLINE -> {
                                        onShareLocation(qrCode, consent, null, null, null)
                                    }
                                    LocationConsent.APPROXIMATE, LocationConsent.PRECISE -> {
                                        if (currentLocation != null) {
                                            onShareLocation(
                                                qrCode,
                                                consent,
                                                currentLocation!!.latitude,
                                                currentLocation!!.longitude,
                                                currentLocation!!.accuracy.toDouble()
                                            )
                                        } else {
                                            // Try to get location again
                                            locationProvider.lastLocation.addOnSuccessListener { location ->
                                                if (location != null) {
                                                    onShareLocation(
                                                        qrCode,
                                                        consent,
                                                        location.latitude,
                                                        location.longitude,
                                                        location.accuracy.toDouble()
                                                    )
                                                } else {
                                                    // Fallback to no location if can't get it
                                                    onShareLocation(qrCode, LocationConsent.DECLINE, null, null, null)
                                                }
                                            }.addOnFailureListener {
                                                onShareLocation(qrCode, LocationConsent.DECLINE, null, null, null)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        enabled = selectedConsent != null && !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandOrange,
                            disabledContainerColor = BrandOrange.copy(alpha = 0.5f)
                        )
                    ) {
                        if (isSubmitting) {
                            Text(
                                text = "Sending...",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        } else {
                            Text(
                                text = "Confirm & Notify Owner",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Back button
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MutedTextLight,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier
                            .clickable {
                                showLocationConsentOptions = false
                                selectedConsent = null
                            }
                            .padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Contact Owner Section
            if (pet.ownerPhone != null || pet.ownerEmail != null) {
                Text(
                    text = "Contact Owner",
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
                        label = "Call: $phone",
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
                        label = "Email: $email",
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
                        title = "Medical Information",
                        content = medical,
                        backgroundColor = Color.Red.copy(alpha = 0.1f),
                        iconColor = Color.Red
                    )
                }
            }

            // Notes
            pet.notes?.let { notes ->
                if (notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoCard(
                        icon = Icons.Default.Info,
                        title = "Notes",
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
                    .background(Color(0xFFF2F2F7), RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MutedTextLight
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Your privacy matters. We'll only share your location with ${pet.name}'s owner with your explicit consent.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MutedTextLight
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F7))
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
                tint = MutedTextLight
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

/**
 * Location consent option card for 3-tier GDPR consent
 */
@Composable
private fun LocationConsentOption(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    isRecommended: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) BrandOrange else Color(0xFFE5E5EA)
    val backgroundColor = if (isSelected) BrandOrange.copy(alpha = 0.1f) else Color(0xFFF2F2F7)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isSelected) BrandOrange.copy(alpha = 0.2f) else Color(0xFFE5E5EA),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isSelected) BrandOrange else MutedTextLight
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = if (isSelected) BrandOrange else MaterialTheme.colorScheme.onSurface
                    )
                    if (isRecommended) {
                        Text(
                            text = "Recommended",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color.White,
                            modifier = Modifier
                                .background(TealAccent, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MutedTextLight
                )
            }

            // Selection indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        if (isSelected) BrandOrange else Color.Transparent,
                        CircleShape
                    )
                    .then(
                        if (!isSelected) Modifier.background(
                            Color.Transparent,
                            CircleShape
                        ) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(0xFFE5E5EA), CircleShape)
                    )
                }
            }
        }
    }
}
