package com.example.snowboard.User

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

/**
 * Only the guard clause is unit tested: the HTTP call goes to a hard-coded https URL through
 * HttpURLConnection, which can't be faked from a plain JVM test without changing EmailJs.
 */
class EmailJsTest {

    @Test
    fun send_withoutATemplateId_failsBeforeAnyNetworkCall() {
        try {
            EmailJs.send("", mapOf("a" to "b"))
            fail("expected an IOException")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("not configured"))
        }
    }

    @Test
    fun send_withoutATemplateId_failsEvenWithNoParams() {
        try {
            EmailJs.send("", emptyMap())
            fail("expected an IOException")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("EMAILJS"))
        }
    }
}
