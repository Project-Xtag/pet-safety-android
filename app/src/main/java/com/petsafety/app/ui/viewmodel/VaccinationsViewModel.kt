package com.petsafety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petsafety.app.R
import com.petsafety.app.data.model.CreateVaccinationRequest
import com.petsafety.app.data.model.UpdateVaccinationRequest
import com.petsafety.app.data.model.Vaccination
import com.petsafety.app.data.model.VaccineCatalogEntry
import com.petsafety.app.data.network.model.ErrorResponse
import com.petsafety.app.data.repository.VaccinationRepository
import com.petsafety.app.util.StringProvider
import com.petsafety.app.util.VaccinationCertificateEncoder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import retrofit2.HttpException
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

    private val _catalog = MutableStateFlow<List<VaccineCatalogEntry>>(emptyList())
    val catalog: StateFlow<List<VaccineCatalogEntry>> = _catalog.asStateFlow()

    /**
     * Fired after any successful mutation (create / cert upload). The screen
     * binds this once to `appStateViewModel.refreshVaccinationGate()` so the
     * home card + pet-detail section re-resolve — single refresh call site.
     */
    var onDidMutate: (() -> Unit)? = null

    private val errorJson = Json { ignoreUnknownKeys = true }

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

    /** A.4 catalog for the add form, species + country filtered. Empty on failure → form shows the empty-region state. */
    fun loadCatalog(species: String, country: String) {
        viewModelScope.launch {
            _catalog.value = try {
                repository.fetchCatalog(species, country)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /**
     * A.3 create. A 400 carries the server's ALREADY-LOCALIZED validation
     * message (e.g. the rabies age-floor `api.vaccination.age_too_young`) —
     * surface that verbatim. Any other failure → a generic message (never the
     * server's raw 404/5xx body).
     */
    fun create(
        petId: String,
        request: CreateVaccinationRequest,
        onResult: (success: Boolean, message: String?, created: Vaccination?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val created = repository.create(petId, request)
                onDidMutate?.invoke()
                onResult(true, null, created)
            } catch (e: HttpException) {
                val msg = if (e.code() == 400) {
                    serverMessage(e) ?: stringProvider.getString(R.string.vaccinations_save_failed)
                } else {
                    stringProvider.getString(R.string.vaccinations_save_failed)
                }
                onResult(false, msg, null)
            } catch (e: Exception) {
                onResult(false, stringProvider.getString(R.string.vaccinations_save_failed), null)
            }
        }
    }

    /**
     * A.3 update (PUT). Fires onDidMutate (gate refresh) — an edit can move the
     * expiry/status, so the home summary may change. A 400 surfaces the server's
     * localized message (e.g. rabies floor on a changed administered date).
     */
    fun update(
        petId: String,
        id: String,
        request: UpdateVaccinationRequest,
        onResult: (success: Boolean, message: String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.update(petId, id, request)
                onDidMutate?.invoke()
                onResult(true, null)
            } catch (e: HttpException) {
                val msg = if (e.code() == 400) serverMessage(e) ?: stringProvider.getString(R.string.vaccinations_save_failed)
                          else stringProvider.getString(R.string.vaccinations_save_failed)
                onResult(false, msg)
            } catch (e: Exception) {
                onResult(false, stringProvider.getString(R.string.vaccinations_save_failed))
            }
        }
    }

    /**
     * A.3 delete (soft-delete). Fires onDidMutate (gate refresh) — counts change.
     *
     * Optimistically drops the row from the shared state BEFORE the network call
     * so the detail's by-id lookup goes null and it pops immediately (no waiting
     * on the delete + re-pull round-trip — that lag was visible/confusing). Same
     * single observe-pop trigger, just fed sooner. Rolled back if the call fails.
     */
    fun delete(petId: String, id: String, onResult: (success: Boolean, message: String?) -> Unit) {
        val snapshot = _uiState.value.vaccinations
        _uiState.value = _uiState.value.copy(vaccinations = snapshot.filterNot { it.id == id })
        viewModelScope.launch {
            try {
                repository.delete(petId, id)
                onDidMutate?.invoke()
                onResult(true, null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(vaccinations = snapshot) // rollback
                onResult(false, stringProvider.getString(R.string.vaccinations_delete_failed))
            }
        }
    }

    /** A.5 cert delete. Does NOT fire onDidMutate (cert actions don't move the summary). */
    fun deleteCertificate(petId: String, id: String, onResult: (success: Boolean, message: String?) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteCertificate(petId, id)
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, stringProvider.getString(R.string.vaccinations_cert_failed))
            }
        }
    }

    /** A.5 cert upload. Caller passes the encoder output so the Part has a concrete MIME. */
    fun uploadCertificate(
        petId: String,
        vaccinationId: String,
        encoded: VaccinationCertificateEncoder.Encoded,
        onResult: (success: Boolean, message: String?, url: String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val url = repository.uploadCertificate(petId, vaccinationId, encoded)
                // Deliberately NO onDidMutate here: the locked split is that
                // create/edit/delete refresh the gate, but cert add/replace/remove
                // do NOT (a cert change doesn't move expiry/status, so the home
                // summary is unaffected). The repo's per-pet re-pull already
                // updated the local row.
                onResult(true, null, url)
            } catch (e: Exception) {
                onResult(false, stringProvider.getString(R.string.vaccinations_cert_failed), null)
            }
        }
    }

    /** Pulls the server's localized `error` string out of an HttpException body. */
    private fun serverMessage(e: HttpException): String? = runCatching {
        val body = e.response()?.errorBody()?.string().orEmpty()
        errorJson.decodeFromString(ErrorResponse.serializer(), body).error.takeIf { it.isNotBlank() }
    }.getOrNull()
}
