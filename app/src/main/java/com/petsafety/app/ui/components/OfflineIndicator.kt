package com.petsafety.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.petsafety.app.data.sync.SyncService
import com.petsafety.app.R
import kotlinx.coroutines.launch

@Composable
fun OfflineIndicator(syncService: SyncService, isConnected: Boolean) {
    val pendingCount by syncService.pendingCount.collectAsState()
    val isSyncing by syncService.isSyncing.collectAsState()
    val syncStatus by syncService.syncStatus.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        syncService.updatePendingCount()
    }

    val shouldShow = !isConnected || pendingCount > 0 || expanded

    AnimatedVisibility(visible = shouldShow) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(statusBackgroundColor(isConnected, pendingCount))
                .clickable { expanded = !expanded }
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    imageVector = statusIcon(isConnected, pendingCount, isSyncing),
                    contentDescription = stringResource(R.string.offline_mode),
                    tint = statusColor(isConnected, pendingCount)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        statusTitle(isConnected, pendingCount, isSyncing),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        statusSubtitle(isConnected, pendingCount),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                // Sync Now button (shown when connected with pending actions)
                if (isConnected && pendingCount > 0 && !isSyncing) {
                    TextButton(
                        onClick = {
                            scope.launch { syncService.performFullSync() }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(stringResource(R.string.sync_now), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (pendingCount > 0) {
                        Text(
                            pluralStringResource(R.plurals.queued_actions, pendingCount, pendingCount),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    val statusMessage = when (val status = syncStatus) {
                        SyncService.SyncStatus.Completed -> stringResource(R.string.sync_completed)
                        SyncService.SyncStatus.NoConnection -> stringResource(R.string.sync_no_connection)
                        is SyncService.SyncStatus.Failed -> stringResource(R.string.sync_failed, status.reason)
                        else -> null
                    }
                    if (!statusMessage.isNullOrBlank()) {
                        Text(statusMessage, style = MaterialTheme.typography.labelSmall)
                    }
                    // Explanation when offline
                    if (!isConnected && pendingCount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.changes_sync_restored),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun statusIcon(isConnected: Boolean, pendingCount: Int, isSyncing: Boolean) = when {
    !isConnected -> Icons.Default.WifiOff
    isSyncing -> Icons.Default.Sync
    pendingCount > 0 -> Icons.Default.Inbox
    else -> Icons.Default.CheckCircle
}

@Composable
private fun statusTitle(isConnected: Boolean, pendingCount: Int, isSyncing: Boolean): String = when {
    !isConnected -> stringResource(R.string.offline_mode)
    isSyncing -> stringResource(R.string.syncing)
    pendingCount > 0 -> stringResource(R.string.pending_changes)
    else -> stringResource(R.string.online)
}

@Composable
private fun statusSubtitle(isConnected: Boolean, pendingCount: Int): String = when {
    !isConnected -> stringResource(R.string.offline_changes_sync)
    pendingCount > 0 -> stringResource(R.string.tap_view_queued_actions)
    else -> stringResource(R.string.connected)
}

@Composable
private fun statusColor(isConnected: Boolean, pendingCount: Int) = when {
    !isConnected -> MaterialTheme.colorScheme.error
    pendingCount > 0 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.primary
}

@Composable
private fun statusBackgroundColor(isConnected: Boolean, pendingCount: Int) = when {
    !isConnected -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
    pendingCount > 0 -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
}
