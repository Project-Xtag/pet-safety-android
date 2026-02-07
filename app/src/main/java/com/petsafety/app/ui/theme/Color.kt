package com.petsafety.app.ui.theme

import androidx.compose.ui.graphics.Color

// Pet Safety Brand Colors - matching web app design
//
// | UI Element           | Color       | Hex     |
// | Primary CTA buttons  | Coral       | #FF914D |
// | Links & focus rings  | Teal        | #4DB8C4 |
// | Tab bar / nav tint   | Teal        | #4DB8C4 |
// | Badges & tags        | Coral       | #FF914D |
// | Highlights / accents | Golden      | #FFEB2E |
// | Delete / error       | Destructive | #F75757 |
// | Screen background    | Cream/Dark  | #F9F5F0 / #1F1B2E |
// | Section backgrounds  | Peach       | #FDEDD8 |
// | Secondary text       | Gray        | #737373 |
// | Success states       | Sage        | #A6C4B8 |
// | Upgrade banners      | Golden Light| #FFFF80 |

// Primary Brand Color (Coral) - for CTAs, badges
val BrandOrange = Color(0xFFFF914D)
val BrandOrangeDark = Color(0xFFF5975B) // Slightly adjusted for dark mode visibility

// Secondary Accent (Teal) - for links, focus rings, nav
val TealAccent = Color(0xFF4DB8C4)

// Golden Accent - for highlights
val GoldenAccent = Color(0xFFFFEB2E)
val GoldenLight = Color(0xFFFFFF80) // For upgrade banners background

// Background Colors
val BackgroundLight = Color(0xFFF9F5F0) // Warm cream
val BackgroundDark = Color(0xFF1F1B2E)  // Dark blue-gray

// Surface/Card Colors
val PeachBackground = Color(0xFFFDEDD8) // Warm peach for headers/cards
val CardBackgroundLight = Color(0xFFFDEDD8)
val CardBackgroundDark = Color(0xFF262626)

// Text Colors
val MutedTextLight = Color(0xFF737373)
val MutedTextDark = Color(0xFF999999)

// State Colors
val ErrorColor = Color(0xFFF75757) // Destructive red
val SuccessColor = Color(0xFFA6C4B8) // Sage green
val SuccessGreen = Color(0xFFA6C4B8) // Alias for SuccessColor
val WarningColor = Color(0xFFFFA726)
val InfoBlue = Color(0xFF007AFF)

// Border/Divider Colors
val BorderLight = Color(0xFFE0E0E0)
val BorderDark = Color(0xFF424242)

// Input/Field Background Colors
val InputBackgroundLight = Color(0xFFF2F2F7)
val InputBackgroundDark = Color(0xFF2C2C2E)

// Legacy aliases for compatibility
val BrandColor = BrandOrange
val BrandColorDark = BrandOrangeDark
