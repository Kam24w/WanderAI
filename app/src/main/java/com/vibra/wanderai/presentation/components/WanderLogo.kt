package com.vibra.wanderai.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vibra.wanderai.R

/**
 * @param size       Tamaño del composable
 * @param padding    Espacio interno entre el borde y la imagen
 * @param onDark     true  → fondo blanco circular (para pantallas oscuras navy/teal)
 *                   false → sin fondo, logo directamente sobre el surface
 */
@Composable
fun WanderLogo(
    size: Dp = 56.dp,
    padding: Dp = 6.dp,
    onDark: Boolean = false
) {
    if (onDark) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.logo_wanderai),
                contentDescription = "WanderAI",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(size - padding * 2)
                    .padding(padding / 4)
            )
        }
    } else {
        Image(
            painter = painterResource(R.drawable.logo_wanderai),
            contentDescription = "WanderAI",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(size)
        )
    }
}
