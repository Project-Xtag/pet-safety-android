package com.petsafety.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.petsafety.app.R
import com.petsafety.app.data.model.VaccinationStatus
import com.petsafety.app.ui.theme.ErrorColor
import com.petsafety.app.ui.theme.SuccessGreen
import com.petsafety.app.ui.theme.WarningColor

/**
 * The single status → colour / icon / label mapping for the whole feature.
 * Every vaccination surface (pet-detail section, home card, list, detail)
 * renders status through this one component so the vocabulary never drifts.
 */
@Composable
fun VaccinationStatusPill(
    status: VaccinationStatus,
    modifier: Modifier = Modifier
) {
    val color: Color
    val icon: ImageVector
    val labelRes: Int
    when (status) {
        VaccinationStatus.VALID -> {
            color = SuccessGreen; icon = Icons.Filled.CheckCircle; labelRes = R.string.vaccination_status_valid
        }
        VaccinationStatus.EXPIRING -> {
            color = WarningColor; icon = Icons.Filled.Schedule; labelRes = R.string.vaccination_status_expiring
        }
        VaccinationStatus.EXPIRED -> {
            color = ErrorColor; icon = Icons.Filled.Warning; labelRes = R.string.vaccination_status_expired
        }
    }

    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(labelRes),
            color = color,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}
