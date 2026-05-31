package com.petsafety.app.data.vaccination

import com.petsafety.app.data.model.VaccinationHomeSummary
import com.petsafety.app.data.network.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-scoped, summary-anchored single source of truth for whether the
 * vaccination feature is available to the current user, plus (when on) the
 * home-summary payload that the home card renders.
 *
 * The home-summary endpoint is the ONLY availability signal:
 *  - 404            → [VaccinationAvailability.Off]  (definitive; overrides a prior On)
 *  - 200            → [VaccinationAvailability.On]    (even with all-zero counts = on-but-empty)
 *  - 5xx/transient/offline → preserve a prior On, else [VaccinationAvailability.Unknown]
 *
 * No other call ever touches this reducer. A per-pet CRUD/list 404 is a
 * genuine not-found handled by the per-pet ViewModel and is NEVER read back as
 * feature availability.
 *
 * The reducer ([reduce]) and the home-card predicate ([computeShowsHomeCard])
 * are pure and unit-tested; the async lifecycle below (job-cancel, auth keying)
 * is verified by the device two-account litmus.
 */
@Singleton
class VaccinationGate @Inject constructor(
    private val apiService: ApiService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _availability = MutableStateFlow<VaccinationAvailability>(VaccinationAvailability.Unknown)
    val availability: StateFlow<VaccinationAvailability> = _availability.asStateFlow()

    /**
     * Gate for the HOME card only: shown when the feature is on AND the user
     * has at least one pet with records — see [computeShowsHomeCard].
     */
    val showsHomeCard: StateFlow<Boolean> = _availability
        .map { computeShowsHomeCard(it) }
        .stateIn(scope, SharingStarted.Eagerly, false)

    private var resolveJob: Job? = null
    private var currentUserId: String? = null

    /**
     * Drive the gate off the authenticated user id (NOT a bare isAuthenticated
     * flag) so an account swap idA→idB re-resolves rather than carrying idA's
     * answer. Same id → no-op (avoids redundant fetches on recomposition).
     */
    fun onAuthUserChanged(userId: String?) {
        if (userId == currentUserId) return
        currentUserId = userId
        reset()
        if (userId != null) resolve()
    }

    /** Back to Unknown (fail-closed) and cancel any in-flight resolve. Logout / account switch. */
    fun reset() {
        resolveJob?.cancel()
        _availability.value = VaccinationAvailability.Unknown
    }

    /**
     * Re-resolve from the server — the foreground (ProcessLifecycleOwner
     * ON_START) and pull-to-refresh path. UNCONDITIONAL by design: it must NOT
     * route through [onAuthUserChanged]'s same-id no-op, because a foreground
     * for the SAME user is exactly when the flag may have flipped under a
     * stable identity (the rollout case). A 404 here can flip an established
     * On → Off.
     */
    fun refresh() = resolve()

    /**
     * Coalesces to a single in-flight resolve: if one is already running, this
     * is a no-op, so a near-simultaneous ON_START + screen re-entry + manual
     * pull collapse to one summary call. An auth change still forces a fresh
     * resolve because [reset] cancels the in-flight job first (so it's no
     * longer active when [onAuthUserChanged] calls through).
     */
    fun resolve() {
        if (resolveJob?.isActive == true) return
        resolveJob = scope.launch { resolveInternal() }
    }

    private suspend fun resolveInternal() {
        val outcome = try {
            val summary = apiService.getVaccinationSummary().data?.summary
            // 200 with a body → available; a 200 with a malformed/empty body is
            // treated as transient (don't regress a known-good On).
            if (summary != null) ResolveOutcome.Available(summary) else ResolveOutcome.Transient
        } catch (e: HttpException) {
            // 404 is the ONLY definitive off signal; other HTTP errors are transient.
            if (e.code() == 404) ResolveOutcome.NotFound else ResolveOutcome.Transient
        } catch (e: Exception) {
            // Network/timeout/offline/decode — transient.
            ResolveOutcome.Transient
        }
        _availability.value = reduce(outcome, _availability.value)
    }
}

/** Outcome of one summary resolve, decoupled from the reducer for testability. */
internal sealed interface ResolveOutcome {
    data class Available(val summary: VaccinationHomeSummary) : ResolveOutcome
    data object NotFound : ResolveOutcome
    data object Transient : ResolveOutcome
}

/**
 * Pure reducer: the locked availability semantics in one place.
 *
 *  - Available → On(summary)                 (200, overrides anything)
 *  - NotFound  → Off                          (404, definitive — overrides a prior On)
 *  - Transient → preserve a prior On, else Unknown
 *
 * The Transient → preserve-On branch is a deliberate offline-first tradeoff:
 * an already-on user keeps reading cached records when a resolve fails
 * (offline / 5xx). Cost: a user turned OFF server-side mid-session keeps the
 * surfaces until the next SUCCESSFUL resolve. Conscious line, not an accident —
 * availability should not flicker off on a dropped request.
 */
internal fun reduce(
    outcome: ResolveOutcome,
    prior: VaccinationAvailability
): VaccinationAvailability = when (outcome) {
    is ResolveOutcome.Available -> VaccinationAvailability.On(outcome.summary)
    ResolveOutcome.NotFound -> VaccinationAvailability.Off
    ResolveOutcome.Transient ->
        if (prior is VaccinationAvailability.On) prior else VaccinationAvailability.Unknown
}

/**
 * HOME-card predicate: on AND the user has records. Keyed purely off the total,
 * NOT off `urgent.isEmpty()` — an all-valid user with records still gets the
 * reassurance card. The on-but-empty user (total == 0) hides the card but
 * keeps the pet-detail section + add CTA. Do not change this predicate.
 */
internal fun computeShowsHomeCard(availability: VaccinationAvailability): Boolean =
    availability is VaccinationAvailability.On && availability.summary.totalPetsWithVaccinations != 0

/** Tri-state availability. [Unknown] is fail-closed (every surface hidden until resolved). */
sealed interface VaccinationAvailability {
    data object Unknown : VaccinationAvailability
    data object Off : VaccinationAvailability
    data class On(val summary: VaccinationHomeSummary) : VaccinationAvailability
}

/** True only in the [VaccinationAvailability.On] state — gates the pet-detail section + every add CTA. */
val VaccinationAvailability.isOn: Boolean
    get() = this is VaccinationAvailability.On
