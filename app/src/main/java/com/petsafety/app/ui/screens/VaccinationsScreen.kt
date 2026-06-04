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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import com.petsafety.app.ui.components.BrandButton
import com.petsafety.app.ui.components.VaccinationMandatoryPill
import com.petsafety.app.ui.components.VaccinationStatusPill
import com.petsafety.app.ui.util.LocaleFormatting
import com.petsafety.app.ui.viewmodel.VaccinationsViewModel
import java.time.LocalDate

/**
 * Full vaccination list for one pet (slice 1b, polished in the iOS-parity pass).
 * Reached by the canonical route `pet_vaccinations/{petId}`.
 *
 * Display only this slice: rows are inert-tap (detail is slice 3); the add FAB
 * opens the form (slice 2b).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccinationsScreen(
    petId: String,
    onBack: () -> Unit,
    onAdd: () -> Unit = {},
    onOpenDetail: (String) -> Unit = {},
    viewModel: VaccinationsViewModel = hiltViewModel()
) {
    val ui by viewModel.uiState.collectAsState()
    LaunchedEffect(petId) { viewModel.load(petId) }

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
            FloatingActionButton(onClick = onAdd) {
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
            when {
                ui.isLoading && ui.vaccinations.isEmpty() ->
                    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }

                ui.errorMessage != null && ui.vaccinations.isEmpty() ->
                    CenteredState(
                        icon = { Icon(Icons.Filled.Warning, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        text = ui.errorMessage ?: stringResource(R.string.vaccinations_load_failed)
                    ) {
                        BrandButton(text = stringResource(R.string.try_again), onClick = { viewModel.load(petId) })
                    }

                ui.vaccinations.isEmpty() ->
                    CenteredState(
                        icon = { Icon(Icons.Filled.Vaccines, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        text = stringResource(R.string.vaccinations_section_empty_hint)
                    )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (active.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.vaccinations_section_active)) }
                        item { GroupedCard(active, onOpenDetail) }
                    }
                    if (expired.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.vaccinations_section_expired)) }
                        item { GroupedCard(expired, onOpenDetail) }
                    }
                    item { DisclaimerFooter() }
                }
            }
        }
    }
}

private fun Vaccination.expiryDateOrMax(): LocalDate =
    expiresAt?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.MAX

private fun formatDate(iso: String): String =
    runCatching { LocaleFormatting.formatDate(LocalDate.parse(iso)) }.getOrDefault(iso)

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

/** A rounded grouped card holding the section's rows with dividers — the Material take on iOS's inset-grouped list. */
@Composable
private fun GroupedCard(rows: List<Vaccination>, onRowClick: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column {
            rows.forEachIndexed { index, v ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                VaccinationRow(v, onClick = { onRowClick(v.id) })
            }
        }
    }
}

@Composable
private fun VaccinationRow(vaccination: Vaccination, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = vaccination.vaccineNameSnapshot,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.vaccinations_administered_value, formatDate(vaccination.administeredAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = vaccination.expiresAt
                    ?.let { stringResource(R.string.vaccinations_expires_value, formatDate(it)) }
                    ?: stringResource(R.string.vaccinations_no_expiry),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.size(12.dp))
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (vaccination.isMandatory) VaccinationMandatoryPill()
            VaccinationStatusPill(status = vaccination.status)
        }
    }
}

@Composable
private fun CenteredState(
    icon: @Composable () -> Unit,
    text: String,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon()
        Spacer(Modifier.height(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}

@Composable
private fun DisclaimerFooter() {
    Text(
        text = stringResource(R.string.vaccinations_disclaimer),
        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}
