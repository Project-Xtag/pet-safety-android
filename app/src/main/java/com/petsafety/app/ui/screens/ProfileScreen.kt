package com.petsafety.app.ui.screens

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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.petsafety.app.R
import com.petsafety.app.ui.theme.BackgroundLight
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.theme.MutedTextLight
import com.petsafety.app.ui.theme.PeachBackground
import com.petsafety.app.ui.theme.TealAccent
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
    ORDERS
}

@Composable
fun ProfileScreen(
    appStateViewModel: AppStateViewModel,
    authViewModel: AuthViewModel
) {
    var section by remember { mutableStateOf(ProfileSection.MAIN) }
    val prefsViewModel: NotificationPreferencesViewModel = hiltViewModel()

    when (section) {
        ProfileSection.MAIN -> ProfileMain(
            authViewModel = authViewModel,
            appStateViewModel = appStateViewModel,
            onNavigate = { section = it },
            onLogout = { authViewModel.logout() }
        )
        ProfileSection.PERSONAL -> PersonalInfoScreen(authViewModel, appStateViewModel) { section = ProfileSection.MAIN }
        ProfileSection.ADDRESS -> AddressScreen(authViewModel, appStateViewModel) { section = ProfileSection.MAIN }
        ProfileSection.CONTACTS -> ContactsScreen { section = ProfileSection.MAIN }
        ProfileSection.PRIVACY -> PrivacyModeScreen { section = ProfileSection.MAIN }
        ProfileSection.NOTIFICATIONS -> NotificationPreferencesScreen(prefsViewModel) { section = ProfileSection.MAIN }
        ProfileSection.HELP -> HelpSupportScreen(authViewModel, appStateViewModel) { section = ProfileSection.MAIN }
        ProfileSection.ORDERS -> OrdersScreen { section = ProfileSection.MAIN }
    }
}

@Composable
private fun ProfileMain(
    authViewModel: AuthViewModel,
    appStateViewModel: AppStateViewModel,
    onNavigate: (ProfileSection) -> Unit,
    onLogout: () -> Unit
) {
    val user by authViewModel.currentUser.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    // Extract string resources outside lambdas
    val accountDeletedMessage = stringResource(R.string.account_deleted)
    val deleteAccountFailedMessage = stringResource(R.string.delete_account_failed)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Column(
            modifier = Modifier
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
                    text = "Account",
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
                if (user != null) {
                    if (user!!.fullName.isNotEmpty()) {
                        Text(
                            text = user!!.fullName,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = user!!.email,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MutedTextLight
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Premium Badge
                    Row(
                        modifier = Modifier
                            .background(
                                TealAccent.copy(alpha = 0.1f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = TealAccent
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PREMIUM MEMBER",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = TealAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Menu Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    ProfileMenuRow(
                        icon = Icons.Default.Person,
                        title = stringResource(R.string.personal_information),
                        onClick = { onNavigate(ProfileSection.PERSONAL) }
                    )
                    HorizontalDivider(color = Color(0xFFF2F2F7))
                    ProfileMenuRow(
                        icon = Icons.Default.Home,
                        title = stringResource(R.string.address),
                        onClick = { onNavigate(ProfileSection.ADDRESS) }
                    )
                    HorizontalDivider(color = Color(0xFFF2F2F7))
                    ProfileMenuRow(
                        icon = Icons.Default.People,
                        title = stringResource(R.string.contacts),
                        onClick = { onNavigate(ProfileSection.CONTACTS) }
                    )
                    HorizontalDivider(color = Color(0xFFF2F2F7))
                    ProfileMenuRow(
                        icon = Icons.Default.Lock,
                        title = stringResource(R.string.privacy_mode),
                        onClick = { onNavigate(ProfileSection.PRIVACY) }
                    )
                    HorizontalDivider(color = Color(0xFFF2F2F7))
                    ProfileMenuRow(
                        icon = Icons.Default.Notifications,
                        title = stringResource(R.string.notifications),
                        onClick = { onNavigate(ProfileSection.NOTIFICATIONS) }
                    )
                    HorizontalDivider(color = Color(0xFFF2F2F7))
                    ProfileMenuRow(
                        icon = Icons.Default.HelpOutline,
                        title = stringResource(R.string.help_support),
                        onClick = { onNavigate(ProfileSection.HELP) }
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

            Spacer(modifier = Modifier.height(24.dp))

            // Danger Zone
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DANGER ZONE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    ),
                    color = Color.Red.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = Color.Red.copy(alpha = 0.3f),
                            spotColor = Color.Red.copy(alpha = 0.3f)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
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
                        text = if (isDeleting) "Deleting..." else stringResource(R.string.delete_account),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "This will permanently delete your account and all data",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MutedTextLight,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.log_out)) },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
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

    // Delete Account Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_account)) },
            text = {
                Text("This action cannot be undone. Your account will be permanently deleted, all personal data will be removed, and any active subscriptions will be cancelled.")
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
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
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
            tint = MutedTextLight
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
    var firstName by remember { mutableStateOf(user?.firstName ?: "") }
    var lastName by remember { mutableStateOf(user?.lastName ?: "") }
    var phone by remember { mutableStateOf(user?.phone ?: "") }
    var isSaving by remember { mutableStateOf(false) }

    val updatedMessage = stringResource(R.string.updated)
    val updateFailedMessage = stringResource(R.string.update_failed)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Header
        SubScreenHeader(
            title = stringResource(R.string.personal_information),
            onBack = onBack
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
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StyledOutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = stringResource(R.string.first_name)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StyledOutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = stringResource(R.string.last_name)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StyledOutlinedTextField(
                        value = user?.email ?: "",
                        onValueChange = {},
                        label = stringResource(R.string.email),
                        enabled = false
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StyledOutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = stringResource(R.string.phone)
                    )
                }
            }

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
                            onBack()
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
    var address by remember { mutableStateOf(user?.address ?: "") }
    var addressLine2 by remember { mutableStateOf("") }
    var city by remember { mutableStateOf(user?.city ?: "") }
    var postalCode by remember { mutableStateOf(user?.postalCode ?: "") }
    var country by remember { mutableStateOf(user?.country ?: "") }
    var isSaving by remember { mutableStateOf(false) }

    val addressUpdatedMessage = stringResource(R.string.address_updated)
    val addressUpdateFailedMessage = stringResource(R.string.update_failed)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        SubScreenHeader(
            title = stringResource(R.string.address),
            onBack = onBack
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
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StyledOutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = "Street Address"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StyledOutlinedTextField(
                        value = addressLine2,
                        onValueChange = { addressLine2 = it },
                        label = "Address Line 2 (Optional)"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StyledOutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = stringResource(R.string.city)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StyledOutlinedTextField(
                        value = postalCode,
                        onValueChange = { postalCode = it },
                        label = stringResource(R.string.post_code)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StyledOutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        label = stringResource(R.string.country)
                    )
                }
            }

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
                            onBack()
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
        }
    }
}

@Composable
private fun ContactsScreen(onBack: () -> Unit) {
    var primaryEmail by remember { mutableStateOf("") }
    var secondaryEmail by remember { mutableStateOf("") }
    var primaryPhone by remember { mutableStateOf("") }
    var secondaryPhone by remember { mutableStateOf("") }
    var showPrimaryOnTag by remember { mutableStateOf(true) }
    var showSecondaryOnTag by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        SubScreenHeader(
            title = stringResource(R.string.contacts),
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 100.dp)
        ) {
            // Email Section
            Text(
                text = "Email Addresses",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StyledOutlinedTextField(
                        value = primaryEmail,
                        onValueChange = { primaryEmail = it },
                        label = "Primary Email"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StyledOutlinedTextField(
                        value = secondaryEmail,
                        onValueChange = { secondaryEmail = it },
                        label = "Secondary Email (Optional)"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Phone Section
            Text(
                text = "Phone Numbers",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StyledOutlinedTextField(
                        value = primaryPhone,
                        onValueChange = { primaryPhone = it },
                        label = "Primary Phone"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Show on QR tag",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedTextLight
                        )
                        Switch(
                            checked = showPrimaryOnTag,
                            onCheckedChange = { showPrimaryOnTag = it }
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = Color(0xFFF2F2F7)
                    )
                    StyledOutlinedTextField(
                        value = secondaryPhone,
                        onValueChange = { secondaryPhone = it },
                        label = "Secondary Phone (Optional)"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Show on QR tag",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedTextLight
                        )
                        Switch(
                            checked = showSecondaryOnTag,
                            onCheckedChange = { showSecondaryOnTag = it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = TealAccent.copy(alpha = 0.1f))
            ) {
                Text(
                    text = "Contacts marked as visible will be displayed on your pet's QR tag when scanned by someone who finds your pet.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = MutedTextLight,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PrivacyModeScreen(onBack: () -> Unit) {
    var hidePersonalInfo by remember { mutableStateOf(false) }
    var hideAddress by remember { mutableStateOf(false) }
    var shareLocationWithFinders by remember { mutableStateOf(true) }
    var publicProfile by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
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
            // Privacy Settings
            Text(
                text = "Privacy Mode",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsToggleRow(
                        title = "Hide Personal Information",
                        subtitle = "Your name will not be visible to finders",
                        checked = hidePersonalInfo,
                        onCheckedChange = { hidePersonalInfo = it }
                    )
                    HorizontalDivider(color = Color(0xFFF2F2F7))
                    SettingsToggleRow(
                        title = "Hide Address Details",
                        subtitle = "Your address will not be shared",
                        checked = hideAddress,
                        onCheckedChange = { hideAddress = it }
                    )
                    HorizontalDivider(color = Color(0xFFF2F2F7))
                    SettingsToggleRow(
                        title = "Share Location with Finders",
                        subtitle = "Allow finders to share their location with you",
                        checked = shareLocationWithFinders,
                        onCheckedChange = { shareLocationWithFinders = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Public Profile
            Text(
                text = "Public Profile",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsToggleRow(
                        title = "Public Profile",
                        subtitle = "Allow others to see your public profile",
                        checked = publicProfile,
                        onCheckedChange = { publicProfile = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Data Privacy Links
            Text(
                text = "Data Privacy",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    ProfileMenuRow(
                        icon = Icons.Default.Lock,
                        title = "Privacy Policy",
                        onClick = { /* Open privacy policy */ }
                    )
                    HorizontalDivider(color = Color(0xFFF2F2F7))
                    ProfileMenuRow(
                        icon = Icons.Default.Person,
                        title = "Data Management",
                        onClick = { /* Open data management */ }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationPreferencesScreen(
    viewModel: NotificationPreferencesViewModel,
    onBack: () -> Unit
) {
    val preferences by viewModel.preferences.collectAsState()

    var pushEnabled by remember { mutableStateOf(preferences.notifyByPush) }
    var emailEnabled by remember { mutableStateOf(preferences.notifyByEmail) }
    var smsEnabled by remember { mutableStateOf(preferences.notifyBySms) }
    var missingPetAlerts by remember { mutableStateOf(true) }
    var nearbyAlerts by remember { mutableStateOf(true) }
    var orderUpdates by remember { mutableStateOf(true) }
    var accountActivity by remember { mutableStateOf(true) }
    var productUpdates by remember { mutableStateOf(false) }
    var marketingEmails by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
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
            // Notification Channels
            Text(
                text = "Notification Channels",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsToggleRow(
                        title = "Push Notifications",
                        subtitle = "Receive push notifications on your device",
                        checked = pushEnabled,
                        onCheckedChange = { pushEnabled = it }
                    )
                    HorizontalDivider(color = Color(0xFFF2F2F7))
                    SettingsToggleRow(
                        title = "Email Notifications",
                        subtitle = "Receive notifications via email",
                        checked = emailEnabled,
                        onCheckedChange = { emailEnabled = it }
                    )
                    HorizontalDivider(color = Color(0xFFF2F2F7))
                    SettingsToggleRow(
                        title = "SMS Notifications",
                        subtitle = "Receive notifications via SMS",
                        checked = smsEnabled,
                        onCheckedChange = { smsEnabled = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pet Alerts
            Text(
                text = "Pet Alerts",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsToggleRow(
                        title = "Missing Pet Alerts",
                        subtitle = "Get notified about missing pets in your area",
                        checked = missingPetAlerts,
                        onCheckedChange = { missingPetAlerts = it }
                    )
                    HorizontalDivider(color = Color(0xFFF2F2F7))
                    SettingsToggleRow(
                        title = "Nearby Alerts (10km)",
                        subtitle = "Alerts for pets within 10km of your location",
                        checked = nearbyAlerts,
                        onCheckedChange = { nearbyAlerts = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Account & Orders
            Text(
                text = "Account & Orders",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsToggleRow(
                        title = "Order Updates",
                        subtitle = "Shipping and delivery notifications",
                        checked = orderUpdates,
                        onCheckedChange = { orderUpdates = it }
                    )
                    HorizontalDivider(color = Color(0xFFF2F2F7))
                    SettingsToggleRow(
                        title = "Account Activity",
                        subtitle = "Login and security notifications",
                        checked = accountActivity,
                        onCheckedChange = { accountActivity = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Optional Updates
            Text(
                text = "Optional Updates",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsToggleRow(
                        title = "Product Updates",
                        subtitle = "New features and improvements",
                        checked = productUpdates,
                        onCheckedChange = { productUpdates = it }
                    )
                    HorizontalDivider(color = Color(0xFFF2F2F7))
                    SettingsToggleRow(
                        title = "Marketing Emails",
                        subtitle = "Offers and promotions",
                        checked = marketingEmails,
                        onCheckedChange = { marketingEmails = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Note: Critical alerts about your pets will always be sent regardless of these settings.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MutedTextLight,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun HelpSupportScreen(
    authViewModel: AuthViewModel,
    appStateViewModel: AppStateViewModel,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
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
                .padding(top = 20.dp, bottom = 100.dp)
        ) {
            // Contact Support
            Text(
                text = "Get Help",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    ProfileMenuRow(
                        icon = Icons.Default.HelpOutline,
                        title = "Contact Support",
                        onClick = { /* Open contact form */ }
                    )
                    HorizontalDivider(color = Color(0xFFF2F2F7))
                    ProfileMenuRow(
                        icon = Icons.Default.HelpOutline,
                        title = "Frequently Asked Questions",
                        onClick = { /* Open FAQ */ }
                    )
                    HorizontalDivider(color = Color(0xFFF2F2F7))
                    ProfileMenuRow(
                        icon = Icons.Default.Person,
                        title = "User Guides",
                        onClick = { /* Open guides */ }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Legal
            Text(
                text = "Legal",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    ProfileMenuRow(
                        icon = Icons.Default.Lock,
                        title = "Terms of Service",
                        onClick = { /* Open terms */ }
                    )
                    HorizontalDivider(color = Color(0xFFF2F2F7))
                    ProfileMenuRow(
                        icon = Icons.Default.Lock,
                        title = "Privacy Policy",
                        onClick = { /* Open privacy */ }
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
                        text = "Pet Safety",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Version 1.0.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedTextLight
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Our support team typically responds within 24 hours.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MutedTextLight,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// Helper Composables

@Composable
private fun SubScreenHeader(
    title: String,
    onBack: () -> Unit
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
                contentDescription = "Back",
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
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MutedTextLight
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
