package com.uoa.safedriveafrica.ui.splashscreens

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.uoa.core.utils.Constants.Companion.DRIVER_EMAIL_ID
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.safedriveafrica.MainActivity
import com.uoa.safedriveafrica.R
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.rules.TestRule
import org.junit.runners.model.Statement

/**
 * Basic UI flow tests covering the splash screens.
 */
@RunWith(AndroidJUnit4::class)
class EntryFlowTest {
    private val activityRule = LaunchActivityRule()
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val composeTestRule = AndroidComposeTestRule(
        activityRule = activityRule,
        activityProvider = { rule -> rule.getActivity() }
    )

    @Before
    fun clearProfilePrefs() {
        val prefs = targetContext.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().remove(DRIVER_PROFILE_ID).remove(DRIVER_EMAIL_ID).apply()
        activityRule.launch()
        composeTestRule.waitForIdle()
    }

    private fun waitForText(text: String, timeoutMillis: Long = 12_000) {
        composeTestRule.waitUntil(timeoutMillis) {
            runCatching {
                composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
        composeTestRule.onNodeWithText(text).assertExists()
    }

    @Test
    fun appLaunchShowsWelcomeScreen() {
        waitForText(targetContext.getString(R.string.welcome_title))
    }

    @Test
    fun continueFromWelcomeNavigatesToDisclaimer() {
        val continueLabel = targetContext.getString(R.string.continue_button)
        waitForText(continueLabel)
        composeTestRule.onNodeWithText(continueLabel).performClick()
        waitForText(targetContext.getString(R.string.disclaimer_title))
    }

    @Test
    fun continueFromDisclaimerNavigatesToOnboarding() {
        val continueLabel = targetContext.getString(R.string.continue_button)
        waitForText(continueLabel)
        composeTestRule.onNodeWithText(continueLabel).performClick()
        waitForText(targetContext.getString(R.string.disclaimer_title))
        val agreeLabel = targetContext.getString(R.string.agree_continue)
        composeTestRule.onNodeWithText(agreeLabel).performClick()
        waitForText(targetContext.getString(R.string.onboarding_title))
    }

    class LaunchActivityRule : TestRule {
        private var scenario: ActivityScenario<MainActivity>? = null

        fun launch() {
            scenario?.close()
            scenario = ActivityScenario.launch(MainActivity::class.java)
        }

        fun getActivity(): MainActivity {
            val current = scenario ?: throw IllegalStateException("Activity not launched")
            var activity: MainActivity? = null
            current.onActivity { activity = it }
            return activity ?: throw IllegalStateException("Activity not set")
        }

        override fun apply(base: Statement, description: Description): Statement {
            return object : Statement() {
                override fun evaluate() {
                    try {
                        base.evaluate()
                    } finally {
                        scenario?.close()
                    }
                }
            }
        }
    }
}
