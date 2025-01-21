package com.uoa.core.network.model.nominatim

data class OverpassResponse(
    val elements: List<Element>
) {
    data class Element(
        val id: Long,
        val tags: Map<String, String>?,
        val lat: Double?,
        val lon: Double?
    )
}

