package com.vibra.wanderai.presentation.profile

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibra.wanderai.R
import com.vibra.wanderai.domain.model.Achievement
import com.vibra.wanderai.domain.model.Itinerary
import com.vibra.wanderai.domain.model.TravelerStats
import com.vibra.wanderai.presentation.common.color
import com.vibra.wanderai.presentation.common.formatCopShort
import com.vibra.wanderai.presentation.common.formatDuration
import com.vibra.wanderai.presentation.common.icon
import com.vibra.wanderai.presentation.common.itineraryTitleRes
import com.vibra.wanderai.presentation.common.labelRes
import com.vibra.wanderai.presentation.common.wanderViewModel
import com.vibra.wanderai.presentation.components.SectionHeader
import com.vibra.wanderai.presentation.components.WanderCard
import com.vibra.wanderai.presentation.components.WanderTopBar
import com.vibra.wanderai.ui.theme.WanderTheme
import java.util.Locale

@Composable
fun ProfileScreen(
    onSettingsClick: () -> Unit,
    onOpenItinerary: () -> Unit,
) {
    val viewModel = wanderViewModel {
        ProfileViewModel(it.authRepository, it.travelerRepository, it.itineraryRepository, it.deleteAccount)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var editing by rememberSaveable { mutableStateOf(false) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }

    val reportText = state.stats?.let { stats ->
        stringResource(
            R.string.profile_report_text,
            state.user?.displayName.orEmpty(),
            stats.trips,
            stats.placesVisited,
            String.format(Locale.US, "%.1f", stats.weeklyDistanceKm.sum()),
            state.savedItineraries.size,
        )
    }
    val shareTitle = stringResource(R.string.profile_report)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        WanderTopBar(
            actions = {
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings_title))
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ProfileHeader(name = state.user?.displayName.orEmpty(), email = state.user?.email.orEmpty())

            state.stats?.let { stats ->
                AchievementsCard(stats)
                ExplorationCard(stats)
                DistanceCard(stats)
            }

            SavedItinerariesCard(
                itineraries = state.savedItineraries,
                onOpen = {
                    viewModel.openItinerary(it)
                    onOpenItinerary()
                },
                onDelete = viewModel::deleteItinerary,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionButton(
                    text = stringResource(R.string.profile_report),
                    icon = Icons.Outlined.Share,
                    container = WanderTheme.colors.brandGold,
                    content = WanderTheme.colors.brandNavy,
                    enabled = reportText != null,
                    modifier = Modifier.weight(1f),
                ) {
                    val send = Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, reportText)
                    context.startActivity(Intent.createChooser(send, shareTitle))
                }
                ActionButton(
                    text = stringResource(R.string.profile_edit),
                    icon = Icons.Outlined.Edit,
                    container = WanderTheme.colors.brandNavy,
                    content = Color.White,
                    modifier = Modifier.weight(1f),
                ) { editing = true }
            }
            DeleteAccountButton(
                isDeleting = state.isDeletingAccount,
                onClick = { confirmingDelete = true },
            )
            Spacer(Modifier.height(4.dp))
        }
    }

    if (editing) {
        EditNameDialog(
            initial = state.user?.displayName.orEmpty(),
            isSaving = state.isSavingName,
            failed = state.nameUpdateFailed,
            onDismiss = {
                editing = false
                viewModel.clearNameError()
            },
            onSave = { name -> viewModel.updateDisplayName(name) { editing = false } },
        )
    }

    if (confirmingDelete || state.accountDeletionFailed) {
        DeleteAccountDialog(
            isDeleting = state.isDeletingAccount,
            failed = state.accountDeletionFailed,
            onConfirm = viewModel::deleteAccountAndData,
            onDismiss = {
                confirmingDelete = false
                viewModel.clearDeleteError()
            },
        )
    }
}

/** GDPR: erases the Supabase user and, by cascade, all of their data. */
@Composable
private fun DeleteAccountButton(isDeleting: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = !isDeleting,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        if (isDeleting) {
            CircularProgressIndicator(strokeWidth = 2.dp, color = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
        } else {
            Icon(Icons.Outlined.PersonRemove, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.profile_delete_account), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DeleteAccountDialog(
    isDeleting: Boolean,
    failed: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        icon = { Icon(Icons.Outlined.PersonRemove, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(R.string.profile_delete_title)) },
        text = {
            Column {
                Text(stringResource(R.string.profile_delete_message))
                if (failed) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.profile_delete_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                } else {
                    Text(stringResource(R.string.profile_delete_confirm), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@Composable
private fun ProfileHeader(name: String, email: String) {
    val colors = WanderTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(colors.heroStart, colors.heroEnd)))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .border(3.dp, Brush.sweepGradient(listOf(colors.brandTeal, colors.brandGold, colors.brandTeal)), CircleShape)
                .padding(5.dp)
                .clip(CircleShape)
                .background(colors.brandTeal),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.initials(),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = name.uppercase(),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, null, tint = colors.brandTeal, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(3.dp))
                Text(stringResource(R.string.profile_city), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
            }
            Text(email, color = Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AchievementsCard(stats: TravelerStats) {
    WanderCard {
        SectionHeader(
            title = stringResource(R.string.profile_total_achievements),
            icon = Icons.Outlined.EmojiEvents,
            iconTint = WanderTheme.colors.brandGold,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            BigStat(stats.trips.toString(), stringResource(R.string.profile_trips), Modifier.weight(1f))
            BigStat(stats.placesVisited.toString(), stringResource(R.string.profile_places), Modifier.weight(1f))
            BigStat(
                "${stats.achievements.count(Achievement::isUnlocked)}/${stats.achievements.size}",
                stringResource(R.string.profile_badges),
                Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(14.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(stats.achievements, key = { it.type }) { AchievementBadge(it) }
        }
    }
}

@Composable
private fun BigStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AchievementBadge(achievement: Achievement) {
    val tint = if (achievement.isUnlocked) WanderTheme.colors.brandGold else MaterialTheme.colorScheme.outline
    val label = stringResource(achievement.type.labelRes)
    Column(
        modifier = Modifier.width(84.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.15f))
                .border(2.dp, tint.copy(alpha = if (achievement.isUnlocked) 1f else 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (achievement.isUnlocked) achievement.type.icon else Icons.Outlined.Lock,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2, minLines = 2)
        if (achievement.isUnlocked) {
            Text(
                stringResource(R.string.profile_achievement_unlocked),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = WanderTheme.colors.brandGold,
            )
        } else {
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { achievement.progress },
                color = WanderTheme.colors.brandTeal,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .width(56.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
            )
        }
    }
}

@Composable
private fun ExplorationCard(stats: TravelerStats) {
    val days = stringArrayResource(R.array.weekdays_short).toList()
    val description = stringResource(R.string.chart_exploration_a11y) + ": " +
        days.zip(stats.weeklyExplorationMinutes).joinToString { (d, m) -> "$d ${formatDuration(m)}" }
    WanderCard {
        SectionHeader(
            title = stringResource(R.string.profile_exploration_time),
            icon = Icons.Outlined.Timeline,
            iconTint = WanderTheme.colors.brandTeal,
            trailing = {
                Text(
                    stringResource(R.string.profile_weekly),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp),
                )
            },
        )
        Text(
            text = stringResource(R.string.profile_total_week, formatDuration(stats.weeklyExplorationMinutes.sum())),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        AreaLineChart(
            values = stats.weeklyExplorationMinutes.map(Int::toFloat),
            labels = days,
            formatValue = { formatDuration(it.toInt()) },
            accessibilityDescription = description,
        )
    }
}

@Composable
private fun DistanceCard(stats: TravelerStats) {
    val weekLabel = stringResource(R.string.chart_week_prefix)
    val labels = stats.weeklyDistanceKm.indices.map { "$weekLabel${it + 1}" }
    val description = stringResource(R.string.chart_distance_a11y) + ": " +
        labels.zip(stats.weeklyDistanceKm).joinToString { (w, km) -> "$w ${String.format(Locale.US, "%.1f", km)} km" }
    WanderCard {
        SectionHeader(
            title = stringResource(R.string.profile_distance),
            icon = Icons.Outlined.BarChart,
            iconTint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.profile_distance_total, String.format(Locale.US, "%.1f", stats.weeklyDistanceKm.sum())),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        ColumnChart(
            values = stats.weeklyDistanceKm,
            labels = labels,
            formatValue = { if (it >= 10f) "${it.toInt()} km" else String.format(Locale.US, "%.1f km", it) },
            accessibilityDescription = description,
        )
    }
}

@Composable
private fun SavedItinerariesCard(
    itineraries: List<Itinerary>,
    onOpen: (Itinerary) -> Unit,
    onDelete: (Itinerary) -> Unit,
) {
    WanderCard {
        SectionHeader(
            title = stringResource(R.string.profile_saved_itineraries),
            icon = Icons.Outlined.Bookmarks,
            iconTint = WanderTheme.colors.accentWarm,
        )
        Spacer(Modifier.height(10.dp))
        if (itineraries.isEmpty()) {
            Text(
                stringResource(R.string.profile_no_itineraries),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        itineraries.forEach { itinerary ->
            val category = itinerary.dominantCategory
            val tint = category.color()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onOpen(itinerary) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(tint, tint.copy(alpha = 0.55f)))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(category.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(category.itineraryTitleRes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${formatDuration(itinerary.totalMinutes)} · ${formatCopShort(itinerary.estimatedCostCop)} COP · " +
                            pluralStringResource(R.plurals.profile_stops, itinerary.stops.size, itinerary.stops.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.Outlined.Route, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                IconButton(onClick = { onDelete(itinerary) }) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.common_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: ImageVector,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        modifier = modifier.height(52.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun EditNameDialog(
    initial: String,
    isSaving: Boolean,
    failed: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_edit_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name_label)) },
                    singleLine = true,
                    enabled = !isSaving,
                    isError = failed,
                    supportingText = if (failed) {
                        { Text(stringResource(R.string.profile_edit_failed)) }
                    } else {
                        null
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }, enabled = !isSaving && name.isNotBlank()) {
                Text(stringResource(R.string.common_save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

private fun String.initials(): String =
    split(' ').filter(String::isNotBlank).take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "W" }
