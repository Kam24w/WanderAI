package com.vibra.wanderai.presentation.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibra.wanderai.R
import com.vibra.wanderai.domain.model.SignUpResult
import com.vibra.wanderai.domain.repository.AuthRepository
import com.vibra.wanderai.domain.usecase.CredentialValidator
import com.vibra.wanderai.domain.usecase.NameIssue
import com.vibra.wanderai.domain.usecase.PasswordRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthFormError(@param:StringRes val messageRes: Int) {
    EMPTY_FIELDS(R.string.error_empty_fields),
    INVALID_EMAIL(R.string.error_invalid_email),
    FIX_FIELDS(R.string.error_fix_fields),
    LOGIN_FAILED(R.string.error_login_failed),
    REGISTER_FAILED(R.string.error_register_failed),
}

@get:StringRes
val NameIssue.messageRes: Int
    get() = when (this) {
        NameIssue.TOO_SHORT -> R.string.error_name_short
        NameIssue.TOO_LONG -> R.string.error_name_long
        NameIssue.INVALID_CHARACTERS -> R.string.error_name_chars
    }

@get:StringRes
val PasswordRule.labelRes: Int
    get() = when (this) {
        PasswordRule.MIN_LENGTH -> R.string.password_rule_length
        PasswordRule.UPPERCASE -> R.string.password_rule_upper
        PasswordRule.DIGIT -> R.string.password_rule_digit
        PasswordRule.SPECIAL -> R.string.password_rule_special
        PasswordRule.NOT_PREDICTABLE -> R.string.password_rule_predictable
    }

// ── Login ─────────────────────────────────────────────────────────────────────

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: AuthFormError? = null,
)

/** On success Supabase emits Authenticated and the root gate opens the app. */
class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun submit() {
        val current = _state.value
        val email = current.email.trim()
        val error = when {
            email.isBlank() || current.password.isBlank() -> AuthFormError.EMPTY_FIELDS
            !CredentialValidator.isValidEmail(email) -> AuthFormError.INVALID_EMAIL
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.signIn(email, current.password)
            _state.update {
                it.copy(isLoading = false, error = if (result.isFailure) AuthFormError.LOGIN_FAILED else null)
            }
        }
    }
}

// ── Register ──────────────────────────────────────────────────────────────────

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: AuthFormError? = null,
    /** Set when Supabase requires e-mail confirmation before the first sign-in. */
    val confirmationSentTo: String? = null,
    /** Field errors are shown after the first submit attempt (then update live). */
    val showFieldErrors: Boolean = false,
) {
    val nameIssue: NameIssue? get() = CredentialValidator.validateName(name)
    val isEmailValid: Boolean get() = CredentialValidator.isValidEmail(email)
    val failedPasswordRules: Set<PasswordRule> get() = CredentialValidator.failedPasswordRules(password)
    val passwordsMatch: Boolean get() = password == confirmPassword && confirmPassword.isNotEmpty()

    val isValid: Boolean
        get() = nameIssue == null && isEmailValid && failedPasswordRules.isEmpty() && passwordsMatch
}

class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun onNameChange(value: String) =
        _state.update { it.copy(name = value.take(CredentialValidator.NAME_MAX), error = null) }
    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }
    fun onConfirmPasswordChange(value: String) = _state.update { it.copy(confirmPassword = value, error = null) }

    fun submit() {
        val s = _state.value
        val email = s.email.trim()
        if (!s.isValid) {
            val error = if (listOf(s.name, email, s.password, s.confirmPassword).any(String::isBlank)) {
                AuthFormError.EMPTY_FIELDS
            } else {
                AuthFormError.FIX_FIELDS
            }
            _state.update { it.copy(error = error, showFieldErrors = true) }
            return
        }
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.signUp(s.name.trim(), email, s.password)
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            confirmationSentTo = email.takeIf { result == SignUpResult.CONFIRMATION_REQUIRED },
                        )
                    }
                }
                .onFailure { _state.update { it.copy(isLoading = false, error = AuthFormError.REGISTER_FAILED) } }
        }
    }
}
