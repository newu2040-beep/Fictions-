package com.editingcells.fictions.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.editingcells.fictions.ui.theme.LocalFileTypePalette
import com.editingcells.fictions.ui.theme.adaptiveGlassContentColor
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/**
 * CompositionLocal holding the active Kyant0 [Backdrop] recorded from the underlying screen content.
 * Provided ONLY to foreground floating overlays outside the [com.kyant.backdrop.backdrops.layerBackdrop] node
 * to prevent recursive RenderNode self-drawing cycles and maintain 60fps.
 */
val LocalVaultBackdrop = compositionLocalOf<Backdrop?> { null }

/**
 * Normalized scroll offset (0f..1f) used to shift the specular rim highlight gently as the user scrolls.
 */
val LocalGlassScrollPhase = compositionLocalOf { 0f }

/**
 * Optional gyroscope / accelerometer listener that returns a subtle (-1f..1f) 2D tilt offset
 * so the iOS 27-style specular rim highlight shifts gently as the device tilts.
 * Never crashes if sensors are unavailable (e.g., emulators or headless environments).
 */
@Composable
fun rememberGyroSpecularOffset(): State<Offset> {
    val context = LocalContext.current
    val offsetState = remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(context) {
        val sensorManager = runCatching {
            context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        }.getOrNull()

        val sensor = runCatching {
            sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
                ?: sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
                ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }.getOrNull()

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val values = event?.values ?: return
                if (values.size >= 2) {
                    val nx = (values[0] / 9.8f).coerceIn(-1f, 1f)
                    val ny = (values[1] / 9.8f).coerceIn(-1f, 1f)
                    offsetState.value = Offset(nx, ny)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (sensorManager != null && sensor != null) {
            runCatching {
                sensorManager.registerListener(
                    listener,
                    sensor,
                    SensorManager.SENSOR_DELAY_UI
                )
            }
        }

        onDispose {
            runCatching {
                sensorManager?.unregisterListener(listener)
            }
        }
    }

    return offsetState
}

/**
 * Single reusable iOS 27-style Liquid Glass composable powered by Kyant0/AndroidLiquidGlass (`com.kyant.backdrop`).
 *
 * Used strictly on floating surfaces (max 3 visible at once):
 * 1. Floating pill bottom navigation bar
 * 2. Raised circular "+" FAB
 * 3. Floating top pill / Viewer floating toolbars / Multi-select pill / Glass bottom sheets
 *
 * Features:
 * - Live backdrop blur (theme-tuned radius) + subtle edge refraction via `lens()` on Android 13+ (API 33+)
 * - Graceful fallback to translucent blur + adaptive tint below Android 13
 * - 1dp gradient rim highlight (bright top-left, soft bottom-right) that shifts gently with scroll & gyro
 * - Springy press physics: when interactive (`onClick != null` or pressed), scales to 0.96 with a springy bulge
 *   and brighter rim highlight, then settles smoothly
 * - Automatic WCAG AA luminance-aware foreground content color
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = LocalVaultBackdrop.current,
    shape: Shape = RoundedCornerShape(32.dp),
    blurRadius: Dp = Dp.Unspecified,
    refractionHeight: Dp = 14.dp,
    refractionAmount: Dp = 24.dp,
    surfaceTint: Color? = null,
    elevation: Dp = 12.dp,
    onClick: (() -> Unit)? = null,
    contentDescription: String? = null,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalFileTypePalette.current
    val isDark = palette.isDark
    val effectiveBlur = if (blurRadius != Dp.Unspecified) blurRadius else palette.glassDefaultBlur
    val scrollPhase = LocalGlassScrollPhase.current
    val gyroOffset by rememberGyroSpecularOffset()

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Springy press scale (0.96f on press with springy bulge)
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "glass_press_scale"
    )

    // Brighter rim & subtle refraction bulge on press
    val pressRimBoost by animateFloatAsState(
        targetValue = if (isPressed) 1.35f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "glass_rim_boost"
    )

    val resolvedTint = surfaceTint ?: palette.glassSurfaceTint
    val adaptiveContentColor = remember(resolvedTint, isDark) {
        adaptiveGlassContentColor(resolvedTint, isDark)
    }

    // Specular 1dp gradient rim highlight: bright top-left, soft bottom-right, shifting gently with scroll & gyro
    val shiftX = (scrollPhase * 120f) + (gyroOffset.x * 80f)
    val shiftY = (scrollPhase * 60f) + (gyroOffset.y * 80f)

    val topLeftHighlightAlpha = ((if (isDark) 0.42f else 0.94f) * pressRimBoost).coerceIn(0f, 1f)
    val midRimAlpha = ((if (isDark) 0.12f else 0.38f) * pressRimBoost).coerceIn(0f, 1f)
    val bottomRightAlpha = ((if (isDark) 0.28f else 0.26f) * pressRimBoost).coerceIn(0f, 1f)

    val rimBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = topLeftHighlightAlpha),
            Color.White.copy(alpha = midRimAlpha),
            MaterialTheme.colorScheme.primary.copy(alpha = bottomRightAlpha)
        ),
        start = Offset(0f + shiftX, 0f + shiftY),
        end = Offset(600f + shiftX, 600f + shiftY)
    )

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            role = Role.Button,
            onClickLabel = contentDescription,
            onClick = onClick
        )
    } else {
        Modifier
    }

    val scaleModifier = if (onClick != null) {
        Modifier.graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
        }
    } else {
        Modifier
    }

    val glassModifier = if (backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                spotColor = Color.Black.copy(alpha = if (isDark) 0.36f else 0.18f)
            )
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(effectiveBlur.toPx())
                    val maxSafeRefraction = (size.minDimension * 0.36f).coerceAtLeast(1f)
                    val bulgeFactor = if (isPressed) 1.15f else 1f
                    val safeHeight = (refractionHeight.toPx() * bulgeFactor).coerceAtMost(maxSafeRefraction)
                    val safeAmount = (refractionAmount.toPx() * bulgeFactor).coerceAtMost(maxSafeRefraction * 1.5f)
                    lens(
                        safeHeight,
                        safeAmount,
                        true
                    )
                },
                onDrawSurface = {
                    drawRect(resolvedTint)
                    drawRect(palette.glassAccentTint)
                }
            )
            .border(width = 1.dp, brush = rimBrush, shape = shape)
    } else {
        // Fallback below Android 13 or when no backdrop layer is attached: translucent blur + adaptive tint
        val fallbackColor = if (surfaceTint != null) {
            surfaceTint.copy(alpha = surfaceTint.alpha.coerceAtLeast(0.86f))
        } else if (isDark) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        } else {
            Color.White.copy(alpha = 0.90f)
        }

        Modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                spotColor = Color.Black.copy(alpha = if (isDark) 0.32f else 0.16f)
            )
            .clip(shape)
            .background(color = fallbackColor, shape = shape)
            .background(color = palette.glassAccentTint, shape = shape)
            .border(width = 1.dp, brush = rimBrush, shape = shape)
    }

    CompositionLocalProvider(LocalContentColor provides adaptiveContentColor) {
        Box(
            modifier = modifier
                .then(scaleModifier)
                .then(glassModifier)
                .then(clickModifier),
            contentAlignment = contentAlignment,
            content = content
        )
    }
}
