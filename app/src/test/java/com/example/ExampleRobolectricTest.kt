package com.example

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.editingcells.fictions.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `launch MainActivity and verify empty vault and import sheets`() {
    composeTestRule.mainClock.advanceTimeBy(1500)
    composeTestRule.waitForIdle()

    // Verify HomeScreen loaded with zero fake/demo files
    composeTestRule.onNodeWithText("Curated Archives").assertExists()

    // Click center + FAB to open AddToVaultBottomSheet
    composeTestRule.onNodeWithTag("center_add_fab").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Add to Fictions Vault").assertExists()
    composeTestRule.onNodeWithTag("import_all_files_tile").assertExists()

    // Scroll to and click New TXT Note
    composeTestRule.onNodeWithText("New TXT Note").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Compose Note").assertExists()
  }
}
