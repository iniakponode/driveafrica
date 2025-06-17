package com.uoa.driveafrica.ui.splashscreens

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.uoa.driveafrica.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Basic UI flow tests covering the splash screens.
 */
@RunWith(AndroidJUnit4::class)
class EntryFlowTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunchShowsWelcomeScreen() {
        composeTestRule.onNodeWithText("Welcome to the Safe Drive Africa APP").assertExists()
    }

    @Test
    fun continueFromWelcomeNavigatesToDisclaimer() {
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("Disclaimer").assertExists()
    }

    @Test
    fun continueFromDisclaimerNavigatesToEntryPoint() {
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("Disclaimer").assertExists()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithTag("entryPointProgress").assertExists()
    }
}
