package com.petsafety.app.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import com.petsafety.app.data.model.SubscriptionStatus
import com.petsafety.app.ui.components.BrandButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.petsafety.app.R
import com.petsafety.app.data.model.Invoice
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.theme.PeachBackground
import com.petsafety.app.ui.viewmodel.SubscriptionViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

@Composable
fun BillingScreen(
    onBack: () -> Unit,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val invoices by viewModel.invoices.collectAsState()
    val subscription by viewModel.subscription.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current
    var showCancelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadSubscription()
        viewModel.loadInvoices()
    }

    // Refresh subscription on app foreground resume (cross-platform sync)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshIfStale()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PeachBackground)
                .padding(vertical = 16.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(
                text = stringResource(R.string.billing_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.align(Alignment.Center)
            )
            IconButton(
                onClick = {
                    viewModel.loadSubscription()
                    viewModel.loadInvoices()
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Manage Subscription Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isProcessing) {
                            viewModel.openPortal { url ->
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CreditCard, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.billing_manage_subscription),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = stringResource(R.string.billing_manage_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BrandOrange, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Current Plan
            subscription?.let { sub ->
                Text(
                    text = stringResource(R.string.billing_current_plan),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = stringResource(R.string.billing_plan_label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = sub.resolvedPlanName.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = stringResource(R.string.billing_status_label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = sub.displayStatus,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (sub.isActive) Color(0xFF4CAF50) else BrandOrange
                            )
                        }
                        // Trial end date
                        if (sub.isTrialing && sub.trialEndsAt != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = stringResource(R.string.trial_ends_on), style = MaterialTheme.typography.bodySmall, color = BrandOrange)
                                Text(text = formatIsoDate(sub.trialEndsAt), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = BrandOrange)
                            }
                        }
                        // Renewal date (for active paid plans)
                        if (sub.isActive && !sub.isTrialing && sub.currentPeriodEnd != null && sub.cancelAtPeriodEnd != true) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = stringResource(R.string.plan_renews), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = formatIsoDate(sub.currentPeriodEnd), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            }
                        }
                        // Cancels on date (when user has cancelled but period hasn't ended)
                        if (sub.cancelAtPeriodEnd == true && sub.currentPeriodEnd != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = stringResource(R.string.plan_cancels_on), style = MaterialTheme.typography.bodySmall, color = BrandOrange)
                                Text(text = formatIsoDate(sub.currentPeriodEnd), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = BrandOrange)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                // Past Due Warning
                if (sub.status == SubscriptionStatus.PAST_DUE) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = BrandOrange)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.billing_past_due_title),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = BrandOrange
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.billing_past_due_message),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            BrandButton(
                                text = stringResource(R.string.billing_update_payment),
                                onClick = {
                                    viewModel.openPortal { url ->
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                },
                                enabled = !isProcessing
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Cancel Subscription button — only for active paid plans
                if (sub.isActive && sub.isPaid) {
                    TextButton(
                        onClick = { showCancelDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing
                    ) {
                        Text(
                            text = stringResource(R.string.cancel_confirm),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Invoices
            Text(
                text = stringResource(R.string.billing_invoices),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BrandOrange)
                }
            } else if (invoices.isEmpty()) {
                Text(
                    text = stringResource(R.string.billing_no_invoices),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        invoices.forEachIndexed { index, invoice ->
                            InvoiceRow(invoice = invoice, onClick = {
                                invoice.pdfUrl?.let { url ->
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            })
                            if (index < invoices.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                            }
                        }
                    }
                }
            }

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Cancel Subscription Dialog
    if (showCancelDialog) {
        val accessUntil = subscription?.currentPeriodEnd?.let { formatIsoDate(it) } ?: ""
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text(stringResource(R.string.cancel_subscription_title)) },
            text = {
                Column {
                    Text(
                        text = "\u2022 ${stringResource(R.string.cancel_warning_a)}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "\u2022 ${stringResource(R.string.cancel_warning_b)}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "\u2022 ${stringResource(R.string.cancel_warning_c)}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "\u2022 ${stringResource(R.string.cancel_warning_d)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (accessUntil.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.cancel_access_until, accessUntil),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cancelSuccessMsg = context.getString(R.string.cancel_success, accessUntil)
                        viewModel.cancelSubscription { _ ->
                            Toast.makeText(context, cancelSuccessMsg, Toast.LENGTH_LONG).show()
                        }
                        showCancelDialog = false
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.cancel_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text(stringResource(R.string.keep_subscription))
                }
            }
        )
    }
}

@Composable
private fun InvoiceRow(invoice: Invoice, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = invoice.pdfUrl != null, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = invoice.number ?: stringResource(R.string.billing_invoice),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
            Text(
                text = formatDate(invoice.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatAmount(invoice.amount, invoice.currency),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = invoice.status?.replaceFirstChar { it.uppercase() } ?: stringResource(R.string.billing_unknown),
                style = MaterialTheme.typography.bodySmall,
                color = if (invoice.status == "paid") Color(0xFF4CAF50) else BrandOrange
            )
        }
        if (invoice.pdfUrl != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Description, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(20.dp))
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp * 1000)
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return formatter.format(date)
}

private fun formatAmount(amount: Int, currency: String): String {
    val value = amount / 100.0
    return try {
        val formatter = NumberFormat.getCurrencyInstance()
        formatter.currency = Currency.getInstance(currency.uppercase())
        formatter.format(value)
    } catch (_: Exception) {
        "$currency ${String.format("%.2f", value)}"
    }
}

private fun formatIsoDate(isoDate: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val date = parser.parse(isoDate.substringBefore('.').substringBefore('Z'))
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        if (date != null) formatter.format(date) else isoDate.substringBefore('T')
    } catch (_: Exception) {
        isoDate.substringBefore('T')
    }
}
