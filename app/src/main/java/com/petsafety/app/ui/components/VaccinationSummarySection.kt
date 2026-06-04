package com.petsafety.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.petsafety.app.R
import com.petsafety.app.data.model.Vaccination
import com.petsafety.app.ui.a11y.markAsButton
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.viewmodel.VaccinationsViewModel

/**
 * Pet-detail vaccination card. Shows the latest few records with status pills,
 * a "show all" link (whenever records exist — so the list is reachable even
 * with 1–3 records, matching iOS), and a prominent add button.
 *
 * Renders ONLY when the feature is on (parent gates with `availability.isOn`),
 * so the per-pet VM load never runs for an off user.
 */
@Composable
fun VaccinationSummarySection(
    petId: String,
    onAdd: () -> Unit,
    onShowAll: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VaccinationsViewModel = hiltViewModel()
) {
    val ui by viewModel.uiState.collectAsState()
    LaunchedEffect(petId) { viewModel.load(petId) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Vaccines,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.vaccinations_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            val records = ui.vaccinations
            when {
                ui.isLoading && records.isEmpty() ->
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)

                records.isEmpty() ->
                    Text(
                        text = stringResource(R.string.vaccinations_section_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                else -> {
                    records.take(3).forEach { v -> VaccinationRowCompact(v) }
                    // Always reachable when records exist (not only > 3), so the
                    // full list is one tap away even with 1–3 records.
                    Text(
                        text = stringResource(R.string.vaccinations_show_all, records.size),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = BrandOrange,
                        modifier = Modifier.clickable { onShowAll() }.markAsButton()
                    )
                }
            }

            BrandButton(
                text = stringResource(R.string.vaccinations_add_cta),
                onClick = onAdd,
                icon = Icons.Filled.Add,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun VaccinationRowCompact(vaccination: Vaccination) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = vaccination.vaccineNameSnapshot,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (vaccination.isMandatory) VaccinationMandatoryPill()
            VaccinationStatusPill(status = vaccination.status)
        }
    }
}
