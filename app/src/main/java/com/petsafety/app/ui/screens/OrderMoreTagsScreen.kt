package com.petsafety.app.ui.screens

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.petsafety.app.R
import com.petsafety.app.data.network.model.AddressDetails
import com.petsafety.app.data.network.model.CreateTagOrderRequest
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.theme.TealAccent
import com.petsafety.app.ui.util.AdaptiveLayout
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import com.petsafety.app.ui.viewmodel.OrdersViewModel

@Composable
fun OrderMoreTagsScreen(
    appStateViewModel: AppStateViewModel,
    onBack: () -> Unit = {},
    onDone: () -> Unit
) {
    val viewModel: OrdersViewModel = hiltViewModel()
    val context = LocalContext.current
    val checkoutUrl by viewModel.checkoutUrl.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val orderCreateFailedMessage = stringResource(R.string.order_create_failed)
    val checkoutFailedMessage = stringResource(R.string.checkout_failed)

    // Launch Chrome Custom Tab when checkout URL is available
    LaunchedEffect(checkoutUrl) {
        checkoutUrl?.let { url ->
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.launchUrl(context, Uri.parse(url))
            viewModel.handleCheckoutCancelled() // Clear URL so it doesn't re-launch
            onDone()
        }
    }

    val petNames = remember { mutableStateListOf("") }
    val ownerName = remember { mutableStateOf("") }
    val email = remember { mutableStateOf("") }
    val phone = remember { mutableStateOf("") }
    val street1 = remember { mutableStateOf("") }
    val street2 = remember { mutableStateOf("") }
    val city = remember { mutableStateOf("") }
    val province = remember { mutableStateOf("") }
    val postCode = remember { mutableStateOf("") }
    val country = remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = AdaptiveLayout.MaxContentWidth)
                .fillMaxSize()
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                    )
                    .padding(24.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(TealAccent.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = TealAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.order_more_tags_title),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = stringResource(R.string.get_qr_tags_subtitle),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 100.dp)
            ) {
                // Pet Names Section
                SectionCard(
                    title = stringResource(R.string.pet_names),
                    icon = Icons.Default.Pets
                ) {
                    petNames.forEachIndexed { index, value ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StyledTextField(
                                value = value,
                                onValueChange = { petNames[index] = it },
                                label = "${stringResource(R.string.pet_name)} ${index + 1}",
                                modifier = Modifier.weight(1f)
                            )
                            if (petNames.size > 1) {
                                IconButton(
                                    onClick = { petNames.removeAt(index) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.remove),
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                        if (index < petNames.lastIndex) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = { petNames.add("") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = TealAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.add_another_pet),
                            color = TealAccent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Contact Details Section
                SectionCard(
                    title = stringResource(R.string.contact_details),
                    icon = Icons.Default.Person
                ) {
                    StyledTextField(
                        value = ownerName.value,
                        onValueChange = { ownerName.value = it },
                        label = stringResource(R.string.full_name),
                        leadingIcon = Icons.Default.Person
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    StyledTextField(
                        value = email.value,
                        onValueChange = { email.value = it },
                        label = stringResource(R.string.email),
                        leadingIcon = Icons.Default.Email
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    StyledTextField(
                        value = phone.value,
                        onValueChange = { phone.value = it },
                        label = stringResource(R.string.phone),
                        leadingIcon = Icons.Default.Phone
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Shipping Address Section
                SectionCard(
                    title = stringResource(R.string.shipping_address),
                    icon = Icons.Default.Home
                ) {
                    StyledTextField(
                        value = street1.value,
                        onValueChange = { street1.value = it },
                        label = stringResource(R.string.street)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    StyledTextField(
                        value = street2.value,
                        onValueChange = { street2.value = it },
                        label = stringResource(R.string.street2)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StyledTextField(
                            value = city.value,
                            onValueChange = { city.value = it },
                            label = stringResource(R.string.city),
                            modifier = Modifier.weight(1f)
                        )
                        StyledTextField(
                            value = postCode.value,
                            onValueChange = { postCode.value = it },
                            label = stringResource(R.string.post_code),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StyledTextField(
                            value = province.value,
                            onValueChange = { province.value = it },
                            label = stringResource(R.string.province),
                            modifier = Modifier.weight(1f)
                        )
                        StyledTextField(
                            value = country.value,
                            onValueChange = { country.value = it },
                            label = stringResource(R.string.country),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pricing Info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = TealAccent.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.shipping),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.shipping_price),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Bottom Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(20.dp)
            ) {
                Button(
                    onClick = {
                        val validPetNames = petNames.filter { it.isNotBlank() }
                        if (validPetNames.isEmpty()) return@Button

                        val request = CreateTagOrderRequest(
                            petNames = validPetNames,
                            ownerName = ownerName.value,
                            email = email.value,
                            shippingAddress = AddressDetails(
                                street1 = street1.value,
                                street2 = street2.value.ifBlank { null },
                                city = city.value,
                                province = province.value.ifBlank { null },
                                postCode = postCode.value,
                                country = country.value,
                                phone = phone.value
                            ),
                            paymentMethod = "stripe",
                            shippingCost = 3.90
                        )
                        // Create order first, then redirect to Stripe Checkout
                        viewModel.createOrder(request) { response, message ->
                            if (response != null) {
                                viewModel.createTagCheckout(
                                    quantity = validPetNames.size,
                                    countryCode = country.value.takeIf { it.isNotBlank() }
                                ) { errorMsg ->
                                    appStateViewModel.showError(errorMsg ?: checkoutFailedMessage)
                                }
                            } else {
                                appStateViewModel.showError(message ?: orderCreateFailedMessage)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = BrandOrange.copy(alpha = 0.3f),
                            spotColor = BrandOrange.copy(alpha = 0.3f)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.proceed_to_payment),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = TealAccent
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}

@Composable
private fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        leadingIcon = leadingIcon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedBorderColor = TealAccent
        )
    )
}
