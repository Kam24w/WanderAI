package com.vibra.wanderai.presentation.recommendations

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.vibra.wanderai.R

/**
 * Small modal to type an exact Plan value with the numeric keyboard instead of
 * dragging the slider. [parse] turns the text into a value; values outside
 * [isInRange] keep the dialog open with an error.
 */
@Composable
fun <T : Any> ExactValueDialog(
    title: String,
    initialText: String,
    suffix: String,
    decimal: Boolean,
    rangeLabelStart: String,
    rangeLabelEnd: String,
    sanitize: (String) -> String,
    parse: (String) -> T?,
    isInRange: (T) -> Boolean,
    onConfirm: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    var field by remember {
        mutableStateOf(TextFieldValue(initialText, selection = TextRange(0, initialText.length)))
    }
    var showError by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val submit = {
        val value = parse(field.text)
        if (value != null && isInRange(value)) onConfirm(value) else showError = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = field,
                onValueChange = {
                    field = it.copy(text = sanitize(it.text))
                    showError = false
                },
                singleLine = true,
                suffix = { Text(suffix) },
                isError = showError,
                supportingText = {
                    Text(
                        if (showError) stringResource(R.string.reco_edit_invalid, rangeLabelStart, rangeLabelEnd)
                        else stringResource(R.string.reco_edit_range, rangeLabelStart, rangeLabelEnd),
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.focusRequester(focusRequester),
            )
        },
        confirmButton = {
            TextButton(onClick = submit, enabled = field.text.isNotBlank()) {
                Text(stringResource(R.string.common_save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}
