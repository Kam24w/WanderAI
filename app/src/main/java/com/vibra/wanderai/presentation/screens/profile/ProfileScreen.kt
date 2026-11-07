package com.vibra.wanderai.presentation.screens.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.ExploreOff
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibra.wanderai.R
import com.vibra.wanderai.presentation.components.WanderTopBar
import com.vibra.wanderai.ui.theme.GoldAccent
import com.vibra.wanderai.ui.theme.NavyDeep
import com.vibra.wanderai.ui.theme.TealBrand

@Composable
fun ProfileScreen(
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onVibraClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar with settings action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WanderTopBar(modifier = Modifier.weight(1f))
            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.settings_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Hero header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to NavyDeep,
                                0.7f to Color(0xFF0F2440),
                                1.0f to MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .padding(top = 28.dp, bottom = 36.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Avatar ring
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .background(
                                Brush.linearGradient(listOf(TealBrand, NavyDeep)),
                                CircleShape
                            )
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(TealBrand),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "C",
                                color = Color.White,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Carlos Mejía",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Outlined.LocationOn,
                            null,
                            tint = TealBrand,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Pasto, Colombia",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    icon = Icons.Outlined.EmojiEvents,
                    value = "25",
                    label = stringResource(R.string.profile_total_logros),
                    tint = GoldAccent,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Outlined.LocationOn,
                    value = "110",
                    label = stringResource(R.string.profile_places),
                    tint = TealBrand,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Outlined.Route,
                    value = "48 km",
                    label = stringResource(R.string.profile_distance),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(24.dp))

            // Saved itineraries section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Route,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.profile_saved_itineraries),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(Modifier.height(12.dp))

            ItineraryCard(
                title = "Cultura e Historia",
                subtitle = "3h · \$40,000 COP",
                tag = "Cultural",
                tagColor = NavyDeep
            )

            Spacer(Modifier.height(10.dp))

            ItineraryCard(
                title = "Naturaleza y Café",
                subtitle = "3h · \$40,000 COP",
                tag = "Naturaleza",
                tagColor = TealBrand
            )

            Spacer(Modifier.height(24.dp))

            // Vibra quick-access
            Button(
                onClick = onVibraClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Outlined.Watch, null, tint = Color.White,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Vibra Watch",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = onLogoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(52.dp)
            ) {
                Text(
                    stringResource(R.string.logout_button),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ItineraryCard(
    title: String,
    subtitle: String,
    tag: String,
    tagColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(tagColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Route,
                    null,
                    tint = tagColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                modifier = Modifier
                    .background(tagColor.copy(alpha = 0.12f), shape = MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(tag, style = MaterialTheme.typography.labelSmall, color = tagColor, fontWeight = FontWeight.Medium)
            }
        }
    }
}
