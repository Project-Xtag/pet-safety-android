package com.petsafety.app.ui.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Locale-aware formatting utilities for dates, times, and currencies.
 *
 * All formatting respects the device locale (Locale.getDefault()) so that
 * dates, times, and currency values render correctly for every supported
 * language (en, hu, sk, cs, de, es, pt, ro).
 *
 * Usage:
 *   LocaleFormatting.formatDate(instant)            // "4 Feb 2026" (en-GB) / "2026. febr. 4." (hu)
 *   LocaleFormatting.formatDate("2026-02-04T10:30:00Z")
 *   LocaleFormatting.formatCurrency(9.99)           // "£9.99" (en-GB) / "9,99 GBP" (hu)
 *   LocaleFormatting.formatDateTime(instant)        // "4 Feb 2026, 10:30" (en-GB)
 *
 * See LOCALIZATION_CONVENTION.md for the full cross-platform formatting rules.
 */
object LocaleFormatting {

    /**
     * Formats an [Instant] as a locale-aware date string.
     *
     * @param instant  The point in time to format.
     * @param style    The date style (SHORT, MEDIUM, LONG, FULL). Defaults to MEDIUM.
     * @return A date string formatted for the device locale and system time zone.
     */
    fun formatDate(instant: Instant, style: FormatStyle = FormatStyle.MEDIUM): String {
        val formatter = DateTimeFormatter.ofLocalizedDate(style)
            .withLocale(Locale.getDefault())
        return instant.atZone(ZoneId.systemDefault()).format(formatter)
    }

    /**
     * Parses an ISO-8601 date-time string and formats it as a locale-aware date.
     *
     * Accepts strings such as "2026-02-04T10:30:00Z" or "2026-02-04T10:30:00.000Z".
     * If parsing fails the raw [dateString] is returned as a fallback.
     *
     * @param dateString An ISO-8601 date-time string.
     * @param style      The date style. Defaults to MEDIUM.
     * @return A locale-formatted date string, or the original string on parse failure.
     */
    fun formatDate(dateString: String, style: FormatStyle = FormatStyle.MEDIUM): String {
        return try {
            val instant = Instant.parse(dateString)
            formatDate(instant, style)
        } catch (e: Exception) {
            // The backend sometimes omits the trailing Z; try appending it.
            try {
                val instant = Instant.parse(dateString.take(19) + "Z")
                formatDate(instant, style)
            } catch (_: Exception) {
                dateString // ultimate fallback: return raw string
            }
        }
    }

    /**
     * Formats a [LocalDate] as a locale-aware date string.
     *
     * @param date  The local date to format.
     * @param style The date style. Defaults to MEDIUM.
     * @return A date string formatted for the device locale.
     */
    fun formatDate(date: LocalDate, style: FormatStyle = FormatStyle.MEDIUM): String {
        val formatter = DateTimeFormatter.ofLocalizedDate(style)
            .withLocale(Locale.getDefault())
        return date.format(formatter)
    }

    /**
     * Formats a monetary amount using the device locale and the given currency code.
     *
     * The locale controls grouping separators, decimal separators, and symbol
     * placement while the [currencyCode] determines the symbol/ISO code shown.
     *
     * @param amount       The numeric amount.
     * @param currencyCode An ISO 4217 currency code (e.g. "GBP", "EUR", "USD").
     *                     Defaults to "GBP" which is the app's primary currency.
     * @return A formatted currency string such as "£9.99" or "9,99 GBP".
     */
    fun formatCurrency(amount: Double, currencyCode: String = "GBP"): String {
        val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
        format.currency = Currency.getInstance(currencyCode)
        return format.format(amount)
    }

    /**
     * Formats an [Instant] as a locale-aware date **and** time string.
     *
     * The date portion uses [FormatStyle.MEDIUM] and the time portion uses
     * [FormatStyle.SHORT] (hours and minutes only, no seconds).
     *
     * @param instant The point in time to format.
     * @return A date-time string formatted for the device locale and system time zone.
     */
    fun formatDateTime(instant: Instant): String {
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
        return instant.atZone(ZoneId.systemDefault()).format(formatter)
    }

    /**
     * Parses an ISO-8601 date-time string and formats it as a locale-aware date and time.
     *
     * @param dateString An ISO-8601 date-time string.
     * @return A locale-formatted date-time string, or the original string on parse failure.
     */
    fun formatDateTime(dateString: String): String {
        return try {
            val instant = Instant.parse(dateString)
            formatDateTime(instant)
        } catch (e: Exception) {
            try {
                val instant = Instant.parse(dateString.take(19) + "Z")
                formatDateTime(instant)
            } catch (_: Exception) {
                dateString
            }
        }
    }

    /**
     * Formats a time-only value from an [Instant] using the device locale.
     *
     * @param instant The point in time.
     * @return A short time string such as "10:30" or "10:30 AM".
     */
    fun formatTime(instant: Instant): String {
        val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
        return instant.atZone(ZoneId.systemDefault()).format(formatter)
    }
}
