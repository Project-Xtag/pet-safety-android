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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.petsafety.app.data.local.BiometricHelper
import com.petsafety.app.ui.components.BrandButton
import com.petsafety.app.ui.components.SecondaryButton
import com.petsafety.app.ui.theme.BackgroundLight
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.theme.MutedTextLight
import com.petsafety.app.ui.theme.PeachBackground
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import com.petsafety.app.ui.viewmodel.AuthViewModel
import com.petsafety.app.R

@Composable
fun AuthScreen(
    appStateViewModel: AppStateViewModel,
    authViewModel: AuthViewModel
) {
    var email by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var showOtpField by remember { mutableStateOf(false) }
    var showBiometricEnableDialog by remember { mutableStateOf(false) }

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
            .background(BackgroundLight)
    ) {
        Column(
            modifier = Modifier
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
                        color = MutedTextLight
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
                            .background(Color.White)
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
                                tint = MutedTextLight,
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

                    Spacer(modifier = Modifier.height(24.dp))

                    // Send Login Code Button
                    BrandButton(
                        text = stringResource(R.string.send_login_code),
                        onClick = {
                            authViewModel.login(
                                email = email,
                                onSuccess = {
                                    showOtpField = true
                                    appStateViewModel.showSuccess(codeSentMessage)
                                },
                                onFailure = { message ->
                                    appStateViewModel.showError(message ?: loginFailedMessage)
                                }
                            )
                        },
                        enabled = email.isNotBlank()
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
                            .background(Color.White)
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
                                            text = "000000",
                                            style = MaterialTheme.typography.headlineLarge.copy(
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = TextAlign.Center,
                                                letterSpacing = 4.sp
                                            ),
                                            color = MutedTextLight.copy(alpha = 0.5f)
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
                                email = email,
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

                    // Use different email link
                    TextButton(
                        onClick = {
                            showOtpField = false
                            otpCode = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.use_different_email),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MutedTextLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // T&Cs and Privacy Policy Disclaimer
                val termsText = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = MutedTextLight, fontSize = 12.sp)) {
                        append("By logging in, you agree to our ")
                    }
                    pushStringAnnotation(tag = "terms", annotation = "https://pet-er.app/terms")
                    withStyle(style = SpanStyle(color = BrandOrange, fontSize = 12.sp, fontWeight = FontWeight.Medium)) {
                        append("Terms of Service")
                    }
                    pop()
                    withStyle(style = SpanStyle(color = MutedTextLight, fontSize = 12.sp)) {
                        append(" and ")
                    }
                    pushStringAnnotation(tag = "privacy", annotation = "https://pet-er.app/privacy")
                    withStyle(style = SpanStyle(color = BrandOrange, fontSize = 12.sp, fontWeight = FontWeight.Medium)) {
                        append("Privacy Policy")
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

            Spacer(modifier = Modifier.height(40.dp))
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
