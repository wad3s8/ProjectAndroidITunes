package com.example.projectandroid.ui

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Locale

internal fun formatDuration(millis: Long?): String {
    if (millis == null || millis < 0) return "—"
    val seconds = millis / 1_000
    return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
}

internal fun formatPrice(price: Double?, currency: String?, locale: Locale = Locale.getDefault()): String {
    if (price == null || price < 0 || currency == null) return "—"
    return runCatching {
        NumberFormat.getCurrencyInstance(locale).apply {
            this.currency = Currency.getInstance(currency)
        }.format(price)
    }.getOrDefault("—")
}

internal fun formatReleaseDate(value: String?, locale: Locale = Locale.getDefault()): String {
    if (value == null) return "—"
    return runCatching {
        val source = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }
        val date = requireNotNull(source.parse(value.take(10)))
        SimpleDateFormat("dd MMMM yyyy", locale).format(date)
    }.getOrDefault("—")
}

