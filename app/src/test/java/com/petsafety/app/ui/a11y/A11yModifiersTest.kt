package com.petsafety.app.ui.a11y

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies the H65 chunk-2 a11y modifiers actually apply the semantics they
 * promise — these run as the regression guard for every screen that uses
 * [markAsHeading] or [markAsButton].
 *
 * Robolectric (JVM) keeps these in the regular `testDebugUnitTest` task; no
 * device or emulator is required.
 */
/**
 * Use the base Robolectric `Application` (not the project's `@HiltAndroidApp`
 * one) so the test does not boot WorkManager / Hilt / Sentry — we are only
 * verifying composable semantics in isolation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class A11yModifiersTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun `markAsHeading sets the Heading semantic property`() {
        composeTestRule.setContent {
            Text(
                text = "Buddy",
                modifier = Modifier.markAsHeading(),
            )
        }

        composeTestRule.onNodeWithText("Buddy").assert(
            SemanticsMatcher("is a heading") { node ->
                node.config.contains(SemanticsProperties.Heading)
            }
        )
    }

    @Test
    fun `markAsButton sets the Button role on the underlying node`() {
        composeTestRule.setContent {
            Text(
                text = "Add health info",
                modifier = Modifier
                    .clickable { /* no-op */ }
                    .markAsButton(),
            )
        }

        composeTestRule.onNodeWithText("Add health info").assert(
            SemanticsMatcher("has Button role") { node ->
                node.config.contains(SemanticsProperties.Role) &&
                    node.config[SemanticsProperties.Role] == Role.Button
            }
        )
    }

    @Test
    fun `Text without markAsHeading does not have the Heading property`() {
        composeTestRule.setContent {
            Text(text = "Plain text")
        }

        composeTestRule.onNodeWithText("Plain text").assert(
            SemanticsMatcher("is not marked as a heading") { node ->
                !node.config.contains(SemanticsProperties.Heading)
            }
        )
    }
}
