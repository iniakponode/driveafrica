package com.uoa.sensor.utils

import android.content.Context
import java.util.Locale
import kotlin.math.max

private val KMH_COUNTRY_CODES = setOf("NG", "CM", "TZ")

fun displaySpeedLimitKmh(context: Context, speedLimitKmh: Int): Int {
    if (speedLimitKmh <= 0) return speedLimitKmh
    val countryCode = context.resources.configuration.locales[0].country.uppercase(Locale.ROOT)
    if (!KMH_COUNTRY_CODES.contains(countryCode)) return speedLimitKmh
    return roundUpToStep(speedLimitKmh, stepFor(speedLimitKmh))
}

private fun stepFor(value: Int): Int {
    return if (value < 60) 5 else 10
}

private fun roundUpToStep(value: Int, step: Int): Int {
    val safeStep = max(1, step)
    return ((value + safeStep - 1) / safeStep) * safeStep
}
