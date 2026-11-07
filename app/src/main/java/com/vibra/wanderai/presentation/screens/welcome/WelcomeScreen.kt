package com.vibra.wanderai.presentation.screens.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibra.wanderai.R
import com.vibra.wanderai.ui.theme.GoldAccent
import com.vibra.wanderai.ui.theme.NavyDeep
import com.vibra.wanderai.ui.theme.TealBrand

@Composable
fun WelcomeScreen(
    modifier: Modifier = Modifier,
    onGetStarted: () -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Logo placeholder ─────────────────────────────────────────────────
        // Replace this Box with an Image(painter = painterResource(R.drawable.ic_logo))
        // once the logo PNG/SVG is added to res/drawable/.
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(NavyDeep, TealBrand)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "W",
                color = GoldAccent,
                fontSize = 44.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Brand name ───────────────────────────────────────────────────────
        Text(
            text = "Wander",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = NavyDeep
        )
        Text(
            text = "AI",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = TealBrand,
            modifier = Modifier.padding(top = (-10).dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.welcome_subtitle),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.welcome_description),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(40.dp))

        // ── Actions ──────────────────────────────────────────────────────────
        Button(
            onClick = onGetStarted,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.get_started),
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.login_button),
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onRegisterClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.register_button),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
