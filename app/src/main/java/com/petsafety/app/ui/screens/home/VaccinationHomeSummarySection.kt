package com.petsafety.app.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.petsafety.app.R
import com.petsafety.app.data.model.UrgentVaccination
import com.petsafety.app.data.model.VaccinationStatus
import com.petsafety.app.data.vaccination.VaccinationAvailability
import com.petsafety.app.ui.components.VaccinationStatusPill
import com.petsafety.app.ui.theme.SuccessGreen
import kotlin.math.abs

/**
 * Home-screen vaccination summary, inserted between My Pets and Quick Actions.
 *
 * The parent (PetsListScreen) gates this with `showsHomeCard` (= feature on AND
 * the user has records), so this only composes when there's something to show.
 * Two states: the reassurance "all up to date" line, or the urgent rows.
 */
@Composable
fun VaccinationHomeSummarySection(
    availability: VaccinationAvailability,
    onUrgentTap: (UrgentVaccination) -> Unit,
    modifier: Modifier = Modifier
) {
    // The card only renders in the On state; the cast guards a transient flip.
    val summary = (availability as? VaccinationAvailability.On)?.summary ?: return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                    text = stringResource(R.string.vaccinations_home_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (summary.urgent.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.vaccinations_home_all_valid),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                summary.urgent.forEach { urgent ->
                    UrgentVaccinationRow(urgent = urgent, onClick = { onUrgentTap(urgent) })
                }
            }
        }
    }
}

@Composable
private fun UrgentVaccinationRow(
    urgent: UrgentVaccination,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = urgent.petProfileImage,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = urgent.petName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = urgent.vaccineName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = daysCopy(urgent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        VaccinationStatusPill(status = urgent.statusEnum)
    }
}

/**
 * Human "N days" copy off the SIGNED day count: branch on status, use the
 * magnitude. `days_until_expiry` is negative when overdue.
 */
@Composable
private fun daysCopy(urgent: UrgentVaccination): String {
    val days = abs(urgent.daysUntilExpiry)
    return if (urgent.statusEnum == VaccinationStatus.EXPIRED) {
        pluralStringResource(R.plurals.vaccination_expired_days_ago, days, days)
    } else {
        pluralStringResource(R.plurals.vaccination_expires_in_days, days, days)
    }
}
