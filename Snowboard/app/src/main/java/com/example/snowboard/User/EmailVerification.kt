package com.example.snowboard.User

import com.example.snowboard.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.text.DateFormat
import java.util.Date

/**
 * One-time code emailed to the account's address before a sensitive action (deleting the account).
 * Firebase can't email codes and Cloud Functions need the Blaze plan, so EmailJS sends it
 * and the code is checked here.
 */
class EmailVerification {

    enum class Result { CORRECT, WRONG, EXPIRED }

    private var code: String? = null
    private var expiresAt = 0L
    private var attemptsLeft = 0

    /** Makes a new code and emails it to [email]. Must be called from a coroutine. */
    suspend fun sendCode(email: String) {
        val newCode = (SecureRandom().nextInt(CODE_RANGE) + CODE_MIN).toString()
        val newExpiresAt = System.currentTimeMillis() + CODE_LIFETIME_MS

        withContext(Dispatchers.IO) {
            EmailJs.send(
                BuildConfig.EMAILJS_TEMPLATE_ID,
                mapOf(
                    "email" to email,
                    "passcode" to newCode,
                    "time" to DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(newExpiresAt))
                )
            )
        }

        // Only replaces the old code once the new one is on its way
        code = newCode
        expiresAt = newExpiresAt
        attemptsLeft = MAX_ATTEMPTS
    }

    fun check(input: String): Result {
        val expected = code
        if (expected == null || System.currentTimeMillis() > expiresAt || attemptsLeft <= 0) {
            return Result.EXPIRED
        }
        if (input.trim() == expected) {
            code = null
            return Result.CORRECT
        }
        attemptsLeft--
        return if (attemptsLeft <= 0) Result.EXPIRED else Result.WRONG
    }

    companion object {
        private const val CODE_MIN = 100_000
        private const val CODE_RANGE = 900_000
        private const val CODE_LIFETIME_MS = 10 * 60 * 1000L
        private const val MAX_ATTEMPTS = 5
    }
}
