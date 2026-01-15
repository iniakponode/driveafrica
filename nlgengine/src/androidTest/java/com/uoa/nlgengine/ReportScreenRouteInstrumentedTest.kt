package com.uoa.nlgengine

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.uoa.core.Sdadb
import com.uoa.core.database.entities.DriverProfileEntity
import com.uoa.core.database.entities.NLGReportEntity
import com.uoa.core.database.entities.ReportStatisticsEntity
import com.uoa.core.database.entities.TripEntity
import com.uoa.core.database.entities.UnsafeBehaviourEntity
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.utils.DateUtils
import com.uoa.core.utils.PeriodType
import com.uoa.nlgengine.presentation.ui.ReportScreenRoute
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ReportScreenRouteInstrumentedTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<TestHiltActivity>()

    @Inject
    lateinit var database: Sdadb

    private lateinit var context: Context

    @Before
    fun setUp() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        System.setProperty("ALLOW_REPORTS_WITHOUT_KEYS", "true")
        System.setProperty("DISABLE_LARGE_IMAGES", "true")
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        runBlocking {
            database.clearAllTables()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            database.clearAllTables()
        }
    }

    @Test
    fun lastTrip_noUnsafeBehaviours_showsNoDataMessage() {
        val profileId = UUID.randomUUID()
        seedDriverProfile(profileId)
        setDriverProfileId(profileId)

        composeRule.setContent {
            val navController = TestNavHostController(context).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            ReportScreenRoute(
                navController = navController,
                periodType = PeriodType.LAST_TRIP,
                startDate = 0L,
                endDate = 0L
            )
        }

        val expected = "You have no trips recorded that matches your selection at the moment."
        waitForText(expected)
    }

    @Test
    fun today_cachedReport_showsCachedContent() {
        val profileId = UUID.randomUUID()
        val tripId = UUID.randomUUID()
        val today = LocalDate.now()
        val reportText = "Cached report for today."
        val dateMillis = DateUtils.convertToEpochMilli(today)

        seedDriverProfile(profileId)
        seedTrip(tripId, profileId, today)
        val locationId = seedLocation(today)
        seedUnsafeBehaviour(tripId, profileId, locationId, today)
        seedReportStatistics(profileId, tripId, today, today)
        seedNlgReport(profileId, tripId, today, today, reportText)
        setDriverProfileId(profileId)

        composeRule.setContent {
            val navController = TestNavHostController(context).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            ReportScreenRoute(
                navController = navController,
                periodType = PeriodType.TODAY,
                startDate = dateMillis,
                endDate = dateMillis
            )
        }

        waitForText(reportText)
    }

    @Test
    fun customPeriod_cachedReport_usesExactRange() {
        val profileId = UUID.randomUUID()
        val tripId = UUID.randomUUID()
        val today = LocalDate.now()

        val rangeOneStart = today.minusDays(3)
        val rangeOneEnd = today.minusDays(1)
        val rangeTwoStart = today.minusDays(7)
        val rangeTwoEnd = today.minusDays(5)

        val reportOne = "Cached report for range one."
        val reportTwo = "Cached report for range two."

        seedDriverProfile(profileId)
        seedTrip(tripId, profileId, today)
        val locationOneId = seedLocation(rangeOneStart)
        val locationTwoId = seedLocation(rangeTwoStart)
        seedUnsafeBehaviour(tripId, profileId, locationOneId, rangeOneStart)
        seedUnsafeBehaviour(tripId, profileId, locationTwoId, rangeTwoStart)
        seedReportStatistics(profileId, tripId, rangeOneStart, rangeOneEnd)
        seedReportStatistics(profileId, tripId, rangeTwoStart, rangeTwoEnd)
        seedNlgReport(profileId, tripId, rangeOneStart, rangeOneEnd, reportOne)
        seedNlgReport(profileId, tripId, rangeTwoStart, rangeTwoEnd, reportTwo)
        setDriverProfileId(profileId)

        val rangeOneStartMillis = DateUtils.convertToEpochMilli(rangeOneStart)
        val rangeOneEndMillis = DateUtils.convertToEpochMilli(rangeOneEnd)
        val rangeTwoStartMillis = DateUtils.convertToEpochMilli(rangeTwoStart)
        val rangeTwoEndMillis = DateUtils.convertToEpochMilli(rangeTwoEnd)

        lateinit var startDateState: androidx.compose.runtime.MutableState<Long>
        lateinit var endDateState: androidx.compose.runtime.MutableState<Long>

        composeRule.setContent {
            startDateState = remember { mutableStateOf(rangeOneStartMillis) }
            endDateState = remember { mutableStateOf(rangeOneEndMillis) }
            val navController = TestNavHostController(context).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            ReportScreenRoute(
                navController = navController,
                periodType = PeriodType.CUSTOM_PERIOD,
                startDate = startDateState.value,
                endDate = endDateState.value
            )
        }

        waitForText(reportOne)
        composeRule.onAllNodesWithText(reportTwo).assertCountEquals(0)

        composeRule.runOnUiThread {
            startDateState.value = rangeTwoStartMillis
            endDateState.value = rangeTwoEndMillis
        }

        waitForText(reportTwo)
        composeRule.onAllNodesWithText(reportOne).assertCountEquals(0)
    }

    private fun setDriverProfileId(profileId: UUID) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(DRIVER_PROFILE_ID, profileId.toString())
            .commit()
    }

    private fun seedDriverProfile(profileId: UUID) = runBlocking {
        database.driverProfileDao().insertDriverProfile(
            DriverProfileEntity(
                driverProfileId = profileId,
                email = "tester@example.com",
                sync = false
            )
        )
    }

    private fun seedTrip(tripId: UUID, profileId: UUID, date: LocalDate) = runBlocking {
        database.tripDao().insertTrip(
            TripEntity(
                id = tripId,
                driverPId = profileId,
                startDate = DateUtils.convertLocalDateToDate(date),
                endDate = DateUtils.convertLocalDateToDate(date),
                startTime = DateUtils.convertToEpochMilli(date),
                endTime = DateUtils.convertToEpochMilli(date) + 1_000L,
                influence = "none",
                sync = false
            )
        )
    }

    private fun seedLocation(date: LocalDate): UUID = runBlocking {
        val locationId = UUID.randomUUID()
        database.locationDataDao().insertLocation(
            com.uoa.core.database.entities.LocationEntity(
                id = locationId,
                latitude = 6.5244,
                longitude = 3.3792,
                timestamp = DateUtils.convertToEpochMilli(date),
                date = DateUtils.convertLocalDateToDate(date),
                altitude = 10.0,
                speed = 12.0f,
                distance = 0.5f,
                speedLimit = 50.0,
                processed = false,
                sync = false
            )
        )
        locationId
    }

    private fun seedUnsafeBehaviour(
        tripId: UUID,
        profileId: UUID,
        locationId: UUID,
        date: LocalDate
    ) = runBlocking {
        database.unsafeBehaviourDao().insertUnsafeBehaviour(
            UnsafeBehaviourEntity(
                id = UUID.randomUUID(),
                tripId = tripId,
                driverProfileId = profileId,
                locationId = locationId,
                behaviorType = "Speeding",
                severity = 1.0f,
                timestamp = DateUtils.convertToEpochMilli(date) + 500L,
                date = DateUtils.convertLocalDateToDate(date),
                updatedAt = null,
                updated = false,
                processed = false,
                sync = false
            )
        )
    }

    private fun seedReportStatistics(
        profileId: UUID,
        tripId: UUID,
        startDate: LocalDate,
        endDate: LocalDate
    ) = runBlocking {
        database.reportStatisticsDao().insertReportStatistics(
            ReportStatisticsEntity(
                id = UUID.randomUUID(),
                driverProfileId = profileId,
                tripId = tripId,
                createdDate = LocalDate.now(),
                startDate = startDate,
                endDate = endDate,
                totalIncidences = 1,
                mostFrequentUnsafeBehaviour = "Speeding",
                mostFrequentBehaviourCount = 1,
                mostFrequentBehaviourOccurrences = "[]",
                tripsPerAggregationUnit = "{}",
                incidencesPerAggregationUnit = "{}",
                incidencesPerTrip = "{}",
                tripsWithAlcoholInfluencePerAggregationUnit = "{}"
            )
        )
    }

    private fun seedNlgReport(
        profileId: UUID,
        tripId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
        reportText: String
    ) = runBlocking {
        database.nlgReportDao().insertReport(
            NLGReportEntity(
                id = UUID.randomUUID(),
                userId = profileId,
                tripId = tripId,
                reportText = reportText,
                startDate = startDate.atStartOfDay(),
                endDate = endDate.atStartOfDay(),
                createdDate = LocalDateTime.now(),
                sync = false
            )
        )
    }

    private fun waitForText(text: String, timeoutMs: Long = 15_000L) {
        composeRule.waitUntil(timeoutMs) {
            runCatching {
                composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
        composeRule.onNodeWithText(text).assertExists()
    }
}
