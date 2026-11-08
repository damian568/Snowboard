package com.example.snowboard.User

import com.example.snowboard.User.EmailVerification.Result
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException

class EmailVerificationTest {

    private val params = slot<Map<String, String>>()
    private val verification = EmailVerification()

    @Before
    fun setUp() {
        mockkObject(EmailJs)
        every { EmailJs.send(any(), capture(params)) } just runs
    }

    @After
    fun tearDown() {
        unmockkObject(EmailJs)
    }

    /** Sends a code and returns the one that was "emailed". */
    private fun sendCode(email: String = "user@example.com"): String {
        runBlocking { verification.sendCode(email) }
        return params.captured.getValue("passcode")
    }

    private fun wrongCode(code: String) = if (code == "111111") "222222" else "111111"

    @Test
    fun sendCode_emailsASixDigitCodeToTheAddress() {
        val code = sendCode("rider@example.com")

        assertEquals("rider@example.com", params.captured["email"])
        assertTrue("code $code", code.matches(Regex("[1-9][0-9]{5}")))
        assertTrue(params.captured.getValue("time").isNotBlank())
        verify(exactly = 1) { EmailJs.send(any(), any()) }
    }

    @Test
    fun check_beforeAnyCodeWasSent_isExpired() {
        assertEquals(Result.EXPIRED, verification.check("123456"))
    }

    @Test
    fun check_correctCode_isCorrect() {
        val code = sendCode()
        assertEquals(Result.CORRECT, verification.check(code))
    }

    @Test
    fun check_ignoresSurroundingWhitespace() {
        val code = sendCode()
        assertEquals(Result.CORRECT, verification.check("  $code \n"))
    }

    @Test
    fun check_codeCanOnlyBeUsedOnce() {
        val code = sendCode()
        assertEquals(Result.CORRECT, verification.check(code))
        assertEquals(Result.EXPIRED, verification.check(code))
    }

    @Test
    fun check_wrongCode_isWrong() {
        val code = sendCode()
        assertEquals(Result.WRONG, verification.check(wrongCode(code)))
    }

    @Test
    fun check_correctCodeStillWorksAfterAFewMistakes() {
        val code = sendCode()
        repeat(3) { assertEquals(Result.WRONG, verification.check(wrongCode(code))) }
        assertEquals(Result.CORRECT, verification.check(code))
    }

    @Test
    fun check_fifthWrongAttemptExpiresTheCode() {
        val code = sendCode()
        repeat(4) { assertEquals(Result.WRONG, verification.check(wrongCode(code))) }
        assertEquals(Result.EXPIRED, verification.check(wrongCode(code)))
        // Even the right code is refused once the attempts ran out
        assertEquals(Result.EXPIRED, verification.check(code))
    }

    @Test
    fun check_afterTheLifetime_isExpired() {
        val code = sendCode()
        EmailVerification::class.java.getDeclaredField("expiresAt").apply {
            isAccessible = true
            setLong(verification, System.currentTimeMillis() - 1)
        }
        assertEquals(Result.EXPIRED, verification.check(code))
    }

    @Test
    fun sendCode_again_replacesTheCodeAndResetsTheAttempts() {
        val first = sendCode()
        repeat(4) { verification.check(wrongCode(first)) }

        var second = sendCode()
        while (second == first) second = sendCode()

        assertEquals(Result.WRONG, verification.check(first))
        assertEquals(Result.CORRECT, verification.check(second))
    }

    @Test
    fun sendCode_whenEmailingFails_keepsThePreviousCode() {
        val first = sendCode()
        every { EmailJs.send(any(), any()) } throws IOException("offline")

        try {
            runBlocking { verification.sendCode("user@example.com") }
            fail("expected an IOException")
        } catch (e: IOException) {
            assertEquals("offline", e.message)
        }

        assertEquals(Result.CORRECT, verification.check(first))
    }
}
