package com.petsafety.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.petsafety.app.R
import com.petsafety.app.data.model.Vaccination
import com.petsafety.app.data.model.VaccinationStatus
import com.petsafety.app.ui.components.VaccinationStatusPill
import com.petsafety.app.ui.viewmodel.VaccinationsViewModel
import java.time.LocalDate

/**
 * Full vaccination list for one pet (slice 1b). Reached by the canonical route
 * `pet_vaccinations/{petId}` — slice 4's deep-link convergence builds its back
 * stack onto THIS route, so it stays the single canonical entry.
 *
 * Slice scope: DISPLAY only. Rows are inert-tap (detail is slice 3) and the
 * add FAB is a no-op (the form route `vaccination_form/{petId}` isn't
 * registered until slice 2 — a live navigate to it would crash NavHost).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccinationsScreen(
    petId: String,
    onBack: () -> Unit,
    viewModel: VaccinationsViewModel = hiltViewModel()
) {
    val ui by viewModel.uiState.collectAsState()
    LaunchedEffect(petId) { viewModel.load(petId) }

    // Active = valid + expiring, soonest expiry first (no-expiry sorts last).
    // Expired = most-overdue first (earliest past date first). Both use a
    // UTC date parse to line up with the status boundary.
    val active = ui.vaccinations
        .filter { it.status != VaccinationStatus.EXPIRED }
        .sortedBy { it.expiryDateOrMax() }
    val expired = ui.vaccinations
        .filter { it.status == VaccinationStatus.EXPIRED }
        .sortedBy { it.expiryDateOrMax() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vaccinations_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            // Inert until slice 2 registers `vaccination_form/{petId}`. A live
            // navigate to an unregistered route would crash NavHost.
            FloatingActionButton(onClick = { /* TODO(slice 2): navigate to vaccination_form/$petId */ }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.vaccinations_add_cta))
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = ui.isLoading,
            onRefresh = { viewModel.load(petId) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (ui.isLoading && ui.vaccinations.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (ui.vaccinations.isEmpty()) {
                EmptyVaccinations()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (active.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.vaccinations_section_active)) }
                        items(active, key = { it.id }) { VaccinationListRow(it) }
                    }
                    if (expired.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.vaccinations_section_expired)) }
                        items(expired, key = { it.id }) { VaccinationListRow(it) }
                    }
                    item { DisclaimerFooter() }
                }
            }
        }
    }
}

private fun Vaccination.expiryDateOrMax(): LocalDate =
    expiresAt?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.MAX

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun VaccinationListRow(vaccination: Vaccination) {
    // Display-only this slice — no onClick (detail is slice 3).
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = vaccination.vaccineNameSnapshot,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
            VaccinationStatusPill(status = vaccination.status)
        }
        Text(
            text = vaccination.administeredAt + (vaccination.expiresAt?.let { " → $it" } ?: ""),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyVaccinations() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.vaccinations_section_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp))
        DisclaimerFooter()
    }
}

@Composable
private fun DisclaimerFooter() {
    Text(
        text = stringResource(R.string.vaccinations_disclaimer),
        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp)
    )
}
