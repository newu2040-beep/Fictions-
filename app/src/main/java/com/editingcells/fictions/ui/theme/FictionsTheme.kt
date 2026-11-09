package com.editingcells.fictions.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.editingcells.fictions.domain.model.PastelTheme
import com.editingcells.fictions.domain.model.ThemeMode
import com.editingcells.fictions.domain.model.VaultFileType
import com.example.R

val PlusJakartaSansFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium)
)

/**
 * Canonical Material 3 WindowSizeClass buckets for width & height.
 */
enum class VaultWindowSizeClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

/**
 * Fine-grained width buckets covering 5.4" small phones (<360dp),
 * 6.1" standard phones (360-411dp), 6.9" large phones (412-599dp),
 * foldables (600-839dp), and tablets (840dp+).
 */
enum class WidthBucket {
    COMPACT_SMALL, // < 360dp
    MEDIUM_PHONE,  // 360dp - 411dp
    LARGE_PHONE,   // 412dp - 599dp
    FOLDABLE,      // 600dp - 839dp
    TABLET         // >= 840dp
}

/**
 * Responsive spacing & sizing scale (4, 8, 12, 16, 24, 32dp base grid)
 * scaled slightly by [WidthBucket] with zero hardcoded screen assumptions.
 */
@Immutable
data class ResponsiveLayoutSpec(
    val widthDp: Dp = 390.dp,
    val heightDp: Dp = 844.dp,
    val widthSizeClass: VaultWindowSizeClass = VaultWindowSizeClass.COMPACT,
    val heightSizeClass: VaultWindowSizeClass = VaultWindowSizeClass.MEDIUM,
    val widthBucket: WidthBucket = WidthBucket.MEDIUM_PHONE,
    val isLandscape: Boolean = false,
    val scaleFactor: Float = 1f,
    val space4: Dp = 4.dp,
    val space8: Dp = 8.dp,
    val space12: Dp = 12.dp,
    val space16: Dp = 16.dp,
    val space20: Dp = 20.dp,
    val space24: Dp = 24.dp,
    val space32: Dp = 32.dp,
    val screenMargin: Dp = 18.dp,
    val cardMinGridWidth: Dp = 164.dp,
    val folderMinGridWidth: Dp = 160.dp,
    val floatingBarMaxWidth: Dp = 480.dp,
    val sheetMaxWidth: Dp = 560.dp,
    val contentMaxWidth: Dp = 960.dp,
    val bottomBarClearance: Dp = 176.dp
)

val LocalResponsiveLayout = staticCompositionLocalOf { ResponsiveLayoutSpec() }

fun calculateResponsiveLayoutSpec(maxWidth: Dp, maxHeight: Dp): ResponsiveLayoutSpec {
    val widthClass = when {
        maxWidth < 600.dp -> VaultWindowSizeClass.COMPACT
        maxWidth < 840.dp -> VaultWindowSizeClass.MEDIUM
        else -> VaultWindowSizeClass.EXPANDED
    }
    val heightClass = when {
        maxHeight < 480.dp -> VaultWindowSizeClass.COMPACT
        maxHeight < 900.dp -> VaultWindowSizeClass.MEDIUM
        else -> VaultWindowSizeClass.EXPANDED
    }
    val bucket = when {
        maxWidth < 360.dp -> WidthBucket.COMPACT_SMALL
        maxWidth < 412.dp -> WidthBucket.MEDIUM_PHONE
        maxWidth < 600.dp -> WidthBucket.LARGE_PHONE
        maxWidth < 840.dp -> WidthBucket.FOLDABLE
        else -> WidthBucket.TABLET
    }
    val scale = when (bucket) {
        WidthBucket.COMPACT_SMALL -> 0.90f
        WidthBucket.MEDIUM_PHONE -> 1.00f
        WidthBucket.LARGE_PHONE -> 1.06f
        WidthBucket.FOLDABLE -> 1.12f
        WidthBucket.TABLET -> 1.18f
    }
    val isLandscape = maxWidth > maxHeight

    // GridCells.Adaptive(minSize):
    // - Small phone (<360dp): minWidth ~150dp -> 1-2 columns
    // - Normal/Large phone (360-599dp): minWidth ~162-176dp -> 2-3 columns
    // - Foldable/Tablet (600dp+): minWidth ~188-204dp -> 3-4+ columns
    val adaptiveCardMinWidth = when (bucket) {
        WidthBucket.COMPACT_SMALL -> 148.dp
        WidthBucket.MEDIUM_PHONE -> 160.dp
        WidthBucket.LARGE_PHONE -> 174.dp
        WidthBucket.FOLDABLE -> 188.dp
        WidthBucket.TABLET -> 208.dp
    }

    val horizontalMargin = when (bucket) {
        WidthBucket.COMPACT_SMALL -> 16.dp
        WidthBucket.MEDIUM_PHONE -> 16.dp
        WidthBucket.LARGE_PHONE -> 20.dp
        WidthBucket.FOLDABLE -> 24.dp
        WidthBucket.TABLET -> 28.dp
    }

    return ResponsiveLayoutSpec(
        widthDp = maxWidth,
        heightDp = maxHeight,
        widthSizeClass = widthClass,
        heightSizeClass = heightClass,
        widthBucket = bucket,
        isLandscape = isLandscape,
        scaleFactor = scale,
        space4 = 4.dp,
        space8 = 8.dp,
        space12 = (12f * scale).dp,
        space16 = (16f * scale).dp,
        space20 = (20f * scale).dp,
        space24 = (24f * scale).dp,
        space32 = (32f * scale).dp,
        screenMargin = horizontalMargin,
        cardMinGridWidth = adaptiveCardMinWidth,
        folderMinGridWidth = adaptiveCardMinWidth,
        floatingBarMaxWidth = 480.dp,
        sheetMaxWidth = 560.dp,
        contentMaxWidth = 960.dp,
        bottomBarClearance = 176.dp
    )
}

@Immutable
data class FileTypePalette(
    val pdfBadgeBg: Color,
    val pdfBadgeFg: Color,
    val csvBadgeBg: Color,
    val csvBadgeFg: Color,
    val txtBadgeBg: Color,
    val txtBadgeFg: Color,
    val ambientBlobPrimary: Color,
    val ambientBlobSecondary: Color,
    val ambientBlobTertiary: Color,
    val glassSurfaceTint: Color,
    val glassAccentTint: Color,
    val glassDefaultBlur: Dp,
    val onGlassContentColor: Color,
    val onGlassMutedColor: Color,
    val isDark: Boolean
) {
    fun badgeBackground(type: VaultFileType): Color = when (type) {
        VaultFileType.PDF -> pdfBadgeBg
        VaultFileType.CSV -> csvBadgeBg
        VaultFileType.TXT -> txtBadgeFg.copy(alpha = 0.14f)
    }

    fun badgeForeground(type: VaultFileType): Color = when (type) {
        VaultFileType.PDF -> pdfBadgeFg
        VaultFileType.CSV -> csvBadgeFg
        VaultFileType.TXT -> txtBadgeFg
    }
}

val LocalFileTypePalette = staticCompositionLocalOf {
    FileTypePalette(
        pdfBadgeBg = Color(0xFFFFE4E6),
        pdfBadgeFg = Color(0xFFE11D48),
        csvBadgeBg = Color(0xFFD1FAE5),
        csvBadgeFg = Color(0xFF059669),
        txtBadgeBg = Color(0xFFDBEAFE),
        txtBadgeFg = Color(0xFF2563EB),
        ambientBlobPrimary = Color(0xFFDDD6FE),
        ambientBlobSecondary = Color(0xFFFBCFE8),
        ambientBlobTertiary = Color(0xFFC7D2FE),
        glassSurfaceTint = Color(0xFFFDFBFF).copy(alpha = 0.62f),
        glassAccentTint = Color(0xFF6D28D9).copy(alpha = 0.10f),
        glassDefaultBlur = 22.dp,
        onGlassContentColor = Color(0xFF161124),
        onGlassMutedColor = Color(0xFF4A4160),
        isDark = false
    )
}

/**
 * Picks high-contrast WCAG AA text/icon color based on backdrop & surface luminance.
 */
fun adaptiveGlassContentColor(surfaceTint: Color, isDarkTheme: Boolean): Color {
    val lum = surfaceTint.luminance()
    return when {
        surfaceTint.alpha > 0.45f && lum < 0.28f -> Color(0xFFF9F7FF)
        surfaceTint.alpha > 0.45f && lum > 0.62f -> Color(0xFF141021)
        isDarkTheme -> Color(0xFFF6F3FF)
        else -> Color(0xFF141021)
    }
}

private fun buildColorScheme(pastelTheme: PastelTheme, isDark: Boolean): ColorScheme {
    return when (pastelTheme) {
        PastelTheme.LAVENDER -> if (!isDark) {
            lightColorScheme(
                primary = Color(0xFF6D28D9),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFEDE9FE),
                onPrimaryContainer = Color(0xFF2E1065),
                secondary = Color(0xFF7C3AED),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFF3E8FF),
                onSecondaryContainer = Color(0xFF3B0764),
                tertiary = Color(0xFFDB2777),
                background = Color(0xFFF7F5FF),
                onBackground = Color(0xFF171226),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF171226),
                surfaceVariant = Color(0xFFEEEAF8),
                onSurfaceVariant = Color(0xFF4E4563),
                outline = Color(0xFFD2C8E6)
            )
        } else {
            darkColorScheme(
                primary = Color(0xFFB49BFF),
                onPrimary = Color(0xFF1C0E3B),
                primaryContainer = Color(0xFF4C1D95),
                onPrimaryContainer = Color(0xFFEDE9FE),
                secondary = Color(0xFFC4B5FD),
                onSecondary = Color(0xFF2E1065),
                secondaryContainer = Color(0xFF2E1E4F),
                onSecondaryContainer = Color(0xFFDDD6FE),
                tertiary = Color(0xFFF472B6),
                background = Color(0xFF0D0A14),
                onBackground = Color(0xFFF5F2FF),
                surface = Color(0xFF171224),
                onSurface = Color(0xFFF5F2FF),
                surfaceVariant = Color(0xFF231C36),
                onSurfaceVariant = Color(0xFFC2B8DC),
                outline = Color(0xFF3E345B)
            )
        }

        PastelTheme.PEACH -> if (!isDark) {
            lightColorScheme(
                primary = Color(0xFFC2410C),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFFEDD5),
                onPrimaryContainer = Color(0xFF431407),
                secondary = Color(0xFFEA580C),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFFFF1E6),
                onSecondaryContainer = Color(0xFF7C2D12),
                tertiary = Color(0xFFBE123C),
                background = Color(0xFFFFF8F4),
                onBackground = Color(0xFF23140D),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF23140D),
                surfaceVariant = Color(0xFFF9ECE2),
                onSurfaceVariant = Color(0xFF63483B),
                outline = Color(0xFFE5CEC0)
            )
        } else {
            darkColorScheme(
                primary = Color(0xFFFB923C),
                onPrimary = Color(0xFF3E1205),
                primaryContainer = Color(0xFF9A3412),
                onPrimaryContainer = Color(0xFFFFEDD5),
                secondary = Color(0xFFFDBA74),
                onSecondary = Color(0xFF431407),
                secondaryContainer = Color(0xFF381E15),
                onSecondaryContainer = Color(0xFFFED7AA),
                tertiary = Color(0xFFFB7185),
                background = Color(0xFF120C09),
                onBackground = Color(0xFFFFF3ED),
                surface = Color(0xFF1F1510),
                onSurface = Color(0xFFFFF3ED),
                surfaceVariant = Color(0xFF2E1F19),
                onSurfaceVariant = Color(0xFFD8BCB0),
                outline = Color(0xFF4F372E)
            )
        }

        PastelTheme.MINT -> if (!isDark) {
            lightColorScheme(
                primary = Color(0xFF047857),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFD1FAE5),
                onPrimaryContainer = Color(0xFF064E3B),
                secondary = Color(0xFF059669),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFECFDF5),
                onSecondaryContainer = Color(0xFF065F46),
                tertiary = Color(0xFF0369A1),
                background = Color(0xFFF3FBF7),
                onBackground = Color(0xFF0C2019),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF0C2019),
                surfaceVariant = Color(0xFFE2F2EB),
                onSurfaceVariant = Color(0xFF39594E),
                outline = Color(0xFFC2DFD3)
            )
        } else {
            darkColorScheme(
                primary = Color(0xFF34D399),
                onPrimary = Color(0xFF022C22),
                primaryContainer = Color(0xFF065F46),
                onPrimaryContainer = Color(0xFFD1FAE5),
                secondary = Color(0xFF6EE7B7),
                onSecondary = Color(0xFF022C22),
                secondaryContainer = Color(0xFF133028),
                onSecondaryContainer = Color(0xFFA7F3D0),
                tertiary = Color(0xFF38BDF8),
                background = Color(0xFF08120E),
                onBackground = Color(0xFFECFDF5),
                surface = Color(0xFF10201A),
                onSurface = Color(0xFFECFDF5),
                surfaceVariant = Color(0xFF193028),
                onSurfaceVariant = Color(0xFFAECFC3),
                outline = Color(0xFF2D5245)
            )
        }

        PastelTheme.SKY -> if (!isDark) {
            lightColorScheme(
                primary = Color(0xFF0369A1),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFE0F2FE),
                onPrimaryContainer = Color(0xFF0C4A6E),
                secondary = Color(0xFF0284C7),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFF0F9FF),
                onSecondaryContainer = Color(0xFF075985),
                tertiary = Color(0xFF4F46E5),
                background = Color(0xFFF3F9FF),
                onBackground = Color(0xFF0C1A26),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF0C1A26),
                surfaceVariant = Color(0xFFE3EFF9),
                onSurfaceVariant = Color(0xFF3F5769),
                outline = Color(0xFFC3DAEB)
            )
        } else {
            darkColorScheme(
                primary = Color(0xFF38BDF8),
                onPrimary = Color(0xFF082F49),
                primaryContainer = Color(0xFF0369A1),
                onPrimaryContainer = Color(0xFFE0F2FE),
                secondary = Color(0xFF7DD3FC),
                onSecondary = Color(0xFF082F49),
                secondaryContainer = Color(0xFF142A3A),
                onSecondaryContainer = Color(0xFFBAE6FD),
                tertiary = Color(0xFF818CF8),
                background = Color(0xFF081017),
                onBackground = Color(0xFFF0F9FF),
                surface = Color(0xFF101C28),
                onSurface = Color(0xFFF0F9FF),
                surfaceVariant = Color(0xFF192B3D),
                onSurfaceVariant = Color(0xFFAEC9DE),
                outline = Color(0xFF2E4963)
            )
        }

        PastelTheme.ROSE -> if (!isDark) {
            lightColorScheme(
                primary = Color(0xFFBE123C),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFFE4E6),
                onPrimaryContainer = Color(0xFF4C0519),
                secondary = Color(0xFFE11D48),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFFFF1F2),
                onSecondaryContainer = Color(0xFF881337),
                tertiary = Color(0xFF7C3AED),
                background = Color(0xFFFFF6F8),
                onBackground = Color(0xFF240F15),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF240F15),
                surfaceVariant = Color(0xFFF9E6EA),
                onSurfaceVariant = Color(0xFF66424B),
                outline = Color(0xFFE7C4CC)
            )
        } else {
            darkColorScheme(
                primary = Color(0xFFFB7185),
                onPrimary = Color(0xFF4C0519),
                primaryContainer = Color(0xFF9F1239),
                onPrimaryContainer = Color(0xFFFFE4E6),
                secondary = Color(0xFFFDA4AF),
                onSecondary = Color(0xFF4C0519),
                secondaryContainer = Color(0xFF381520),
                onSecondaryContainer = Color(0xFFFECDD3),
                tertiary = Color(0xFFA78BFA),
                background = Color(0xFF14090C),
                onBackground = Color(0xFFFFF1F2),
                surface = Color(0xFF211116),
                onSurface = Color(0xFFFFF1F2),
                surfaceVariant = Color(0xFF331B22),
                onSurfaceVariant = Color(0xFFDCCfd4),
                outline = Color(0xFF57313D)
            )
        }

        PastelTheme.SAND -> if (!isDark) {
            lightColorScheme(
                primary = Color(0xFFB45309),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFEF3C7),
                onPrimaryContainer = Color(0xFF451A03),
                secondary = Color(0xFFD97706),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFFFFBEB),
                onSecondaryContainer = Color(0xFF78350F),
                tertiary = Color(0xFF0F766E),
                background = Color(0xFFFCF9F2),
                onBackground = Color(0xFF211A0E),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF211A0E),
                surfaceVariant = Color(0xFFF3EDE0),
                onSurfaceVariant = Color(0xFF5E533F),
                outline = Color(0xFFE0D6C1)
            )
        } else {
            darkColorScheme(
                primary = Color(0xFFFBBF24),
                onPrimary = Color(0xFF3D1702),
                primaryContainer = Color(0xFF92400E),
                onPrimaryContainer = Color(0xFFFEF3C7),
                secondary = Color(0xFFFCD34D),
                onSecondary = Color(0xFF451A03),
                secondaryContainer = Color(0xFF332510),
                onSecondaryContainer = Color(0xFFFDE68A),
                tertiary = Color(0xFF2DD4BF),
                background = Color(0xFF120F0A),
                onBackground = Color(0xFFFEFCE8),
                surface = Color(0xFF1E1A11),
                onSurface = Color(0xFFFEFCE8),
                surfaceVariant = Color(0xFF2E271A),
                onSurfaceVariant = Color(0xFFD6C9B1),
                outline = Color(0xFF4F4430)
            )
        }
    }
}

private fun buildFileTypePalette(pastelTheme: PastelTheme, isDark: Boolean): FileTypePalette {
    val (blob1, blob2, blob3) = when (pastelTheme) {
        PastelTheme.LAVENDER -> if (!isDark) {
            Triple(Color(0xFFDDD6FE), Color(0xFFFBCFE8), Color(0xFFC7D2FE))
        } else {
            Triple(Color(0xFF4C1D95), Color(0xFF831843), Color(0xFF312E81))
        }
        PastelTheme.PEACH -> if (!isDark) {
            Triple(Color(0xFFFED7AA), Color(0xFFFECDD3), Color(0xFFFEF08A))
        } else {
            Triple(Color(0xFF7C2D12), Color(0xFF881337), Color(0xFF713F12))
        }
        PastelTheme.MINT -> if (!isDark) {
            Triple(Color(0xFFA7F3D0), Color(0xFFBAE6FD), Color(0xFFD9F99D))
        } else {
            Triple(Color(0xFF064E3B), Color(0xFF0C4A6E), Color(0xFF14532D))
        }
        PastelTheme.SKY -> if (!isDark) {
            Triple(Color(0xFFBAE6FD), Color(0xFFC7D2FE), Color(0xFFA5F3FC))
        } else {
            Triple(Color(0xFF0C4A6E), Color(0xFF312E81), Color(0xFF164E63))
        }
        PastelTheme.ROSE -> if (!isDark) {
            Triple(Color(0xFFFECDD3), Color(0xFFF5D0FE), Color(0xFFFED7AA))
        } else {
            Triple(Color(0xFF881337), Color(0xFF581C87), Color(0xFF7C2D12))
        }
        PastelTheme.SAND -> if (!isDark) {
            Triple(Color(0xFFFDE68A), Color(0xFFFED7AA), Color(0xFFA7F3D0))
        } else {
            Triple(Color(0xFF78350F), Color(0xFF7C2D12), Color(0xFF134E4A))
        }
    }

    // Theme-tuned blur & faint tint adapting to content behind
    val themeBlur = when (pastelTheme) {
        PastelTheme.LAVENDER -> 24.dp
        PastelTheme.PEACH -> 22.dp
        PastelTheme.MINT -> 22.dp
        PastelTheme.SKY -> 25.dp
        PastelTheme.ROSE -> 23.dp
        PastelTheme.SAND -> 21.dp
    }

    val accentTint = Color(pastelTheme.previewPrimaryHex).copy(alpha = if (isDark) 0.14f else 0.08f)
    val baseGlassTint = if (isDark) {
        Color(0xFF171324).copy(alpha = 0.64f)
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.68f)
    }

    return if (!isDark) {
        FileTypePalette(
            pdfBadgeBg = Color(0xFFFFE4E6),
            pdfBadgeFg = Color(0xFFBE123C),
            csvBadgeBg = Color(0xFFD1FAE5),
            csvBadgeFg = Color(0xFF047857),
            txtBadgeBg = Color(0xFFDBEAFE),
            txtBadgeFg = Color(0xFF1D4ED8),
            ambientBlobPrimary = blob1,
            ambientBlobSecondary = blob2,
            ambientBlobTertiary = blob3,
            glassSurfaceTint = baseGlassTint,
            glassAccentTint = accentTint,
            glassDefaultBlur = themeBlur,
            onGlassContentColor = Color(0xFF151022),
            onGlassMutedColor = Color(0xFF49405E),
            isDark = false
        )
    } else {
        FileTypePalette(
            pdfBadgeBg = Color(0xFF4C0519),
            pdfBadgeFg = Color(0xFFFB7185),
            csvBadgeBg = Color(0xFF064E3B),
            csvBadgeFg = Color(0xFF34D399),
            txtBadgeBg = Color(0xFF1E3A8A),
            txtBadgeFg = Color(0xFF60A5FA),
            ambientBlobPrimary = blob1,
            ambientBlobSecondary = blob2,
            ambientBlobTertiary = blob3,
            glassSurfaceTint = baseGlassTint,
            glassAccentTint = accentTint,
            glassDefaultBlur = themeBlur,
            onGlassContentColor = Color(0xFFF6F3FF),
            onGlassMutedColor = Color(0xFFC4BBD9),
            isDark = true
        )
    }
}

val FictionsTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.8).sp
    ),
    displayMedium = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

val FictionsShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(20.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun FictionsTheme(
    pastelTheme: PastelTheme = PastelTheme.LAVENDER,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = buildColorScheme(pastelTheme, isDark)
    val palette = buildFileTypePalette(pastelTheme, isDark)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val responsiveSpec = remember(maxWidth, maxHeight) {
            calculateResponsiveLayoutSpec(maxWidth = maxWidth, maxHeight = maxHeight)
        }

        CompositionLocalProvider(
            LocalFileTypePalette provides palette,
            LocalResponsiveLayout provides responsiveSpec
        ) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = FictionsTypography,
                shapes = FictionsShapes,
                content = content
            )
        }
    }
}
