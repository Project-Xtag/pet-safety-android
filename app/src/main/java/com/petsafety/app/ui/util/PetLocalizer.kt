package com.petsafety.app.ui.util

import android.content.Context
import com.petsafety.app.R

/**
 * Translates raw English database values (species, breed, sex) to the current device locale.
 * Falls back to the original value if no string resource exists.
 */
object PetLocalizer {

    /**
     * Maps English species name from DB → localized display string.
     */
    fun localizeSpecies(context: Context, raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val resId = when (raw.lowercase()) {
            "dog" -> R.string.species_dog
            "cat" -> R.string.species_cat
            "bird" -> R.string.species_bird
            "rabbit" -> R.string.species_rabbit
            "other" -> R.string.species_other
            else -> return raw.replaceFirstChar { it.uppercase() }
        }
        return context.getString(resId)
    }

    /**
     * Maps English breed name from DB → localized display string.
     * Tries to find a matching string resource, falls back to raw value.
     */
    fun localizeBreed(context: Context, raw: String?, species: String? = null): String {
        if (raw.isNullOrBlank()) return ""

        // Normalize to resource name format: "Domestic Shorthair" → "breed_cat_domestic_shorthair"
        val normalized = raw.lowercase()
            .replace(Regex("[\\s/]+"), "_")
            .replace(Regex("[^a-z0-9_]"), "")

        // Try species-prefixed key first
        val speciesLower = (species ?: "").lowercase()
        if (speciesLower.isNotEmpty()) {
            val prefixedName = "breed_${speciesLower}_$normalized"
            val resId = getStringResId(context, prefixedName)
            if (resId != 0) return context.getString(resId)
        }

        // Try without species prefix
        val plainName = "breed_$normalized"
        val resId = getStringResId(context, plainName)
        if (resId != 0) return context.getString(resId)

        // Special aliases
        val aliases = mapOf(
            "dsh" to "breed_cat_domestic_shorthair",
            "domestic shorthair" to "breed_cat_domestic_shorthair",
            "dlh" to "breed_cat_domestic_longhair",
            "domestic longhair" to "breed_cat_domestic_longhair",
            "cross" to "breed_mixed",
            "crossbreed" to "breed_mixed",
            "mixed" to "breed_mixed",
            "mixed breed" to "breed_mixed",
        )
        val aliasKey = aliases[raw.lowercase()]
        if (aliasKey != null) {
            val aliasResId = getStringResId(context, aliasKey)
            if (aliasResId != 0) return context.getString(aliasResId)
        }

        return raw
    }

    /**
     * Maps English sex value from DB → localized display string.
     * Sex terms are species-dependent (e.g., HU dog male = "hím", cat male = "kandúr").
     */
    fun localizeSex(context: Context, raw: String?, species: String? = null): String {
        if (raw.isNullOrBlank()) return ""
        val sexLower = raw.lowercase()

        if (sexLower == "unknown") {
            return context.getString(R.string.sex_unknown)
        }

        val speciesLower = (species ?: "").lowercase()

        // Try species-specific key first (e.g., sex_dog_male, sex_cat_female)
        if (speciesLower == "dog" || speciesLower == "cat") {
            val speciesResName = "sex_${speciesLower}_$sexLower"
            val resId = getStringResId(context, speciesResName)
            if (resId != 0) return context.getString(resId)
        }

        // Fall back to generic key (e.g., sex_male, sex_female)
        val genericResName = "sex_$sexLower"
        val resId = getStringResId(context, genericResName)
        if (resId != 0) return context.getString(resId)

        return raw.replaceFirstChar { it.uppercase() }
    }

    private fun getStringResId(context: Context, name: String): Int {
        return context.resources.getIdentifier(name, "string", context.packageName)
    }
}
