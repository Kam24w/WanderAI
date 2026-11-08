package com.vibra.wanderai.presentation.recommendations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkAdded
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibra.wanderai.R
import com.vibra.wanderai.domain.model.Itinerary
import com.vibra.wanderai.domain.model.ItineraryFilters
import com.vibra.wanderai.domain.model.ItineraryStop
import com.vibra.wanderai.domain.model.PlaceCategory
import com.vibra.wanderai.presentation.common.color
import com.vibra.wanderai.presentation.common.formatCop
import com.vibra.wanderai.presentation.common.formatCopShort
import com.vibra.wanderai.presentation.common.formatDistance
import com.vibra.wanderai.presentation.common.formatDuration
import com.vibra.wanderai.presentation.common.icon
import com.vibra.wanderai.presentation.common.itineraryTitleRes
import com.vibra.wanderai.presentation.common.NumberInput
import com.vibra.wanderai.presentation.common.labelRes
import com.vibra.wanderai.presentation.common.walkingLabel
import com.vibra.wanderai.presentation.common.wanderViewModel
import com.vibra.wanderai.presentation.components.PrimaryButton
import com.vibra.wanderai.presentation.components.SectionCard
import com.vibra.wanderai.presentation.components.StatusPill
import com.vibra.wanderai.presentation.components.WanderSlider
import com.vibra.wanderai.presentation.components.WanderTopBar
import com.vibra.wanderai.ui.theme.WanderTheme

@Composable
fun RecommendationsScreen(
    onSettingsClick: () -> Unit,
    onShowOnMap: () -> Unit,
    onOpenChat: () -> Unit,
) {
    val viewModel = wanderViewModel {
        RecommendationsViewModel(it.generateItinerary, it.itineraryRepository, it.observeTravelerLocation, it.planSession)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        WanderTopBar(
            actions = {
                IconButton(onClick = onOpenChat) {
                    Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = stringResource(R.string.reco_open_chat))
                }
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
            Header(isLiveLocation = state.location.isLive)
            TimeCard(state.filters.availableMinutes, viewModel::onTimeChange)
            BudgetCard(state.filters.budgetCop, viewModel::onBudgetChange, viewModel::setExactBudget)
            DistanceCard(state.filters.maxDistanceKm, viewModel::onDistanceChange, viewModel::setExactDistance)
            CategoryCard(state.filters.categories, viewModel::toggleCategory)

            AnimatedVisibility(visible = state.noMatches) {
                InfoBanner(text = stringResource(R.string.reco_no_matches))
            }
            AnimatedVisibility(visible = state.filters.categories.isEmpty()) {
                InfoBanner(text = stringResource(R.string.reco_select_category))
            }

            PrimaryButton(
                text = stringResource(R.string.reco_generate),
                icon = Icons.Outlined.AutoAwesome,
                enabled = state.canGenerate,
                loading = state.isGenerating,
                onClick = viewModel::generate,
            )
            OutlinedButton(
                onClick = onOpenChat,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, tint = WanderTheme.colors.brandTeal)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.reco_open_chat), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
        }
    }

    state.result?.let { itinerary ->
        ItineraryResultSheet(
            itinerary = itinerary,
            isSaved = state.isResultSaved,
            onDismiss = viewModel::dismissResult,
            onSave = viewModel::saveResult,
            onShowOnMap = {
                viewModel.activateResult()
                onShowOnMap()
            },
            onStopClick = { stop ->
                viewModel.showStopOnMap(stop.place.id)
                onShowOnMap()
            },
        )
    }
}

@Composable
private fun Header(isLiveLocation: Boolean) {
    Column {
        Text(
            text = stringResource(R.string.reco_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.reco_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            StatusPill(
                text = stringResource(if (isLiveLocation) R.string.gps_live else R.string.gps_reference),
                color = if (isLiveLocation) WanderTheme.colors.success else WanderTheme.colors.brandGold,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun TimeCard(minutes: Int, onChange: (Float) -> Unit) {
    SectionCard(
        title = stringResource(R.string.reco_time),
        icon = Icons.Outlined.Schedule,
        iconTint = WanderTheme.colors.brandTeal,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnalogClock(totalMinutes = minutes)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                BigValue(formatDuration(minutes))
                WanderSlider(
                    value = minutes.toFloat(),
                    onValueChange = onChange,
                    valueRange = ItineraryFilters.TIME_RANGE_MINUTES.first.toFloat()..ItineraryFilters.TIME_RANGE_MINUTES.last.toFloat(),
                    startLabel = formatDuration(ItineraryFilters.TIME_RANGE_MINUTES.first),
                    endLabel = formatDuration(ItineraryFilters.TIME_RANGE_MINUTES.last),
                    accessibilityLabel = stringResource(R.string.reco_time),
                    valueDescription = formatDuration(minutes),
                    accent = WanderTheme.colors.brandTeal,
                )
            }
        }
    }
}

@Composable
private fun BudgetCard(budget: Int, onChange: (Float) -> Unit, onExact: (Int) -> Unit) {
    val range = ItineraryFilters.BUDGET_RANGE_COP
    var editing by rememberSaveable { mutableStateOf(false) }
    SectionCard(
        title = stringResource(R.string.reco_budget),
        icon = Icons.Outlined.AttachMoney,
        iconTint = WanderTheme.colors.brandGold,
    ) {
        BigValue(formatCop(budget), onClick = { editing = true })
        WanderSlider(
            value = budget.toFloat(),
            onValueChange = onChange,
            valueRange = range.first.toFloat()..range.last.toFloat(),
            startLabel = formatCopShort(range.first),
            endLabel = formatCopShort(range.last) + " COP",
            accessibilityLabel = stringResource(R.string.reco_budget),
            valueDescription = formatCop(budget),
            accent = MaterialTheme.colorScheme.primary,
        )
    }
    if (editing) {
        ExactValueDialog(
            title = stringResource(R.string.reco_edit_budget_title),
            initialText = budget.toString(),
            suffix = "COP",
            decimal = false,
            rangeLabelStart = formatCop(range.first),
            rangeLabelEnd = formatCop(range.last),
            sanitize = NumberInput::sanitizeCop,
            parse = NumberInput::parseCop,
            isInRange = { it in range },
            onConfirm = {
                onExact(it)
                editing = false
            },
            onDismiss = { editing = false },
        )
    }
}

@Composable
private fun DistanceCard(km: Float, onChange: (Float) -> Unit, onExact: (Float) -> Unit) {
    val range = ItineraryFilters.DISTANCE_RANGE_KM
    val label = NumberInput.formatKm(km)
    var editing by rememberSaveable { mutableStateOf(false) }
    SectionCard(
        title = stringResource(R.string.reco_distance),
        icon = Icons.Outlined.NearMe,
        iconTint = WanderTheme.colors.accentWarm,
    ) {
        BigValue(label, onClick = { editing = true })
        WanderSlider(
            value = km,
            onValueChange = onChange,
            valueRange = range,
            startLabel = formatDistance((range.start * 1000).toInt()),
            endLabel = formatDistance((range.endInclusive * 1000).toInt()),
            accessibilityLabel = stringResource(R.string.reco_distance),
            valueDescription = label,
            accent = WanderTheme.colors.accentWarm,
        )
    }
    if (editing) {
        ExactValueDialog(
            title = stringResource(R.string.reco_edit_distance_title),
            initialText = NumberInput.editableKm(km),
            suffix = "km",
            decimal = true,
            rangeLabelStart = NumberInput.formatKm(range.start),
            rangeLabelEnd = NumberInput.formatKm(range.endInclusive),
            sanitize = NumberInput::sanitizeKm,
            parse = NumberInput::parseKm,
            isInRange = { it in range },
            onConfirm = {
                onExact(it)
                editing = false
            },
            onDismiss = { editing = false },
        )
    }
}

@Composable
private fun CategoryCard(selected: Set<PlaceCategory>, onToggle: (PlaceCategory) -> Unit) {
    SectionCard(title = stringResource(R.string.reco_place_type)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PlaceCategory.selectable.forEach { category ->
                CategoryTile(
                    category = category,
                    selected = category in selected,
                    onToggle = { onToggle(category) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CategoryTile(
    category: PlaceCategory,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navy = WanderTheme.colors.brandNavy
    val container by animateColorAsState(
        if (selected) navy else MaterialTheme.colorScheme.surfaceContainerHigh, label = "tileBg"
    )
    val content by animateColorAsState(
        if (selected) Color.White else category.color(), label = "tileFg"
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() }),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(container)
                .then(
                    if (selected) Modifier
                    else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(category.icon, contentDescription = null, tint = content, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(category.labelRes),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** Big figure of a card. With [onClick] it becomes tappable to type the exact value. */
@Composable
private fun BigValue(text: String, onClick: (() -> Unit)? = null) {
    val modifier = if (onClick != null) {
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                onClickLabel = stringResource(R.string.reco_edit_action),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(end = 4.dp)
    } else {
        Modifier
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = (-0.5).sp,
        )
        if (onClick != null) {
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Outlined.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun InfoBanner(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
    }
}

// ── Result sheet ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItineraryResultSheet(
    itinerary: Itinerary,
    isSaved: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onShowOnMap: () -> Unit,
    onStopClick: (ItineraryStop) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = WanderTheme.colors.brandGold)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.reco_result_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(itinerary.dominantCategory.itineraryTitleRes),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryChip(Icons.Outlined.Schedule, formatDuration(itinerary.totalMinutes), Modifier.weight(1f))
                SummaryChip(Icons.Outlined.AttachMoney, formatCopShort(itinerary.estimatedCostCop), Modifier.weight(1f))
                SummaryChip(Icons.Outlined.NearMe, formatDistance(itinerary.totalDistanceMeters), Modifier.weight(1f))
            }
            Spacer(Modifier.height(18.dp))

            itinerary.stops.forEachIndexed { index, stop ->
                StopRow(
                    index = index,
                    stop = stop,
                    isLast = index == itinerary.stops.lastIndex,
                    onClick = { onStopClick(stop) },
                )
            }

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onSave,
                    enabled = !isSaved,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                ) {
                    Icon(
                        if (isSaved) Icons.Outlined.BookmarkAdded else Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(if (isSaved) R.string.reco_saved else R.string.reco_save), fontWeight = FontWeight.Bold)
                }
                PrimaryButton(
                    text = stringResource(R.string.reco_show_map),
                    icon = Icons.Outlined.Map,
                    onClick = onShowOnMap,
                    modifier = Modifier.weight(1.4f),
                )
            }
        }
    }
}

@Composable
private fun SummaryChip(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = WanderTheme.colors.brandTeal, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun StopRow(index: Int, stop: ItineraryStop, isLast: Boolean, onClick: () -> Unit) {
    val categoryColor = stop.place.category.color()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClickLabel = stringResource(R.string.chat_show_on_map), onClick = onClick),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(categoryColor),
                contentAlignment = Alignment.Center,
            ) {
                Text("${index + 1}", color = Color.White, fontWeight = FontWeight.ExtraBold)
            }
            if (!isLast) {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(44.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f).padding(top = 4.dp)) {
            Text(
                text = stop.place.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(stop.place.category.labelRes) + " · ★ " + stop.place.rating +
                    " · " + walkingLabel(stop.walkingMinutes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = if (stop.place.averageCostCop == 0) stringResource(R.string.common_free) else formatCopShort(stop.place.averageCostCop),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (stop.place.averageCostCop == 0) WanderTheme.colors.success else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
