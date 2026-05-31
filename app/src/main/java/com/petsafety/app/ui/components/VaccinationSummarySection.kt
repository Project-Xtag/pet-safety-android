package com.petsafety.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
 * Pet-detail vaccination card, mirroring HealthInfoSection's shape. Shows the
 * latest few records with a status pill, or an empty hint + add CTA.
 *
 * Renders ONLY when the feature is on — the parent (PetDetailScreen) gates this
 * with `availability.isOn`, so this composable (and its per-pet VM load) never
 * enters composition for an off user, keeping CRUD calls off entirely.
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
                    imageVector = Icons.Filled.LocalHospital,
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
            if (records.isEmpty()) {
                Text(
                    text = stringResource(R.string.vaccinations_section_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.vaccinations_add_cta),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = BrandOrange,
                    modifier = Modifier
                        .clickable { onAdd() }
                        .markAsButton()
                )
            } else {
                records.take(3).forEach { v -> VaccinationRowCompact(v) }
                if (records.size > 3) {
                    Text(
                        text = stringResource(R.string.vaccinations_show_all, records.size),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = BrandOrange,
                        modifier = Modifier
                            .clickable { onShowAll() }
                            .markAsButton()
                    )
                }
            }
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
        VaccinationStatusPill(status = vaccination.status)
    }
}
