package com.vibra.wanderai.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibra.wanderai.R
import com.vibra.wanderai.ui.theme.WanderTheme

/** "Wander" + teal "AI" wordmark. */
@Composable
fun WanderWordmark(
    modifier: Modifier = Modifier,
    onDark: Boolean = false,
    fontSizeSp: Int = 20,
) {
    val base = if (onDark) Color.White else MaterialTheme.colorScheme.onSurface
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = base)) { append("Wander") }
            withStyle(SpanStyle(color = WanderTheme.colors.brandTeal)) { append("AI") }
        },
        modifier = modifier,
        fontSize = fontSizeSp.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.3).sp,
    )
}

/**
 * Top bar from the reference: centred logo + wordmark, optional back button
 * on the left and contextual actions on the right. When [title] is set it
 * replaces the wordmark (e.g. "Explora Pasto").
 */
@Composable
fun WanderTopBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    onBackClick: (() -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
            .statusBarsPadding()
            .height(60.dp)
            .padding(horizontal = 8.dp),
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                onBackClick != null -> IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.common_back),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                navigationIcon != null -> navigationIcon()
                else -> Spacer(Modifier.size(48.dp))
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (title == null) {
                WanderLogo(size = 30.dp)
                Spacer(Modifier.width(6.dp))
                WanderWordmark()
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}
