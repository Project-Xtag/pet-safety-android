package com.petsafety.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.theme.PeachBackground
import com.petsafety.app.ui.viewmodel.PetSetupUiState
import com.petsafety.app.ui.viewmodel.PetSetupViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TOTAL_STEPS = 10

/**
 * Guided, one-step-per-screen pet-onboarding wizard shown after a user
 * scans a new SENRA tag from their order.
 */
@Composable
fun PetSetupWizardScreen(
    qrCode: String,
    onDone: () -> Unit,
    viewModel: PetSetupViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoBytes by remember { mutableStateOf<ByteArray?>(null) }

    LaunchedEffect(qrCode) { viewModel.start(qrCode) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            photoUri = uri
            scope.launch {
                photoBytes = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    }.getOrNull()
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Top bar — cancel
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDone) { Text("Cancel") }
        }

        if (ui.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandOrange)
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (ui.step <= TOTAL_STEPS) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Step ${ui.step} of $TOTAL_STEPS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { ui.step.toFloat() / TOTAL_STEPS },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = BrandOrange,
                )
            }

            Spacer(Modifier.height(24.dp))
            WizardGraphic(stepIcon(ui.step))
            Spacer(Modifier.height(18.dp))
            Text(
                stepTitle(ui),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            val subtitle = stepSubtitle(ui)
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(22.dp))
            StepBody(ui, viewModel, photoUri, onPickPhoto = {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }, onClearPhoto = {
                photoUri = null
                photoBytes = null
            }, onDone = onDone)

            ui.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(24.dp))
        }

        if (ui.step <= TOTAL_STEPS) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = {
                        if (ui.step == 1) onDone() else viewModel.goToStep(ui.step - 1)
                    },
                    enabled = !ui.committing,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = null)
                    Text("Back")
                }
                Button(
                    onClick = {
                        if (ui.step == TOTAL_STEPS) viewModel.commit(photoBytes)
                        else viewModel.goToStep(ui.step + 1)
                    },
                    enabled = canProceed(ui) && !ui.committing,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                    modifier = Modifier.weight(1f),
                ) {
                    if (ui.committing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            if (ui.step == TOTAL_STEPS) Icons.Filled.Check else Icons.Filled.KeyboardArrowRight,
                            contentDescription = null,
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(if (ui.step == TOTAL_STEPS) "Finish" else "Next", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun canProceed(ui: PetSetupUiState): Boolean = when (ui.step) {
    1 -> ui.petName.isNotBlank()
    3 -> ui.species.isNotBlank()
    else -> true
}

private fun stepIcon(step: Int): ImageVector = when (step) {
    1 -> Icons.Filled.Sell
    2 -> Icons.Filled.AutoAwesome
    3 -> Icons.Filled.Pets
    4 -> Icons.Filled.Search
    5 -> Icons.Filled.Favorite
    6 -> Icons.Filled.Cake
    7 -> Icons.Filled.Palette
    8 -> Icons.Filled.PhotoCamera
    9 -> Icons.Filled.Medication
    10 -> Icons.Filled.Star
    11 -> Icons.Filled.QrCodeScanner
    else -> Icons.Filled.Celebration
}

private fun displayName(ui: PetSetupUiState): String =
    ui.petName.ifBlank { "your pet" }

private fun stepTitle(ui: PetSetupUiState): String = when (ui.step) {
    1 -> "Which pet is this tag for?"
    2 -> "Great to have ${displayName(ui)} on board!"
    3 -> "Dog or cat?"
    4 -> "What breed is ${displayName(ui)}?"
    5 -> "Boy or girl?"
    6 -> "How old is ${displayName(ui)}?"
    7 -> "What colour is ${displayName(ui)}?"
    8 -> "Add a photo"
    9 -> "Allergies or medications?"
    10 -> "Any unique features?"
    11 -> "${displayName(ui)}'s tag is ready!"
    else -> "Congratulations!"
}

private fun stepSubtitle(ui: PetSetupUiState): String = when (ui.step) {
    1 -> "Pick which pet you're setting this tag up for."
    2 -> "Set up your SENRA tag in a few quick steps. Let's go!"
    4 -> "If it's a mix or you're not sure, leave it blank."
    6 -> "A rough age is fine."
    8 -> "Optional — you can skip this and add one later."
    9 -> "Important health information for whoever finds your pet."
    10 -> "Anything that helps someone recognise your pet."
    11 -> if (ui.remainingAfterThis == 1) "There's 1 more tag left to set up."
          else "There are ${ui.remainingAfterThis} more tags left to set up."
    12 -> "${displayName(ui)} is now protected by the SENRA community."
    else -> ""
}

@Composable
private fun StepBody(
    ui: PetSetupUiState,
    viewModel: PetSetupViewModel,
    photoUri: Uri?,
    onPickPhoto: () -> Unit,
    onClearPhoto: () -> Unit,
    onDone: () -> Unit,
) {
    when (ui.step) {
        1 -> {
            if (ui.orderItems.isEmpty()) {
                Text(
                    "There's no tag to set up for this code — it may already be activated, or it belongs to a different account.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            } else {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ui.orderItems.forEach { item ->
                        val name = item.petName ?: "Pet"
                        val selected = !item.petName.isNullOrBlank() && ui.petName == item.petName
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    if (selected) 2.dp else 1.dp,
                                    if (selected) BrandOrange else Color(0x22000000),
                                    RoundedCornerShape(14.dp),
                                )
                                .background(if (selected) BrandOrange.copy(alpha = 0.08f) else Color.Transparent)
                                .clickable { viewModel.edit { it.copy(petName = item.petName ?: "") } }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Filled.Pets, contentDescription = null, tint = BrandOrange)
                            Text(name, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        2 -> {
            Text(
                "A few quick questions about your pet. You can go back any time — we'll activate the tag at the end.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PeachBackground)
                    .padding(16.dp),
            )
        }
        3 -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WizardChoiceCard("Dog", Icons.Filled.Pets, ui.species == "dog", Modifier.weight(1f)) {
                viewModel.edit { it.copy(species = "dog") }
            }
            WizardChoiceCard("Cat", Icons.Filled.Pets, ui.species == "cat", Modifier.weight(1f)) {
                viewModel.edit { it.copy(species = "cat") }
            }
        }
        4 -> WizardField("Breed", ui.breed, "e.g. Hungarian Vizsla, mixed") { v ->
            viewModel.edit { it.copy(breed = v) }
        }
        5 -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WizardChoiceCard("Boy", null, ui.sex == "male", Modifier.weight(1f)) {
                viewModel.edit { it.copy(sex = "male") }
            }
            WizardChoiceCard("Girl", null, ui.sex == "female", Modifier.weight(1f)) {
                viewModel.edit { it.copy(sex = "female") }
            }
            WizardChoiceCard("Not sure", null, ui.sex == "unknown", Modifier.weight(1f)) {
                viewModel.edit { it.copy(sex = "unknown") }
            }
        }
        6 -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) {
                WizardField("Years", ui.ageYears, "0", numeric = true) { v ->
                    viewModel.edit { it.copy(ageYears = v.filter { c -> c.isDigit() }) }
                }
            }
            Box(Modifier.weight(1f)) {
                WizardField("Months", ui.ageMonths, "0", numeric = true) { v ->
                    viewModel.edit { it.copy(ageMonths = v.filter { c -> c.isDigit() }) }
                }
            }
        }
        7 -> WizardField("Colour", ui.color, "e.g. black and white") { v ->
            viewModel.edit { it.copy(color = v) }
        }
        8 -> {
            if (photoUri != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = null,
                        modifier = Modifier.size(160.dp).clip(RoundedCornerShape(18.dp)),
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onClearPhoto) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Remove photo")
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(PeachBackground)
                        .clickable(onClick = onPickPhoto)
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = BrandOrange)
                    Spacer(Modifier.height(8.dp))
                    Text("Choose a photo", fontWeight = FontWeight.SemiBold)
                }
            }
        }
        9 -> Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            WizardField("Allergies", ui.allergies, "e.g. chicken, certain medication") { v ->
                viewModel.edit { it.copy(allergies = v) }
            }
            WizardField("Medications", ui.medications, "Any regular medication") { v ->
                viewModel.edit { it.copy(medications = v) }
            }
        }
        10 -> WizardField(
            "Unique features",
            ui.uniqueFeatures,
            "e.g. white chest patch, shy with strangers",
            singleLine = false,
        ) { v -> viewModel.edit { it.copy(uniqueFeatures = v) } }
        11 -> Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PeachBackground)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Next steps", fontWeight = FontWeight.Bold)
                Text("1.  Point your camera at the QR code on the next tag")
                Text("2.  Tap the link when it appears on screen")
                Text("3.  Follow the steps to set up your next pet")
            }
            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Done for now", fontWeight = FontWeight.Bold) }
        }
        else -> Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Set up your contact details and privacy settings so the right information shows when someone finds your pet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Go to my pets", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun WizardGraphic(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .background(
                brush = Brush.linearGradient(listOf(BrandOrange, BrandOrange.copy(alpha = 0.78f))),
                shape = RoundedCornerShape(26.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(42.dp))
    }
}

@Composable
private fun WizardChoiceCard(
    label: String,
    icon: ImageVector?,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                2.dp,
                if (selected) BrandOrange else Color(0x22000000),
                RoundedCornerShape(16.dp),
            )
            .background(if (selected) BrandOrange.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) BrandOrange else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WizardField(
    label: String,
    value: String,
    placeholder: String,
    numeric: Boolean = false,
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = singleLine,
        keyboardOptions = if (numeric) {
            KeyboardOptions(keyboardType = KeyboardType.Number)
        } else {
            KeyboardOptions.Default
        },
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
    )
}
