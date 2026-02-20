package com.petsafety.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import com.petsafety.app.util.WebUrlHelper
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.petsafety.app.R
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.theme.PeachBackground
import com.petsafety.app.ui.theme.TealAccent
import com.petsafety.app.ui.util.AdaptiveLayout
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import com.petsafety.app.ui.viewmodel.AuthViewModel
import com.petsafety.app.ui.viewmodel.NotificationPreferencesViewModel

private enum class ProfileSection {
    MAIN,
    PERSONAL,
    ADDRESS,
    CONTACTS,
    PRIVACY,
    NOTIFICATIONS,
    HELP,
    ORDERS,
    BILLING,
    REFERRAL,
    PRICING
}

@Composable
fun ProfileScreen(
    appStateViewModel: AppStateViewModel,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    var section by remember { mutableStateOf(ProfileSection.MAIN) }
    val prefsViewModel: NotificationPreferencesViewModel = hiltViewModel()

    when (section) {
        ProfileSection.MAIN -> ProfileMain(
            authViewModel = authViewModel,
            appStateViewModel = appStateViewModel,
            onNavigate = { section = it },
            onLogout = { authViewModel.logout() },
            modifier = modifier
        )
        ProfileSection.PERSONAL -> PersonalInfoScreen(authViewModel, appStateViewModel) { section = ProfileSection.MAIN }
        ProfileSection.ADDRESS -> AddressScreen(authViewModel, appStateViewModel) { section = ProfileSection.MAIN }
        ProfileSection.CONTACTS -> ContactsScreen(authViewModel, appStateViewModel) { section = ProfileSection.MAIN }
        ProfileSection.PRIVACY -> PrivacyModeScreen(authViewModel, appStateViewModel) { section = ProfileSection.MAIN }
        ProfileSection.NOTIFICATIONS -> NotificationPreferencesScreen(prefsViewModel, appStateViewModel) { section = ProfileSection.MAIN }
        ProfileSection.HELP -> HelpSupportScreen(authViewModel, appStateViewModel) { section = ProfileSection.MAIN }
        ProfileSection.ORDERS -> OrdersScreen { section = ProfileSection.MAIN }
        ProfileSection.BILLING -> BillingScreen(onBack = { section = ProfileSection.MAIN })
        ProfileSection.REFERRAL -> ReferralScreen(onBack = { section = ProfileSection.MAIN })
        ProfileSection.PRICING -> PricingScreen(onBack = { section = ProfileSection.MAIN })
    }
}

@Composable
private fun ProfileMain(
    authViewModel: AuthViewModel,
    appStateViewModel: AppStateViewModel,
    onNavigate: (ProfileSection) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val user by authViewModel.currentUser.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = AdaptiveLayout.MaxContentWidth)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header Section with Peach Background
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PeachBackground)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(60.dp))

                // Title
                Text(
                    text = stringResource(R.string.account_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Avatar
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .shadow(10.dp, CircleShape)
                        .clip(CircleShape)
                        .background(TealAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // User Info
                user?.let { currentUser ->
                    if (currentUser.fullName.isNotEmpty()) {
                        Text(
                            text = currentUser.fullName,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = currentUser.email,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Menu Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    ProfileMenuRow(
                        icon = Icons.Default.Person,
                        title = stringResource(R.string.personal_information),
                        onClick = { onNavigate(ProfileSection.PERSONAL) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ProfileMenuRow(
                        icon = Icons.Default.Home,
                        title = stringResource(R.string.address),
                        onClick = { onNavigate(ProfileSection.ADDRESS) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ProfileMenuRow(
                        icon = Icons.Default.People,
                        title = stringResource(R.string.contacts),
                        onClick = { onNavigate(ProfileSection.CONTACTS) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ProfileMenuRow(
                        icon = Icons.Default.Lock,
                        title = stringResource(R.string.privacy_mode),
                        onClick = { onNavigate(ProfileSection.PRIVACY) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ProfileMenuRow(
                        icon = Icons.Default.Notifications,
                        title = stringResource(R.string.notifications),
                        onClick = { onNavigate(ProfileSection.NOTIFICATIONS) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ProfileMenuRow(
                        icon = Icons.Default.HelpOutline,
                        title = stringResource(R.string.help_support),
                        onClick = { onNavigate(ProfileSection.HELP) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ProfileMenuRow(
                        icon = Icons.Default.WorkspacePremium,
                        title = stringResource(R.string.subscription_title),
                        onClick = { onNavigate(ProfileSection.PRICING) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ProfileMenuRow(
                        icon = Icons.Default.CreditCard,
                        title = stringResource(R.string.billing_title),
                        onClick = { onNavigate(ProfileSection.BILLING) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ProfileMenuRow(
                        icon = Icons.Default.CardGiftcard,
                        title = stringResource(R.string.referral_title),
                        onClick = { onNavigate(ProfileSection.REFERRAL) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Logout Button
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(52.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = BrandOrange.copy(alpha = 0.3f),
                        spotColor = BrandOrange.copy(alpha = 0.3f)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.log_out),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }

    // Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.log_out)) },
            text = { Text(stringResource(R.string.logout_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.log_out))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = Color(0xFFC7C7CC)
        )
    }
}

@Composable
private fun PersonalInfoScreen(
    authViewModel: AuthViewModel,
    appStateViewModel: AppStateViewModel,
    onBack: () -> Unit
) {
    val user by authViewModel.currentUser.collectAsState()
    var isEditing by remember { mutableStateOf(false) }
    var firstName by remember { mutableStateOf(user?.firstName ?: "") }
    var lastName by remember { mutableStateOf(user?.lastName ?: "") }
    var phone by remember { mutableStateOf(user?.phone ?: "") }
    var isSaving by remember { mutableStateOf(false) }

    val updatedMessage = stringResource(R.string.updated)
    val updateFailedMessage = stringResource(R.string.update_failed)

    val firstNameLabel = stringResource(R.string.first_name)
    val lastNameLabel = stringResource(R.string.last_name)
    val emailLabel = stringResource(R.string.email)
    val phoneLabel = stringResource(R.string.phone)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SubScreenHeader(
            title = stringResource(R.string.personal_information),
            onBack = {
                if (isEditing) {
                    // Cancel edit: reset fields and exit edit mode
                    firstName = user?.firstName ?: ""
                    lastName = user?.lastName ?: ""
                    phone = user?.phone ?: ""
                    isEditing = false
                } else {
                    onBack()
                }
            },
            trailingContent = if (!isEditing) {
                {
                    TextButton(onClick = {
                        // Reset fields to current values when entering edit mode
                        firstName = user?.firstName ?: ""
                        lastName = user?.lastName ?: ""
                        phone = user?.phone ?: ""
                        isEditing = true
                    }) {
                        Text(
                            text = stringResource(R.string.edit),
                            color = BrandOrange,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else null
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 100.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (isEditing) {
                        StyledOutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = firstNameLabel
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        StyledOutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            label = lastNameLabel
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        StyledOutlinedTextField(
                            value = user?.email ?: "",
                            onValueChange = {},
                            label = emailLabel,
                            enabled = false
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        StyledOutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = phoneLabel
                        )
                    } else {
                        ReadOnlyField(label = firstNameLabel, value = user?.firstName ?: "")
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ReadOnlyField(label = lastNameLabel, value = user?.lastName ?: "")
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ReadOnlyField(label = emailLabel, value = user?.email ?: "")
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ReadOnlyField(label = phoneLabel, value = user?.phone ?: "")
                    }
                }
            }

            if (isEditing) {
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        isSaving = true
                        authViewModel.updateProfile(
                            updates = mapOf("first_name" to firstName, "last_name" to lastName, "phone" to phone)
                        ) { success, message ->
                            isSaving = false
                            if (success) {
                                appStateViewModel.showSuccess(updatedMessage)
                                isEditing = false
                            } else {
                                appStateViewModel.showError(message ?: updateFailedMessage)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = BrandOrange.copy(alpha = 0.3f),
                            spotColor = BrandOrange.copy(alpha = 0.3f)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.save),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = {
                        firstName = user?.firstName ?: ""
                        lastName = user?.lastName ?: ""
                        phone = user?.phone ?: ""
                        isEditing = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AddressScreen(
    authViewModel: AuthViewModel,
    appStateViewModel: AppStateViewModel,
    onBack: () -> Unit
) {
    val user by authViewModel.currentUser.collectAsState()
    var isEditing by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf(user?.address ?: "") }
    var addressLine2 by remember { mutableStateOf("") }
    var city by remember { mutableStateOf(user?.city ?: "") }
    var postalCode by remember { mutableStateOf(user?.postalCode ?: "") }
    var country by remember { mutableStateOf(user?.country ?: "") }
    var isSaving by remember { mutableStateOf(false) }

    val addressUpdatedMessage = stringResource(R.string.address_updated)
    val addressUpdateFailedMessage = stringResource(R.string.update_failed)

    val streetLabel = stringResource(R.string.street_address)
    val line2Label = stringResource(R.string.address_line_2_optional)
    val cityLabel = stringResource(R.string.city)
    val postCodeLabel = stringResource(R.string.post_code)
    val countryLabel = stringResource(R.string.country)

    fun resetFields() {
        address = user?.address ?: ""
        addressLine2 = ""
        city = user?.city ?: ""
        postalCode = user?.postalCode ?: ""
        country = user?.country ?: ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SubScreenHeader(
            title = stringResource(R.string.address),
            onBack = {
                if (isEditing) {
                    resetFields()
                    isEditing = false
                } else {
                    onBack()
                }
            },
            trailingContent = if (!isEditing) {
                {
                    TextButton(onClick = {
                        resetFields()
                        isEditing = true
                    }) {
                        Text(
                            text = stringResource(R.string.edit),
                            color = BrandOrange,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else null
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 100.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (isEditing) {
                        StyledOutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = streetLabel
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        StyledOutlinedTextField(
                            value = addressLine2,
                            onValueChange = { addressLine2 = it },
                            label = line2Label
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        StyledOutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = cityLabel
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        StyledOutlinedTextField(
                            value = postalCode,
                            onValueChange = { postalCode = it },
                            label = postCodeLabel
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        StyledOutlinedTextField(
                            value = country,
                            onValueChange = { country = it },
                            label = countryLabel
                        )
                    } else {
                        ReadOnlyField(label = streetLabel, value = user?.address ?: "")
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ReadOnlyField(label = cityLabel, value = user?.city ?: "")
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ReadOnlyField(label = postCodeLabel, value = user?.postalCode ?: "")
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ReadOnlyField(label = countryLabel, value = user?.country ?: "")
                    }
                }
            }

            if (isEditing) {
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        isSaving = true
                        authViewModel.updateProfile(
                            updates = mapOf(
                                "address" to address,
                                "city" to city,
                                "postal_code" to postalCode,
                                "country" to country
                            )
                        ) { success, message ->
                            isSaving = false
                            if (success) {
                                appStateViewModel.showSuccess(addressUpdatedMessage)
                                isEditing = false
                            } else {
                                appStateViewModel.showError(message ?: addressUpdateFailedMessage)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = BrandOrange.copy(alpha = 0.3f),
                            spotColor = BrandOrange.copy(alpha = 0.3f)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.save),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = {
                        resetFields()
                        isEditing = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactsScreen(
    authViewModel: AuthViewModel,
    appStateViewModel: AppStateViewModel,
    onBack: () -> Unit
) {
    val user by authViewModel.currentUser.collectAsState()
    var isEditing by remember { mutableStateOf(false) }
    var secondaryEmail by remember { mutableStateOf(user?.secondaryEmail ?: "") }
    var primaryPhone by remember { mutableStateOf(user?.phone ?: "") }
    var secondaryPhone by remember { mutableStateOf(user?.secondaryPhone ?: "") }
    var isSaving by remember { mutableStateOf(false) }

    val contactsUpdatedMessage = stringResource(R.string.updated)
    val contactsUpdateFailedMessage = stringResource(R.string.update_failed)

    val primaryEmailLabel = stringResource(R.string.primary_email)
    val secondaryEmailLabel = stringResource(R.string.secondary_email_optional)
    val primaryPhoneLabel = stringResource(R.string.primary_phone)
    val secondaryPhoneLabel = stringResource(R.string.secondary_phone_optional)

    fun resetFields() {
        secondaryEmail = user?.secondaryEmail ?: ""
        primaryPhone = user?.phone ?: ""
        secondaryPhone = user?.secondaryPhone ?: ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SubScreenHeader(
            title = stringResource(R.string.contacts),
            onBack = {
                if (isEditing) {
                    resetFields()
                    isEditing = false
                } else {
                    onBack()
                }
            },
            trailingContent = if (!isEditing) {
                {
                    TextButton(onClick = {
                        resetFields()
                        isEditing = true
                    }) {
                        Text(
                            text = stringResource(R.string.edit),
                            color = BrandOrange,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else null
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 100.dp)
        ) {
            // Info Card (above contacts)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = TealAccent.copy(alpha = 0.1f))
            ) {
                Text(
                    text = stringResource(R.string.contacts_qr_tag_info),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Email Section
            Text(
                text = stringResource(R.string.email_addresses),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (isEditing) {
                        StyledOutlinedTextField(
                            value = user?.email ?: "",
                            onValueChange = {},
                            label = primaryEmailLabel,
                            enabled = false
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        StyledOutlinedTextField(
                            value = secondaryEmail,
                            onValueChange = { secondaryEmail = it },
                            label = secondaryEmailLabel
                        )
                    } else {
                        ReadOnlyField(label = primaryEmailLabel, value = user?.email ?: "")
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ReadOnlyField(label = secondaryEmailLabel, value = user?.secondaryEmail ?: "")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Phone Section
            Text(
                text = stringResource(R.string.phone_numbers),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (isEditing) {
                        StyledOutlinedTextField(
                            value = primaryPhone,
                            onValueChange = { primaryPhone = it },
                            label = primaryPhoneLabel
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                        StyledOutlinedTextField(
                            value = secondaryPhone,
                            onValueChange = { secondaryPhone = it },
                            label = secondaryPhoneLabel
                        )
                    } else {
                        ReadOnlyField(label = primaryPhoneLabel, value = user?.phone ?: "")
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ReadOnlyField(label = secondaryPhoneLabel, value = user?.secondaryPhone ?: "")
                    }
                }
            }

            if (isEditing) {
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        isSaving = true
                        authViewModel.updateProfile(
                            updates = mapOf(
                                "phone" to primaryPhone.trim(),
                                "secondary_phone" to secondaryPhone.trim(),
                                "secondary_email" to secondaryEmail.trim()
                            )
                        ) { success, message ->
                            isSaving = false
                            if (success) {
                                appStateViewModel.showSuccess(contactsUpdatedMessage)
                                isEditing = false
                            } else {
                                appStateViewModel.showError(message ?: contactsUpdateFailedMessage)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = BrandOrange.copy(alpha = 0.3f),
                            spotColor = BrandOrange.copy(alpha = 0.3f)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.save),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = {
                        resetFields()
                        isEditing = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacyModeScreen(
    authViewModel: AuthViewModel,
    appStateViewModel: AppStateViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()

    // Initialize state from user's current settings
    var showPhonePublicly by remember(currentUser?.showPhonePublicly) {
        mutableStateOf(currentUser?.showPhonePublicly ?: true)
    }
    var showEmailPublicly by remember(currentUser?.showEmailPublicly) {
        mutableStateOf(currentUser?.showEmailPublicly ?: true)
    }
    var showAddressPublicly by remember(currentUser?.showAddressPublicly) {
        mutableStateOf(currentUser?.showAddressPublicly ?: true)
    }

    val privacyUpdatedMessage = stringResource(R.string.privacy_settings_updated)
    val privacyFailedMessage = stringResource(R.string.privacy_settings_failed)

    // Update function that saves to backend
    fun updatePrivacySetting(field: String, value: Boolean) {
        authViewModel.updateProfile(
            mapOf(field to value)
        ) { success, error ->
            if (success) {
                appStateViewModel.showSuccess(privacyUpdatedMessage)
            } else {
                appStateViewModel.showError(error ?: privacyFailedMessage)
                // Revert the toggle on failure
                when (field) {
                    "show_phone_publicly" -> showPhonePublicly = !value
                    "show_email_publicly" -> showEmailPublicly = !value
                    "show_address_publicly" -> showAddressPublicly = !value
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SubScreenHeader(
            title = stringResource(R.string.privacy_mode),
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 100.dp)
        ) {
            // Contact Visibility Section
            Text(
                text = stringResource(R.string.contact_visibility),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.contact_visibility_desc),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsToggleRow(
                        title = stringResource(R.string.show_phone_number),
                        subtitle = stringResource(R.string.show_phone_subtitle),
                        checked = showPhonePublicly,
                        onCheckedChange = {
                            showPhonePublicly = it
                            updatePrivacySetting("show_phone_publicly", it)
                        },
                        enabled = !isLoading
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                    SettingsToggleRow(
                        title = stringResource(R.string.show_email_address),
                        subtitle = stringResource(R.string.show_email_subtitle),
                        checked = showEmailPublicly,
                        onCheckedChange = {
                            showEmailPublicly = it
                            updatePrivacySetting("show_email_publicly", it)
                        },
                        enabled = !isLoading
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                    SettingsToggleRow(
                        title = stringResource(R.string.show_address),
                        subtitle = stringResource(R.string.show_address_subtitle),
                        checked = showAddressPublicly,
                        onCheckedChange = {
                            showAddressPublicly = it
                            updatePrivacySetting("show_address_publicly", it)
                        },
                        enabled = !isLoading
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Data Privacy Links
            Text(
                text = stringResource(R.string.data_privacy),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    ProfileMenuRow(
                        icon = Icons.Default.Lock,
                        title = stringResource(R.string.privacy_policy),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(WebUrlHelper.privacyUrl))
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationPreferencesScreen(
    viewModel: NotificationPreferencesViewModel,
    appStateViewModel: AppStateViewModel,
    onBack: () -> Unit
) {
    val preferences by viewModel.preferences.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    var pushEnabled by remember { mutableStateOf(preferences.notifyByPush) }
    var emailEnabled by remember { mutableStateOf(preferences.notifyByEmail) }
    var smsEnabled by remember { mutableStateOf(preferences.notifyBySms) }
    var missingPetAlerts by remember { mutableStateOf(true) }
    var orderUpdates by remember { mutableStateOf(true) }
    var accountActivity by remember { mutableStateOf(true) }

    val atLeastOneChannelMessage = stringResource(R.string.at_least_one_channel_required)
    val savedMessage = stringResource(R.string.updated)
    val failedMessage = stringResource(R.string.update_failed)

    // Load preferences from backend on screen entry
    LaunchedEffect(Unit) {
        viewModel.loadPreferences()
    }

    // Sync local state when backend preferences arrive
    LaunchedEffect(preferences) {
        pushEnabled = preferences.notifyByPush
        emailEnabled = preferences.notifyByEmail
        smsEnabled = preferences.notifyBySms
    }

    // Helper to persist channel changes
    fun saveChannels(push: Boolean, email: Boolean, sms: Boolean) {
        viewModel.updatePreferences(
            com.petsafety.app.data.model.NotificationPreferences(
                notifyByPush = push,
                notifyByEmail = email,
                notifyBySms = sms
            )
        )
        viewModel.savePreferences()
    }

    // Show save result
    val showSuccess by viewModel.showSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            appStateViewModel.showSuccess(savedMessage)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            appStateViewModel.showError(it)
            viewModel.clearMessages()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SubScreenHeader(
            title = stringResource(R.string.notifications),
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 100.dp)
        ) {
            // Info card at top
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BrandOrange.copy(alpha = 0.1f))
            ) {
                Text(
                    text = stringResource(R.string.notification_channel_info),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Notification Channels
            Text(
                text = stringResource(R.string.notification_channels),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsToggleRow(
                        title = stringResource(R.string.push_notifications),
                        subtitle = stringResource(R.string.push_notifications_subtitle),
                        checked = pushEnabled,
                        onCheckedChange = {
                            if (!it && !emailEnabled && !smsEnabled) {
                                appStateViewModel.showError(atLeastOneChannelMessage)
                            } else {
                                pushEnabled = it
                                saveChannels(it, emailEnabled, smsEnabled)
                            }
                        },
                        enabled = !isSaving,
                        leadingIcon = Icons.Default.Notifications,
                        iconTint = BrandOrange
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                    SettingsToggleRow(
                        title = stringResource(R.string.email_notifications),
                        subtitle = stringResource(R.string.email_notifications_subtitle),
                        checked = emailEnabled,
                        onCheckedChange = {
                            if (!it && !pushEnabled && !smsEnabled) {
                                appStateViewModel.showError(atLeastOneChannelMessage)
                            } else {
                                emailEnabled = it
                                saveChannels(pushEnabled, it, smsEnabled)
                            }
                        },
                        enabled = !isSaving,
                        leadingIcon = Icons.Default.Email,
                        iconTint = Color(0xFF2196F3)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                    SettingsToggleRow(
                        title = stringResource(R.string.sms_notifications),
                        subtitle = stringResource(R.string.sms_notifications_subtitle),
                        checked = smsEnabled,
                        onCheckedChange = {
                            if (!it && !pushEnabled && !emailEnabled) {
                                appStateViewModel.showError(atLeastOneChannelMessage)
                            } else {
                                smsEnabled = it
                                saveChannels(pushEnabled, emailEnabled, it)
                            }
                        },
                        enabled = !isSaving,
                        leadingIcon = Icons.Default.Sms,
                        iconTint = Color(0xFF4CAF50)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pet Alerts
            Text(
                text = stringResource(R.string.pet_alerts),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsToggleRow(
                        title = stringResource(R.string.missing_pet_alerts),
                        subtitle = stringResource(R.string.missing_pet_alerts_subtitle),
                        checked = missingPetAlerts,
                        onCheckedChange = { missingPetAlerts = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Account & Orders
            Text(
                text = stringResource(R.string.account_and_orders),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsToggleRow(
                        title = stringResource(R.string.order_updates),
                        subtitle = stringResource(R.string.order_updates_subtitle),
                        checked = orderUpdates,
                        onCheckedChange = { orderUpdates = it }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                    SettingsToggleRow(
                        title = stringResource(R.string.account_activity),
                        subtitle = stringResource(R.string.account_activity_subtitle),
                        checked = accountActivity,
                        onCheckedChange = { accountActivity = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun HelpSupportScreen(
    authViewModel: AuthViewModel,
    appStateViewModel: AppStateViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showContactForm by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteErrorDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var isCheckingDelete by remember { mutableStateOf(false) }
    var deleteErrorMessage by remember { mutableStateOf("") }
    var missingPetNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var showFaqScreen by remember { mutableStateOf(false) }
    var showGuidesScreen by remember { mutableStateOf(false) }

    // Extract string resources outside lambdas
    val accountDeletedMessage = stringResource(R.string.account_deleted)
    val deleteAccountFailedMessage = stringResource(R.string.delete_account_failed)
    val cannotDeleteMissingPetsMessage = stringResource(R.string.cannot_delete_missing_pets)

    // Sub-navigation for FAQ and Guides
    if (showFaqScreen) {
        FaqScreen(onBack = { showFaqScreen = false })
        return
    }

    if (showGuidesScreen) {
        GuidesScreen(onBack = { showGuidesScreen = false })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SubScreenHeader(
            title = stringResource(R.string.help_support),
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 120.dp)
        ) {
            // Contact Support
            Text(
                text = stringResource(R.string.get_help),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    ProfileMenuRow(
                        icon = Icons.Default.HelpOutline,
                        title = stringResource(R.string.contact_support),
                        onClick = { showContactForm = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ProfileMenuRow(
                        icon = Icons.Default.HelpOutline,
                        title = stringResource(R.string.faq),
                        onClick = { showFaqScreen = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ProfileMenuRow(
                        icon = Icons.Default.Person,
                        title = stringResource(R.string.user_guides),
                        onClick = { showGuidesScreen = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Legal
            Text(
                text = stringResource(R.string.legal),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    ProfileMenuRow(
                        icon = Icons.Default.Lock,
                        title = stringResource(R.string.terms_of_service),
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(WebUrlHelper.termsUrl))
                            )
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ProfileMenuRow(
                        icon = Icons.Default.Lock,
                        title = stringResource(R.string.privacy_policy),
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(WebUrlHelper.privacyUrl))
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = TealAccent.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.app_name_label),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.app_version),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.support_response_time),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Danger Zone
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.danger_zone),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        isCheckingDelete = true
                        authViewModel.canDeleteAccount { response, error ->
                            isCheckingDelete = false
                            if (error != null) {
                                appStateViewModel.showError(error)
                            } else if (response != null) {
                                if (response.canDelete) {
                                    showDeleteDialog = true
                                } else {
                                    deleteErrorMessage = response.message ?: cannotDeleteMissingPetsMessage
                                    missingPetNames = response.missingPets?.map { it.name } ?: emptyList()
                                    showDeleteErrorDialog = true
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                            spotColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !isDeleting && !isCheckingDelete
                ) {
                    if (isDeleting || isCheckingDelete) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = when {
                            isCheckingDelete -> stringResource(R.string.checking_delete_eligibility)
                            isDeleting -> stringResource(R.string.deleting)
                            else -> stringResource(R.string.delete_account)
                        },
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.delete_account_permanent),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // Contact Support Dialog
    if (showContactForm) {
        ContactSupportDialog(
            authViewModel = authViewModel,
            appStateViewModel = appStateViewModel,
            onDismiss = { showContactForm = false }
        )
    }

    // Delete Account Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_account)) },
            text = {
                Text(stringResource(R.string.delete_account_full_warning))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeleting = true
                        authViewModel.deleteAccount { success, message ->
                            isDeleting = false
                            if (success) {
                                appStateViewModel.showSuccess(accountDeletedMessage)
                            } else {
                                appStateViewModel.showError(message ?: deleteAccountFailedMessage)
                            }
                        }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_account))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Cannot Delete Account (Missing Pets) Dialog
    if (showDeleteErrorDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteErrorDialog = false },
            title = { Text(stringResource(R.string.cannot_delete_account)) },
            text = {
                Column {
                    Text(deleteErrorMessage)
                    if (missingPetNames.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.missing_pets_label, missingPetNames.joinToString(", ")),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFFD97706) // Amber color for emphasis
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDeleteErrorDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}

@Composable
private fun ContactSupportDialog(
    authViewModel: AuthViewModel,
    appStateViewModel: AppStateViewModel,
    onDismiss: () -> Unit
) {
    val categoryGeneral = stringResource(R.string.support_category_general)
    val categoryTechnical = stringResource(R.string.support_category_technical)
    val categoryAccount = stringResource(R.string.support_category_account)
    val categoryBilling = stringResource(R.string.support_category_billing)
    val categoryFeature = stringResource(R.string.support_category_feature)
    val categoryOther = stringResource(R.string.support_category_other)

    var selectedCategory by remember { mutableStateOf(categoryGeneral) }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    val categories = listOf(categoryGeneral, categoryTechnical, categoryAccount, categoryBilling, categoryFeature, categoryOther)

    val supportSuccessFormat = stringResource(R.string.contact_support_success, "%1\$s")

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text(
                text = stringResource(R.string.contact_support),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Category selector
                Column {
                    Text(
                        text = stringResource(R.string.contact_support_category),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCategoryDropdown = !showCategoryDropdown },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = selectedCategory)
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (showCategoryDropdown) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column {
                                categories.forEach { category ->
                                    Text(
                                        text = category,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedCategory = category
                                                showCategoryDropdown = false
                                            }
                                            .padding(12.dp),
                                        color = if (category == selectedCategory) TealAccent else Color.Unspecified
                                    )
                                }
                            }
                        }
                    }
                }

                // Subject field
                OutlinedTextField(
                    value = subject,
                    onValueChange = { if (it.length <= 200) subject = it },
                    label = { Text(stringResource(R.string.contact_support_subject)) },
                    placeholder = { Text(stringResource(R.string.contact_support_subject_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isSubmitting
                )

                // Message field
                OutlinedTextField(
                    value = message,
                    onValueChange = { if (it.length <= 5000) message = it },
                    label = { Text(stringResource(R.string.contact_support_message)) },
                    placeholder = { Text(stringResource(R.string.contact_support_message_hint)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    enabled = !isSubmitting
                )

                Text(
                    text = stringResource(R.string.char_count, message.length),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSubmitting = true
                    authViewModel.submitSupportRequest(
                        category = selectedCategory,
                        subject = subject,
                        message = message,
                        onSuccess = { ticketId ->
                            isSubmitting = false
                            appStateViewModel.showSuccess(String.format(supportSuccessFormat, ticketId))
                            onDismiss()
                        },
                        onError = { error ->
                            isSubmitting = false
                            appStateViewModel.showError(error)
                        }
                    )
                },
                enabled = subject.isNotBlank() && message.isNotBlank() && !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = TealAccent)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.contact_support_sending))
                } else {
                    Text(stringResource(R.string.contact_support_submit))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

// Helper Composables

@Composable
private fun SubScreenHeader(
    title: String,
    onBack: () -> Unit,
    trailingContent: @Composable (() -> Unit)? = null
) {
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
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = stringResource(R.string.back),
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Center)
        )

        if (trailingContent != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            ) {
                trailingContent()
            }
        }
    }
}

@Composable
private fun ReadOnlyField(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value.ifBlank { "—" },
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
            color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StyledOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = iconTint
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
