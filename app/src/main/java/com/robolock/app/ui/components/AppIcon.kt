package com.robolock.app.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.robolock.rules.SupportedPackages

/**
 * Shows a stable brand-colour glyph for supported apps and falls back to a launcher icon for any
 * future app that does not yet have a controlled treatment.
 *
 * Android launcher masks vary across devices; owning the rounded-square presentation keeps the
 * visual geometry identical without bundling third-party bitmap marks.
 */
@Composable
fun AppIcon(
    packageName: String,
    icon: Drawable?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    cornerRadius: Dp = 13.dp,
) {
    val shape = RoundedCornerShape(cornerRadius)
    // Launcher masks differ by device and can turn the same app into a circle, squircle or framed
    // badge. The reference uses stable rounded-square marks, so supported apps use the controlled
    // brand treatment below on every device.
    if (packageName == SupportedPackages.INSTAGRAM ||
        packageName == SupportedPackages.YOUTUBE ||
        packageName == SupportedPackages.WHATSAPP
    ) {
        FallbackAppIcon(packageName, modifier, size, shape)
        return
    }
    if (icon != null) {
        val bitmap = remember(icon, size) {
            runCatching { icon.toBitmap().asImageBitmap() }.getOrNull()
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier.size(size).clip(shape),
            )
            return
        }
    }
    FallbackAppIcon(packageName, modifier, size, shape)
}

@Composable
private fun FallbackAppIcon(
    packageName: String,
    modifier: Modifier,
    size: Dp,
    shape: RoundedCornerShape,
) {
    val style = fallbackStyleFor(packageName)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(style.brush),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = style.glyph,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.56f),
        )
    }
}

private data class FallbackStyle(val brush: Brush, val glyph: ImageVector)

private fun fallbackStyleFor(packageName: String): FallbackStyle = when (packageName) {
    SupportedPackages.INSTAGRAM -> FallbackStyle(
        brush = Brush.linearGradient(listOf(Color(0xFFF9CE34), Color(0xFFEE2A7B), Color(0xFF6228D7))),
        glyph = Icons.Outlined.PhotoCamera,
    )
    SupportedPackages.YOUTUBE -> FallbackStyle(
        brush = Brush.linearGradient(listOf(Color(0xFFFF3B3B), Color(0xFFE10E24))),
        glyph = Icons.Filled.PlayArrow,
    )
    SupportedPackages.WHATSAPP -> FallbackStyle(
        brush = Brush.linearGradient(listOf(Color(0xFF3BE07A), Color(0xFF14B84B))),
        glyph = Icons.Outlined.ChatBubbleOutline,
    )
    else -> FallbackStyle(
        brush = Brush.linearGradient(listOf(Color(0xFF3A3A44), Color(0xFF23232A))),
        glyph = Icons.Filled.PlayArrow,
    )
}
