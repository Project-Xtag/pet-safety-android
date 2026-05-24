package com.petsafety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petsafety.app.data.events.PetsEventBus
import com.petsafety.app.data.model.UnactivatedOrderItem
import com.petsafety.app.data.network.model.UpdatePetRequest
import com.petsafety.app.data.repository.OrdersRepository
import com.petsafety.app.data.repository.PetsRepository
import com.petsafety.app.data.repository.QrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

/**
 * Holds the draft for the step-by-step pet-onboarding wizard. The pet
 * was auto-registered (name only) at order time; the draft is kept in
 * memory and committed in one go after the last detail step (update the
 * pet -> upload photo -> activate the scanned tag).
 */
/**
 * Two branches the wizard can take. Ordered (default) is the flow after
 * a tag the user paid for arrives — the pet was auto-registered at
 * order time and we just fill in details + activate the tag. Promo is
 * the flow after a shelter/event tag scan — there is no pre-registered
 * pet, so step 1 collects a name and the final commit uses
 * /qr-tags/claim-promo to create the pet, activate the tag, and grant
 * any subscription trial atomically. Mirrors the web's PetSetup.tsx and
 * iOS's PetSetupWizardMode.
 */
enum class PetSetupWizardMode { ORDERED, PROMO }

data class PetSetupUiState(
    val qrCode: String = "",
    val mode: PetSetupWizardMode = PetSetupWizardMode.ORDERED,
    val loading: Boolean = true,
    val orderItems: List<UnactivatedOrderItem> = emptyList(),
    val step: Int = 1,
    val committing: Boolean = false,
    val committedPetId: String? = null,
    val selectedPetId: String? = null,
    val error: String? = null,
    val petName: String = "",
    val species: String = "",
    val breed: String = "",
    val sex: String = "",
    val ageYears: String = "",
    val ageMonths: String = "",
    val color: String = "",
    val allergies: String = "",
    val medications: String = "",
    val uniqueFeatures: String = "",
) {
    // Promo claims are always single-tag; the multi-tag scan-next step
    // is only reachable from ordered orders with more than one pet.
    val remainingAfterThis: Int get() =
        if (mode == PetSetupWizardMode.PROMO) 0
        else (orderItems.size - 1).coerceAtLeast(0)
}

@HiltViewModel
class PetSetupViewModel @Inject constructor(
    private val petsRepository: PetsRepository,
    private val qrRepository: QrRepository,
    private val ordersRepository: OrdersRepository,
    private val petsEventBus: PetsEventBus,
) : ViewModel() {

    private val _ui = MutableStateFlow(PetSetupUiState())
    val ui: StateFlow<PetSetupUiState> = _ui.asStateFlow()

    private var started = false

    fun start(qrCode: String, mode: PetSetupWizardMode = PetSetupWizardMode.ORDERED) {
        if (started) return
        started = true
        _ui.update { it.copy(qrCode = qrCode, mode = mode) }
        viewModelScope.launch {
            if (mode == PetSetupWizardMode.PROMO) {
                // Promo tags have no pre-registered order items — skip the
                // /unactivated-for-qr fetch entirely. Step 1 collects the
                // new pet's name; the final commit uses /claim-promo.
                _ui.update { it.copy(loading = false, orderItems = emptyList()) }
                return@launch
            }
            val items = try {
                ordersRepository.getUnactivatedTagsForQRCode(qrCode)
            } catch (e: Exception) {
                Timber.w(e, "getUnactivatedTagsForQRCode failed for %s", qrCode)
                emptyList()
            }
            _ui.update {
                it.copy(
                    loading = false,
                    orderItems = items,
                    // Single-pet order — pre-select and skip step 1 entirely
                    // so the user lands on the intro screen, matching the
                    // web wizard's stepOffset=1 behaviour.
                    selectedPetId = if (items.size == 1) items.first().petId else it.selectedPetId,
                    petName = if (items.size == 1) items.first().petName else it.petName,
                    step = if (items.size == 1) 2 else it.step,
                )
            }
        }
    }

    fun edit(transform: (PetSetupUiState) -> PetSetupUiState) = _ui.update(transform)

    fun goToStep(step: Int) = _ui.update { it.copy(step = step, error = null) }

    fun commit(photoBytes: ByteArray?) {
        viewModelScope.launch {
            val s = _ui.value
            _ui.update { it.copy(committing = true, error = null) }
            try {
                when (s.mode) {
                    PetSetupWizardMode.ORDERED -> commitOrdered(s, photoBytes)
                    PetSetupWizardMode.PROMO -> commitPromo(s, photoBytes)
                }
                petsEventBus.requestRefresh()
                _ui.update {
                    it.copy(
                        committing = false,
                        step = if (it.remainingAfterThis > 0) 11 else 12,
                    )
                }
            } catch (e: Exception) {
                Timber.w(e, "pet-setup commit failed (mode=%s)", s.mode)
                _ui.update { it.copy(committing = false, error = e.localizedMessage) }
            }
        }
    }

    /** Ordered flow: post the 2026-05-24 auto-create revert, the pet
     *  does NOT exist at order time — the wizard sends the full pet
     *  payload to /qr-tags/activate, which atomically creates the
     *  pet AND activates the tag in one round-trip (mirrors
     *  commitPromo).
     *
     *  Backwards-compat for older orders whose order_item still has
     *  a pet_id (pre-revert placeholder that survived the cleanup
     *  migration's safety filter — e.g. user added a photo): if
     *  selectedPetId is set, fall back to the update-then-activate
     *  path so the existing pet record is preserved. */
    private suspend fun commitOrdered(s: PetSetupUiState, photoBytes: ByteArray?) {
        val existingPetId = s.selectedPetId
        val dob = computeDateOfBirth(s.ageYears, s.ageMonths)

        if (existingPetId != null) {
            // Legacy path: pet record survived the migration, update + activate.
            if (s.committedPetId != existingPetId) {
                val request = UpdatePetRequest(
                    name = s.petName.trim(),
                    species = s.species.ifBlank { null },
                    breed = s.breed.trim().ifBlank { null },
                    color = s.color.trim().ifBlank { null },
                    allergies = s.allergies.trim().ifBlank { null },
                    medications = s.medications.trim().ifBlank { null },
                    uniqueFeatures = s.uniqueFeatures.trim().ifBlank { null },
                    sex = s.sex.takeIf { it.isNotBlank() && it != "unknown" },
                    dateOfBirth = dob,
                    dobIsApproximate = if (dob != null) true else null,
                )
                petsRepository.updatePet(existingPetId, request)
                _ui.update { it.copy(committedPetId = existingPetId) }
                uploadPhotoNonFatal(existingPetId, photoBytes)
            }
            qrRepository.activateTag(s.qrCode, existingPetId)
            return
        }

        // Post-revert path: no pre-existing pet. Single atomic call
        // creates the pet and activates the tag.
        if (s.committedPetId != null) return
        val petData = com.petsafety.app.data.network.model.CreatePetRequest(
            name = s.petName.trim(),
            species = s.species.ifBlank { "dog" },
            breed = s.breed.trim().ifBlank { null },
            color = s.color.trim().ifBlank { null },
            allergies = s.allergies.trim().ifBlank { null },
            medications = s.medications.trim().ifBlank { null },
            uniqueFeatures = s.uniqueFeatures.trim().ifBlank { null },
            sex = s.sex.takeIf { it.isNotBlank() && it != "unknown" },
            dateOfBirth = dob,
            dobIsApproximate = if (dob != null) true else null,
        )
        val tag = qrRepository.activateTagWithNewPet(s.qrCode, petData)
        val newPetId = tag.petId
        _ui.update { it.copy(committedPetId = newPetId, selectedPetId = newPetId) }
        if (newPetId != null) uploadPhotoNonFatal(newPetId, photoBytes)
    }

    /** Promo: pet does NOT exist. One atomic /claim-promo call creates it,
     * activates the tag, and grants any subscription trial in the batch. */
    private suspend fun commitPromo(s: PetSetupUiState, photoBytes: ByteArray?) {
        if (s.committedPetId != null) return
        val dob = computeDateOfBirth(s.ageYears, s.ageMonths)
        val petData = com.petsafety.app.data.network.model.CreatePetRequest(
            name = s.petName.trim(),
            species = s.species.ifBlank { "dog" },
            breed = s.breed.trim().ifBlank { null },
            color = s.color.trim().ifBlank { null },
            allergies = s.allergies.trim().ifBlank { null },
            medications = s.medications.trim().ifBlank { null },
            uniqueFeatures = s.uniqueFeatures.trim().ifBlank { null },
            sex = s.sex.takeIf { it.isNotBlank() && it != "unknown" },
            dateOfBirth = dob,
            dobIsApproximate = if (dob != null) true else null,
        )
        val response = qrRepository.claimPromoTag(qrCode = s.qrCode, pet = petData)
        val newPetId = response.pet?.id
        _ui.update { it.copy(committedPetId = newPetId, selectedPetId = newPetId) }
        if (newPetId != null) uploadPhotoNonFatal(newPetId, photoBytes)
    }

    private suspend fun uploadPhotoNonFatal(petId: String, photoBytes: ByteArray?) {
        if (photoBytes == null) return
        try {
            petsRepository.uploadProfilePhoto(petId, photoBytes)
        } catch (e: Exception) {
            Timber.w(e, "pet photo upload failed (non-fatal)")
        }
    }

    private fun computeDateOfBirth(years: String, months: String): String? {
        val y = years.toIntOrNull() ?: 0
        val m = months.toIntOrNull() ?: 0
        if (y <= 0 && m <= 0) return null
        val cal = Calendar.getInstance()
        cal.add(Calendar.YEAR, -y)
        cal.add(Calendar.MONTH, -m)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }
}
