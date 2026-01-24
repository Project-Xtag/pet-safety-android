package com.petsafety.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Shapes matching iOS app design
// iOS uses 16pt for buttons, 14pt for text fields and secondary buttons

val Shapes = Shapes(
    // Extra small - for chips, badges
    extraSmall = RoundedCornerShape(4.dp),

    // Small - for small cards, tags
    small = RoundedCornerShape(8.dp),

    // Medium - for text fields, secondary buttons (14pt in iOS)
    medium = RoundedCornerShape(14.dp),

    // Large - for primary buttons, cards (16pt in iOS)
    large = RoundedCornerShape(16.dp),

    // Extra large - for bottom sheets, dialogs
    extraLarge = RoundedCornerShape(24.dp)
)

// Custom corner radius values for specific use cases
object CornerRadius {
    val Button = 16.dp      // Primary buttons
    val TextField = 14.dp   // Text fields
    val Card = 16.dp        // Cards
    val Dialog = 24.dp      // Dialogs and bottom sheets
    val Small = 8.dp        // Small elements
}
