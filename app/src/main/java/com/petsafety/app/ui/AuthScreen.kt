package com.petsafety.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.LocalOffer
import com.petsafety.app.data.local.BiometricHelper
import com.petsafety.app.ui.components.BrandButton
import com.petsafety.app.ui.components.SecondaryButton
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.theme.PeachBackground
import com.petsafety.app.ui.util.AdaptiveLayout
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import com.petsafety.app.ui.viewmodel.AuthViewModel
import android.util.Patterns
import com.petsafety.app.R

@Composable
fun AuthScreen(
    appStateViewModel: AppStateViewModel,
    authViewModel: AuthViewModel,
    onNavigateToOrderTags: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var showOtpField by remember { mutableStateOf(false) }
    var showBiometricEnableDialog by remember { mutableStateOf(false) }
    var resendCooldown by remember { mutableIntStateOf(0) }

    // Countdown timer for resend cooldown
    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000)
            resendCooldown--
        }
    }

    val trimmedEmail = email.trim()
    val isValidEmail = trimmedEmail.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()

    val context = LocalContext.current
    val biometricHelper = remember { BiometricHelper(context) }
    val biometricEnabled by authViewModel.biometricEnabled.collectAsState()

    // Extract string resources outside lambdas
    val codeSentMessage = stringResource(R.string.code_sent_to_email, email)
    val loginFailedMessage = stringResource(R.string.login_failed)
    val verificationFailedMessage = stringResource(R.string.verification_failed)
    val biometricEnabledMessage = stringResource(R.string.biometric_enabled)
    val biometricLoginTitle = stringResource(R.string.biometric_login_title)
    val biometricLoginSubtitle = stringResource(R.string.biometric_login_subtitle)
    val useEmailText = stringResource(R.string.use_password)

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
                .verticalScroll(rememberScrollState())
        ) {
            // Logo Section - centered at top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp, bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_new),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.height(180.dp),
                    contentScale = ContentScale.Fit
                )
            }

            // Login Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(PeachBackground)
                    .padding(28.dp)
            ) {
                if (!showOtpField) {
                    // Header
                    Text(
                        text = stringResource(R.string.welcome_back),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.enter_email_subtitle),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Email Address Label
                    Text(
                        text = stringResource(R.string.email_address),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Email Field with icon (matching iOS style)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                width = 1.dp,
                                color = Color.Gray.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_email),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            BasicTextField(
                                value = email,
                                onValueChange = { email = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }

                    // Email validation hint
                    if (email.isNotEmpty() && !isValidEmail) {
                        Text(
                            text = stringResource(R.string.invalid_email),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Send Login Code Button
                    BrandButton(
                        text = stringResource(R.string.send_login_code),
                        onClick = {
                            authViewModel.login(
                                email = trimmedEmail,
                                onSuccess = {
                                    showOtpField = true
                                    resendCooldown = 60
                                    appStateViewModel.showSuccess(codeSentMessage)
                                },
                                onFailure = { message ->
                                    appStateViewModel.showError(message ?: loginFailedMessage)
                                }
                            )
                        },
                        enabled = isValidEmail
                    )

                    // Biometric Login Option (if enabled and has stored session)
                    val activity = context as? FragmentActivity
                    val loginWithBiometricText = stringResource(R.string.login_with_biometric)
                    if (biometricHelper.canUseBiometric() && biometricEnabled && activity != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(
                            onClick = {
                                biometricHelper.showBiometricPrompt(
                                    activity = activity,
                                    title = biometricLoginTitle,
                                    subtitle = biometricLoginSubtitle,
                                    negativeButtonText = useEmailText,
                                    onSuccess = {
                                        authViewModel.onBiometricSuccess()
                                    },
                                    onFailure = { error ->
                                        error?.let { appStateViewModel.showError(it) }
                                    },
                                    onCancel = {
                                        // User cancelled, do nothing
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_fingerprint),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = BrandOrange
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = loginWithBiometricText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = BrandOrange
                            )
                        }
                    }
                } else {
                    // OTP Verification View
                    Text(
                        text = stringResource(R.string.welcome_back),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // "Enter 6-digit code sent to" + email
                    Text(
                        text = stringResource(R.string.otp_sent_to),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // OTP Code Field (large, centered, monospace)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                width = 1.dp,
                                color = Color.Gray.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicTextField(
                            value = otpCode,
                            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) otpCode = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 4.sp
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (otpCode.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.otp_placeholder),
                                            style = MaterialTheme.typography.headlineLarge.copy(
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = TextAlign.Center,
                                                letterSpacing = 4.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Verify Code Button
                    BrandButton(
                        text = stringResource(R.string.verify_code),
                        onClick = {
                            authViewModel.verifyOtp(
                                email = trimmedEmail,
                                code = otpCode,
                                onSuccess = {
                                    // Offer biometric enrollment if available and not already enabled
                                    if (biometricHelper.canUseBiometric() && !biometricEnabled) {
                                        showBiometricEnableDialog = true
                                    }
                                },
                                onFailure = { message ->
                                    appStateViewModel.showError(message ?: verificationFailedMessage)
                                }
                            )
                        },
                        enabled = otpCode.length == 6
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Resend code
                    if (resendCooldown > 0) {
                        Text(
                            text = stringResource(R.string.resend_code_cooldown, resendCooldown),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        TextButton(
                            onClick = {
                                authViewModel.login(
                                    email = trimmedEmail,
                                    onSuccess = {
                                        resendCooldown = 60
                                        appStateViewModel.showSuccess(codeSentMessage)
                                    },
                                    onFailure = { message ->
                                        appStateViewModel.showError(message ?: loginFailedMessage)
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.resend_code_prompt),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = BrandOrange
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Use different email link
                    TextButton(
                        onClick = {
                            showOtpField = false
                            otpCode = ""
                            resendCooldown = 0
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.use_different_email),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // T&Cs and Privacy Policy Disclaimer
                val termsLoginPrefix = stringResource(R.string.terms_login_prefix)
                val termsOfServiceText = stringResource(R.string.terms_of_service)
                val termsAndText = stringResource(R.string.terms_and)
                val privacyPolicyText = stringResource(R.string.privacy_policy)
                val termsText = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)) {
                        append(termsLoginPrefix)
                    }
                    pushStringAnnotation(tag = "terms", annotation = "https://senra.pet/terms-conditions")
                    withStyle(style = SpanStyle(color = BrandOrange, fontSize = 12.sp, fontWeight = FontWeight.Medium)) {
                        append(termsOfServiceText)
                    }
                    pop()
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)) {
                        append(termsAndText)
                    }
                    pushStringAnnotation(tag = "privacy", annotation = "https://senra.pet/privacy-policy")
                    withStyle(style = SpanStyle(color = BrandOrange, fontSize = 12.sp, fontWeight = FontWeight.Medium)) {
                        append(privacyPolicyText)
                    }
                    pop()
                }

                ClickableText(
                    text = termsText,
                    onClick = { offset ->
                        termsText.getStringAnnotations(tag = "terms", start = offset, end = offset)
                            .firstOrNull()?.let {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.item)))
                            }
                        termsText.getStringAnnotations(tag = "privacy", start = offset, end = offset)
                            .firstOrNull()?.let {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.item)))
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center)
                )

            }

            // Register & Order Tag CTAs for new users (outside card)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.dont_have_account),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onNavigateToRegister) {
                    Text(
                        text = stringResource(R.string.register),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = BrandOrange
                    )
                }

                TextButton(
                    onClick = onNavigateToOrderTags
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalOffer,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = BrandOrange
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.start_here_order_free_tag),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = BrandOrange
                    )
                }
            }
        }
    }

    if (showBiometricEnableDialog) {
        AlertDialog(
            onDismissRequest = { showBiometricEnableDialog = false },
            title = { Text(stringResource(R.string.enable_biometric_title)) },
            text = { Text(stringResource(R.string.enable_biometric_message)) },
            confirmButton = {
                BrandButton(
                    text = stringResource(R.string.enable),
                    onClick = {
                        authViewModel.setBiometricEnabled(true)
                        showBiometricEnableDialog = false
                        appStateViewModel.showSuccess(biometricEnabledMessage)
                    },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { showBiometricEnableDialog = false }) {
                    Text(stringResource(R.string.skip))
                }
            }
        )
    }
}
