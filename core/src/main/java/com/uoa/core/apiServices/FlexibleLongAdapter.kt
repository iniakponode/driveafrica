package com.uoa.core.apiServices

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class FlexibleLongAdapter : JsonDeserializer<Long?>, JsonSerializer<Long?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Long? {
        if (json == null || json.isJsonNull) return null
        val primitive = json.asJsonPrimitive
        if (primitive.isNumber) return primitive.asLong
        if (primitive.isString) {
            val raw = primitive.asString.trim()
            if (raw.isEmpty()) return null
            raw.toLongOrNull()?.let { return it }
            try {
                return Instant.parse(raw).toEpochMilli()
            } catch (_: Exception) {
            }
            try {
                return OffsetDateTime.parse(raw).toInstant().toEpochMilli()
            } catch (_: Exception) {
            }
            try {
                return LocalDateTime.parse(raw, DateTimeFormatter.ISO_DATE_TIME)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()
            } catch (_: Exception) {
            }
        }
        return null
    }

    override fun serialize(
        src: Long?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return if (src == null) JsonNull.INSTANCE else JsonPrimitive(src)
    }
}
