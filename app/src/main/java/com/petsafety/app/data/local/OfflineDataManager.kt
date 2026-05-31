package com.petsafety.app.data.local

import com.petsafety.app.data.local.entity.AlertEntity
import com.petsafety.app.data.local.entity.ActionQueueEntity
import com.petsafety.app.data.local.entity.PetEntity
import com.petsafety.app.data.local.entity.SuccessStoryEntity
import com.petsafety.app.data.local.entity.VaccinationEntity
import com.petsafety.app.data.model.MissingPetAlert
import com.petsafety.app.data.model.Pet
import com.petsafety.app.data.model.SuccessStory
import com.petsafety.app.data.model.Vaccination
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID

class OfflineDataManager(private val database: AppDatabase) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun savePet(pet: Pet) {
        database.petDao().upsert(
            PetEntity(
                id = pet.id,
                ownerId = pet.ownerId,
                name = pet.name,
                species = pet.species,
                breed = pet.breed,
                color = pet.color,
                weight = pet.weight,
                microchipNumber = pet.microchipNumber,
                medicalNotes = pet.medicalNotes,
                notes = pet.notes,
                profileImage = pet.profileImage,
                isMissing = pet.isMissing,
                createdAt = pet.createdAt ?: "",
                updatedAt = pet.updatedAt ?: "",
                ageYears = pet.ageYears,
                ageMonths = pet.ageMonths,
                ageText = pet.ageText,
                ageIsApproximate = pet.ageIsApproximate,
                allergies = pet.allergies,
                medications = pet.medications,
                uniqueFeatures = pet.uniqueFeatures,
                sex = pet.sex,
                isNeutered = pet.isNeutered,
                qrCode = pet.qrCode,
                dateOfBirth = pet.dateOfBirth,
                ownerName = pet.ownerName,
                ownerPhone = pet.ownerPhone,
                ownerSecondaryPhone = pet.ownerSecondaryPhone,
                ownerEmail = pet.ownerEmail,
                ownerSecondaryEmail = pet.ownerSecondaryEmail,
                ownerAddress = pet.ownerAddress,
                ownerAddressLine2 = pet.ownerAddressLine2,
                ownerCity = pet.ownerCity,
                ownerPostalCode = pet.ownerPostalCode,
                ownerCountry = pet.ownerCountry,
                lastSyncedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun savePets(pets: List<Pet>) {
        pets.forEach { savePet(it) }
    }

    suspend fun fetchPets(): List<Pet> =
        database.petDao().getAll().map { entity ->
            Pet(
                id = entity.id,
                ownerId = entity.ownerId,
                name = entity.name,
                species = entity.species,
                breed = entity.breed,
                color = entity.color,
                weight = entity.weight,
                microchipNumber = entity.microchipNumber,
                medicalNotes = entity.medicalNotes,
                notes = entity.notes,
                profileImageField = entity.profileImage,
                isMissing = entity.isMissing,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                ageYears = entity.ageYears,
                ageMonths = entity.ageMonths,
                ageText = entity.ageText,
                ageIsApproximate = entity.ageIsApproximate,
                allergies = entity.allergies,
                medications = entity.medications,
                uniqueFeatures = entity.uniqueFeatures,
                sex = entity.sex,
                isNeutered = entity.isNeutered,
                qrCode = entity.qrCode,
                dateOfBirth = entity.dateOfBirth,
                ownerName = entity.ownerName,
                ownerPhone = entity.ownerPhone,
                ownerSecondaryPhone = entity.ownerSecondaryPhone,
                ownerEmail = entity.ownerEmail,
                ownerSecondaryEmail = entity.ownerSecondaryEmail,
                ownerAddress = entity.ownerAddress,
                ownerAddressLine2 = entity.ownerAddressLine2,
                ownerCity = entity.ownerCity,
                ownerPostalCode = entity.ownerPostalCode,
                ownerCountry = entity.ownerCountry
            )
        }

    suspend fun saveAlert(alert: MissingPetAlert) {
        database.alertDao().upsert(
            AlertEntity(
                id = alert.id,
                petId = alert.petId ?: "",
                userId = alert.userId ?: "",
                status = alert.status,
                lastSeenLocation = alert.resolvedLastSeenLocation,
                lastSeenLatitude = alert.resolvedLatitude,
                lastSeenLongitude = alert.resolvedLongitude,
                additionalInfo = alert.additionalInfo ?: alert.legacyDescription,
                createdAt = alert.createdAt ?: "",
                updatedAt = alert.updatedAt ?: "",
                lastSyncedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun fetchAlerts(): List<MissingPetAlert> =
        database.alertDao().getAll().map { entity ->
            MissingPetAlert(
                id = entity.id,
                petId = entity.petId,
                userId = entity.userId,
                status = entity.status,
                lastSeenLocation = entity.lastSeenLocation,
                lastSeenLatitude = entity.lastSeenLatitude,
                lastSeenLongitude = entity.lastSeenLongitude,
                additionalInfo = entity.additionalInfo,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }

    suspend fun deleteAlert(id: String) {
        database.alertDao().deleteById(id)
    }

    suspend fun saveSuccessStory(story: SuccessStory) {
        database.successStoryDao().upsert(
            SuccessStoryEntity(
                id = story.id,
                alertId = story.alertId,
                petId = story.petId,
                ownerId = story.ownerId,
                reunionCity = story.reunionCity,
                reunionLatitude = story.resolvedLatitude,
                reunionLongitude = story.resolvedLongitude,
                storyText = story.storyText,
                isPublic = story.isPublic,
                isConfirmed = story.isConfirmed,
                missingSince = story.missingSince,
                foundAt = story.foundAt,
                createdAt = story.createdAt,
                updatedAt = story.updatedAt,
                deletedAt = story.deletedAt,
                lastSyncedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun fetchSuccessStories(): List<SuccessStory> =
        database.successStoryDao().getAllPublic().map { entity ->
            SuccessStory(
                id = entity.id,
                alertId = entity.alertId,
                petId = entity.petId,
                ownerId = entity.ownerId,
                reunionCity = entity.reunionCity,
                reunionLatitude = entity.reunionLatitude,
                reunionLongitude = entity.reunionLongitude,
                storyText = entity.storyText,
                isPublic = entity.isPublic,
                isConfirmed = entity.isConfirmed,
                missingSince = entity.missingSince,
                foundAt = entity.foundAt,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                deletedAt = entity.deletedAt
            )
        }

    /**
     * Replace the cached vaccination set for one pet. We delete-then-insert
     * (rather than upsert-only) so a record deleted server-side doesn't linger
     * in the cache after a successful online refresh.
     */
    suspend fun saveVaccinations(petId: String, vaccinations: List<Vaccination>) {
        database.vaccinationDao().deleteForPet(petId)
        vaccinations.forEach { v ->
            database.vaccinationDao().upsert(
                VaccinationEntity(
                    id = v.id,
                    petId = v.petId,
                    vaccineCode = v.vaccineCode,
                    vaccineNameSnapshot = v.vaccineNameSnapshot,
                    administeredAt = v.administeredAt,
                    expiresAt = v.expiresAt,
                    batchNumber = v.batchNumber,
                    vetName = v.vetName,
                    vetClinic = v.vetClinic,
                    certificateUrl = v.certificateUrl,
                    certificateMime = v.certificateMime,
                    notes = v.notes,
                    createdAt = v.createdAt,
                    lastSyncedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun fetchVaccinations(petId: String): List<Vaccination> =
        database.vaccinationDao().getForPet(petId).map { e ->
            Vaccination(
                id = e.id,
                petId = e.petId,
                vaccineCode = e.vaccineCode,
                vaccineNameSnapshot = e.vaccineNameSnapshot,
                administeredAt = e.administeredAt,
                expiresAt = e.expiresAt,
                batchNumber = e.batchNumber,
                vetName = e.vetName,
                vetClinic = e.vetClinic,
                certificateUrl = e.certificateUrl,
                certificateMime = e.certificateMime,
                notes = e.notes,
                createdAt = e.createdAt
            )
        }

    /**
     * Action queue cleanup policy (audit #183):
     *
     *  * `completeAction(id)` — drops the row outright. Successful syncs leave
     *    no audit trail; the matching server state IS the record.
     *
     *  * `failAction(id, error)` — increments retryCount. After 5 failures
     *    the row is dropped (we treat 5 retries × server-side enforced
     *    backoff as "permanent failure"; further retries would just spam
     *    the server with bad payloads). Until then it stays at status='failed'
     *    and is picked up on the next sync window.
     *
     *  * `clearAll()` — wipes every queued action; called on logout +
     *    account deletion so pending offline edits don't follow a logged-out
     *    user back into a different account.
     *
     *  * No time-based expiration. Actions queued offline (a sighting
     *    reported on a hike, a pet-found toggle in airplane mode) stay
     *    queued until the device next syncs successfully or fails 5x.
     *    A multi-week stale entry is preserved on purpose — that user's
     *    intent shouldn't expire just because their phone was off.
     *
     * If you change this policy, also update the comment in
     * `data/local/dao/ActionQueueDao.kt` and check that
     * `data/sync/SyncManager` still treats status='failed' rows as
     * eligible for the next sync attempt.
     */
    suspend fun queueAction(type: String, data: Map<String, Any?>): String {
        val id = UUID.randomUUID().toString()
        val jsonData = JsonObject(data.mapValues { toJsonElement(it.value) }).toString()
        database.actionQueueDao().insert(
            ActionQueueEntity(
                id = id,
                actionType = type,
                actionDataJson = jsonData,
                createdAt = System.currentTimeMillis(),
                status = "pending",
                retryCount = 0,
                errorMessage = null
            )
        )
        return id
    }

    private fun toJsonElement(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is String -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is Int -> JsonPrimitive(value)
        is Long -> JsonPrimitive(value)
        is Double -> JsonPrimitive(value)
        is Float -> JsonPrimitive(value)
        else -> JsonPrimitive(value.toString())
    }

    suspend fun getPendingActions(): List<ActionQueueEntity> =
        database.actionQueueDao().getPending()

    suspend fun getPendingCount(): Int =
        database.actionQueueDao().countPending()

    suspend fun completeAction(id: String) {
        database.actionQueueDao().deleteById(id)
    }

    suspend fun failAction(id: String, errorMessage: String, incrementRetry: Boolean = true) {
        val existing = database.actionQueueDao().getById(id) ?: return
        val retryCount = if (incrementRetry) existing.retryCount + 1 else existing.retryCount
        if (retryCount >= 5) {
            database.actionQueueDao().deleteById(id)
        } else {
            database.actionQueueDao().insert(
                existing.copy(
                    status = "failed",
                    retryCount = retryCount,
                    errorMessage = errorMessage
                )
            )
        }
    }

    /**
     * Wipe every cached row and every queued offline action.
     *
     * Called from AuthRepository.logout() + deleteAccount() so the user's
     * pets, alerts, stories, and pending offline edits don't persist on
     * this device after sign-out. Room is SQLCipher-encrypted, but the
     * key lives on-device — GDPR right-to-erasure expects the PII itself
     * gone, not "encrypted until someone extracts the key". Queued
     * actions are particularly sensitive since they can contain owner
     * phone / email / address from an unsynced edit.
     */
    suspend fun clearAllData() {
        database.actionQueueDao().deleteAll()
        database.alertDao().deleteAll()
        database.successStoryDao().deleteAll()
        database.vaccinationDao().deleteAll()
        database.petDao().deleteAll()
    }
}
