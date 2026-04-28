package com.petsafety.app.ui.a11y

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

/**
 * Marks the composable as a heading so TalkBack announces it as such and the
 * user can jump between headings with the screen-reader heading-navigation
 * gesture. Use on Text styled as `headlineLarge`, `headlineMedium`,
 * `headlineSmall`, or `titleLarge` when those represent the page or section
 * title.
 */
fun Modifier.markAsHeading(): Modifier = this.semantics { heading() }

/**
 * Marks the composable as having an accessibility Button role. Use on
 * `Modifier.clickable` Text and Box CTAs that aren't a Material `Button`/
 * `IconButton`/`TextButton` — without this, TalkBack reads them as plain
 * text and users don't know they're tappable.
 */
fun Modifier.markAsButton(): Modifier = this.semantics { role = Role.Button }
