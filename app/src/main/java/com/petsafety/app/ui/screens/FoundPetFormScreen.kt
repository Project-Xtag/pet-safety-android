package com.petsafety.app.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.petsafety.app.R
import com.petsafety.app.data.local.FoundPetManageTokenStore
import com.petsafety.app.data.model.CommunityFoundPet
import com.petsafety.app.data.repository.CommunityFoundPetsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Sheet form a community member fills out when they find a stray pet.
 * Web/iOS parity: same fields, same validation, same multipart submit.
 *
 * Auth is optional — anonymous reporters get a single-use manage token
 * persisted to SharedPreferences so the same device can mark the report
 * as reunited later without an account.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoundPetFormScreen(
    onDismiss: () -> Unit,
    onSubmitted: (CommunityFoundPet) -> Unit,
) {
    val vm: FoundPetFormViewModel = hiltViewModel()
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) vm.setPhoto(bytes)
        }
    }

    val locationProvider = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) captureLocation(context, locationProvider, vm)
    }

    LaunchedEffect(state.submittedReport) {
        state.submittedReport?.let {
            onSubmitted(it)
            onDismiss()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.found_pet_form_title)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(
                        onClick = { vm.submit() },
                        enabled = !state.isSubmitting,
                    ) {
                        Text(stringResource(R.string.found_pet_form_submit), fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SpeciesSection(state.species) { vm.species.value = it }
            SexSection(state.sex) { vm.sex.value = it }
            DetailsSection(state, vm)
            FoundAtSection(state.foundAt) { vm.foundAt.value = it }
            LocationSection(
                state = state,
                onGps = {
                    if (ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        captureLocation(context, locationProvider, vm)
                    } else {
                        locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                onAddressChange = { vm.address.value = it },
            )
            PhotoSection(
                photoBytes = state.photoBytes,
                onPick = {
                    photoPicker.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                onClear = { vm.clearPhoto() },
            )
            ContactSection(state, vm)
            state.validationError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
            state.networkError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
            if (state.isSubmitting) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun captureLocation(
    context: android.content.Context,
    provider: com.google.android.gms.location.FusedLocationProviderClient,
    vm: FoundPetFormViewModel,
) {
    @Suppress("MissingPermission")
    provider.lastLocation.addOnSuccessListener { loc ->
        if (loc != null) vm.setCoordinate(loc.latitude, loc.longitude)
    }
}

// MARK: - Sections

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeciesSection(
    selected: CommunityFoundPet.Species,
    onSelect: (CommunityFoundPet.Species) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionHeader(stringResource(R.string.found_pet_form_species))
        val options = listOf(
            CommunityFoundPet.Species.DOG to stringResource(R.string.lost_and_found_species_dog_singular),
            CommunityFoundPet.Species.CAT to stringResource(R.string.lost_and_found_species_cat_singular),
            CommunityFoundPet.Species.OTHER to stringResource(R.string.lost_and_found_species_other_singular),
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, (value, label) ->
                SegmentedButton(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                ) { Text(label) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SexSection(
    selected: CommunityFoundPet.Sex,
    onSelect: (CommunityFoundPet.Sex) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionHeader(stringResource(R.string.found_pet_form_sex))
        val options = listOf(
            CommunityFoundPet.Sex.MALE to stringResource(R.string.found_pet_form_sex_male),
            CommunityFoundPet.Sex.FEMALE to stringResource(R.string.found_pet_form_sex_female),
            CommunityFoundPet.Sex.UNKNOWN to stringResource(R.string.found_pet_form_sex_unknown),
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, (value, label) ->
                SegmentedButton(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                ) { Text(label) }
            }
        }
    }
}

@Composable
private fun DetailsSection(state: FoundPetFormState, vm: FoundPetFormViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(stringResource(R.string.found_pet_form_details))
        OutlinedTextField(
            value = state.breed,
            onValueChange = { if (it.length <= 120) vm.breed.value = it },
            label = { Text(stringResource(R.string.found_pet_form_breed_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.color,
            onValueChange = { if (it.length <= 120) vm.color.value = it },
            label = { Text(stringResource(R.string.found_pet_form_color_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.description,
            onValueChange = { if (it.length <= 2000) vm.description.value = it },
            label = { Text(stringResource(R.string.found_pet_form_description_placeholder)) },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FoundAtSection(value: Long, onChange: (Long) -> Unit) {
    val context = LocalContext.current
    val formatter = remember {
        SimpleDateFormat("yyyy.MM.dd. HH:mm", Locale.getDefault())
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionHeader(stringResource(R.string.found_pet_form_found_at))
        Button(
            onClick = {
                val cal = Calendar.getInstance().apply { timeInMillis = value }
                DatePickerDialog(
                    context,
                    { _, y, m, d ->
                        cal.set(Calendar.YEAR, y); cal.set(Calendar.MONTH, m); cal.set(Calendar.DAY_OF_MONTH, d)
                        TimePickerDialog(
                            context,
                            { _, h, min ->
                                cal.set(Calendar.HOUR_OF_DAY, h); cal.set(Calendar.MINUTE, min)
                                onChange(cal.timeInMillis.coerceAtMost(System.currentTimeMillis()))
                            },
                            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true,
                        ).show()
                    },
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH),
                ).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
            },
            colors = ButtonDefaults.outlinedButtonColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(formatter.format(Date(value)))
        }
    }
}

@Composable
private fun LocationSection(
    state: FoundPetFormState,
    onGps: () -> Unit,
    onAddressChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionHeader(stringResource(R.string.found_pet_form_location))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onGps, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.found_pet_form_use_gps))
            }
            state.lat?.let {
                Text(
                    String.format(Locale.US, "%.4f, %.4f", it, state.lng ?: 0.0),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OutlinedTextField(
            value = state.address,
            onValueChange = { if (it.length <= 500) onAddressChange(it) },
            label = { Text(stringResource(R.string.found_pet_form_address_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            stringResource(R.string.found_pet_form_location_hint),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PhotoSection(photoBytes: ByteArray?, onPick: () -> Unit, onClear: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionHeader(stringResource(R.string.found_pet_form_photo))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (photoBytes != null) {
                AsyncImage(
                    model = photoBytes,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = Color.Gray)
                }
            }
            Column {
                Button(onClick = onPick, shape = RoundedCornerShape(12.dp)) {
                    Text(stringResource(if (photoBytes == null) R.string.found_pet_form_add_photo else R.string.found_pet_form_change_photo))
                }
                if (photoBytes != null) {
                    TextButton(onClick = onClear) {
                        Text(stringResource(R.string.found_pet_form_remove_photo), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        Text(
            stringResource(R.string.found_pet_form_photo_hint),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ContactSection(state: FoundPetFormState, vm: FoundPetFormViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(stringResource(R.string.found_pet_form_contact))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.found_pet_form_share_contact), modifier = Modifier.weight(1f))
            Switch(checked = state.shareContact, onCheckedChange = { vm.shareContact.value = it })
        }
        if (state.shareContact) {
            OutlinedTextField(
                value = state.reporterName,
                onValueChange = { if (it.length <= 200) vm.reporterName.value = it },
                label = { Text(stringResource(R.string.found_pet_form_name_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.reporterEmail,
                onValueChange = { vm.reporterEmail.value = it },
                label = { Text(stringResource(R.string.found_pet_form_email_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.reporterPhone,
                onValueChange = { vm.reporterPhone.value = it },
                label = { Text(stringResource(R.string.found_pet_form_phone_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// MARK: - State + VM

data class FoundPetFormState(
    val species: CommunityFoundPet.Species = CommunityFoundPet.Species.DOG,
    val sex: CommunityFoundPet.Sex = CommunityFoundPet.Sex.UNKNOWN,
    val breed: String = "",
    val color: String = "",
    val description: String = "",
    val foundAt: Long = System.currentTimeMillis(),
    val lat: Double? = null,
    val lng: Double? = null,
    val address: String = "",
    val photoBytes: ByteArray? = null,
    val shareContact: Boolean = false,
    val reporterName: String = "",
    val reporterEmail: String = "",
    val reporterPhone: String = "",
    val isSubmitting: Boolean = false,
    val validationError: String? = null,
    val networkError: String? = null,
    val submittedReport: CommunityFoundPet? = null,
)

@HiltViewModel
class FoundPetFormViewModel @Inject constructor(
    private val repository: CommunityFoundPetsRepository,
    private val manageTokenStore: FoundPetManageTokenStore,
) : ViewModel() {
    val species = kotlinx.coroutines.flow.MutableStateFlow(CommunityFoundPet.Species.DOG)
    val sex = kotlinx.coroutines.flow.MutableStateFlow(CommunityFoundPet.Sex.UNKNOWN)
    val breed = kotlinx.coroutines.flow.MutableStateFlow("")
    val color = kotlinx.coroutines.flow.MutableStateFlow("")
    val description = kotlinx.coroutines.flow.MutableStateFlow("")
    val foundAt = kotlinx.coroutines.flow.MutableStateFlow(System.currentTimeMillis())
    val lat = kotlinx.coroutines.flow.MutableStateFlow<Double?>(null)
    val lng = kotlinx.coroutines.flow.MutableStateFlow<Double?>(null)
    val address = kotlinx.coroutines.flow.MutableStateFlow("")
    private val _photoBytes = kotlinx.coroutines.flow.MutableStateFlow<ByteArray?>(null)
    val shareContact = kotlinx.coroutines.flow.MutableStateFlow(false)
    val reporterName = kotlinx.coroutines.flow.MutableStateFlow("")
    val reporterEmail = kotlinx.coroutines.flow.MutableStateFlow("")
    val reporterPhone = kotlinx.coroutines.flow.MutableStateFlow("")

    private val _isSubmitting = kotlinx.coroutines.flow.MutableStateFlow(false)
    private val _validationError = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private val _networkError = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private val _submitted = kotlinx.coroutines.flow.MutableStateFlow<CommunityFoundPet?>(null)

    /** Snapshot exposed to the composable as a single flow. */
    val state: kotlinx.coroutines.flow.StateFlow<FoundPetFormState> = run {
        val combined: kotlinx.coroutines.flow.Flow<FoundPetFormState> =
            kotlinx.coroutines.flow.combine(
                kotlinx.coroutines.flow.combine(species, sex, breed, color, description) { sp, sx, br, cl, de ->
                    listOf<Any?>(sp, sx, br, cl, de)
                },
                kotlinx.coroutines.flow.combine(foundAt, lat, lng, address, _photoBytes) { fa, la, ln, ad, ph ->
                    listOf<Any?>(fa, la, ln, ad, ph)
                },
                kotlinx.coroutines.flow.combine(shareContact, reporterName, reporterEmail, reporterPhone, _isSubmitting) { sh, rn, re, rp, sub ->
                    listOf<Any?>(sh, rn, re, rp, sub)
                },
                kotlinx.coroutines.flow.combine(_validationError, _networkError, _submitted) { ve, ne, su ->
                    listOf<Any?>(ve, ne, su)
                },
            ) { a, b, c, d ->
                @Suppress("UNCHECKED_CAST")
                FoundPetFormState(
                    species = a[0] as CommunityFoundPet.Species,
                    sex = a[1] as CommunityFoundPet.Sex,
                    breed = a[2] as String,
                    color = a[3] as String,
                    description = a[4] as String,
                    foundAt = b[0] as Long,
                    lat = b[1] as Double?,
                    lng = b[2] as Double?,
                    address = b[3] as String,
                    photoBytes = b[4] as ByteArray?,
                    shareContact = c[0] as Boolean,
                    reporterName = c[1] as String,
                    reporterEmail = c[2] as String,
                    reporterPhone = c[3] as String,
                    isSubmitting = c[4] as Boolean,
                    validationError = d[0] as String?,
                    networkError = d[1] as String?,
                    submittedReport = d[2] as CommunityFoundPet?,
                )
            }
        combined.stateIn(
            viewModelScope,
            kotlinx.coroutines.flow.SharingStarted.Eagerly,
            FoundPetFormState(),
        )
    }

    fun setCoordinate(latitude: Double, longitude: Double) {
        lat.value = latitude
        lng.value = longitude
    }

    fun setPhoto(bytes: ByteArray) { _photoBytes.value = bytes }
    fun clearPhoto() { _photoBytes.value = null }

    fun submit() {
        _validationError.value = null
        _networkError.value = null
        val s = state.value
        if (s.lat == null && s.address.isBlank()) {
            _validationError.value = "found_pet_form_validation_location"
            return
        }
        if (s.photoBytes == null && s.description.trim().isEmpty()) {
            _validationError.value = "found_pet_form_validation_evidence"
            return
        }
        _isSubmitting.value = true
        viewModelScope.launch {
            try {
                val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val response = repository.create(
                    species = s.species,
                    sex = s.sex,
                    foundAtIso = isoFmt.format(Date(s.foundAt)),
                    lat = s.lat ?: 0.0,
                    lng = s.lng ?: 0.0,
                    breed = s.breed.takeIf { it.isNotBlank() },
                    color = s.color.takeIf { it.isNotBlank() },
                    description = s.description.takeIf { it.isNotBlank() },
                    foundAddress = s.address.takeIf { it.isNotBlank() },
                    reporterName = if (s.shareContact) s.reporterName.takeIf { it.isNotBlank() } else null,
                    reporterEmail = if (s.shareContact) s.reporterEmail.takeIf { it.isNotBlank() } else null,
                    reporterPhone = if (s.shareContact) s.reporterPhone.takeIf { it.isNotBlank() } else null,
                    photoBytes = s.photoBytes,
                )
                if (response != null) {
                    manageTokenStore.append(
                        FoundPetManageTokenStore.Entry(
                            id = response.report.id,
                            token = response.manageToken,
                        )
                    )
                    _submitted.value = response.report
                }
            } catch (t: Throwable) {
                _networkError.value = t.localizedMessage
            } finally {
                _isSubmitting.value = false
            }
        }
    }
}

