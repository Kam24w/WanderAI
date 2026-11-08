package com.vibra.wanderai.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibra.wanderai.R
import com.vibra.wanderai.domain.model.ChatMessage
import com.vibra.wanderai.domain.model.ChatRole
import com.vibra.wanderai.domain.model.GeoPoint
import com.vibra.wanderai.domain.model.Place
import com.vibra.wanderai.domain.model.PlaceSource
import com.vibra.wanderai.presentation.common.NumberInput
import com.vibra.wanderai.presentation.common.color
import com.vibra.wanderai.presentation.common.currentLanguageCode
import com.vibra.wanderai.presentation.common.formatCopShort
import com.vibra.wanderai.presentation.common.formatDistance
import com.vibra.wanderai.presentation.common.formatDuration
import com.vibra.wanderai.presentation.common.icon
import com.vibra.wanderai.presentation.common.labelRes
import com.vibra.wanderai.presentation.common.wanderViewModel
import com.vibra.wanderai.presentation.components.IconBadge
import com.vibra.wanderai.presentation.components.WanderTopBar
import com.vibra.wanderai.ui.theme.WanderTheme
import kotlin.math.roundToInt

/**
 * Conversational travel assistant (Groq). Every question goes out with the traveler's
 * coordinates and the Plan filters; answers end with tappable place cards that open
 * the Explore map on that place.
 */
@Composable
fun TravelChatScreen(
    onBack: () -> Unit,
    onShowPlaceOnMap: () -> Unit,
) {
    val viewModel = wanderViewModel {
        TravelChatViewModel(it.travelChatRepository, it.chatHistoryRepository, it.planSession, it.observeTravelerLocation)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val language = currentLanguageCode()
    var input by rememberSaveable { mutableStateOf("") }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val send = { text: String ->
        viewModel.send(text, language)
        input = ""
    }

    // Keep the newest bubble (or the "thinking" row) in view.
    val itemCount = state.messages.size + (if (state.isSending || state.error != null) 1 else 0) + 1
    LaunchedEffect(itemCount) { listState.animateScrollToItem(itemCount - 1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding(),
    ) {
        WanderTopBar(
            title = stringResource(R.string.chat_title),
            onBackClick = onBack,
            actions = {
                IconButton(onClick = {
                    viewModel.loadHistory()
                    showHistory = true
                }) {
                    Icon(Icons.Outlined.History, contentDescription = stringResource(R.string.chat_history_title))
                }
                if (state.messages.isNotEmpty()) {
                    IconButton(onClick = viewModel::newConversation) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = stringResource(R.string.chat_new))
                    }
                }
            },
        )
        FiltersStrip(state)

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "welcome") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistantBubble(text = stringResource(R.string.chat_welcome))
                    if (state.messages.isEmpty()) {
                        listOf(R.string.chat_suggestion_dinner, R.string.chat_suggestion_free, R.string.chat_suggestion_view)
                            .forEach { res ->
                                val suggestion = stringResource(res)
                                SuggestionChip(
                                    onClick = { send(suggestion) },
                                    enabled = !state.isSending,
                                    label = { Text(suggestion) },
                                )
                            }
                    }
                }
            }
            items(state.messages, key = { it.id }) { message ->
                when (message.role) {
                    ChatRole.USER -> UserBubble(message.text)
                    ChatRole.ASSISTANT -> AssistantMessage(
                        message = message,
                        origin = state.location.point,
                        onPlaceClick = { place ->
                            viewModel.showOnMap(place)
                            onShowPlaceOnMap()
                        },
                    )
                }
            }
            if (state.isSending) {
                item(key = "thinking") { ThinkingRow() }
            } else {
                state.error?.let { error ->
                    item(key = "error") {
                        ErrorRow(text = stringResource(error.messageRes), onRetry = { viewModel.retry(language) })
                    }
                }
            }
        }

        if (state.saveFailed) {
            Text(
                stringResource(R.string.chat_history_save_failed),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        InputBar(
            value = input,
            onValueChange = { input = it },
            enabled = !state.isSending,
            onSend = { send(input) },
        )
    }

    if (showHistory) {
        ChatHistorySheet(
            state = state.history,
            currentConversationId = state.conversationId,
            onOpen = {
                viewModel.openConversation(it)
                showHistory = false
            },
            onDelete = viewModel::deleteConversation,
            onDeleteAll = viewModel::deleteAllHistory,
            onRetry = viewModel::loadHistory,
            onDismiss = { showHistory = false },
        )
    }
}

@Composable
private fun FiltersStrip(state: TravelChatUiState) {
    val filters = state.filters
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Tune, contentDescription = null, tint = WanderTheme.colors.brandTeal, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(
                R.string.chat_filters,
                formatDuration(filters.availableMinutes),
                formatCopShort(filters.budgetCop),
                NumberInput.formatKm(filters.maxDistanceKm),
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(if (state.location.isLive) R.string.gps_live else R.string.gps_reference),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (state.location.isLive) WanderTheme.colors.success else WanderTheme.colors.brandGold,
        )
    }
}

@Composable
private fun UserBubble(text: String) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Surface(
            shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
            color = WanderTheme.colors.brandNavy,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Text(
                text = text,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun AssistantBubble(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(WanderTheme.colors.brandTeal),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun AssistantMessage(message: ChatMessage, origin: GeoPoint, onPlaceClick: (Place) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistantBubble(message.text)
        if (message.recommendations.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(start = 38.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(message.recommendations, key = { it.id }) { place ->
                    RecommendationCard(
                        place = place,
                        distanceMeters = origin.distanceTo(place.location).roundToInt(),
                        onClick = { onPlaceClick(place) },
                    )
                }
            }
        }
    }
}

/** Interactive card attached to an AI answer: tap to fly to the place on the map. */
@Composable
private fun RecommendationCard(place: Place, distanceMeters: Int, onClick: () -> Unit) {
    val showOnMap = stringResource(R.string.chat_show_on_map)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClickLabel = showOnMap, onClick = onClick),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon = place.category.icon, tint = place.category.color(), size = 38.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = place.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(place.category.labelRes) +
                            if (place.source == PlaceSource.GOOGLE) " · " + stringResource(R.string.chat_place_nearby) else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = WanderTheme.colors.brandGold, modifier = Modifier.size(14.dp))
                Text(
                    text = " ${place.rating} · " +
                        (if (place.averageCostCop == 0) stringResource(R.string.common_free) else formatCopShort(place.averageCostCop)) +
                        (if (place.isCostEstimated && place.averageCostCop > 0) " " + stringResource(R.string.chat_cost_estimated) else "") +
                        " · " + formatDistance(distanceMeters),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Map, contentDescription = null, tint = WanderTheme.colors.brandTeal, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(showOnMap, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = WanderTheme.colors.brandTeal)
            }
        }
    }
}

@Composable
private fun ThinkingRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(start = 38.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        CircularProgressIndicator(strokeWidth = 2.dp, color = WanderTheme.colors.brandTeal, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.chat_thinking), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ErrorRow(text: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
        TextButton(onClick = onRetry) { Text(stringResource(R.string.chat_retry), fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun InputBar(value: String, onValueChange: (String) -> Unit, enabled: Boolean, onSend: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(stringResource(R.string.chat_input_hint)) },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (enabled && value.isNotBlank()) onSend() }),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WanderTheme.colors.brandTeal),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = onSend,
                enabled = enabled && value.isNotBlank(),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = WanderTheme.colors.brandTeal, contentColor = Color.White),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.chat_send))
            }
        }
    }
}
