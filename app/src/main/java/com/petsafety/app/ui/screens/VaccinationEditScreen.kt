package com.petsafety.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.petsafety.app.R
import com.petsafety.app.data.model.UpdateVaccinationRequest
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import com.petsafety.app.ui.viewmodel.VaccinationsViewModel
import java.time.LocalDate

/**
 * Edit an existing vaccination. Reached (push) from the detail screen, using the
 * shared (pet_detail-scoped) [VaccinationsViewModel]. `vaccine_code` is immutable
 * — the vaccine is shown read-only with a "delete & re-add" note and is NOT in
 * [UpdateVaccinationRequest]. Editable: administered (≤ today), expiry, details.
 * Save → PUT → gate refresh (onDidMutate) + success toast + pop. A 400 (e.g.
 * rabies floor on a changed administered date) surfaces inline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccinationEditScreen(
    petId: String,
    vaccinationId: String,
    onBack: () -> Unit,
    appStateViewModel: AppStateViewModel,
    viewModel: VaccinationsViewModel
) {
    val ui by viewModel.uiState.collectAsState()
    val record = ui.vaccinations.firstOrNull { it.id == vaccinationId }

    LaunchedEffect(Unit) {
        viewModel.onDidMutate = { appStateViewModel.refreshVaccinationGate() }
    }
    LaunchedEffect(record == null, ui.isLoading) {
        if (record == null && !ui.isLoading) onBack()
    }

    val savedMsg = stringResource(R.string.vaccination_saved)

    if (record == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val hadExpiry = record.expiresAt != null
    // Prefilled, keyed on the record id so a background re-pull doesn't reset edits.
    var administeredAt by remember(record.id) { mutableStateOf(parseIso(record.administeredAt)) }
    var hasExpiry by remember(record.id) { mutableStateOf(hadExpiry) }
    var expiresAt by remember(record.id) { mutableStateOf(record.expiresAt?.let { parseIso(it) }) }
    var batch by remember(record.id) { mutableStateOf(record.batchNumber ?: "") }
    var vetName by remember(record.id) { mutableStateOf(record.vetName ?: "") }
    var vetClinic by remember(record.id) { mutableStateOf(record.vetClinic ?: "") }
    var notes by remember(record.id) { mutableStateOf(record.notes ?: "") }
    var actionError by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    fun save() {
        val adm = administeredAt ?: return
        saving = true
        actionError = null
        val request = UpdateVaccinationRequest(
            administeredAt = adm.toString(),
            // Sent when the record had an expiry OR the user toggled one on; else
            // omitted (server leaves it unchanged). Clearing = delete + re-add.
            expiresAt = if (hadExpiry || hasExpiry) expiresAt?.toString() else null,
            batchNumber = batch, // sent as-is incl "" so clearing persists
            vetName = vetName,
            vetClinic = vetClinic,
            notes = notes
        )
        viewModel.update(petId, vaccinationId, request) { ok, message ->
            saving = false
            if (ok) { appStateViewModel.showSuccess(savedMsg); onBack() }
            else actionError = message
        }
    }

    val canSave = administeredAt != null && !saving

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vaccination_edit_title)) },
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Vaccine — read-only (immutable)
            FormSection {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.vaccination_field_vaccine), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(record.vaccineNameSnapshot, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    text = stringResource(R.string.vaccination_change_vaccine_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FormSection {
                DateField(
                    label = stringResource(R.string.vaccination_field_administered),
                    date = administeredAt, maxToday = true, onPick = { administeredAt = it }
                )
                if (hadExpiry) {
                    DateField(
                        label = stringResource(R.string.vaccination_field_expiry),
                        date = expiresAt, maxToday = false, onPick = { expiresAt = it }
                    )
                } else {
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
                    }
                }
            }

            FormSection {
                OutlinedTextField(value = batch, onValueChange = { batch = it },
                    label = { Text(stringResource(R.string.vaccination_field_batch)) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = vetName, onValueChange = { vetName = it },
                    label = { Text(stringResource(R.string.vaccination_field_vet)) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = vetClinic, onValueChange = { vetClinic = it },
                    label = { Text(stringResource(R.string.vaccination_field_clinic)) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.vaccination_field_notes)) },
                    modifier = Modifier.fillMaxWidth())
            }

            actionError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun parseIso(iso: String): LocalDate? = runCatching { LocalDate.parse(iso) }.getOrNull()
