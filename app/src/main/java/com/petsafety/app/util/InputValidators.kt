package com.petsafety.app.util

import android.util.Patterns

object InputValidators {

    // MARK: - Email (Android Patterns + basic structural check)

    fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return false
        if (trimmed.length > MAX_EMAIL) return false
        return Patterns.EMAIL_ADDRESS.matcher(trimmed).matches() &&
            trimmed.contains(".") &&
            !trimmed.endsWith(".")
    }

    // MARK: - Phone (E.164: optional +, 7-15 digits)

    fun isValidPhone(phone: String): Boolean {
        val stripped = phone.trim()
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
        if (stripped.isEmpty()) return false
        val digits = stripped.filter { it.isDigit() }
        return stripped.matches(Regex("^\\+?[0-9]{7,15}$")) && digits.length >= 7
    }

    // MARK: - Microchip (ISO 11784/11785: 9-17 digits)

    fun isValidMicrochip(chip: String): Boolean {
        val trimmed = chip.trim()
        if (trimmed.isEmpty()) return true // optional field
        return trimmed.length in 9..17 && trimmed.all { it.isDigit() }
    }

    // MARK: - OTP (exactly 6 digits)

    fun isValidOTP(code: String): Boolean {
        val trimmed = code.trim()
        return trimmed.length == 6 && trimmed.all { it.isDigit() }
    }

    // MARK: - Weight (positive, max 500 kg)

    fun isValidWeight(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return true // optional
        val value = trimmed.toDoubleOrNull() ?: return false
        return value > 0 && value <= 500
    }

    // MARK: - Reward amount (positive number)

    fun isValidRewardAmount(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return true // optional
        val value = trimmed.toDoubleOrNull() ?: return false
        return value > 0 && value <= 1_000_000
    }

    // MARK: - Coordinates

    fun isValidLatitude(lat: Double): Boolean = lat in -90.0..90.0

    fun isValidLongitude(lng: Double): Boolean = lng in -180.0..180.0

    fun isValidCoordinate(latitude: Double, longitude: Double): Boolean =
        isValidLatitude(latitude) && isValidLongitude(longitude) &&
            !(latitude == 0.0 && longitude == 0.0) // reject null island

    // MARK: - Person name (non-empty, reasonable length)

    fun isValidName(name: String): Boolean {
        val trimmed = name.trim()
        return trimmed.isNotEmpty() && trimmed.length <= MAX_PERSON_NAME
    }

    // MARK: - Text length limits

    const val MAX_PET_NAME = 100
    const val MAX_BREED = 100
    const val MAX_COLOR = 100
    const val MAX_MICROCHIP = 17
    const val MAX_MEDICAL_NOTES = 2000
    const val MAX_NOTES = 2000
    const val MAX_UNIQUE_FEATURES = 1000
    const val MAX_ALLERGIES = 1000
    const val MAX_MEDICATIONS = 1000
    const val MAX_ALERT_DESCRIPTION = 2000
    const val MAX_REWARD_AMOUNT = 20
    const val MAX_LOCATION_TEXT = 500
    const val MAX_POSTAL_CODE = 20
    const val MAX_PERSON_NAME = 100
    const val MAX_EMAIL = 254
    const val MAX_PHONE = 20

    fun isWithinLimit(text: String, maxLength: Int): Boolean = text.length <= maxLength

    // MARK: - Locale-aware name ordering

    /**
     * Format display name in culturally appropriate order.
     * Hungarian, Japanese, Chinese, Korean use family-name-first order.
     */
    fun formatDisplayName(firstName: String?, lastName: String?, locale: String? = null): String {
        val first = firstName?.trim().orEmpty()
        val last = lastName?.trim().orEmpty()

        if (first.isEmpty() && last.isEmpty()) return ""
        if (first.isEmpty()) return last
        if (last.isEmpty()) return first

        val lang = (locale ?: java.util.Locale.getDefault().language).lowercase().take(2)
        val familyFirst = setOf("hu", "ja", "zh", "ko")

        return if (lang in familyFirst) "$last $first" else "$first $last"
    }
}
