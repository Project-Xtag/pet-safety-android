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
import androidx.compose.foundation.layout.imePadding
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
 *
 * Copy is Hungarian (the canonical locale); other locales follow via
 * the standard strings.xml extraction.
 */
@Composable
fun PetSetupWizardScreen(
    qrCode: String,
    onDone: () -> Unit,
    viewModel: PetSetupViewModel = hiltViewModel(),
    mode: com.petsafety.app.ui.viewmodel.PetSetupWizardMode =
        com.petsafety.app.ui.viewmodel.PetSetupWizardMode.ORDERED,
    // Optional navigation hooks for step 11 / step 12 CTAs. Hosts that
    // can route the user to the scanner / contact / privacy screens pass
    // these; hosts that can't (e.g. a deep-link sheet on top of the
    // splash) leave them null and the buttons collapse to onDone — same
    // convention as the iOS wizard.
    onScanNextTag: (() -> Unit)? = null,
    onSetContactDetails: (() -> Unit)? = null,
    onSetPrivacySettings: (() -> Unit)? = null,
) {
    val ui by viewModel.ui.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoBytes by remember { mutableStateOf<ByteArray?>(null) }

    LaunchedEffect(qrCode) { viewModel.start(qrCode, mode) }

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

    // imePadding(): activity uses enableEdgeToEdge(), so the soft
    // keyboard draws on top of content. Without this, the bottom
    // Vissza/Befejezés button row sits under the IME and step 10's
    // multiline "Egyedi ismertetőjelek" TextField traps the user
    // with no visible way to commit.
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        // Top bar — cancel
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDone) { Text("Mégse") }
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
                    "${ui.step}. lépés / $TOTAL_STEPS",
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
                // Standing notice (web parity): warns that pet identity
                // fields (name/species/breed) are locked at registration.
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrandOrange.copy(alpha = 0.10f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Filled.Sell,
                        contentDescription = null,
                        tint = Color(0xFFA6500F),
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        "A kedvenc neve, faja és fajtája a regisztráció után már nem módosítható — válaszd ki őket gondosan.",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFA6500F),
                    )
                }
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
            StepBody(
                ui = ui,
                viewModel = viewModel,
                photoUri = photoUri,
                onPickPhoto = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onClearPhoto = {
                    photoUri = null
                    photoBytes = null
                },
                onDone = onDone,
                onScanNextTag = onScanNextTag,
                onSetContactDetails = onSetContactDetails,
                onSetPrivacySettings = onSetPrivacySettings,
            )

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
                    Text("Vissza")
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
                    Text(if (ui.step == TOTAL_STEPS) "Befejezés" else "Tovább", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun canProceed(ui: PetSetupUiState): Boolean = when (ui.step) {
    // Both modes gate on having a pet name in hand. Promo collects
    // it by free-text input; ordered collects it by tapping a row
    // in the multi-pet picker (which fills petName). Post the
    // 2026-05-24 revert we can no longer key off selectedPetId for
    // the ordered branch because order_items.pet_id is NULL for
    // every unactivated row.
    1 -> ui.petName.trim().isNotEmpty()
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
    ui.petName.ifBlank { "a kedvenced" }

private fun stepTitle(ui: PetSetupUiState): String = when (ui.step) {
    1 -> if (ui.mode == com.petsafety.app.ui.viewmodel.PetSetupWizardMode.PROMO) {
        "Mi a kedvenced neve?"
    } else {
        "Melyik kedvenced ez?"
    }
    2 -> "Nagyszerű, hogy ${displayName(ui)} csatlakozott!"
    3 -> "Kutya vagy macska?"
    4 -> "Milyen fajta ${displayName(ui)}?"
    5 -> "Fiú vagy lány?"
    6 -> "Hány éves ${displayName(ui)}?"
    7 -> "Milyen színű?"
    8 -> "Tölts fel egy fotót"
    9 -> "Allergia vagy gyógyszer?"
    10 -> "Egyedi ismertetőjelek"
    11 -> "${displayName(ui)} bilétája kész!"
    else -> "Gratulálunk!"
}

private fun stepSubtitle(ui: PetSetupUiState): String = when (ui.step) {
    1 -> "Válaszd ki, melyik kedvenced bilétáját állítjuk be most."
    2 -> "Pár pillanat, és beállítjuk a SENRA bilétáját. Kezdjük is!"
    4 -> "Ha keverék vagy nem tudod, hagyd üresen."
    6 -> "Elég egy nagyjábóli érték is."
    8 -> "Nem kötelező — ki is hagyhatod, és később pótolhatod."
    9 -> "Fontos egészségügyi tudnivalók a megtaláló számára."
    10 -> "Bármi, ami segít felismerni a kedvenced."
    11 -> if (ui.remainingAfterThis == 1) "Még 1 biléta van hátra a rendelésből."
          else "Még ${ui.remainingAfterThis} biléta van hátra a rendelésből."
    12 -> "${displayName(ui)} mostantól védve van a SENRA közösségével."
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
    onScanNextTag: (() -> Unit)? = null,
    onSetContactDetails: (() -> Unit)? = null,
    onSetPrivacySettings: (() -> Unit)? = null,
) {
    when (ui.step) {
        1 -> {
            if (ui.mode == com.petsafety.app.ui.viewmodel.PetSetupWizardMode.PROMO) {
                // Promo flow: single text input. No picker — the pet does
                // not yet exist; we create it on commit via /claim-promo.
                OutlinedTextField(
                    value = ui.petName,
                    onValueChange = { viewModel.edit { s -> s.copy(petName = it) } },
                    label = { Text("A kedvenc neve") },
                    placeholder = { Text("Pl. Bodri") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (ui.orderItems.isEmpty()) {
                Text(
                    "Ehhez a kódhoz nincs beállítható biléta — lehet, hogy már aktiváltad, vagy nem ehhez a fiókhoz tartozik.",
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
                        val name = item.petName.ifBlank { "Kedvenc" }
                        // Selection key: petName, not petId. Post the
                        // 2026-05-24 revert order_items.pet_id is NULL
                        // for all unactivated rows so `selectedPetId
                        // == item.petId` collapsed to null == null and
                        // marked every row selected at once. pet_name
                        // is deduped server-side (DISTINCT ON pet_name)
                        // so it's a safe key here.
                        val selected = ui.petName.isNotEmpty() && ui.petName == item.petName
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
                                .clickable { viewModel.edit { it.copy(selectedPetId = item.petId, petName = item.petName) } }
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
                "Néhány gyors kérdés a kedvencedről. Bármikor visszaléphetsz — a végén aktiváljuk a bilétát.",
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
            WizardChoiceCard("Kutya", Icons.Filled.Pets, ui.species == "dog", Modifier.weight(1f)) {
                viewModel.edit { it.copy(species = "dog") }
            }
            WizardChoiceCard("Macska", Icons.Filled.Pets, ui.species == "cat", Modifier.weight(1f)) {
                viewModel.edit { it.copy(species = "cat") }
            }
        }
        4 -> WizardField("Fajta", ui.breed, "Pl. magyar vizsla, keverék") { v ->
            viewModel.edit { it.copy(breed = v) }
        }
        5 -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WizardChoiceCard("Fiú", null, ui.sex == "male", Modifier.weight(1f)) {
                viewModel.edit { it.copy(sex = "male") }
            }
            WizardChoiceCard("Lány", null, ui.sex == "female", Modifier.weight(1f)) {
                viewModel.edit { it.copy(sex = "female") }
            }
            WizardChoiceCard("Nem tudom", null, ui.sex == "unknown", Modifier.weight(1f)) {
                viewModel.edit { it.copy(sex = "unknown") }
            }
        }
        6 -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) {
                WizardField("Év", ui.ageYears, "0", numeric = true) { v ->
                    viewModel.edit { it.copy(ageYears = v.filter { c -> c.isDigit() }) }
                }
            }
            Box(Modifier.weight(1f)) {
                WizardField("Hónap", ui.ageMonths, "0", numeric = true) { v ->
                    viewModel.edit { it.copy(ageMonths = v.filter { c -> c.isDigit() }) }
                }
            }
        }
        7 -> WizardField("Szín", ui.color, "Pl. fekete-fehér") { v ->
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
                        Text("Fotó eltávolítása")
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
                    Text("Fotó kiválasztása", fontWeight = FontWeight.SemiBold)
                }
            }
        }
        9 -> Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            WizardField("Allergiák", ui.allergies, "Pl. csirke, bizonyos gyógyszerek") { v ->
                viewModel.edit { it.copy(allergies = v) }
            }
            WizardField("Gyógyszerek", ui.medications, "Rendszeresen szedett gyógyszerek") { v ->
                viewModel.edit { it.copy(medications = v) }
            }
        }
        10 -> WizardField(
            "Egyedi ismertetőjelek",
            ui.uniqueFeatures,
            "Pl. fehér folt a mellkason, félénk idegenekkel",
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
                Text("Következő lépések", fontWeight = FontWeight.Bold)
                Text("1.  Irányítsd a kamerát a következő biléta QR-kódjára")
                Text("2.  Koppints a linkre, amikor megjelenik a képernyőn")
                Text("3.  Kövesd a lépéseket a következő kedvenc beállításához")
            }
            // Primary CTA — open the scanner directly when the host
            // provides a handler; falls back to dismiss so the user can
            // navigate manually. Matches the iOS wizard's pattern.
            Button(
                onClick = { (onScanNextTag ?: onDone).invoke() },
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Következő biléta beolvasása", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text("Később folytatom", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        else -> Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "${displayName(ui)} mostantól védve van a SENRA közösségével. Állítsd be az elérhetőségeidet és az adatvédelmi beállításokat, hogy a megfelelő információk jelenjenek meg, ha valaki megtalálja.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            // Primary: contact details.
            Button(
                onClick = { (onSetContactDetails ?: onDone).invoke() },
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Elérhetőségek beállítása", fontWeight = FontWeight.Bold) }
            // Secondary: privacy settings (outlined).
            Button(
                onClick = { (onSetPrivacySettings ?: onDone).invoke() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandOrange),
                border = androidx.compose.foundation.BorderStroke(2.dp, BrandOrange),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Adatvédelmi beállítások", fontWeight = FontWeight.Bold) }
            // Tertiary: skip to My Pets.
            TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text("Ugrás a kedvenceimhez", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
