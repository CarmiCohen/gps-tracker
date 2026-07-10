package com.gps19.app

import androidx.compose.ui.graphics.Color

/**
 * R799: New Color System.
 * Decouples Role Identity from Semantic Status.
 * v9.1.0:
 * - R799e [Active]: JD Vivid Green (#78BE20) introduced as primary branding.
 * v9.0.4:
 * - R799d: Changed Viewer color to Cyan (#06B6D4) from Orange.
 * v8.9.40:
 * - R865/R866 [Active]: Unified Identity Green (#367C2B) enforced as Tracker Primary.
 * v8.8.3:
 * - R866 [Active]: Added BrandJd (#367C2B) to synchronize with colors.xml.
 */

const val FORENSIC_PINK_COLOR = 0xFFF472B6.toInt()

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
val Slate950 = Color(0xFF020617)
val Slate900 = Color(0xFF0F172A)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155) // Added for Issue #059 UI buttons
val Slate400 = Color(0xFF94A3B8)
val Slate100 = Color(0xFFF1F5F9)
val White = Color(0xFFFFFFFF)
