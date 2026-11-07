package com.vibra.wanderai.presentation.screens.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vibra.wanderai.R
import com.vibra.wanderai.presentation.components.WanderTopBar
import com.vibra.wanderai.ui.theme.GoldAccent
import com.vibra.wanderai.ui.theme.NavyDeep
import com.vibra.wanderai.ui.theme.TealBrand

@Composable
fun ExploreScreen(onVibraClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        WanderTopBar()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Map placeholder ──────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    NavyDeep.copy(alpha = 0.08f),
                                    TealBrand.copy(alpha = 0.05f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Map,
                                contentDescription = null,
                                tint = TealBrand,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.explore_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.explore_coming_soon),
                            style = MaterialTheme.typography.bodySmall,
                            color = TealBrand,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Mock location pins
                    MockPin(
                        label = "Plz. Mayor",
                        color = GoldAccent,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 40.dp, top = 32.dp)
                    )
                    MockPin(
                        label = "Café A-5",
                        color = TealBrand,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 36.dp, top = 48.dp)
                    )
                    MockPin(
                        label = "Museo",
                        color = NavyDeep,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 56.dp, bottom = 40.dp)
                    )
                }
            }

            // ── Nearby places ────────────────────────────────────────────────
            Text(
                text = "Cerca de ti",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NearbyPlaceCard(
                    name = "Plaza Mayor",
                    distance = "0.3 km",
                    tag = "Cultural",
                    tagColor = NavyDeep,
                    modifier = Modifier.weight(1f)
                )
                NearbyPlaceCard(
                    name = "Café de la Catedral",
                    distance = "0.5 km",
                    tag = "Café",
                    tagColor = TealBrand,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Watch Sync Status card ────────────────────────────────────────
            WatchSyncBannerCard(onVibraClick = onVibraClick)

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun MockPin(label: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.9f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.LocationOn, null, tint = Color.White,
            modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(3.dp))
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun NearbyPlaceCard(
    name: String,
    distance: String,
    tag: String,
    tagColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .background(tagColor.copy(alpha = 0.1f), MaterialTheme.shapes.small)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(tag, style = MaterialTheme.typography.labelSmall,
                    color = tagColor, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            Text(name, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(2.dp))
                Text(distance, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun WatchSyncBannerCard(onVibraClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(NavyDeep.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Watch, null, tint = NavyDeep, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.vibra_sync_status),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.vibra_disconnected),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.explore_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = onVibraClick,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep)
            ) {
                Icon(Icons.Outlined.Watch, null, tint = Color.White,
                    modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.explore_connect_vibra),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
