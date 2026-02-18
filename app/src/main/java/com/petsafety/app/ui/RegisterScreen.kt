package com.petsafety.app.ui

import android.content.Intent
import android.net.Uri
import android.util.Patterns
import com.petsafety.app.util.WebUrlHelper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petsafety.app.R
import com.petsafety.app.util.LocalizedLogo
import com.petsafety.app.ui.components.BrandButton
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.theme.PeachBackground
import com.petsafety.app.ui.util.AdaptiveLayout
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import com.petsafety.app.ui.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    appStateViewModel: AppStateViewModel,
    authViewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit = {}
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var showOtpField by remember { mutableStateOf(false) }
    var resendCooldown by remember { mutableIntStateOf(0) }

    // Countdown timer for resend cooldown
    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000)
            resendCooldown--
        }
    }

    val trimmedEmail = email.trim()
    val trimmedFirstName = firstName.trim()
    val isValidEmail = trimmedEmail.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()

    val context = LocalContext.current

    // Extract string resources outside lambdas
    val codeSentMessage = stringResource(R.string.code_sent_to_email, email)
    val loginFailedMessage = stringResource(R.string.login_failed)
    val verificationFailedMessage = stringResource(R.string.verification_failed)

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
            // Logo Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp, bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(LocalizedLogo.drawableRes),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.height(180.dp),
                    contentScale = ContentScale.Fit
                )
            }

            // Registration Card
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
                        text = stringResource(R.string.create_account),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.enter_details_subtitle),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // First Name Label
                    Text(
                        text = stringResource(R.string.first_name),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // First Name Field
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
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            BasicTextField(
                                value = firstName,
                                onValueChange = { firstName = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Last Name Label
                    Text(
                        text = stringResource(R.string.last_name),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Last Name Field
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
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            BasicTextField(
                                value = lastName,
                                onValueChange = { lastName = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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

                    // Email Field
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

                    // Create Account Button
                    BrandButton(
                        text = stringResource(R.string.create_account),
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
                        enabled = trimmedFirstName.isNotBlank() && isValidEmail
                    )
                } else {
                    // OTP Verification View
                    Text(
                        text = stringResource(R.string.create_account),
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

                    // OTP Code Field
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
                                    // Update profile with first/last name
                                    val updates = mutableMapOf<String, Any>(
                                        "first_name" to trimmedFirstName
                                    )
                                    if (lastName.trim().isNotBlank()) {
                                        updates["last_name"] = lastName.trim()
                                    }
                                    authViewModel.updateProfile(updates) { _, _ -> }
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
                                    email = email,
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
                val termsRegisterPrefix = stringResource(R.string.terms_register_prefix)
                val termsOfServiceText = stringResource(R.string.terms_of_service)
                val termsAndText = stringResource(R.string.terms_and)
                val privacyPolicyText = stringResource(R.string.privacy_policy)
                val termsText = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)) {
                        append(termsRegisterPrefix)
                    }
                    pushStringAnnotation(tag = "terms", annotation = WebUrlHelper.termsUrl)
                    withStyle(style = SpanStyle(color = BrandOrange, fontSize = 12.sp, fontWeight = FontWeight.Medium)) {
                        append(termsOfServiceText)
                    }
                    pop()
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)) {
                        append(termsAndText)
                    }
                    pushStringAnnotation(tag = "privacy", annotation = WebUrlHelper.privacyUrl)
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

            // Already have an account? Log in
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.already_have_account),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onNavigateToLogin) {
                    Text(
                        text = stringResource(R.string.log_in),
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
}
