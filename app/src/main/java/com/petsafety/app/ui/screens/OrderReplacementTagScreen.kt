package com.petsafety.app.ui.screens

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.petsafety.app.data.network.model.PostaPointDetails
import com.petsafety.app.ui.components.PostaPointPicker
import com.petsafety.app.util.SupportedCountries
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.theme.TealAccent
import com.petsafety.app.ui.util.PetLocalizer
import androidx.compose.ui.platform.LocalContext
import com.petsafety.app.ui.util.AdaptiveLayout
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import com.petsafety.app.ui.viewmodel.OrdersViewModel

@Composable
fun OrderReplacementTagScreen(
    pet: Pet,
    appStateViewModel: AppStateViewModel,
    authViewModel: com.petsafety.app.ui.viewmodel.AuthViewModel? = null,
    onBack: () -> Unit = {},
    onDone: () -> Unit
) {
    val viewModel: OrdersViewModel = hiltViewModel()
    val context = androidx.compose.ui.platform.LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val deliveryPoints by viewModel.deliveryPoints.collectAsState()
    val isSearchingPoints by viewModel.isSearchingPoints.collectAsState()
    val eligibility by viewModel.replacementEligibility.collectAsState()
    val isCheckingEligibility by viewModel.isCheckingEligibility.collectAsState()
    val shippingPrices by viewModel.shippingPrices.collectAsState()

    val replacementOrderedMessage = stringResource(R.string.replacement_ordered)
    val replacementFailedMessage = stringResource(R.string.replacement_failed)
    val searchFailedMessage = stringResource(R.string.postapoint_search_failed)

    // Check eligibility and fetch shipping prices on screen load
    LaunchedEffect(Unit) {
        viewModel.checkReplacementEligibility()
        viewModel.fetchShippingPrices()
    }

    val street1 = remember { mutableStateOf("") }
    val street2 = remember { mutableStateOf("") }
    val city = remember { mutableStateOf("") }
    val postCode = remember { mutableStateOf("") }
    val selectedCountryCode = remember {
        val detected = java.util.Locale.getDefault().country ?: ""
        mutableStateOf(if (SupportedCountries.findByCode(detected) != null) detected else "")
    }
    val countryDropdownExpanded = remember { mutableStateOf(false) }
    val deliveryMethod = remember { mutableStateOf("home_delivery") }
    val selectedPostaPoint = remember { mutableStateOf<PostaPointDetails?>(null) }
    val hasSearchedPoints = remember { mutableStateOf(false) }

    // Pre-fill from user profile + device locale country detection
    val currentUser = authViewModel?.let {
        val user by it.currentUser.collectAsState()
        user
    }
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            if (street1.value.isBlank()) user.address?.let { street1.value = it }
            if (city.value.isBlank()) user.city?.let { city.value = it }
            if (postCode.value.isBlank()) user.postalCode?.let { postCode.value = it }
            if (selectedCountryCode.value.isBlank()) {
                val rawCountry = user.country ?: java.util.Locale.getDefault().country ?: ""
                selectedCountryCode.value = SupportedCountries.find(rawCountry)?.code ?: ""
            }
        }
        if (currentUser == null && selectedCountryCode.value.isBlank()) {
            val detected = java.util.Locale.getDefault().country ?: ""
            if (SupportedCountries.findByCode(detected) != null) {
                selectedCountryCode.value = detected
            }
        }
    }

    val isHungary = selectedCountryCode.value.equals("HU", ignoreCase = true)

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
                                text = "${PetLocalizer.localizeSpecies(LocalContext.current, pet.species)}${pet.breed?.let { " • ${PetLocalizer.localizeBreed(LocalContext.current, it, pet.species)}" } ?: ""}",
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

                        CountryDropdown(
                            selectedCode = selectedCountryCode.value,
                            expanded = countryDropdownExpanded.value,
                            onExpandedChange = { countryDropdownExpanded.value = it },
                            onSelect = { code ->
                                selectedCountryCode.value = code
                                countryDropdownExpanded.value = false
                            }
                        )
                    }
                }

                // Delivery Method Section (Hungary only)
                if (isHungary) {
                    Spacer(modifier = Modifier.height(20.dp))

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
                                    imageVector = Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = TealAccent
                                )
                                Text(
                                    text = stringResource(R.string.delivery_method_title),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val huPrices = shippingPrices?.HU
                            val homeDeliveryPriceText = huPrices?.homeDelivery?.let { info ->
                                if (info.currency == "HUF") "${info.amount.toInt()} Ft" else "€${"%.2f".format(info.amount)}"
                            } ?: "..."
                            val postapointPriceText = huPrices?.postapoint?.let { info ->
                                if (info.currency == "HUF") "${info.amount.toInt()} Ft" else "€${"%.2f".format(info.amount)}"
                            } ?: "..."

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { deliveryMethod.value = "home_delivery" },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = deliveryMethod.value == "home_delivery",
                                    onClick = { deliveryMethod.value = "home_delivery" }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.home_delivery_option),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = homeDeliveryPriceText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { deliveryMethod.value = "postapoint" },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = deliveryMethod.value == "postapoint",
                                    onClick = { deliveryMethod.value = "postapoint" }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.postapoint_delivery_option),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = postapointPriceText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (deliveryMethod.value == "postapoint") {
                                Spacer(modifier = Modifier.height(12.dp))
                                PostaPointPicker(
                                    deliveryPoints = deliveryPoints,
                                    selectedPoint = selectedPostaPoint.value,
                                    isSearching = isSearchingPoints,
                                    hasSearched = hasSearchedPoints.value,
                                    onSearch = { zip ->
                                        hasSearchedPoints.value = true
                                        viewModel.getDeliveryPoints(zip) { errorMsg ->
                                            appStateViewModel.showError(errorMsg ?: searchFailedMessage)
                                        }
                                    },
                                    onSelect = { selectedPostaPoint.value = it }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Info Card - dynamic based on eligibility
                if (isCheckingEligibility) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                } else {
                    val isFree = eligibility?.isFreeReplacement == true
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isFree) TealAccent.copy(alpha = 0.1f) else BrandOrange.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            if (isFree) {
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
                            } else {
                                val cost = eligibility?.shippingCost ?: 0.0
                                val currency = eligibility?.currency ?: "EUR"
                                val formattedCost = if (currency == "HUF") {
                                    "${cost.toInt()} Ft"
                                } else {
                                    "€${"%.2f".format(cost)}"
                                }
                                Text(
                                    text = stringResource(R.string.replacement_shipping_required),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = BrandOrange
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.replacement_shipping_cost, formattedCost),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
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
                                street2 = street2.value.ifBlank { null },
                                city = city.value,
                                province = null,
                                postCode = postCode.value,
                                country = selectedCountryCode.value
                            ),
                            deliveryMethod = if (isHungary) deliveryMethod.value else null,
                            postapointDetails = if (isHungary && deliveryMethod.value == "postapoint") selectedPostaPoint.value else null
                        )
                        viewModel.createReplacementOrder(pet.id, request) { response, errorMsg ->
                            if (response != null) {
                                val checkoutUrl = response.checkoutUrl
                                if (!checkoutUrl.isNullOrBlank() && checkoutUrl.startsWith("https://checkout.stripe.com/")) {
                                    // Redirect to Stripe Checkout for shipping payment
                                    val customTabsIntent = CustomTabsIntent.Builder().build()
                                    customTabsIntent.launchUrl(context, Uri.parse(checkoutUrl))
                                } else {
                                    appStateViewModel.showSuccess(replacementOrderedMessage)
                                    onDone()
                                }
                            } else {
                                appStateViewModel.showError(errorMsg ?: replacementFailedMessage)
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
                    enabled = !isLoading &&
                        street1.value.isNotBlank() &&
                        city.value.isNotBlank() &&
                        postCode.value.isNotBlank() &&
                        selectedCountryCode.value.isNotBlank() &&
                        (!isHungary || deliveryMethod.value != "postapoint" || selectedPostaPoint.value != null)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CountryDropdown(
    selectedCode: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedName = SupportedCountries.findByCode(selectedCode)?.localizedName() ?: ""
    val deviceCountry = java.util.Locale.getDefault().country
    val sortedCountries = remember(deviceCountry) { SupportedCountries.sorted(priorityCode = deviceCountry) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.country)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedBorderColor = TealAccent
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            sortedCountries.forEach { country ->
                DropdownMenuItem(
                    text = { Text(country.localizedName()) },
                    onClick = { onSelect(country.code) }
                )
            }
        }
    }
}
