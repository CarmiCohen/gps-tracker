package com.gps19.app

import androidx.compose.ui.graphics.Color

/**
 * R799: New Color System.
 * Decouples Role Identity from Semantic Status.
 * Oct.14.17:
 * - Brightened Neutral Palette: Shifted Slate950/900 to higher values 
 *   to rule out "Black Screen" rendering bugs on A15 hardware.
 */

// Forensic Authority: Standardized to DeepPink for high-visibility event marking.
const val FORENSIC_PINK_COLOR = 0xFFFF1493.toInt()
val ForensicPink = Color(FORENSIC_PINK_COLOR)

// Brand Colors
val BrandJdVivid = Color(0xFF78BE20) // JD Vivid Green (R799e)
val BrandJd = BrandJdVivid           // Current Brand Authority
val BrandJdDark = Color(0xFF367C2B)  // Legacy Green (#367C2B) preserved for depth

// Role Colors (Identity)
val ViewerCyan = Color(0xFF06B6D4)     // Cyan 500 (Viewer Role primary) (R799d)
val ViewerCyanDark = Color(0xFF0891B2) // Cyan 600

val Lime500 = Color(0xFF84CC16)   // Lime 500 (Legacy Tracker Role primary - Superseded by BrandJd)
val Lime600 = Color(0xFF65A30D)

val Teal500 = Color(0xFF06B6D4)   // Teal 500 (Legacy R815 role color - Superseded)
val Teal600 = Color(0xFF0891B2)

// Semantic Status Colors
val Emerald500 = Color(0xFF10B981) // Success / OK
val Rose500 = Color(0xFFE11D48)    // Error / Alert
val Amber500 = Color(0xFFF59E0B)   // Warning / Pending
val Slate500 = Color(0xFF64748B)   // Inactive / Offline

// UI Accents
val Violet500 = Color(0xFFA78BFA)
val Indigo500 = Color(0xFF6366F1) 
val Purple500 = Color(0xFF800080) // Added for R805 markers

// Neutral Palette (Backgrounds & Surfaces)
// Oct.14.17: Ruler-out for A15 black screen.
val Slate950 = Color(0xFF1E293B) // Changed from #020617 (Near-black) to Slate 800 for visibility
val Slate900 = Color(0xFF334155) // Changed from #0F172A to Slate 700
val Slate800 = Color(0xFF475569)
val Slate700 = Color(0xFF64748B)
val Slate400 = Color(0xFF94A3B8)
val Slate100 = Color(0xFFF1F5F9)
val White = Color(0xFFFFFFFF)
