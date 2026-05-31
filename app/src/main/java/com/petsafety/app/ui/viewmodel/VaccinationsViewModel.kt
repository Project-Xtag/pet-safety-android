package com.petsafety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petsafety.app.R
import com.petsafety.app.data.model.Vaccination
import com.petsafety.app.data.repository.VaccinationRepository
import com.petsafety.app.util.StringProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Per-pet vaccination data (list for the pet-detail section + full list).
 * Gate-AGNOSTIC: it never decides feature on/off. A 404 from the repository is
 * a genuine not-found and surfaces a GENERIC message — never the server's
 * localized 404 string, and never re-interpreted as feature-off (that lives
 * solely in VaccinationGate).
 */
@HiltViewModel
class VaccinationsViewModel @Inject constructor(
    private val repository: VaccinationRepository,
    private val stringProvider: StringProvider
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val vaccinations: List<Vaccination> = emptyList(),
        val errorMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun load(petId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val list = repository.fetchVaccinations(petId)
                _uiState.value = UiState(isLoading = false, vaccinations = list)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = stringProvider.getString(R.string.vaccinations_load_failed)
                )
            }
        }
    }
}
