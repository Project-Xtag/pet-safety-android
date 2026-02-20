package com.petsafety.app.util

import com.petsafety.app.R

object LocalizedLogo {
    private val countryToDrawable = mapOf(
        "hu" to R.drawable.logo_new_hu,
        "sk" to R.drawable.logo_new_sk,
        "de" to R.drawable.logo_new_de,
        "at" to R.drawable.logo_new_de,
        "cz" to R.drawable.logo_new_cs,
        "es" to R.drawable.logo_new_es,
        "pt" to R.drawable.logo_new_pt,
        "ro" to R.drawable.logo_new_ro,
    )

    val drawableRes: Int
        get() = countryToDrawable[WebUrlHelper.countryCode] ?: R.drawable.logo_new
}
