package com.vibra.wanderai.domain.usecase

import java.util.Locale

/** Why a display name is rejected. */
enum class NameIssue { TOO_SHORT, TOO_LONG, INVALID_CHARACTERS }

/** Password rules shown as a checklist while typing; all must pass to register. */
enum class PasswordRule {
    MIN_LENGTH,
    UPPERCASE,
    DIGIT,
    SPECIAL,

    /** No runs like "1234", "abcd", "4321" or "aaaa", and not a well-known weak password. */
    NOT_PREDICTABLE,
}

/**
 * Account and profile input rules (registration, sign-in and profile edits).
 * Pure Kotlin so every rule is unit-tested.
 */
object CredentialValidator {
    const val NAME_MIN = 2
    const val NAME_MAX = 50
    const val PASSWORD_MIN = 8

    /** Supabase Auth hashes with bcrypt, which ignores bytes after 72. */
    const val PASSWORD_MAX = 72

    private const val PREDICTABLE_RUN = 4

    /** "Qwerty#2026" is rejected; short words like "admin" only match the whole password. */
    private const val MIN_COMMON_SUBSTRING = 6

    /**
     * Practical RFC 5321/5322 subset: dot-atom local part (≤ 64 chars, no leading,
     * trailing or double dots), hostname labels of 1–63 chars without edge hyphens and
     * an alphabetic TLD of 2–63 chars; total ≤ 254 chars.
     */
    private val EMAIL = Regex(
        "^(?=.{6,254}$)(?=.{1,64}@)" +
            "[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*" +
            "@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,63}$",
    )

    /** Letters (any language), spaces, apostrophes, dots and hyphens: "María José", "O'Neil". */
    private val NAME_CHARS = Regex("^[\\p{L}\\p{M}][\\p{L}\\p{M} .'’-]*$")

    private val COMMON_PASSWORDS = setOf(
        "password", "contraseña", "contrasena", "qwerty", "qwertyuiop", "asdfghjk", "iloveyou",
        "admin", "welcome", "letmein", "wanderai", "monkey", "dragon", "football", "princess",
        "12345678", "123456789", "1234567890", "87654321", "11111111", "00000000", "abc12345",
    )

    fun validateName(name: String): NameIssue? {
        val trimmed = name.trim()
        val length = trimmed.codePointCount(0, trimmed.length)
        return when {
            length < NAME_MIN -> NameIssue.TOO_SHORT
            length > NAME_MAX -> NameIssue.TOO_LONG
            !NAME_CHARS.matches(trimmed) -> NameIssue.INVALID_CHARACTERS
            else -> null
        }
    }

    fun isValidEmail(email: String): Boolean = EMAIL.matches(email.trim())

    /** Rules [password] does not meet yet (empty set = strong enough). */
    fun failedPasswordRules(password: String): Set<PasswordRule> = buildSet {
        if (password.length !in PASSWORD_MIN..PASSWORD_MAX) add(PasswordRule.MIN_LENGTH)
        if (password.none(Char::isUpperCase)) add(PasswordRule.UPPERCASE)
        if (password.none(Char::isDigit)) add(PasswordRule.DIGIT)
        if (password.none { !it.isLetterOrDigit() && !it.isWhitespace() }) add(PasswordRule.SPECIAL)
        if (password.isEmpty() || isPredictable(password)) add(PasswordRule.NOT_PREDICTABLE)
    }

    fun isStrongPassword(password: String): Boolean = failedPasswordRules(password).isEmpty()

    /** Ascending/descending runs ("1234", "dcba"), repeats ("aaaa") or a common password inside. */
    internal fun isPredictable(password: String): Boolean {
        val lower = password.lowercase(Locale.ROOT)
        val core = lower.filter(Char::isLetterOrDigit)
        if (core in COMMON_PASSWORDS || COMMON_PASSWORDS.any { it.length >= MIN_COMMON_SUBSTRING && lower.contains(it) }) return true

        var ascending = 1
        var descending = 1
        var repeated = 1
        for (i in 1 until lower.length) {
            val step = lower[i].code - lower[i - 1].code
            val sameClass = lower[i].isDigit() == lower[i - 1].isDigit()
            ascending = if (step == 1 && sameClass) ascending + 1 else 1
            descending = if (step == -1 && sameClass) descending + 1 else 1
            repeated = if (step == 0) repeated + 1 else 1
            if (ascending >= PREDICTABLE_RUN || descending >= PREDICTABLE_RUN || repeated >= PREDICTABLE_RUN) return true
        }
        return false
    }
}
