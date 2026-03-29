package com.petsafety.app.ui.util

import android.content.Context
import com.petsafety.app.R
import com.petsafety.app.data.model.BreedData

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

    // Cached breed lookup: locale → (lowercase english → native name)
    private var breedCacheLocale: String = ""
    private var breedCacheMap: Map<String, String> = emptyMap()

    private fun breedLookup(context: Context): Map<String, String> {
        val lang = context.resources.configuration.locales[0].language.lowercase().take(2)
        if (breedCacheLocale == lang) return breedCacheMap
        val map = mutableMapOf<String, String>()
        for (species in listOf("dog", "cat")) {
            for (breed in BreedData.breedsFor(species, context)) {
                map[breed.englishName.lowercase()] = breed.localizedName
            }
        }
        breedCacheLocale = lang
        breedCacheMap = map
        return map
    }

    /**
     * Maps English breed name from DB → localized display string.
     * Looks up the breed in the per-locale breed data.
     */
    fun localizeBreed(context: Context, raw: String?, species: String? = null): String {
        if (raw.isNullOrBlank()) return ""
        val lookup = breedLookup(context)

        lookup[raw.lowercase()]?.let { return it }

        // Special aliases for common abbreviations
        val aliases = mapOf(
            "dsh" to "european shorthair",
            "domestic shorthair" to "european shorthair",
            "dlh" to "european shorthair",
            "domestic longhair" to "european shorthair",
            "cross" to "mixed / crossbreed",
            "crossbreed" to "mixed / crossbreed",
            "mixed" to "mixed / crossbreed",
            "mixed breed" to "mixed / crossbreed",
        )
        val alias = aliases[raw.lowercase()]
        if (alias != null) {
            lookup[alias]?.let { return it }
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
