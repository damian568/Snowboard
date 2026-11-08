package com.example.snowboard.Fragments

import android.os.Looper
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.navigation.NavController
import com.example.snowboard.R
import com.example.snowboard.User.ChangePassword.ChangePasswordScreenFragment
import com.example.snowboard.launchWithNav
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
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
class ChangePasswordScreenFragmentTest {

    private val auth = mockk<FirebaseAuth>()
    private val user = mockk<FirebaseUser>()
    private val nav = mockk<NavController>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns auth
        every { auth.currentUser } returns user
        every { user.email } returns "rider@example.com"
        every { user.reauthenticate(any()) } returns Tasks.forResult<Void>(null)
        every { user.updatePassword(any()) } returns Tasks.forResult<Void>(null)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun withFragment(block: (ChangePasswordScreenFragment, View) -> Unit) {
        launchWithNav<ChangePasswordScreenFragment>(nav).onFragment { block(it, it.requireView()) }
    }

    private fun View.type(id: Int, text: String) = findViewById<EditText>(id).setText(text)
    private fun View.error(id: Int) = findViewById<EditText>(id).error?.toString()
    private fun View.change() = findViewById<View>(R.id.btn_change_password).performClick()

    private fun View.fill(current: String = "Old1!password", new: String = "New1!password", confirm: String = new) {
        type(R.id.edit_current_password, current)
        type(R.id.edit_new_password, new)
        type(R.id.edit_confirm_new_password, confirm)
    }

    private fun neverReauthenticates() = verify(exactly = 0) { user.reauthenticate(any()) }

    @Test
    fun withoutASignedInUser_nothingHappens() = withFragment { _, v ->
        every { auth.currentUser } returns null
        v.fill()

        v.change()

        assertEquals(null, v.error(R.id.edit_current_password))
    }

    @Test
    fun emptyCurrentPassword_isRejected() = withFragment { f, v ->
        v.change()
        assertEquals(f.getString(R.string.error_current_password_required), v.error(R.id.edit_current_password))
        neverReauthenticates()
    }

    @Test
    fun emptyNewPassword_isRejected() = withFragment { f, v ->
        v.type(R.id.edit_current_password, "Old1!password")
        v.change()
        assertEquals(f.getString(R.string.error_new_password_required), v.error(R.id.edit_new_password))
        neverReauthenticates()
    }

    @Test
    fun weakNewPassword_showsTheFirstUnmetRule() = withFragment { f, v ->
        v.fill(new = "short")
        v.change()
        assertEquals(f.getString(R.string.error_password_too_short), v.error(R.id.edit_new_password))
        neverReauthenticates()
    }

    @Test
    fun newPasswordEqualToTheOldOne_isRejected() = withFragment { f, v ->
        v.fill(current = "Same1!password", new = "Same1!password")
        v.change()
        assertEquals(f.getString(R.string.error_new_password_same_as_old), v.error(R.id.edit_new_password))
        neverReauthenticates()
    }

    @Test
    fun mismatchedConfirmation_isRejected() = withFragment { f, v ->
        v.fill(confirm = "Other1!password")
        v.change()
        assertEquals(f.getString(R.string.error_password_mismatch), v.error(R.id.edit_confirm_new_password))
        neverReauthenticates()
    }

    @Test
    fun validChange_reauthenticates_updates_confirms_andGoesBack() = withFragment { f, v ->
        v.fill()

        v.change()
        idle()

        verify { user.reauthenticate(any()) }
        verify { user.updatePassword("New1!password") }
        assertEquals(f.getString(R.string.msg_password_changed), ShadowToast.getTextOfLatestToast())
        verify { nav.popBackStack() }
        assertTrue(v.findViewById<TextView>(R.id.btn_change_password).isEnabled)
    }

    @Test
    fun wrongCurrentPassword_isShownOnThePasswordField() = withFragment { f, v ->
        every { user.reauthenticate(any()) } returns
            Tasks.forException(FirebaseAuthInvalidCredentialsException("ERROR_WRONG_PASSWORD", "bad"))
        v.fill()

        v.change()
        idle()

        assertEquals(f.getString(R.string.error_current_password_incorrect), v.error(R.id.edit_current_password))
        verify(exactly = 0) { user.updatePassword(any()) }
    }

    @Test
    fun otherReauthFailure_isShownAsAToast() = withFragment { _, v ->
        every { user.reauthenticate(any()) } returns Tasks.forException(RuntimeException("too many attempts"))
        v.fill()

        v.change()
        idle()

        assertEquals("too many attempts", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun updateFailure_isShownAsAToast_andStaysOnTheScreen() = withFragment { _, v ->
        every { user.updatePassword(any()) } returns Tasks.forException(RuntimeException("weak by server"))
        v.fill()

        v.change()
        idle()

        assertEquals("weak by server", ShadowToast.getTextOfLatestToast())
        verify(exactly = 0) { nav.popBackStack() }
        assertTrue(v.findViewById<TextView>(R.id.btn_change_password).isEnabled)
    }

    @Test
    fun backButton_popsTheBackStack() = withFragment { _, v ->
        v.findViewById<View>(R.id.btn_back).performClick()
        verify { nav.popBackStack() }
    }

    @Test
    fun eachEyeIcon_togglesItsOwnField() = withFragment { _, v ->
        val visible = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        val hidden = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        val current = v.findViewById<EditText>(R.id.edit_current_password)
        val new = v.findViewById<EditText>(R.id.edit_new_password)
        val confirm = v.findViewById<EditText>(R.id.edit_confirm_new_password)

        v.findViewById<View>(R.id.toggle_current_password).performClick()
        assertEquals(visible, current.inputType)
        assertEquals(hidden, new.inputType)

        v.findViewById<View>(R.id.toggle_new_password).performClick()
        assertEquals(visible, new.inputType)
        assertEquals(hidden, confirm.inputType)

        v.findViewById<View>(R.id.toggle_confirm_new_password).performClick()
        assertEquals(visible, confirm.inputType)

        v.findViewById<View>(R.id.toggle_current_password).performClick()
        assertEquals(hidden, current.inputType)
    }

    @Test
    fun matchIcon_followsTheConfirmation() = withFragment { _, v ->
        v.type(R.id.edit_new_password, "New1!password")
        v.type(R.id.edit_confirm_new_password, "New1!password")
        assertEquals(View.VISIBLE, v.findViewById<View>(R.id.password_match_icon).visibility)

        v.type(R.id.edit_confirm_new_password, "")
        assertEquals(View.GONE, v.findViewById<View>(R.id.password_match_icon).visibility)
    }
}
