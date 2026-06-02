package com.petsafety.app.data.vaccination

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One routing channel for "open a pet's vaccination list from OUTSIDE the pet
 * detail back stack" — the home urgent-row tap AND the `VACCINATION_DUE` push
 * both feed it (and defect A's future inbox-row tap reuses it verbatim). Nothing
 * here is VACCINATION_DUE-specific.
 *
 * Convergence target = the pet's vaccination LIST (`pet_vaccinations/{petId}`),
 * mirroring iOS (which opens `VaccinationsListView`, not a specific record).
 *
 * Cold-launch safe BY CONSTRUCTION — and this is the load-bearing reason it's a
 * [MutableStateFlow], not a SharedFlow like `AppStateViewModel.navigateToAlert`.
 * A killed-app FCM tap fires the push handler before `PetsScreen`'s NavHost
 * exists, so a fire-and-forget emission would be dropped (a fresh SharedFlow
 * collector never sees a value emitted before it subscribed). A StateFlow HOLDS
 * the target and REPLAYS its current value to the late subscriber — so when
 * `PetsScreen` finally mounts and starts collecting, the pending target is still
 * there. The consumer ([com.petsafety.app.ui.screens.PetsScreen]) resolves it
 * once its pets have loaded (clearing it via [consume] — consume exactly once)
 * and no-ops gracefully if that pet is gone between the push and the tap.
 *
 * App-scoped @Singleton, surfaced (NOT folded) through `AppStateViewModel` —
 * same precedent as [VaccinationGate].
 */
@Singleton
class VaccinationDeepLinkCoordinator @Inject constructor() {

    private val _pendingPetId = MutableStateFlow<String?>(null)

    /**
     * Target pet whose vaccination list should open. Set by the push handler /
     * home tap; cleared by `PetsScreen` on consume.
     */
    val pendingPetId: StateFlow<String?> = _pendingPetId.asStateFlow()

    /** Request the deep link. Blank ids are ignored (a malformed push can't trap the UI). */
    fun request(petId: String) {
        if (petId.isNotBlank()) _pendingPetId.value = petId
    }

    /** Consume exactly once — called by the single consume point after it acts. */
    fun consume() {
        _pendingPetId.value = null
    }
}
