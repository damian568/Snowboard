package com.example.snowboard.Fragments

import android.os.Looper
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import com.example.snowboard.R
import com.example.snowboard.User.Model.UserProfile
import com.example.snowboard.User.Register.RegisterScreenFragment
import com.example.snowboard.User.Register.RegisterScreenFragmentDirections
import com.example.snowboard.launchWithNav
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
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
class RegisterScreenFragmentTest {

    private val auth = mockk<FirebaseAuth>(relaxed = true)
    private val nav = mockk<NavController>(relaxed = true)
    private val userDoc = mockk<DocumentReference>()
    private val profile = slot<Any>()

    @Before
    fun setUp() {
        mockkStatic(FirebaseAuth::class, FirebaseFirestore::class)
        every { FirebaseAuth.getInstance() } returns auth
        val users = mockk<CollectionReference>()
        val firestore = mockk<FirebaseFirestore>()
        every { FirebaseFirestore.getInstance() } returns firestore
        every { firestore.collection("users") } returns users
        every { users.document("uid-1") } returns userDoc
        every { userDoc.set(capture(profile)) } returns Tasks.forResult<Void>(null)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun withFragment(block: (RegisterScreenFragment, View) -> Unit) {
        launchWithNav<RegisterScreenFragment>(nav).onFragment { block(it, it.requireView()) }
    }

    private fun View.type(id: Int, text: String) = findViewById<EditText>(id).setText(text)
    private fun View.error(id: Int) = findViewById<EditText>(id).error?.toString()
    private fun View.click(id: Int) = findViewById<View>(id).performClick()

    private fun View.fillValidForm(email: String = "rider@example.com") {
        type(R.id.edit_full_name, "  Rider One ")
        type(R.id.edit_email, email)
        type(R.id.edit_password, "Str0ng!Pass")
        type(R.id.edit_confirm_password, "Str0ng!Pass")
    }

    private fun createsUser(user: FirebaseUser? = mockk {
        every { uid } returns "uid-1"
        every { sendEmailVerification() } returns Tasks.forResult<Void>(null)
    }) {
        val result = mockk<AuthResult> { every { this@mockk.user } returns user }
        every { auth.createUserWithEmailAndPassword(any(), any()) } returns Tasks.forResult(result)
    }

    private fun neverCreatesUser() =
        verify(exactly = 0) { auth.createUserWithEmailAndPassword(any(), any()) }

    // ---- validation, in the order the fields appear ----

    @Test
    fun emptyName_isRejected() = withFragment { f, v ->
        v.click(R.id.btn_register)
        assertEquals(f.getString(R.string.error_full_name_required), v.error(R.id.edit_full_name))
        neverCreatesUser()
    }

    @Test
    fun emptyEmail_isRejected() = withFragment { f, v ->
        v.type(R.id.edit_full_name, "Rider")
        v.click(R.id.btn_register)
        assertEquals(f.getString(R.string.error_email_required), v.error(R.id.edit_email))
        neverCreatesUser()
    }

    @Test
    fun malformedEmail_isRejected() = withFragment { f, v ->
        v.type(R.id.edit_full_name, "Rider")
        v.type(R.id.edit_email, "nope")
        v.click(R.id.btn_register)
        assertEquals(f.getString(R.string.error_email_invalid), v.error(R.id.edit_email))
        neverCreatesUser()
    }

    @Test
    fun emptyPassword_isRejected() = withFragment { f, v ->
        v.type(R.id.edit_full_name, "Rider")
        v.type(R.id.edit_email, "rider@example.com")
        v.click(R.id.btn_register)
        assertEquals(f.getString(R.string.error_password_required), v.error(R.id.edit_password))
        neverCreatesUser()
    }

    @Test
    fun weakPassword_showsTheFirstUnmetRule() = withFragment { f, v ->
        v.type(R.id.edit_full_name, "Rider")
        v.type(R.id.edit_email, "rider@example.com")
        v.type(R.id.edit_password, "weak")
        v.click(R.id.btn_register)
        assertEquals(f.getString(R.string.error_password_too_short), v.error(R.id.edit_password))
        neverCreatesUser()
    }

    @Test
    fun mismatchedConfirmation_isRejected() = withFragment { f, v ->
        v.fillValidForm()
        v.type(R.id.edit_confirm_password, "Different1!")
        v.click(R.id.btn_register)
        assertEquals(f.getString(R.string.error_password_mismatch), v.error(R.id.edit_confirm_password))
        neverCreatesUser()
    }

    // ---- registering ----

    @Test
    fun validForm_createsTheAccount_storesABeginnerProfile_andSendsTheVerificationEmail() = withFragment { f, v ->
        createsUser()
        v.fillValidForm()

        v.click(R.id.btn_register)
        idle()

        verify { auth.createUserWithEmailAndPassword("rider@example.com", "Str0ng!Pass") }
        assertEquals(UserProfile("Rider One", "rider@example.com", "BEGINNER"), profile.captured)
        verify { auth.signOut() }
        assertEquals(
            f.getString(R.string.msg_verification_email_sent, "rider@example.com"),
            ShadowToast.getTextOfLatestToast()
        )
        verify { nav.navigate(RegisterScreenFragmentDirections.actionRegisterScreenFragmentToLoginScreenFragment()) }
    }

    @Test
    fun choosingAdvance_isStoredInTheProfile() = withFragment { _, v ->
        createsUser()
        v.fillValidForm()
        v.click(R.id.level_advance)

        v.click(R.id.btn_register)
        idle()

        assertEquals("ADVANCE", (profile.captured as UserProfile).level)
    }

    @Test
    fun choosingBeginnerAgain_isStoredInTheProfile() = withFragment { _, v ->
        createsUser()
        v.fillValidForm()
        v.click(R.id.level_advance)
        v.click(R.id.level_beginner)

        v.click(R.id.btn_register)
        idle()

        assertEquals("BEGINNER", (profile.captured as UserProfile).level)
    }

    @Test
    fun emailAlreadyInUse_isShownOnTheEmailField() = withFragment { f, v ->
        every { auth.createUserWithEmailAndPassword(any(), any()) } returns
            Tasks.forException(FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "taken"))
        v.fillValidForm()

        v.click(R.id.btn_register)
        idle()

        assertEquals(f.getString(R.string.error_email_already_in_use), v.error(R.id.edit_email))
        assertTrue(v.findViewById<TextView>(R.id.btn_register).isEnabled)
    }

    @Test
    fun otherSignUpFailure_isShownAsAToast() = withFragment { _, v ->
        every { auth.createUserWithEmailAndPassword(any(), any()) } returns Tasks.forException(RuntimeException("network down"))
        v.fillValidForm()

        v.click(R.id.btn_register)
        idle()

        assertEquals("network down", ShadowToast.getTextOfLatestToast())
        assertTrue(v.findViewById<TextView>(R.id.btn_register).isEnabled)
    }

    @Test
    fun failureSavingTheProfile_isShownAsAToast_andDoesNotContinue() = withFragment { _, v ->
        createsUser()
        every { userDoc.set(any()) } returns Tasks.forException(RuntimeException("write denied"))
        v.fillValidForm()

        v.click(R.id.btn_register)
        idle()

        assertEquals("write denied", ShadowToast.getTextOfLatestToast())
        verify(exactly = 0) { nav.navigate(any<NavDirections>()) }
        verify(exactly = 0) { auth.signOut() }
    }

    @Test
    fun resultWithoutAUser_justStopsLoading() = withFragment { _, v ->
        createsUser(user = null)
        v.fillValidForm()

        v.click(R.id.btn_register)
        idle()

        assertTrue(v.findViewById<TextView>(R.id.btn_register).isEnabled)
        verify(exactly = 0) { nav.navigate(any<NavDirections>()) }
    }

    @Test
    fun whileRegistering_theButtonIsDisabledAndEmpty() = withFragment { _, v ->
        every { auth.createUserWithEmailAndPassword(any(), any()) } returns
            com.google.android.gms.tasks.TaskCompletionSource<AuthResult>().task
        v.fillValidForm()

        v.click(R.id.btn_register)

        val button = v.findViewById<TextView>(R.id.btn_register)
        assertFalse(button.isEnabled)
        assertEquals("", button.text.toString())
    }

    // ---- navigation and toggles ----

    @Test
    fun logInLink_opensTheLoginScreen() = withFragment { _, v ->
        v.click(R.id.log_in)
        verify { nav.navigate(RegisterScreenFragmentDirections.actionRegisterScreenFragmentToLoginScreenFragment()) }
    }

    @Test
    fun eyeIcons_toggleBothPasswordFields() = withFragment { _, v ->
        val password = v.findViewById<EditText>(R.id.edit_password)
        val confirm = v.findViewById<EditText>(R.id.edit_confirm_password)
        val visible = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        val hidden = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        v.click(R.id.toggle_password)
        assertEquals(visible, password.inputType)
        assertEquals(hidden, confirm.inputType)

        v.click(R.id.toggle_confirm_password)
        assertEquals(visible, confirm.inputType)

        v.click(R.id.toggle_password)
        v.click(R.id.toggle_confirm_password)
        assertEquals(hidden, password.inputType)
        assertEquals(hidden, confirm.inputType)
    }

    @Test
    fun typingAPassword_showsTheRequirementChecklist() = withFragment { _, v ->
        v.type(R.id.edit_password, "abc")
        assertEquals(View.VISIBLE, v.findViewById<View>(R.id.password_requirements).visibility)
        assertEquals(View.GONE, v.findViewById<View>(R.id.password_strong_icon).visibility)

        v.type(R.id.edit_password, "Str0ng!Pass")
        assertEquals(View.GONE, v.findViewById<View>(R.id.password_requirements).visibility)
        assertEquals(View.VISIBLE, v.findViewById<View>(R.id.password_strong_icon).visibility)
    }
}
