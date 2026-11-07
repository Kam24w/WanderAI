package com.vibra.wanderai.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Museum
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibra.wanderai.R
import com.vibra.wanderai.presentation.components.WanderTopBar
import com.vibra.wanderai.ui.theme.NavyDeep
import com.vibra.wanderai.ui.theme.TealBrand
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    onSettingsClick: () -> Unit = {}
) {
    var timeHours by rememberSaveable { mutableStateOf(2.5f) }
    var budget by rememberSaveable { mutableStateOf(50000f) }
    var maxDistance by rememberSaveable { mutableStateOf(2.5f) }
    var selectedTypes by rememberSaveable { mutableStateOf(setOf<String>()) }

    val placeTypes = listOf(
        Triple(stringResource(R.string.place_restaurant), Icons.Outlined.Restaurant, "restaurant"),
        Triple(stringResource(R.string.place_park), Icons.Outlined.Park, "park"),
        Triple(stringResource(R.string.place_museum), Icons.Outlined.Museum, "museum"),
        Triple(stringResource(R.string.place_tour), Icons.Outlined.Map, "tour")
    )

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Greeting
            Column(Modifier.padding(bottom = 4.dp)) {
                Text(
                    text = stringResource(R.string.home_greeting),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.home_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal
                )
            }

            // ── Tiempo disponible ────────────────────────────────────────────
            SectionCard(
                title = stringResource(R.string.time_available).uppercase(),
                icon = Icons.Outlined.Schedule,
                iconTint = TealBrand
            ) {
                val hours = timeHours.toInt()
                val minutes = ((timeHours - hours) * 60).roundToInt()
                Text(
                    text = "${hours}h ${minutes.toString().padStart(2, '0')}min",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = timeHours,
                    onValueChange = { timeHours = it },
                    valueRange = 0.5f..8f,
                    colors = SliderDefaults.colors(
                        thumbColor = TealBrand,
                        activeTrackColor = TealBrand,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("30 min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("8 hrs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ── Presupuesto ──────────────────────────────────────────────────
            SectionCard(
                title = stringResource(R.string.budget_label).uppercase(),
                icon = Icons.Outlined.AttachMoney,
                iconTint = NavyDeep
            ) {
                Text(
                    text = "$${formatCOP(budget.toInt())} COP",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = budget,
                    onValueChange = { budget = it },
                    valueRange = 10000f..500000f,
                    colors = SliderDefaults.colors(
                        thumbColor = NavyDeep,
                        activeTrackColor = NavyDeep,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("\$10K", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("\$500K", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ── Distancia máxima ─────────────────────────────────────────────
            SectionCard(
                title = stringResource(R.string.max_distance).uppercase(),
                icon = Icons.Outlined.NearMe,
                iconTint = TealBrand
            ) {
                Text(
                    text = "${"%.1f".format(maxDistance)} km",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = maxDistance,
                    onValueChange = { maxDistance = it },
                    valueRange = 0.5f..10f,
                    colors = SliderDefaults.colors(
                        thumbColor = TealBrand,
                        activeTrackColor = TealBrand,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("500 m", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("10 km", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ── Tipo de lugar ────────────────────────────────────────────────
            SectionCard(title = stringResource(R.string.place_type_label).uppercase()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    placeTypes.forEach { (label, icon, key) ->
                        PlaceTypeButton(
                            label = label,
                            icon = icon,
                            selected = key in selectedTypes,
                            onClick = {
                                selectedTypes = if (key in selectedTypes) {
                                    selectedTypes - key
                                } else {
                                    selectedTypes + key
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── CTA ──────────────────────────────────────────────────────────
            Button(
                onClick = { /* TODO: generate itinerary */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                enabled = selectedTypes.isNotEmpty()
            ) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.generate_itinerary).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector? = null,
    iconTint: Color = TealBrand,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon?.let {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = iconTint.copy(alpha = 0.1f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(it, null, tint = iconTint, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp
                )
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun PlaceTypeButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(
                    color = if (selected) TealBrand else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) TealBrand else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatCOP(value: Int): String =
    when {
        value >= 1_000_000 -> "${"%.1f".format(value / 1_000_000f)}M"
        value >= 1_000 -> "${value / 1_000}K"
        else -> "$value"
    }
