package com.petsafety.app.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.petsafety.app.R
import com.petsafety.app.data.model.Pet
import com.petsafety.app.data.network.model.AddressDetails
import com.petsafety.app.data.network.model.CreateReplacementOrderRequest
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.theme.TealAccent
import com.petsafety.app.ui.util.AdaptiveLayout
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import com.petsafety.app.ui.viewmodel.OrdersViewModel

@Composable
fun OrderReplacementTagScreen(
    pet: Pet,
    appStateViewModel: AppStateViewModel,
    onDone: () -> Unit
) {
    val viewModel: OrdersViewModel = hiltViewModel()

    val replacementOrderedMessage = stringResource(R.string.replacement_ordered)
    val replacementFailedMessage = stringResource(R.string.replacement_failed)

    val street1 = remember { mutableStateOf("") }
    val city = remember { mutableStateOf("") }
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
            // Header with Pet Info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                    )
                    .padding(24.dp)
            ) {
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
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = TealAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.order_replacement_title),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = stringResource(R.string.replacement_subtitle),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
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
                // Pet Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Pet Photo
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(TealAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!pet.profileImage.isNullOrBlank()) {
                                AsyncImage(
                                    model = pet.profileImage,
                                    contentDescription = pet.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Pets,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = TealAccent
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pet.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${pet.species}${pet.breed?.let { " • $it" } ?: ""}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Shipping Address Section
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
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = TealAccent
                            )
                            Text(
                                text = stringResource(R.string.shipping_address),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        StyledTextField(
                            value = street1.value,
                            onValueChange = { street1.value = it },
                            label = stringResource(R.string.street)
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

                        StyledTextField(
                            value = country.value,
                            onValueChange = { country.value = it },
                            label = stringResource(R.string.country)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = TealAccent.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.free_replacement),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TealAccent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.replacement_shipping_info),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        val request = CreateReplacementOrderRequest(
                            shippingAddress = AddressDetails(
                                street1 = street1.value,
                                city = city.value,
                                postCode = postCode.value,
                                country = country.value
                            )
                        )
                        viewModel.createReplacementOrder(pet.id, request) { success, message ->
                            if (success) {
                                appStateViewModel.showSuccess(replacementOrderedMessage)
                                onDone()
                            } else {
                                appStateViewModel.showError(message ?: replacementFailedMessage)
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
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                ) {
                    Text(
                        text = stringResource(R.string.order_replacement_button),
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
