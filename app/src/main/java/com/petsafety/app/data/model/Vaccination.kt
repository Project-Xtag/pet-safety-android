package com.petsafety.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * A single vaccination record (Stage B, §1.6). Wire shape mirrors the locked
 * A.3 contract; `vaccine_code` is opaque (never parsed) and immutable on edit.
 *
 * Date-only fields (`administered_at`, `expires_at`) stay as `String` and are
 * parsed through a fixed UTC calendar in [status] — same decision as iOS — so
 * a date-only value never day-shifts across the device timezone.
 */
@Serializable
data class Vaccination(
    val id: String,
    @SerialName("pet_id") val petId: String,
    @SerialName("vaccine_code") val vaccineCode: String,
    @SerialName("vaccine_name_snapshot") val vaccineNameSnapshot: String,
    @SerialName("administered_at") val administeredAt: String,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("batch_number") val batchNumber: String? = null,
    @SerialName("vet_name") val vetName: String? = null,
    @SerialName("vet_clinic") val vetClinic: String? = null,
    @SerialName("certificate_url") val certificateUrl: String? = null,
    @SerialName("certificate_mime") val certificateMime: String? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    // CURRENT catalog mandatory flag (COALESCE, false if code absent) — drives the
    // "Kötelező" pill. Round-trips Room (see VaccinationEntity + OfflineDataManager).
    @SerialName("is_mandatory") val isMandatory: Boolean = false
) {
    /**
     * Client-derived status for the per-pet CRUD list, where the server does
     * NOT send a status field. Computed date-only in UTC to line up exactly
     * with the server's signed `days_until_expiry` (no timezone day-shift).
     *
     * Locked boundary (must match the server so the pill agrees across the
     * per-pet list and the home summary):
     *   null or ≥30 days → VALID · 0–29 days → EXPIRING · <0 days → EXPIRED
     * i.e. strict `< 30`, mirroring the server's
     * `expires_at < CURRENT_DATE + INTERVAL '30 days'`. (The iOS reference uses
     * `<= 30` — a latent off-by-one at day 30; flagged for iOS to reconcile.)
     *
     * Do NOT use this for the home summary — its `urgent[]` already carries an
     * authoritative server `status`; consume that verbatim (see [UrgentVaccination]).
     */
    val status: VaccinationStatus
        get() {
            val exp = expiresAt
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?: return VaccinationStatus.VALID
            val today = LocalDate.now(ZoneOffset.UTC)
            val days = ChronoUnit.DAYS.between(today, exp)
            return when {
                days < 0 -> VaccinationStatus.EXPIRED
                days < 30 -> VaccinationStatus.EXPIRING
                else -> VaccinationStatus.VALID
            }
        }
}

/** The one status vocabulary the whole feature renders against (single pill mapping). */
enum class VaccinationStatus { VALID, EXPIRING, EXPIRED }
