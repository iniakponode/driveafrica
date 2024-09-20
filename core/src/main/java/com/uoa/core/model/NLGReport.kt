package com.uoa.core.model

data class NLGReport(
    val id: Int,
    val userId: String,
    val reportText: String,
    val dateRange: String,
    val synced: Boolean,
)
