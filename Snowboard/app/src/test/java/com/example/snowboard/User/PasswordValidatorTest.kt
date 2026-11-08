package com.example.snowboard.User

import com.example.snowboard.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordValidatorTest {

    @Test
    fun strongPassword_isValid() {
        assertNull(PasswordValidator.validate("Str0ng!Pass"))
    }

    @Test
    fun tooShort_reportsLengthError() {
        assertEquals(R.string.error_password_too_short, PasswordValidator.validate("Ab1!xyz"))
    }

    @Test
    fun exactlyMinLength_passesLengthRule() {
        assertTrue(PasswordValidator.Rule.LENGTH.isMet("a".repeat(PasswordValidator.MIN_LENGTH)))
        assertFalse(PasswordValidator.Rule.LENGTH.isMet("a".repeat(PasswordValidator.MIN_LENGTH - 1)))
    }

    @Test
    fun missingUppercase_reportsUppercaseError() {
        assertEquals(R.string.error_password_no_uppercase, PasswordValidator.validate("str0ng!pass"))
    }

    @Test
    fun missingLowercase_reportsLowercaseError() {
        assertEquals(R.string.error_password_no_lowercase, PasswordValidator.validate("STR0NG!PASS"))
    }

    @Test
    fun missingNumber_reportsNumberError() {
        assertEquals(R.string.error_password_no_number, PasswordValidator.validate("Strong!Pass"))
    }

    @Test
    fun missingSymbol_reportsSymbolError() {
        assertEquals(R.string.error_password_no_symbol, PasswordValidator.validate("Str0ngPass"))
    }

    @Test
    fun whitespaceIsNotASymbol() {
        assertFalse(PasswordValidator.Rule.SYMBOL.isMet("Str0ng Pass"))
        assertTrue(PasswordValidator.Rule.SYMBOL.isMet("Str0ng Pass!"))
    }

    @Test
    fun emptyPassword_failsOnLengthFirst() {
        assertEquals(R.string.error_password_too_short, PasswordValidator.validate(""))
    }

    @Test
    fun firstFailingRuleWins_inDeclaredOrder() {
        assertEquals(R.string.error_password_too_short, PasswordValidator.validate("a"))
        assertEquals(R.string.error_password_no_uppercase, PasswordValidator.validate("abcdefgh"))
    }

    @Test
    fun everyRule_hasLabelAndErrorResources() {
        PasswordValidator.Rule.entries.forEach {
            assertTrue(it.labelRes != 0)
            assertTrue(it.errorRes != 0)
        }
        assertEquals(5, PasswordValidator.Rule.entries.size)
    }

    @Test
    fun unicodeLettersCountAsCase() {
        assertTrue(PasswordValidator.Rule.UPPERCASE.isMet("Ж"))
        assertTrue(PasswordValidator.Rule.LOWERCASE.isMet("ж"))
    }
}
