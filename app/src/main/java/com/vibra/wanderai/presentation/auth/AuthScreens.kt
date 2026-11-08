package com.vibra.wanderai.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.vibra.wanderai.domain.usecase.CredentialValidator
import com.vibra.wanderai.domain.usecase.PasswordRule
import com.vibra.wanderai.R
import com.vibra.wanderai.presentation.common.wanderViewModel
import com.vibra.wanderai.presentation.components.PrimaryButton
import com.vibra.wanderai.presentation.components.WanderLogo
import com.vibra.wanderai.presentation.components.WanderWordmark
import com.vibra.wanderai.ui.theme.WanderTheme

private object AuthRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
}

/** Unauthenticated graph. There is intentionally no route into the app from here. */
@Composable
fun AuthNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = AuthRoutes.LOGIN) {
        composable(AuthRoutes.LOGIN) {
            LoginScreen(onRegisterClick = { navController.navigate(AuthRoutes.REGISTER) })
        }
        composable(AuthRoutes.REGISTER) {
            RegisterScreen(
                onBackToLogin = {
                    if (!navController.popBackStack()) navController.navigate(AuthRoutes.LOGIN)
                },
            )
        }
    }
}

// ── Login ─────────────────────────────────────────────────────────────────────

@Composable
fun LoginScreen(onRegisterClick: () -> Unit) {
    val viewModel = wanderViewModel { LoginViewModel(it.authRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current

    AuthScaffold(
        title = stringResource(R.string.login_title),
        subtitle = stringResource(R.string.login_subtitle),
    ) {
        AuthTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = stringResource(R.string.email_label),
            icon = Icons.Outlined.Mail,
            enabled = !state.isLoading,
            keyboardType = KeyboardType.Email,
            onImeAction = { focus.moveFocus(FocusDirection.Down) },
        )
        Spacer(Modifier.height(12.dp))
        PasswordField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = stringResource(R.string.password_label),
            enabled = !state.isLoading,
            imeAction = ImeAction.Done,
            onImeAction = {
                focus.clearFocus()
                viewModel.submit()
            },
        )
        FormError(state.error)
        Spacer(Modifier.height(22.dp))
        PrimaryButton(
            text = stringResource(R.string.login_submit),
            icon = Icons.AutoMirrored.Outlined.Login,
            loading = state.isLoading,
            onClick = {
                focus.clearFocus()
                viewModel.submit()
            },
        )
        Spacer(Modifier.height(12.dp))
        SwitchAuthRow(
            question = stringResource(R.string.login_no_account),
            action = stringResource(R.string.login_go_register),
            enabled = !state.isLoading,
            onClick = onRegisterClick,
        )
    }
}

// ── Register ──────────────────────────────────────────────────────────────────

@Composable
fun RegisterScreen(onBackToLogin: () -> Unit) {
    val viewModel = wanderViewModel { RegisterViewModel(it.authRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current

    AuthScaffold(
        title = stringResource(R.string.register_title),
        subtitle = stringResource(R.string.register_subtitle),
        onBackClick = onBackToLogin,
    ) {
        val sentTo = state.confirmationSentTo
        if (sentTo != null) {
            ConfirmationSent(email = sentTo, onBackToLogin = onBackToLogin)
            return@AuthScaffold
        }

        val showErrors = state.showFieldErrors
        AuthTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            label = stringResource(R.string.name_label),
            icon = Icons.Outlined.Person,
            enabled = !state.isLoading,
            keyboardType = KeyboardType.Text,
            onImeAction = { focus.moveFocus(FocusDirection.Down) },
            error = state.nameIssue?.takeIf { showErrors }?.let { stringResource(it.messageRes) },
            supporting = stringResource(R.string.name_counter, state.name.trim().length, CredentialValidator.NAME_MAX),
        )
        Spacer(Modifier.height(12.dp))
        AuthTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = stringResource(R.string.email_label),
            icon = Icons.Outlined.Mail,
            enabled = !state.isLoading,
            keyboardType = KeyboardType.Email,
            onImeAction = { focus.moveFocus(FocusDirection.Down) },
            error = if (showErrors && !state.isEmailValid) stringResource(R.string.error_invalid_email) else null,
        )
        Spacer(Modifier.height(12.dp))
        PasswordField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = stringResource(R.string.password_label),
            supporting = stringResource(R.string.password_hint),
            enabled = !state.isLoading,
            imeAction = ImeAction.Next,
            onImeAction = { focus.moveFocus(FocusDirection.Down) },
            isError = showErrors && state.failedPasswordRules.isNotEmpty(),
        )
        AnimatedVisibility(visible = state.password.isNotEmpty() || showErrors) {
            PasswordChecklist(failed = state.failedPasswordRules, highlightFailures = showErrors)
        }
        Spacer(Modifier.height(12.dp))
        PasswordField(
            value = state.confirmPassword,
            onValueChange = viewModel::onConfirmPasswordChange,
            label = stringResource(R.string.confirm_password_label),
            supporting = if (showErrors && !state.passwordsMatch) stringResource(R.string.error_passwords_mismatch) else null,
            isError = showErrors && !state.passwordsMatch,
            enabled = !state.isLoading,
            imeAction = ImeAction.Done,
            onImeAction = {
                focus.clearFocus()
                viewModel.submit()
            },
        )
        FormError(state.error)
        Spacer(Modifier.height(22.dp))
        PrimaryButton(
            text = stringResource(R.string.register_submit),
            icon = Icons.Outlined.PersonAdd,
            loading = state.isLoading,
            containerColor = WanderTheme.colors.brandTeal,
            onClick = {
                focus.clearFocus()
                viewModel.submit()
            },
        )
        Spacer(Modifier.height(12.dp))
        SwitchAuthRow(
            question = stringResource(R.string.register_have_account),
            action = stringResource(R.string.register_go_login),
            enabled = !state.isLoading,
            onClick = onBackToLogin,
        )
    }
}

@Composable
private fun ConfirmationSent(email: String, onBackToLogin: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Icon(
            Icons.Outlined.MarkEmailRead,
            contentDescription = null,
            tint = WanderTheme.colors.success,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.register_success_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.register_success, email),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(22.dp))
        PrimaryButton(text = stringResource(R.string.register_go_login), onClick = onBackToLogin)
    }
}

// ── Shared building blocks ────────────────────────────────────────────────────

@Composable
private fun AuthScaffold(
    title: String,
    subtitle: String,
    onBackClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = WanderTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(Brush.verticalGradient(listOf(colors.heroStart, colors.heroEnd))),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth().height(48.dp)) {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = Color.White,
                        )
                    }
                }
            }
            WanderLogo(size = 84.dp, padding = 7.dp, onDark = true)
            Spacer(Modifier.height(12.dp))
            WanderWordmark(onDark = true, fontSizeSp = 30)
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.auth_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(26.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            ) {
                Column(Modifier.padding(horizontal = 22.dp, vertical = 26.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(22.dp))
                    content()
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    keyboardType: KeyboardType,
    onImeAction: () -> Unit,
    error: String? = null,
    supporting: String? = null,
) {
    OutlinedTextField(
        isError = error != null,
        supportingText = (error ?: supporting)?.let { { Text(it) } },
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        singleLine = true,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = authFieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onImeAction() }),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    imeAction: ImeAction,
    onImeAction: () -> Unit,
    supporting: String? = null,
    isError: Boolean = false,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        isError = isError,
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = stringResource(
                        if (visible) R.string.password_hide else R.string.password_show
                    ),
                )
            }
        },
        supportingText = supporting?.let { { Text(it) } },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = authFieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(onNext = { onImeAction() }, onDone = { onImeAction() }),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Live password requirements: green when met, red after a failed submit. */
@Composable
private fun PasswordChecklist(failed: Set<PasswordRule>, highlightFailures: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            stringResource(R.string.password_rules_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PasswordRule.entries.forEach { rule ->
            val met = rule !in failed
            val color = when {
                met -> WanderTheme.colors.success
                highlightFailures -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            val state = stringResource(if (met) R.string.password_rule_met else R.string.password_rule_pending)
            val label = stringResource(rule.labelRes)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "$label, $state" },
            ) {
                Icon(
                    if (met) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.bodySmall, color = color)
            }
        }
    }
}

@Composable
private fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = WanderTheme.colors.brandTeal,
    focusedLeadingIconColor = WanderTheme.colors.brandTeal,
    focusedLabelColor = WanderTheme.colors.brandTeal,
    cursorColor = WanderTheme.colors.brandTeal,
)

@Composable
private fun FormError(error: AuthFormError?) {
    AnimatedVisibility(visible = error != null) {
        Row(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = error?.let { stringResource(it.messageRes) }.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun SwitchAuthRow(question: String, action: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(question, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onClick, enabled = enabled) {
            Text(action, fontWeight = FontWeight.Bold, color = WanderTheme.colors.brandTeal)
        }
    }
}
