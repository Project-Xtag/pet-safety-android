package com.petsafety.app.data.vaccination

import com.petsafety.app.data.model.UrgentVaccination
import com.petsafety.app.data.model.Vaccination
import com.petsafety.app.data.model.VaccinationHomeSummary
import com.petsafety.app.data.model.VaccinationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Pure-logic coverage for the load-bearing vaccination decisions: the gate
 * reducer, the home-card predicate, and the client-side status derivation.
 * The async gate lifecycle (job-cancel, auth-id keying) is covered by the
 * device two-account litmus, not here.
 */
class VaccinationGateTest {

    private fun summary(total: Int, urgent: List<UrgentVaccination> = emptyList()) =
        VaccinationHomeSummary(totalPetsWithVaccinations = total, urgent = urgent)

    // ---- reduce(): the locked availability semantics ----

    @Test
    fun `reduce - 200 available always becomes On regardless of prior`() {
        val s = summary(total = 2)
        assertEquals(VaccinationAvailability.On(s), reduce(ResolveOutcome.Available(s), VaccinationAvailability.Unknown))
        assertEquals(VaccinationAvailability.On(s), reduce(ResolveOutcome.Available(s), VaccinationAvailability.Off))
        assertEquals(VaccinationAvailability.On(s), reduce(ResolveOutcome.Available(s), VaccinationAvailability.On(summary(total = 9))))
    }

    @Test
    fun `reduce - 404 NotFound is definitive Off and overrides a prior On`() {
        assertEquals(VaccinationAvailability.Off, reduce(ResolveOutcome.NotFound, VaccinationAvailability.Unknown))
        assertEquals(VaccinationAvailability.Off, reduce(ResolveOutcome.NotFound, VaccinationAvailability.On(summary(total = 3))))
    }

    @Test
    fun `reduce - Transient preserves a prior On (offline-first)`() {
        val on = VaccinationAvailability.On(summary(total = 1))
        assertEquals(on, reduce(ResolveOutcome.Transient, on))
    }

    @Test
    fun `reduce - Transient falls back to Unknown when not already On`() {
        assertEquals(VaccinationAvailability.Unknown, reduce(ResolveOutcome.Transient, VaccinationAvailability.Unknown))
        assertEquals(VaccinationAvailability.Unknown, reduce(ResolveOutcome.Transient, VaccinationAvailability.Off))
    }

    // ---- computeShowsHomeCard(): on AND has records, keyed off total (not urgent) ----

    @Test
    fun `showsHomeCard - true only when On with records`() {
        assertTrue(computeShowsHomeCard(VaccinationAvailability.On(summary(total = 1))))
        // All-valid (records, none urgent) still shows the reassurance card.
        assertTrue(computeShowsHomeCard(VaccinationAvailability.On(summary(total = 4, urgent = emptyList()))))
    }

    @Test
    fun `showsHomeCard - false for on-but-empty, off, and unknown`() {
        assertFalse(computeShowsHomeCard(VaccinationAvailability.On(summary(total = 0))))
        assertFalse(computeShowsHomeCard(VaccinationAvailability.Off))
        assertFalse(computeShowsHomeCard(VaccinationAvailability.Unknown))
    }

    @Test
    fun `isOn - true only for On`() {
        assertTrue(VaccinationAvailability.On(summary(total = 1)).isOn)
        assertFalse(VaccinationAvailability.Off.isOn)
        assertFalse(VaccinationAvailability.Unknown.isOn)
    }

    // ---- Vaccination.status: client-side, date-only, UTC ----

    private fun isoUtcDaysFromToday(days: Long): String =
        LocalDate.now(ZoneOffset.UTC).plusDays(days).format(DateTimeFormatter.ISO_LOCAL_DATE)

    private fun vaccination(expiresAt: String?) = Vaccination(
        id = "v1", petId = "p1", vaccineCode = "rabies", vaccineNameSnapshot = "Rabies",
        administeredAt = "2024-01-01", expiresAt = expiresAt
    )

    @Test
    fun `status - null expiry is valid`() {
        assertEquals(VaccinationStatus.VALID, vaccination(null).status)
    }

    @Test
    fun `status - locked boundary, less-than-30 expiring (matches server)`() {
        assertEquals(VaccinationStatus.VALID, vaccination(isoUtcDaysFromToday(120)).status)
        assertEquals(VaccinationStatus.VALID, vaccination(isoUtcDaysFromToday(30)).status)   // day 30 = valid (>= 30)
        assertEquals(VaccinationStatus.EXPIRING, vaccination(isoUtcDaysFromToday(29)).status) // day 29 = expiring (last expiring day)
        assertEquals(VaccinationStatus.EXPIRING, vaccination(isoUtcDaysFromToday(10)).status)
        assertEquals(VaccinationStatus.EXPIRING, vaccination(isoUtcDaysFromToday(0)).status)  // expires today
        assertEquals(VaccinationStatus.EXPIRED, vaccination(isoUtcDaysFromToday(-1)).status)
    }

    @Test
    fun `urgent statusEnum maps server strings onto the shared vocabulary`() {
        val expired = UrgentVaccination(petId = "p", petName = "Bela", vaccinationId = "v", vaccineName = "Rabies", status = "expired")
        val expiring = expired.copy(status = "expiring")
        assertEquals(VaccinationStatus.EXPIRED, expired.statusEnum)
        assertEquals(VaccinationStatus.EXPIRING, expiring.statusEnum)
    }
}
