package com.petsafety.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.petsafety.app.R
import com.petsafety.app.data.model.PendingRegistration
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.theme.SuccessGreen
import com.petsafety.app.ui.theme.InfoBlue
import com.petsafety.app.ui.viewmodel.PendingRegistrationsViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingRegistrationsScreen(
    onBack: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToCreatePet: (String) -> Unit,
    onNavigateToOrderTags: () -> Unit
) {
    val viewModel: PendingRegistrationsViewModel = hiltViewModel()
    val registrations by viewModel.registrations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val readyToActivate = registrations.filter {
        it.orderStatus.lowercase() in listOf("shipped", "delivered")
    }
    val stillProcessing = registrations.filter {
        it.orderStatus.lowercase() !in listOf("shipped", "delivered")
    }

    LaunchedEffect(Unit) { viewModel.fetchPendingRegistrations() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
            Text(
                text = stringResource(R.string.pending_registrations_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when {
                isLoading && registrations.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandOrange)
                    }
                }
                registrations.isEmpty() && !isLoading -> {
                    // Empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(44.dp),
                                    tint = SuccessGreen
                                )
                            }
                            Text(
                                text = stringResource(R.string.all_caught_up),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = stringResource(R.string.all_caught_up_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                            Button(
                                onClick = onNavigateToOrderTags,
                                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                            ) {
                                Text(stringResource(R.string.order_tags))
                            }
                        }
                    }
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Ready to Activate Section
                            if (readyToActivate.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = SuccessGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.ready_to_activate),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                }
                                items(readyToActivate) { reg ->
                                    PendingRegistrationCard(
                                        registration = reg,
                                        isReady = true,
                                        onScanTag = onNavigateToScanner,
                                        onCreateProfile = { onNavigateToCreatePet(reg.petName) }
                                    )
                                }
                            }

                            // Still Processing Section
                            if (stillProcessing.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.still_processing),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                }
                                items(stillProcessing) { reg ->
                                    PendingRegistrationCard(
                                        registration = reg,
                                        isReady = false,
                                        onScanTag = {},
                                        onCreateProfile = { onNavigateToCreatePet(reg.petName) }
                                    )
                                }
                            }

                            // Help Section
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                HelpSection()
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingRegistrationCard(
    registration: PendingRegistration,
    isReady: Boolean,
    onScanTag: () -> Unit,
    onCreateProfile: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isReady)
                SuccessGreen.copy(alpha = 0.05f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isReady) SuccessGreen.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = registration.petName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = formatDate(registration.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(status = registration.orderStatus)
            }

            // Tracking link for ready items
            if (isReady && registration.mplTrackingNumber != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.clickable {
                        uriHandler.openUri(
                            "https://nyomkovetes.posta.hu/international?itemNumber=${registration.mplTrackingNumber}"
                        )
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = BrandOrange
                    )
                    Text(
                        text = stringResource(R.string.track_package),
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isReady) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onScanTag,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.scan_tag_now))
                    }
                    OutlinedButton(
                        onClick = onCreateProfile,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.create_profile_first))
                    }
                }
            } else {
                OutlinedButton(onClick = onCreateProfile) {
                    Text(stringResource(R.string.create_profile_while_waiting))
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (text, color) = when (status.lowercase()) {
        "shipped" -> stringResource(R.string.status_shipped) to InfoBlue
        "delivered" -> stringResource(R.string.status_delivered) to SuccessGreen
        "processing" -> stringResource(R.string.status_processing) to BrandOrange
        else -> stringResource(R.string.status_pending) to Color.Gray
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun HelpSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.activation_help_title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(12.dp))
            HelpStep(number = "1", text = stringResource(R.string.activation_help_step1))
            HelpStep(number = "2", text = stringResource(R.string.activation_help_step2))
            HelpStep(number = "3", text = stringResource(R.string.activation_help_step3))
            HelpStep(number = "4", text = stringResource(R.string.activation_help_step4))
        }
    }
}

@Composable
private fun HelpStep(number: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(BrandOrange.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = BrandOrange
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(dateString.take(19))
        if (date != null) {
            val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            outputFormat.format(date)
        } else {
            dateString.take(10)
        }
    } catch (e: Exception) {
        dateString.take(10)
    }
}
