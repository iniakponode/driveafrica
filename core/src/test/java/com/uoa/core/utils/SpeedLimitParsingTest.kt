package com.uoa.core.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedLimitParsingTest {

    @Test
    fun parsesMphSpeedLimitToKmh() {
        assertEquals(80, parseSpeedLimit("50 mph"))
    }

    @Test
    fun parsesKmhSpeedLimit() {
        assertEquals(120, parseSpeedLimit("120 km/h"))
    }

    @Test
    fun parsesNumericSpeedLimitAsKmh() {
        assertEquals(80, parseSpeedLimit("80"))
    }

    private fun parseSpeedLimit(raw: String?): Int? {
        val clazz = Class.forName("com.uoa.core.utils.UtilFunctionsKt")
        val method = clazz.getDeclaredMethod("parseSpeedLimitKmh", String::class.java)
        method.isAccessible = true
        return method.invoke(null, raw) as Int?
    }
}
