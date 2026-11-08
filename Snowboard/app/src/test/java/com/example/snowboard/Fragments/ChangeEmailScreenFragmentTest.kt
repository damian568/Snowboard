package com.example.snowboard.Fragments

import android.os.Looper
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.navigation.NavController
import com.example.snowboard.R
import com.example.snowboard.User.ChangeEmail.ChangeEmailScreenFragment
import com.example.snowboard.launchWithNav
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
class ChangeEmailScreenFragmentTest {

    private val auth = mockk<FirebaseAuth>()
    private val user = mockk<FirebaseUser>()
    private val nav = mockk<NavController>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns auth
        every { auth.currentUser } returns user
        every { user.email } returns "old@example.com"
        every { user.reauthenticate(any()) } returns Tasks.forResult<Void>(null)
        every { user.verifyBeforeUpdateEmail(any()) } returns Tasks.forResult<Void>(null)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun withFragment(block: (ChangeEmailScreenFragment, View) -> Unit) {
        launchWithNav<ChangeEmailScreenFragment>(nav).onFragment { block(it, it.requireView()) }
    }

    private fun View.type(id: Int, text: String) = findViewById<EditText>(id).setText(text)
    private fun View.error(id: Int) = findViewById<EditText>(id).error?.toString()
    private fun View.update() = findViewById<View>(R.id.btn_update_email).performClick()

    private fun neverReauthenticates() = verify(exactly = 0) { user.reauthenticate(any()) }

    @Test
    fun withoutASignedInUser_nothingHappens() = withFragment { _, v ->
        every { auth.currentUser } returns null
        v.type(R.id.edit_current_password, "pw")
        v.type(R.id.edit_new_email, "new@example.com")

        v.update()

        assertEquals(null, v.error(R.id.edit_new_email))
    }

    @Test
    fun emptyCurrentPassword_isRejected() = withFragment { f, v ->
        v.update()
        assertEquals(f.getString(R.string.error_current_password_required), v.error(R.id.edit_current_password))
        neverReauthenticates()
    }

    @Test
    fun emptyNewEmail_isRejected() = withFragment { f, v ->
        v.type(R.id.edit_current_password, "pw")
        v.update()
        assertEquals(f.getString(R.string.error_email_required), v.error(R.id.edit_new_email))
        neverReauthenticates()
    }

    @Test
    fun malformedNewEmail_isRejected() = withFragment { f, v ->
        v.type(R.id.edit_current_password, "pw")
        v.type(R.id.edit_new_email, "nope")
        v.update()
        assertEquals(f.getString(R.string.error_email_invalid), v.error(R.id.edit_new_email))
        neverReauthenticates()
    }

    @Test
    fun sameEmailAsBefore_isRejected_ignoringCase() = withFragment { f, v ->
        v.type(R.id.edit_current_password, "pw")
        v.type(R.id.edit_new_email, "OLD@example.com")
        v.update()
        assertEquals(f.getString(R.string.error_new_email_same_as_old), v.error(R.id.edit_new_email))
        neverReauthenticates()
    }

    @Test
    fun validChange_reauthenticates_sendsTheVerification_confirms_andGoesBack() = withFragment { f, v ->
        v.type(R.id.edit_current_password, "pw")
        v.type(R.id.edit_new_email, " new@example.com ")

        v.update()
        idle()

        verify { user.reauthenticate(any()) }
        verify { user.verifyBeforeUpdateEmail("new@example.com") }
        assertEquals(
            f.getString(R.string.msg_verification_email_sent_new_address, "new@example.com"),
            ShadowToast.getTextOfLatestToast()
        )
        verify { nav.popBackStack() }
        assertTrue(v.findViewById<TextView>(R.id.btn_update_email).isEnabled)
    }

    @Test
    fun wrongCurrentPassword_isShownOnThePasswordField() = withFragment { f, v ->
        every { user.reauthenticate(any()) } returns
            Tasks.forException(FirebaseAuthInvalidCredentialsException("ERROR_WRONG_PASSWORD", "bad"))
        v.type(R.id.edit_current_password, "wrong")
        v.type(R.id.edit_new_email, "new@example.com")

        v.update()
        idle()

        assertEquals(f.getString(R.string.error_current_password_incorrect), v.error(R.id.edit_current_password))
        verify(exactly = 0) { user.verifyBeforeUpdateEmail(any()) }
        assertTrue(v.findViewById<TextView>(R.id.btn_update_email).isEnabled)
    }

    @Test
    fun otherReauthFailure_isShownAsAToast() = withFragment { _, v ->
        every { user.reauthenticate(any()) } returns Tasks.forException(RuntimeException("recent login needed"))
        v.type(R.id.edit_current_password, "pw")
        v.type(R.id.edit_new_email, "new@example.com")

        v.update()
        idle()

        assertEquals("recent login needed", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun emailAlreadyInUse_isShownOnTheEmailField() = withFragment { f, v ->
        every { user.verifyBeforeUpdateEmail(any()) } returns
            Tasks.forException(FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "taken"))
        v.type(R.id.edit_current_password, "pw")
        v.type(R.id.edit_new_email, "taken@example.com")

        v.update()
        idle()

        assertEquals(f.getString(R.string.error_email_already_in_use), v.error(R.id.edit_new_email))
        verify(exactly = 0) { nav.popBackStack() }
    }

    @Test
    fun otherUpdateFailure_isShownAsAToast() = withFragment { _, v ->
        every { user.verifyBeforeUpdateEmail(any()) } returns Tasks.forException(RuntimeException("offline"))
        v.type(R.id.edit_current_password, "pw")
        v.type(R.id.edit_new_email, "new@example.com")

        v.update()
        idle()

        assertEquals("offline", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun backButton_popsTheBackStack() = withFragment { _, v ->
        v.findViewById<View>(R.id.btn_back).performClick()
        verify { nav.popBackStack() }
    }

    @Test
    fun eyeIcon_togglesThePasswordVisibility() = withFragment { _, v ->
        val field = v.findViewById<EditText>(R.id.edit_current_password)

        v.findViewById<View>(R.id.toggle_current_password).performClick()
        assertEquals(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, field.inputType)

        v.findViewById<View>(R.id.toggle_current_password).performClick()
        assertEquals(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD, field.inputType)
    }
}
