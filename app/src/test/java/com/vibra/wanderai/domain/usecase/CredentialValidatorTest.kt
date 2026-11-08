package com.vibra.wanderai.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialValidatorTest {

    @Test
    fun `strong passwords pass every rule`() {
        listOf("Pasto#2026", "Volc4n.Galeras", "Ñandú!93x").forEach {
            assertTrue(it, CredentialValidator.isStrongPassword(it))
        }
    }

    @Test
    fun `each missing requirement is reported`() {
        assertEquals(setOf(PasswordRule.MIN_LENGTH), CredentialValidator.failedPasswordRules("Ab1!xy"))
        assertEquals(setOf(PasswordRule.UPPERCASE), CredentialValidator.failedPasswordRules("pasto#2026"))
        assertEquals(setOf(PasswordRule.DIGIT), CredentialValidator.failedPasswordRules("Pasto#Galeras"))
        assertEquals(setOf(PasswordRule.SPECIAL), CredentialValidator.failedPasswordRules("Pasto2026x"))
        assertTrue(PasswordRule.MIN_LENGTH in CredentialValidator.failedPasswordRules("A1!" + "x".repeat(70)))
        assertEquals(PasswordRule.entries.toSet(), CredentialValidator.failedPasswordRules(""))
    }

    @Test
    fun `sequences repeats and common passwords are rejected`() {
        assertTrue(PasswordRule.NOT_PREDICTABLE in CredentialValidator.failedPasswordRules("12345678"))
        listOf("Abcd#2026x", "Pasto#1234", "Pasto#9876", "Paaaa#2026", "Password#1", "Qwerty#2026").forEach {
            assertTrue(it, PasswordRule.NOT_PREDICTABLE in CredentialValidator.failedPasswordRules(it))
        }
        // Three in a row is still fine.
        assertFalse(CredentialValidator.isPredictable("Pasto#123x"))
    }

    @Test
    fun `names need 2 to 50 letters`() {
        assertEquals(NameIssue.TOO_SHORT, CredentialValidator.validateName(" A "))
        assertEquals(NameIssue.TOO_LONG, CredentialValidator.validateName("A".repeat(51)))
        assertEquals(NameIssue.INVALID_CHARACTERS, CredentialValidator.validateName("Camilo123"))
        assertEquals(NameIssue.INVALID_CHARACTERS, CredentialValidator.validateName("<script>"))
        listOf("Al", "María José Muñoz", "O'Neil", "Jean-Luc", "A".repeat(50)).forEach {
            assertNull(it, CredentialValidator.validateName(it))
        }
    }

    @Test
    fun `emails must be standard addresses`() {
        listOf("ana@pasto.co", "camilo.lopez@campusucc.edu.co", "a+viajes@mail.com", "x_y@sub.dominio.org").forEach {
            assertTrue(it, CredentialValidator.isValidEmail(it))
        }
        listOf(
            "", "ana", "ana@", "@pasto.co", "ana@pasto", "ana@pasto.c", "ana..b@pasto.co", ".ana@pasto.co",
            "ana.@pasto.co", "ana@-pasto.co", "ana@pasto-.co", "ana pasto@x.co", "ana@pasto.co.", "ana@pasto.123",
        ).forEach {
            assertFalse(it, CredentialValidator.isValidEmail(it))
        }
    }
}
