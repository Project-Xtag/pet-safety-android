package com.petsafety.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.petsafety.app.R
import com.petsafety.app.data.model.Order
import com.petsafety.app.ui.components.ErrorRetryState
import com.petsafety.app.ui.components.OrdersListSkeleton
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.theme.InfoBlue
import com.petsafety.app.ui.theme.SuccessGreen
import com.petsafety.app.ui.theme.TealAccent
import com.petsafety.app.ui.viewmodel.OrdersViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(onBack: () -> Unit) {
    val viewModel: OrdersViewModel = hiltViewModel()
    val orders by viewModel.orders.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    var selectedOrder by remember { mutableStateOf<Order?>(null) }

    LaunchedEffect(Unit) { viewModel.fetchOrders() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            isLoading && orders.isEmpty() -> {
                Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
                    OrdersListSkeleton(itemCount = 3)
                }
            }
            error != null && orders.isEmpty() -> {
                ErrorRetryState(
                    message = error ?: stringResource(R.string.failed_load_orders),
                    onRetry = { viewModel.refresh() }
                )
            }
            orders.isEmpty() -> {
                EmptyOrdersState()
            }
            selectedOrder != null -> {
                // Capture in local val for smart cast
                val order = selectedOrder
                if (order != null) {
                    OrderDetailScreen(
                        order = order,
                        onBack = { selectedOrder = null }
                    )
                }
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(orders) { order ->
                            OrderCard(
                                order = order,
                                onClick = { selectedOrder = order }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyOrdersState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = TealAccent
                )
            }

            Text(
                text = stringResource(R.string.no_orders),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = stringResource(R.string.no_orders_message),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
private fun OrderCard(
    order: Order,
    onClick: () -> Unit
) {
    val statusColor = when (order.orderStatus.lowercase()) {
        "completed" -> SuccessGreen
        "pending" -> BrandOrange
        "failed" -> MaterialTheme.colorScheme.error
        "processing" -> InfoBlue
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.order_id_short, order.id.take(8)),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatCurrency(order.totalAmount),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = order.orderStatus.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusColor
                    )
                }

                Text(
                    text = formatDate(order.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Items Count
            order.items?.let { items ->
                if (items.isNotEmpty()) {
                    val totalItems = items.sumOf { it.quantity }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = pluralStringResource(R.plurals.item_count, totalItems, totalItems),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderDetailScreen(
    order: Order,
    onBack: () -> Unit
) {
    val statusColor = when (order.orderStatus.lowercase()) {
        "completed" -> SuccessGreen
        "pending" -> BrandOrange
        "failed" -> MaterialTheme.colorScheme.error
        "processing" -> InfoBlue
        else -> MaterialTheme.colorScheme.outline
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Order Information Section
        SectionHeader(title = stringResource(R.string.order_information))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                DetailRow(label = stringResource(R.string.order_id_label), value = "#${order.id.take(8)}")
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.status),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(statusColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = order.orderStatus.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            color = statusColor
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 12.dp))
                DetailRow(label = stringResource(R.string.total_amount), value = formatCurrency(order.totalAmount), isBold = true)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 12.dp))
                DetailRow(label = stringResource(R.string.order_date), value = formatDateLong(order.createdAt))
            }
        }

        // Items Section
        order.items?.let { items ->
            if (items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(title = stringResource(R.string.items))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        items.forEachIndexed { index, item ->
                            Column {
                                Text(
                                    text = item.itemType.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.quantity_label, item.quantity),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (item.qrTagId == null) stringResource(R.string.qr_tag_pending) else stringResource(R.string.qr_tag_assigned),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (item.qrTagId == null) BrandOrange else InfoBlue
                                    )
                                    Text(
                                        text = formatCurrency(item.price),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            if (index < items.size - 1) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.UK)
    formatter.currency = Currency.getInstance("GBP")
    return formatter.format(amount)
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

private fun formatDateLong(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(dateString.take(19))
        if (date != null) {
            val outputFormat = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
            outputFormat.format(date)
        } else {
            dateString.take(10)
        }
    } catch (e: Exception) {
        dateString.take(10)
    }
}
