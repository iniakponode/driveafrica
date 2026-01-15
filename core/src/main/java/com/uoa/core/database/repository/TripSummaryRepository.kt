package com.uoa.core.database.repository

import com.uoa.core.model.TripSummary
import java.util.Date
import java.util.UUID

interface TripSummaryRepository {
    suspend fun insertTripSummary(tripSummary: TripSummary)

    suspend fun getTripSummaryByTripId(tripId: UUID): TripSummary?

    suspend fun getTripSummariesByDriverAndDateRange(
        driverId: UUID,
        startDate: Date,
        endDate: Date
    ): List<TripSummary>

    suspend fun countTripSummariesByDriverAndDateRange(
        driverId: UUID,
        startDate: Date,
        endDate: Date
    ): Int
}
