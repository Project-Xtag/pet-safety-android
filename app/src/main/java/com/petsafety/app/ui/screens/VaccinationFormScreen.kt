package com.petsafety.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.petsafety.app.R
import com.petsafety.app.data.model.CreateVaccinationRequest
import com.petsafety.app.ui.components.CertificatePhotoSheet
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import com.petsafety.app.ui.viewmodel.VaccinationsViewModel
import com.petsafety.app.util.VaccinationCertificateEncoder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Add a vaccination (slice 2b). Reached via the canonical
 * `vaccination_form/{petId}` route from the list FAB (the single add entry).
 *
 * Locked behaviors: opaque vaccine code (show display_name, submit code);
 * administered required + ≤ today; expiry toggle-off → expires_at omitted so
 * the server derives (no client preview); optional cert via the raw picker →
 * encoder (encode==null surfaced, not dropped); rabies validation is
 * server-400-only (surfaced verbatim); create-then-cert with a half-fail
 * tolerated; success toast on save.
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
            // Toggle-off → null → omitted on the wire → server derives. (verified)
            expiresAt = if (hasExpiry) expiresAt?.toString() else null,
            batchNumber = batch.ifBlank { null },
            vetName = vetName.ifBlank { null },
            notes = notes.ifBlank { null }
        )
        viewModel.create(petId, request) { ok, message, created ->
            if (!ok) {
                saving = false
                actionError = message // server-localized (e.g. rabies age-floor)
                return@create
            }
            val encoded = cert
            if (encoded != null && created != null) {
                // Create-then-cert: the record is already saved. A cert failure
                // surfaces separately and does NOT roll back the save.
                viewModel.uploadCertificate(petId, created.id, encoded) { certOk, certMsg, _ ->
                    saving = false
                    if (certOk) appStateViewModel.showSuccess(savedMsg)
                    else appStateViewModel.showError(certMsg ?: certFailedMsg)
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
            // Explicit empty-region state — never a blank/disabled picker. Live
            // path given the users.country data debt (non-ISO-2 → empty catalog).
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
                VaccinePicker(
                    catalog = catalog,
                    selectedCode = selectedCode,
                    onSelect = { selectedCode = it }
                )

                DateField(
                    label = stringResource(R.string.vaccination_field_administered),
                    date = administeredAt,
                    maxToday = true,
                    onPick = { administeredAt = it }
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
                        date = expiresAt,
                        maxToday = false,
                        onPick = { expiresAt = it }
                    )
                }

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
                    value = notes, onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.vaccination_field_notes)) },
                    modifier = Modifier.fillMaxWidth()
                )

                // Optional certificate photo
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (cert == null) stringResource(R.string.add_photo)
                        else stringResource(R.string.vaccination_cert_attached),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showCertPicker = true }
                    )
                    if (cert != null) {
                        TextButton(onClick = { cert = null }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }

                actionError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                Text(
                    text = stringResource(R.string.vaccinations_disclaimer),
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(0.dp))
            }
        }
    }

    if (showCertPicker) {
        CertificatePhotoSheet(
            onDismiss = { showCertPicker = false },
            onRawBytesSelected = { raw ->
                val encoded = VaccinationCertificateEncoder.encode(raw)
                if (encoded == null) {
                    // encode == null: undecodable (e.g. HEIC on API 26/27). Surface, don't drop.
                    actionError = certUnreadableMsg
                } else {
                    cert = encoded
                    actionError = null
                }
                showCertPicker = false
            }
        )
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
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            catalog.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(entry.displayName) }, // show display_name; submit code
                    onClick = {
                        onSelect(entry.code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    label: String,
    date: LocalDate?,
    maxToday: Boolean,
    onPick: (LocalDate) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = date?.toString() ?: "",
        onValueChange = {},
        readOnly = true,
        enabled = false,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
    )
    if (showDialog) {
        val state = rememberDatePickerState(
            selectableDates = if (maxToday) PastOrTodayUtc else AllDates
        )
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
private val PastOrTodayUtc = object : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        utcTimeMillis <= System.currentTimeMillis()
}

@OptIn(ExperimentalMaterial3Api::class)
private val AllDates = object : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean = true
}
