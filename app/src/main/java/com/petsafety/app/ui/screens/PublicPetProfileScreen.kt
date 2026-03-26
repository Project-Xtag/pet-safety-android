package com.petsafety.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.petsafety.app.R
import com.petsafety.app.data.model.Pet
import com.petsafety.app.data.model.User
import com.petsafety.app.data.repository.LocationConsent
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.theme.TealAccent
import com.petsafety.app.ui.theme.MedicalRed
import com.petsafety.app.ui.theme.MedicalRedBg
import com.petsafety.app.ui.theme.PreviewBlue
import com.petsafety.app.ui.theme.PreviewBlueBg
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import com.petsafety.app.ui.viewmodel.AuthViewModel
import com.petsafety.app.ui.util.PetLocalizer
import com.petsafety.app.ui.viewmodel.PublicPetProfileViewModel

@Composable
fun PublicPetProfileScreen(
    qrCode: String,
    appStateViewModel: AppStateViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val viewModel: PublicPetProfileViewModel = hiltViewModel()
    val pet by viewModel.pet.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    LaunchedEffect(qrCode) {
        viewModel.loadPublicProfile(qrCode)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp)
        ) {
            // Back Button and Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.public_profile_preview),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = TealAccent)
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = errorMessage ?: stringResource(R.string.failed_load_profile),
                            color = MedicalRed,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                pet != null -> {
                    PublicPetContent(
                        pet = pet!!,
                        currentUser = currentUser,
                        qrCode = qrCode,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun PublicPetContent(
    pet: Pet,
    currentUser: User?,
    qrCode: String,
    viewModel: PublicPetProfileViewModel
) {
    val context = LocalContext.current

    val isSharing by viewModel.isSharing.collectAsState()
    val shareSuccess by viewModel.shareSuccess.collectAsState()
    val shareError by viewModel.shareError.collectAsState()

    var showConsentDialog by remember { mutableStateOf(false) }
    var shareExactLocation by remember { mutableStateOf(true) }
    var currentLatitude by remember { mutableStateOf<Double?>(null) }
    var currentLongitude by remember { mutableStateOf<Double?>(null) }
    var currentAccuracy by remember { mutableStateOf<Double?>(null) }
    var isGettingLocation by remember { mutableStateOf(false) }

    val locationProvider = remember { LocationServices.getFusedLocationProviderClient(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isGettingLocation = true
            fetchCurrentLocation(locationProvider, context) { lat, lng, acc ->
                currentLatitude = lat
                currentLongitude = lng
                currentAccuracy = acc
                isGettingLocation = false
                showConsentDialog = true
            }
        } else {
            Toast.makeText(context, context.getString(R.string.share_location_denied), Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(shareSuccess) {
        if (shareSuccess) {
            Toast.makeText(context, context.getString(R.string.share_location_success), Toast.LENGTH_LONG).show()
            viewModel.clearShareState()
        }
    }

    LaunchedEffect(shareError) {
        shareError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearShareState()
        }
    }

    // Check if this is the owner viewing their own pet's profile
    val isOwnPet = currentUser != null && pet.ownerId == currentUser.id

    // Compute all public contact info (primary + secondary, filtered by backend privacy)
    val ownerPhones = buildList {
        pet.ownerPhone?.let { add(it) }
            ?: if (isOwnPet && currentUser?.showPhonePublicly == true) currentUser.phone?.let { add(it) } else Unit
        pet.ownerSecondaryPhone?.let { add(it) }
    }
    val ownerEmails = buildList {
        pet.ownerEmail?.let { add(it) }
            ?: if (isOwnPet && currentUser?.showEmailPublicly == true) currentUser.email?.let { add(it) } else Unit
        pet.ownerSecondaryEmail?.let { add(it) }
    }
    // Build formatted owner address from backend fields (already privacy-filtered)
    val ownerAddress = run {
        val addr = pet.ownerAddress
        if (addr.isNullOrBlank()) {
            // Fallback for own pet when address is public
            if (isOwnPet && currentUser?.showAddressPublicly == true) {
                val parts = listOfNotNull(currentUser.address, currentUser.addressLine2, currentUser.city, currentUser.postalCode, currentUser.country)
                    .filter { it.isNotBlank() }
                if (parts.isNotEmpty()) parts.joinToString(", ") else null
            } else null
        } else {
            val parts = listOfNotNull(addr, pet.ownerAddressLine2, pet.ownerCity, pet.ownerPostalCode, pet.ownerCountry)
                .filter { it.isNotBlank() }
            parts.joinToString(", ")
        }
    }

    // Backward-compatible single values for existing checks
    val ownerPhone = ownerPhones.firstOrNull()
    val ownerEmail = ownerEmails.firstOrNull()

    // Check if owner has contact info but it's hidden due to privacy settings
    val hasHiddenContactInfo = isOwnPet && currentUser != null && run {
        val hasPhone = !currentUser.phone.isNullOrBlank()
        val hasEmail = !currentUser.email.isNullOrBlank()
        val phoneHidden = hasPhone && currentUser.showPhonePublicly != true
        val emailHidden = hasEmail && currentUser.showEmailPublicly != true
        (phoneHidden || emailHidden) && ownerPhones.isEmpty() && ownerEmails.isEmpty()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Preview Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .background(PreviewBlueBg, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = PreviewBlue
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.public_profile_subtitle, pet.name),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = PreviewBlue
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Circular Pet Photo
        Box(
            modifier = Modifier
                .size(140.dp)
                .shadow(10.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.1f))
                .clip(CircleShape)
                .background(TealAccent.copy(alpha = 0.2f))
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
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.Center),
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
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.scanned_tag_thanks),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Pet details row (values only, no labels)
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val neuteredLabel = stringResource(R.string.neutered)
            val details = buildList {
                pet.breed?.takeIf { it.isNotBlank() }?.let { add(PetLocalizer.localizeBreed(context, it, pet.species)) }
                (pet.localizedAge(context.resources) ?: pet.age)?.takeIf { it.isNotBlank() }?.let { add(it) }
                // Sex + neutered
                val sexValue = pet.sex?.takeIf { it.isNotBlank() && it.lowercase() != "unknown" }
                if (sexValue != null) {
                    val localizedSex = PetLocalizer.localizeSex(context, sexValue, pet.species)
                    val sexText = if (pet.isNeutered == true) {
                        "$localizedSex, $neuteredLabel"
                    } else {
                        localizedSex
                    }
                    add(sexText)
                } else if (pet.isNeutered == true) {
                    add(neuteredLabel)
                }
            }

            details.forEachIndexed { index, detail ->
                if (index > 0) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Share Location Button
        Button(
            onClick = {
                val permissionStatus = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                )
                if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
                    isGettingLocation = true
                    fetchCurrentLocation(locationProvider, context) { lat, lng, acc ->
                        currentLatitude = lat
                        currentLongitude = lng
                        currentAccuracy = acc
                        isGettingLocation = false
                        showConsentDialog = true
                    }
                } else {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
            enabled = !isGettingLocation && !isSharing
        ) {
            if (isGettingLocation || isSharing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.share_location_with_owner),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        // Location Sharing Consent Dialog
        if (showConsentDialog) {
            AlertDialog(
                onDismissRequest = { showConsentDialog = false },
                title = { Text(stringResource(R.string.share_location_title)) },
                text = {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (shareExactLocation) stringResource(R.string.share_location_exact)
                                       else stringResource(R.string.share_location_approximate),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = shareExactLocation,
                                onCheckedChange = { shareExactLocation = it }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (shareExactLocation) stringResource(R.string.share_location_exact_note)
                                   else stringResource(R.string.share_location_approximate_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showConsentDialog = false
                            viewModel.shareLocation(
                                qrCode = qrCode,
                                consent = if (shareExactLocation) LocationConsent.PRECISE else LocationConsent.APPROXIMATE,
                                latitude = currentLatitude,
                                longitude = currentLongitude,
                                accuracyMeters = currentAccuracy
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                    ) {
                        Text(stringResource(R.string.share_location_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConsentDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.owner_notified_sms_email, pet.name),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Contact Owner Section
        if (ownerPhone != null || ownerEmail != null || ownerAddress != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.contact_owner),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                pet.ownerName?.takeIf { it.isNotBlank() }?.let { name ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.contact_owner_plea_full),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // All public phone numbers
                ownerPhones.forEach { phone ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${phone.replace(" ", "")}")
                                }
                                context.startActivity(intent)
                            },
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
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = TealAccent
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.call_phone, phone),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // All public email addresses
                ownerEmails.forEach { email ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:$email")
                                }
                                context.startActivity(intent)
                            },
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
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = TealAccent
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.email_contact, email),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Owner Address
                if (ownerAddress != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                tint = BrandOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = ownerAddress,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        } else if (hasHiddenContactInfo) {
            // Contact info is hidden due to privacy settings (blue)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .background(PreviewBlueBg, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = PreviewBlue
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.contact_info_hidden),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = PreviewBlue
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.contact_info_hidden_hint),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = PreviewBlue.copy(alpha = 0.8f)
                )
            }
        } else {
            // No contact info set (orange)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .background(BrandOrange.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = BrandOrange
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.contact_info_not_set),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = BrandOrange
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Medical Information (in red)
        pet.medicalNotes?.takeIf { it.isNotBlank() }?.let { medicalNotes ->
            InfoCard(
                title = stringResource(R.string.medical_information),
                content = medicalNotes,
                icon = Icons.Default.LocalHospital,
                backgroundColor = MedicalRedBg,
                titleColor = MedicalRed
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Allergies (in orange)
        pet.allergies?.takeIf { it.isNotBlank() }?.let { allergies ->
            InfoCard(
                title = stringResource(R.string.allergies),
                content = allergies,
                icon = Icons.Default.Warning,
                backgroundColor = BrandOrange.copy(alpha = 0.1f),
                titleColor = BrandOrange
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Notes (in blue)
        pet.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            InfoCard(
                title = stringResource(R.string.notes),
                content = notes,
                icon = Icons.Default.Info,
                backgroundColor = PreviewBlueBg,
                titleColor = PreviewBlue
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // How It Works Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.how_it_works),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.help_reunite_pet, pet.name),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                HowItWorksStep(
                    number = "1",
                    title = stringResource(R.string.step_share_location),
                    description = stringResource(R.string.step_share_location_desc_public, pet.name)
                )
                Spacer(modifier = Modifier.height(12.dp))
                HowItWorksStep(
                    number = "2",
                    title = stringResource(R.string.step_owner_notified),
                    description = stringResource(R.string.step_owner_notified_desc_public)
                )
                Spacer(modifier = Modifier.height(12.dp))
                HowItWorksStep(
                    number = "3",
                    title = stringResource(R.string.step_quick_reunion),
                    description = stringResource(R.string.step_quick_reunion_desc_public, pet.name)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Privacy Notice
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(14.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.your_privacy_notice, pet.name),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
private fun InfoCard(
    title: String,
    content: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    titleColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = titleColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = titleColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun HowItWorksStep(
    number: String,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(TealAccent.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = TealAccent
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@SuppressLint("MissingPermission")
private fun fetchCurrentLocation(
    locationProvider: com.google.android.gms.location.FusedLocationProviderClient,
    context: android.content.Context,
    onResult: (Double?, Double?, Double?) -> Unit
) {
    locationProvider.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
            onResult(location.latitude, location.longitude, location.accuracy.toDouble())
        } else {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
                .setMinUpdateIntervalMillis(2000L)
                .setMaxUpdates(1)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let {
                        onResult(it.latitude, it.longitude, it.accuracy.toDouble())
                    } ?: onResult(null, null, null)
                    locationProvider.removeLocationUpdates(this)
                }
            }

            locationProvider.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        }
    }.addOnFailureListener {
        onResult(null, null, null)
    }
}
