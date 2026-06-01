package com.petsafety.app.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.petsafety.app.R
import com.petsafety.app.data.model.Vaccination
import com.petsafety.app.ui.components.CertificatePhotoSheet
import com.petsafety.app.ui.components.VaccinationStatusPill
import com.petsafety.app.ui.util.LocaleFormatting
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import com.petsafety.app.ui.viewmodel.VaccinationsViewModel
import com.petsafety.app.util.VaccinationCertificateEncoder
import java.time.LocalDate

/**
 * Read-only detail for one vaccination, with edit (push), delete (confirmed),
 * and photo add/replace/remove. Reads the LIVE record by id from the shared
 * (pet_detail-scoped) [VaccinationsViewModel], so edits/cert changes reflect
 * immediately and — on delete — the by-id lookup goes null and the view pops.
 *
 * Single pop trigger: the null-observation [LaunchedEffect] (NOT an explicit pop
 * in the delete handler) — delete → re-pull → record gone → pop, once. The
 * `firstOrNull` is a null-SAFE render guard, not a second pop.
 *
 * Refresh split: delete → gate refresh (counts change); cert add/replace/remove
 * → local row only, NO gate refresh.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccinationDetailScreen(
    petId: String,
    vaccinationId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    appStateViewModel: AppStateViewModel,
    viewModel: VaccinationsViewModel
) {
    val ui by viewModel.uiState.collectAsState()
    val record = ui.vaccinations.firstOrNull { it.id == vaccinationId }

    // Bind the gate-refresh hook (delete fires onDidMutate regardless of how the
    // user reached detail) + guard-load for the edge case the shared VM isn't
    // loaded yet (a future deep-link straight into detail); via the list it's
    // already populated.
    LaunchedEffect(Unit) {
        viewModel.onDidMutate = { appStateViewModel.refreshVaccinationGate() }
        if (ui.vaccinations.isEmpty() && !ui.isLoading) viewModel.load(petId)
    }
    // SINGLE pop trigger: record absent after a load completed (deleted here or
    // gone) → pop once. Guarded by !isLoading so an initial load doesn't pop.
    LaunchedEffect(record == null, ui.isLoading) {
        if (record == null && !ui.isLoading) onBack()
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showCertPicker by remember { mutableStateOf(false) }

    val certUnreadable = stringResource(R.string.vaccination_cert_unreadable)
    val certUpdated = stringResource(R.string.vaccination_cert_updated)
    val certRemoved = stringResource(R.string.vaccination_cert_removed)
    val certFailed = stringResource(R.string.vaccinations_cert_failed)
    val deletedMsg = stringResource(R.string.vaccination_deleted)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(record?.vaccineNameSnapshot ?: stringResource(R.string.vaccinations_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (record != null) {
                        TextButton(onClick = onEdit) { Text(stringResource(R.string.vaccination_edit_title)) }
                    }
                }
            )
        }
    ) { padding ->
        val v = record
        if (v == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: name + status pill + immutable-vaccine note
            FormSection {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = v.vaccineNameSnapshot,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    VaccinationStatusPill(status = v.status)
                }
                Text(
                    text = stringResource(R.string.vaccination_change_vaccine_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Dates
            FormSection {
                LabelledRow(stringResource(R.string.vaccination_field_administered), formatDate(v.administeredAt))
                LabelledRow(
                    stringResource(R.string.vaccination_field_expiry),
                    v.expiresAt?.let { formatDate(it) } ?: stringResource(R.string.vaccinations_no_expiry)
                )
            }

            // Optional details (only when present)
            val hasDetails = !v.batchNumber.isNullOrBlank() || !v.vetName.isNullOrBlank() ||
                !v.vetClinic.isNullOrBlank() || !v.notes.isNullOrBlank()
            if (hasDetails) {
                FormSection {
                    v.batchNumber?.takeIf { it.isNotBlank() }?.let { LabelledRow(stringResource(R.string.vaccination_field_batch), it) }
                    v.vetName?.takeIf { it.isNotBlank() }?.let { LabelledRow(stringResource(R.string.vaccination_field_vet), it) }
                    v.vetClinic?.takeIf { it.isNotBlank() }?.let { LabelledRow(stringResource(R.string.vaccination_field_clinic), it) }
                    v.notes?.takeIf { it.isNotBlank() }?.let { LabelledRow(stringResource(R.string.vaccination_field_notes), it) }
                }
            }

            // Photo (cert) — add / replace / remove. No gate refresh.
            FormSection {
                if (v.certificateUrl != null) {
                    AsyncImage(
                        model = v.certificateUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Row {
                        TextButton(onClick = { showCertPicker = true }) { Text(stringResource(R.string.vaccination_replace_photo)) }
                        TextButton(onClick = {
                            viewModel.deleteCertificate(petId, v.id) { ok, msg ->
                                if (ok) appStateViewModel.showSuccess(certRemoved)
                                else appStateViewModel.showError(msg ?: certFailed)
                            }
                        }) { Text(stringResource(R.string.vaccination_remove_photo), color = MaterialTheme.colorScheme.error) }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showCertPicker = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.add_photo), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Text(
                text = stringResource(R.string.vaccinations_disclaimer),
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TextButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.vaccination_delete), color = MaterialTheme.colorScheme.error) }
        }
    }

    if (showDeleteConfirm && record != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.vaccination_delete_confirm_title)) },
            text = { Text(stringResource(R.string.vaccination_delete_confirm_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete(petId, record.id) { ok, msg ->
                        if (ok) appStateViewModel.showSuccess(deletedMsg) // pop happens via the null-observation
                        else appStateViewModel.showError(msg ?: "")
                    }
                }) { Text(stringResource(R.string.vaccination_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showCertPicker && record != null) {
        CertificatePhotoSheet(
            onDismiss = { showCertPicker = false },
            onRawBytesSelected = { raw ->
                showCertPicker = false
                val encoded = VaccinationCertificateEncoder.encode(raw)
                if (encoded == null) {
                    appStateViewModel.showError(certUnreadable)
                } else {
                    viewModel.uploadCertificate(petId, record.id, encoded) { ok, msg, _ ->
                        if (ok) appStateViewModel.showSuccess(certUpdated)
                        else appStateViewModel.showError(msg ?: certFailed)
                    }
                }
            }
        )
    }
}

private fun formatDate(iso: String): String =
    runCatching { LocaleFormatting.formatDate(LocalDate.parse(iso)) }.getOrDefault(iso)

@Composable
private fun LabelledRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}
