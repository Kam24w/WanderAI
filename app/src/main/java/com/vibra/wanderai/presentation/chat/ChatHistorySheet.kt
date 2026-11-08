package com.vibra.wanderai.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vibra.wanderai.R
import com.vibra.wanderai.domain.model.ChatConversation
import com.vibra.wanderai.ui.theme.WanderTheme
import java.text.DateFormat
import java.util.Date

/**
 * Saved conversations of the signed-in traveler (`chat_history`): resume one,
 * delete it, or wipe the whole history after confirming.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistorySheet(
    state: ChatHistoryState,
    currentConversationId: String?,
    onOpen: (ChatConversation) -> Unit,
    onDelete: (String) -> Unit,
    onDeleteAll: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmDeleteAll by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.History, contentDescription = null, tint = WanderTheme.colors.brandTeal)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.chat_history_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                )
                if (state.conversations.isNotEmpty()) {
                    TextButton(
                        onClick = { confirmDeleteAll = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.chat_history_delete_all), fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (state.deleteFailed) {
                Text(
                    stringResource(R.string.chat_history_delete_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(12.dp))

            when {
                state.isLoading && state.conversations.isEmpty() -> Box(
                    Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = WanderTheme.colors.brandTeal)
                }
                state.loadFailed -> Column {
                    Text(stringResource(R.string.chat_history_error), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.chat_retry), fontWeight = FontWeight.Bold) }
                }
                state.conversations.isEmpty() -> Text(
                    stringResource(R.string.chat_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
                else -> LazyColumn(
                    modifier = Modifier.heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(state.conversations, key = { it.id }) { conversation ->
                        ConversationRow(
                            conversation = conversation,
                            isCurrent = conversation.id == currentConversationId,
                            onOpen = { onOpen(conversation) },
                            onDelete = { pendingDelete = conversation.id },
                        )
                    }
                }
            }
        }
    }

    if (confirmDeleteAll) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.chat_history_delete_all_title),
            message = stringResource(R.string.chat_history_delete_all_message),
            onConfirm = {
                confirmDeleteAll = false
                onDeleteAll()
            },
            onDismiss = { confirmDeleteAll = false },
        )
    }
    pendingDelete?.let { id ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.chat_history_delete_title),
            message = stringResource(R.string.chat_history_delete_message),
            onConfirm = {
                pendingDelete = null
                onDelete(id)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun ConversationRow(
    conversation: ChatConversation,
    isCurrent: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val date = remember(conversation.createdAtMillis) {
        if (conversation.createdAtMillis > 0) {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(conversation.createdAtMillis))
        } else {
            ""
        }
    }
    val container = if (isCurrent) WanderTheme.colors.brandTeal.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(container)
            .clickable(onClickLabel = stringResource(R.string.chat_history_open), onClick = onOpen)
            .padding(start = 14.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, tint = WanderTheme.colors.brandTeal, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                conversation.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOf(date, pluralStringResource(R.plurals.chat_history_messages, conversation.messages.size, conversation.messages.size))
                    .filter(String::isNotEmpty)
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.DeleteOutline,
                contentDescription = stringResource(R.string.chat_history_delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(title: String, message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(R.string.common_delete), fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
