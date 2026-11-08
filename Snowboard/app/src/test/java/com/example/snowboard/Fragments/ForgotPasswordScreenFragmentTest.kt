package com.example.snowboard.Fragments

import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.navigation.NavController
import com.example.snowboard.R
import com.example.snowboard.User.ForgotPassword.ForgotPasswordScreenFragment
import com.example.snowboard.launchWithNav
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
class ForgotPasswordScreenFragmentTest {

    private val auth = mockk<FirebaseAuth>(relaxed = true)
    private val nav = mockk<NavController>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns auth
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun withFragment(block: (ForgotPasswordScreenFragment, View) -> Unit) {
        launchWithNav<ForgotPasswordScreenFragment>(nav).onFragment { block(it, it.requireView()) }
    }

    private fun View.type(text: String) = findViewById<EditText>(R.id.edit_email).setText(text)
    private fun View.error() = findViewById<EditText>(R.id.edit_email).error?.toString()
    private fun View.send() = findViewById<View>(R.id.btn_send_reset_link).performClick()

    @Test
    fun emptyEmail_isRejected() = withFragment { f, v ->
        v.send()
        assertEquals(f.getString(R.string.error_email_required), v.error())
        verify(exactly = 0) { auth.sendPasswordResetEmail(any()) }
    }

    @Test
    fun malformedEmail_isRejected() = withFragment { f, v ->
        v.type("nope")
        v.send()
        assertEquals(f.getString(R.string.error_email_invalid), v.error())
        verify(exactly = 0) { auth.sendPasswordResetEmail(any()) }
    }

    @Test
    fun validEmail_sendsTheLink_confirms_andGoesBack() = withFragment { f, v ->
        every { auth.sendPasswordResetEmail(any()) } returns Tasks.forResult<Void>(null)
        v.type("  rider@example.com ")

        v.send()
        idle()

        verify { auth.sendPasswordResetEmail("rider@example.com") }
        assertEquals(f.getString(R.string.msg_reset_email_sent, "rider@example.com"), ShadowToast.getTextOfLatestToast())
        verify { nav.popBackStack() }
        assertTrue(v.findViewById<TextView>(R.id.btn_send_reset_link).isEnabled)
    }

    @Test
    fun unknownAccount_getsAFriendlyMessage_andStaysOnTheScreen() = withFragment { f, v ->
        every { auth.sendPasswordResetEmail(any()) } returns
            Tasks.forException(FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "no user"))
        v.type("ghost@example.com")

        v.send()
        idle()

        assertEquals(f.getString(R.string.error_no_account_for_email), ShadowToast.getTextOfLatestToast())
        verify(exactly = 0) { nav.popBackStack() }
        assertTrue(v.findViewById<TextView>(R.id.btn_send_reset_link).isEnabled)
    }

    @Test
    fun otherFailure_showsTheFirebaseMessage() = withFragment { _, v ->
        every { auth.sendPasswordResetEmail(any()) } returns Tasks.forException(RuntimeException("rate limited"))
        v.type("rider@example.com")

        v.send()
        idle()

        assertEquals("rate limited", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun whileSending_theButtonIsDisabledAndEmpty() = withFragment { f, v ->
        every { auth.sendPasswordResetEmail(any()) } returns TaskCompletionSource<Void>().task
        v.type("rider@example.com")

        v.send()

        val button = v.findViewById<TextView>(R.id.btn_send_reset_link)
        assertFalse(button.isEnabled)
        assertEquals("", button.text.toString())
        assertTrue(f.getString(R.string.btn_send_reset_link).isNotEmpty())
    }

    @Test
    fun backToLogIn_popsTheBackStack() = withFragment { _, v ->
        v.findViewById<View>(R.id.back_to_log_in).performClick()
        verify { nav.popBackStack() }
    }
}
