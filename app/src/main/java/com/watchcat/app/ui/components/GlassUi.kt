package com.watchcat.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watchcat.app.ui.theme.AppThemeState
import com.watchcat.app.ui.theme.GlassLine
import com.watchcat.app.ui.theme.Ink
import com.watchcat.app.ui.theme.OnAccent
import com.watchcat.app.ui.theme.TextPrimary
import com.watchcat.app.ui.theme.TextSecondary
import com.watchcat.app.ui.theme.TextTertiary
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

@Composable
fun Modifier.glassSurface(
    shape: Shape = RoundedCornerShape(28.dp),
    accent: Color = Color.White,
    blurRadius: Dp = 26.dp,
    fillAlpha: Float = 0.055f,
    borderAlpha: Float = 0.22f,
    elevated: Boolean = false,
): Modifier {
    val hazeState = LocalHazeState.current
    val dark = AppThemeState.isDark
    val line = GlassLine
    val style = remember(blurRadius, dark) {
        HazeStyle(
            backgroundColor = Color.Transparent,
            tints = listOf(
                HazeTint(
                    if (dark) Color.White.copy(alpha = 0.035f) else Color.White.copy(alpha = 0.06f),
                ),
            ),
            blurRadius = if (dark) blurRadius else blurRadius + 8.dp,
            noiseFactor = if (dark) 0.045f else 0.14f,
            fallbackTint = HazeTint(
                if (dark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.24f),
            ),
        )
    }
    val shadow = if (elevated) {
        Modifier.shadow(
            elevation = 20.dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = if (dark) 0.55f else 0.10f),
            spotColor = Color.Black.copy(alpha = if (dark) 0.45f else 0.08f),
        )
    } else {
        Modifier
    }
    return this
        .then(shadow)
        .clip(shape)
        .hazeEffect(state = hazeState, style = style)
        .background(
            brush = Brush.verticalGradient(
                colors = if (dark) {
                    listOf(
                        Color.White.copy(alpha = fillAlpha * 1.35f),
                        Color.White.copy(alpha = fillAlpha * 0.55f),
                        Color.White.copy(alpha = fillAlpha * 0.22f),
                    )
                } else {
                    listOf(
                        Color.White.copy(alpha = 0.48f),
                        Color.White.copy(alpha = 0.34f),
                        Color.White.copy(alpha = 0.26f),
                    )
                },
            ),
            shape = shape,
        )
        .border(
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        line.copy(alpha = if (dark) borderAlpha else 0.12f),
                        line.copy(alpha = if (dark) 0.045f else 0.035f),
                        line.copy(alpha = if (dark) borderAlpha * 0.5f else 0.07f),
                    ),
                ),
            ),
            shape = shape,
        )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    accent: Color = Color.White,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "glassCardScale",
    )
    val clickable = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = onClick,
        )
    } else {
        Modifier
    }
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .glassSurface(shape = shape, accent = accent, elevated = true)
            .then(clickable)
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = TextPrimary,
    size: Dp = 42.dp,
    iconSize: Dp = 19.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(GlassLine.copy(alpha = 0.065f))
            .border(1.dp, GlassLine.copy(alpha = 0.14f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
fun GlassPill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TextSecondary,
    background: Color = GlassLine.copy(alpha = 0.06f),
    borderColor: Color = GlassLine.copy(alpha = 0.13f),
    icon: ImageVector? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(background)
            .border(1.dp, borderColor, CircleShape)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (leading != null) leading()
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
fun GlassActionButton(
    text: String,
    onClick: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primary: Boolean = false,
    enabled: Boolean = true,
) {
    val alpha = if (enabled) 1f else 0.4f
    if (primary) {
        Row(
            modifier = modifier
                .height(46.dp)
                .graphicsLayer { this.alpha = alpha }
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = accent.copy(alpha = 0.5f),
                    spotColor = accent.copy(alpha = 0.4f),
                )
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            accent,
                            lerpColor(accent, Color.White, 0.28f),
                        ),
                    ),
                )
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OnAccent,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = OnAccent,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    } else {
        Row(
            modifier = modifier
                .height(46.dp)
                .graphicsLayer { this.alpha = alpha }
                .clip(CircleShape)
                .background(GlassLine.copy(alpha = 0.06f))
                .border(1.dp, GlassLine.copy(alpha = 0.16f), CircleShape)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(7.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun GlassTopBar(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val hazeState = LocalHazeState.current
    val dark = AppThemeState.isDark
    val style = remember(dark) {
        HazeStyle(
            backgroundColor = Ink.copy(alpha = 0.16f),
            tints = listOf(HazeTint(Ink.copy(alpha = if (dark) 0.20f else 0.18f))),
            blurRadius = 30.dp,
            noiseFactor = 0.035f,
            fallbackTint = HazeTint(Ink.copy(alpha = 0.42f)),
        )
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .hazeEffect(state = hazeState, style = style)
            .background(
                Brush.verticalGradient(
                    if (dark) {
                        listOf(
                            Ink.copy(alpha = 0.58f),
                            Ink.copy(alpha = 0.28f),
                            Ink.copy(alpha = 0.06f),
                        )
                    } else {
                        listOf(
                            Ink.copy(alpha = 0.38f),
                            Ink.copy(alpha = 0.15f),
                            Ink.copy(alpha = 0.04f),
                        )
                    },
                ),
            )
            .drawBehind {
                drawLine(
                    color = GlassLine.copy(alpha = 0.07f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .statusBarsPadding()
            .padding(start = 20.dp, end = 14.dp, top = 10.dp, bottom = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = actions,
            )
        }
    }
}

@Composable
fun GlassDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        GlassLine.copy(alpha = 0.12f),
                        Color.Transparent,
                    ),
                ),
            ),
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = TextTertiary) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        letterSpacing = 1.4.sp,
        modifier = modifier,
    )
}

fun lerpColor(start: Color, stop: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (stop.red - start.red) * f,
        green = start.green + (stop.green - start.green) * f,
        blue = start.blue + (stop.blue - start.blue) * f,
        alpha = start.alpha + (stop.alpha - start.alpha) * f,
    )
}
