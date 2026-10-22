package com.example.snowboard.User

import androidx.annotation.StringRes
import com.example.snowboard.R

object PasswordValidator {

    const val MIN_LENGTH = 8

    enum class Rule(@StringRes val labelRes: Int, @StringRes val errorRes: Int, val isMet: (String) -> Boolean) {
        LENGTH(R.string.password_rule_length, R.string.error_password_too_short, { it.length >= MIN_LENGTH }),
        UPPERCASE(R.string.password_rule_uppercase, R.string.error_password_no_uppercase, { p -> p.any { it.isUpperCase() } }),
        LOWERCASE(R.string.password_rule_lowercase, R.string.error_password_no_lowercase, { p -> p.any { it.isLowerCase() } }),
        NUMBER(R.string.password_rule_number, R.string.error_password_no_number, { p -> p.any { it.isDigit() } }),
        SYMBOL(R.string.password_rule_symbol, R.string.error_password_no_symbol, { p -> p.any { !it.isLetterOrDigit() && !it.isWhitespace() } })
    }

    @StringRes
    fun validate(password: String): Int? = Rule.entries.firstOrNull { !it.isMet(password) }?.errorRes
}
