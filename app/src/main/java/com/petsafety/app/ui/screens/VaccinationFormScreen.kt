package com.petsafety.app.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.petsafety.app.R
import com.petsafety.app.data.model.CreateVaccinationRequest
import com.petsafety.app.ui.components.CertificatePhotoSheet
import com.petsafety.app.ui.util.LocaleFormatting
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import com.petsafety.app.ui.viewmodel.VaccinationsViewModel
import com.petsafety.app.util.VaccinationCertificateEncoder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Add a vaccination (slice 2b; iOS-parity polish). Reached via
 * `vaccination_form/{petId}`. Fields are grouped into cards (mirroring iOS's
 * Form sections); the cert shows a thumbnail preview; toggle-off shows a hint.
 *
 * Locked behaviors: opaque vaccine code; administered required + ≤ today;
 * expiry toggle-off → expires_at omitted (server derives); optional cert via
 * the raw picker → encoder (encode==null surfaced); rabies = server-400 only;
 * create-then-cert half-fail tolerated; success toast on save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccinationFormScreen(
    petId: String,
    species: String,
    country: String,
    onBack: () -> Unit,
    appStateViewModel: AppStateViewModel,
    viewModel: VaccinationsViewModel = hiltViewModel()
) {
    val catalog by viewModel.catalog.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onDidMutate = { appStateViewModel.refreshVaccinationGate() }
        viewModel.loadCatalog(species, country)
    }

    var selectedCode by remember { mutableStateOf<String?>(null) }
    var administeredAt by remember { mutableStateOf<LocalDate?>(null) }
    var hasExpiry by remember { mutableStateOf(false) }
    var expiresAt by remember { mutableStateOf<LocalDate?>(null) }
    var batch by remember { mutableStateOf("") }
    var vetName by remember { mutableStateOf("") }
    var vetClinic by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var cert by remember { mutableStateOf<VaccinationCertificateEncoder.Encoded?>(null) }
    var showCertPicker by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    val savedMsg = stringResource(R.string.vaccination_saved)
    val certFailedMsg = stringResource(R.string.vaccinations_cert_failed)
    val certUnreadableMsg = stringResource(R.string.vaccination_cert_unreadable)

    fun save() {
        val code = selectedCode ?: return
        val adm = administeredAt ?: return
        saving = true
        actionError = null
        val request = CreateVaccinationRequest(
            vaccineCode = code,
            administeredAt = adm.toString(),
            expiresAt = if (hasExpiry) expiresAt?.toString() else null, // toggle-off → omitted → server derives
            batchNumber = batch.ifBlank { null },
            vetName = vetName.ifBlank { null },
            vetClinic = vetClinic.ifBlank { null },
            notes = notes.ifBlank { null }
        )
        viewModel.create(petId, request) { ok, message, created ->
            if (!ok) {
                saving = false
                actionError = message
                return@create
            }
            val encoded = cert
            if (encoded != null && created != null) {
                viewModel.uploadCertificate(petId, created.id, encoded) { certOk, certMsg, _ ->
                    saving = false
                    if (certOk) appStateViewModel.showSuccess(savedMsg)
                    else appStateViewModel.showError(certMsg ?: certFailedMsg) // half-fail: record saved, cert separate
                    onBack()
                }
            } else {
                saving = false
                appStateViewModel.showSuccess(savedMsg)
                onBack()
            }
        }
    }

    val canSave = selectedCode != null && administeredAt != null && !saving

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vaccination_form_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(onClick = { save() }, enabled = canSave) {
                        Text(stringResource(R.string.vaccination_save))
                    }
                }
            )
        }
    ) { padding ->
        if (catalog.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.vaccination_empty_region),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FormSection {
                    VaccinePicker(catalog = catalog, selectedCode = selectedCode, onSelect = { selectedCode = it })
                }

                FormSection {
                    DateField(
                        label = stringResource(R.string.vaccination_field_administered),
                        date = administeredAt, maxToday = true, onPick = { administeredAt = it }
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.vaccination_field_expiry_toggle),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(checked = hasExpiry, onCheckedChange = { hasExpiry = it })
                    }
                    if (hasExpiry) {
                        DateField(
                            label = stringResource(R.string.vaccination_field_expiry),
                            date = expiresAt, maxToday = false, onPick = { expiresAt = it }
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.vaccination_expiry_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FormSection {
                    OutlinedTextField(
                        value = batch, onValueChange = { batch = it },
                        label = { Text(stringResource(R.string.vaccination_field_batch)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = vetName, onValueChange = { vetName = it },
                        label = { Text(stringResource(R.string.vaccination_field_vet)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = vetClinic, onValueChange = { vetClinic = it },
                        label = { Text(stringResource(R.string.vaccination_field_clinic)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes, onValueChange = { notes = it },
                        label = { Text(stringResource(R.string.vaccination_field_notes)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                FormSection { CertRow(cert = cert, onPick = { showCertPicker = true }, onRemove = { cert = null }) }

                actionError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                Text(
                    text = stringResource(R.string.vaccinations_disclaimer),
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showCertPicker) {
        CertificatePhotoSheet(
            onDismiss = { showCertPicker = false },
            onRawBytesSelected = { raw ->
                val encoded = VaccinationCertificateEncoder.encode(raw)
                if (encoded == null) actionError = certUnreadableMsg // surfaced, not dropped
                else { cert = encoded; actionError = null }
                showCertPicker = false
            }
        )
    }
}

@Composable
internal fun FormSection(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun CertRow(
    cert: VaccinationCertificateEncoder.Encoded?,
    onPick: () -> Unit,
    onRemove: () -> Unit
) {
    if (cert == null) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onPick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.add_photo), color = MaterialTheme.colorScheme.primary)
        }
    } else {
        val bitmap = remember(cert) {
            runCatching { BitmapFactory.decodeByteArray(cert.data, 0, cert.data.size) }.getOrNull()
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp))
                )
            }
            Text(
                text = stringResource(R.string.vaccination_cert_attached),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f).padding(start = 12.dp)
            )
            TextButton(onClick = onRemove) { Text(stringResource(R.string.cancel)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaccinePicker(
    catalog: List<com.petsafety.app.data.model.VaccineCatalogEntry>,
    selectedCode: String?,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = catalog.firstOrNull { it.code == selectedCode }?.displayName ?: ""
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.vaccination_field_vaccine)) },
            placeholder = { Text(stringResource(R.string.vaccination_field_vaccine_hint)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            catalog.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(entry.displayName) }, // display_name; submit code
                    onClick = { onSelect(entry.code); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateField(
    label: String,
    date: LocalDate?,
    maxToday: Boolean,
    onPick: (LocalDate) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = date?.let { runCatching { LocaleFormatting.formatDate(it) }.getOrDefault(it.toString()) } ?: "",
        onValueChange = {},
        readOnly = true,
        enabled = false,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().clickable { showDialog = true }
    )
    if (showDialog) {
        val state = rememberDatePickerState(selectableDates = if (maxToday) PastOrTodayUtc else AllDates)
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDialog = false
                }) { Text(stringResource(R.string.vaccination_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) { DatePicker(state = state) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
internal val PastOrTodayUtc = object : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= System.currentTimeMillis()
}

@OptIn(ExperimentalMaterial3Api::class)
internal val AllDates = object : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean = true
}
