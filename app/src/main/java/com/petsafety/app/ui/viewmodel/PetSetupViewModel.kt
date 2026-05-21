package com.petsafety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petsafety.app.data.events.PetsEventBus
import com.petsafety.app.data.model.UnactivatedOrderItem
import com.petsafety.app.data.network.model.CreatePetRequest
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
 * Holds the draft for the step-by-step pet-onboarding wizard. The draft
 * is kept in memory and committed in one go after the last detail step
 * (create pet -> upload photo -> activate the scanned tag).
 */
data class PetSetupUiState(
    val qrCode: String = "",
    val loading: Boolean = true,
    val orderItems: List<UnactivatedOrderItem> = emptyList(),
    val step: Int = 1,
    val committing: Boolean = false,
    val committedPetId: String? = null,
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
    val remainingAfterThis: Int get() = (orderItems.size - 1).coerceAtLeast(0)
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

    fun start(qrCode: String) {
        if (started) return
        started = true
        _ui.update { it.copy(qrCode = qrCode) }
        viewModelScope.launch {
            val items = try {
                ordersRepository.getUnactivatedTagsForQRCode(qrCode)
            } catch (e: Exception) {
                Timber.w(e, "getUnactivatedTagsForQRCode failed for %s", qrCode)
                emptyList()
            }
            val names = items.mapNotNull { it.petName }
            _ui.update {
                it.copy(
                    loading = false,
                    orderItems = items,
                    petName = if (names.size == 1) names.first() else it.petName,
                )
            }
        }
    }

    fun edit(transform: (PetSetupUiState) -> PetSetupUiState) = _ui.update(transform)

    fun goToStep(step: Int) = _ui.update { it.copy(step = step, error = null) }

    /** Create the pet, upload the optional photo, then activate the tag. */
    fun commit(photoBytes: ByteArray?) {
        viewModelScope.launch {
            _ui.update { it.copy(committing = true, error = null) }
            val s = _ui.value
            try {
                var petId = s.committedPetId
                if (petId == null) {
                    val dob = computeDateOfBirth(s.ageYears, s.ageMonths)
                    val request = CreatePetRequest(
                        name = s.petName.trim(),
                        species = s.species,
                        breed = s.breed.trim().ifBlank { null },
                        color = s.color.trim().ifBlank { null },
                        allergies = s.allergies.trim().ifBlank { null },
                        medications = s.medications.trim().ifBlank { null },
                        uniqueFeatures = s.uniqueFeatures.trim().ifBlank { null },
                        sex = s.sex.takeIf { it.isNotBlank() && it != "unknown" },
                        dateOfBirth = dob,
                        dobIsApproximate = if (dob != null) true else null,
                    )
                    val pet = petsRepository.createPet(request)
                    petId = pet.id
                    _ui.update { it.copy(committedPetId = pet.id) }
                    if (photoBytes != null) {
                        try {
                            petsRepository.uploadProfilePhoto(pet.id, photoBytes)
                        } catch (e: Exception) {
                            Timber.w(e, "pet photo upload failed (non-fatal)")
                        }
                    }
                }
                qrRepository.activateTag(s.qrCode, petId)
                petsEventBus.requestRefresh()
                _ui.update {
                    it.copy(committing = false, step = if (it.remainingAfterThis > 0) 11 else 12)
                }
            } catch (e: Exception) {
                Timber.w(e, "pet-setup commit failed")
                _ui.update { it.copy(committing = false, error = e.localizedMessage) }
            }
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
