package com.example.snowboard.Fragments

import android.content.Context
import android.os.Looper
import android.text.InputType
import android.view.View
import android.widget.EditText
import androidx.navigation.NavController
import androidx.test.core.app.ApplicationProvider
import com.example.snowboard.R
import com.example.snowboard.User.Login.LoginScreenFragment
import com.example.snowboard.User.Login.LoginScreenFragmentDirections
import com.example.snowboard.launchWithNav
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowToast
import com.google.android.gms.tasks.TaskCompletionSource

@RunWith(RobolectricTestRunner::class)
class LoginScreenFragmentTest {

    private val auth = mockk<FirebaseAuth>(relaxed = true)
    private val nav = mockk<NavController>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(FirebaseAuth::class, FirebaseFirestore::class, GoogleSignIn::class)
        every { FirebaseAuth.getInstance() } returns auth
        every { FirebaseFirestore.getInstance() } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun withFragment(block: (LoginScreenFragment, View) -> Unit) {
        launchWithNav<LoginScreenFragment>(nav).onFragment { block(it, it.requireView()) }
    }

    private fun View.type(id: Int, text: String) = findViewById<EditText>(id).setText(text)
    private fun View.error(id: Int) = findViewById<EditText>(id).error?.toString()
    private fun View.click(id: Int) = findViewById<View>(id).performClick()

    private fun signIn(user: FirebaseUser?) {
        val result = mockk<AuthResult> { every { this@mockk.user } returns user }
        every { auth.signInWithEmailAndPassword(any(), any()) } returns Tasks.forResult(result)
    }

    private fun user(verified: Boolean) = mockk<FirebaseUser> {
        every { reload() } returns Tasks.forResult<Void>(null)
        every { isEmailVerified } returns verified
    }

    // ---- validation ----

    @Test
    fun emptyEmail_isRejected() = withFragment { f, v ->
        v.click(R.id.btn_login)
        assertEquals(f.getString(R.string.error_email_required), v.error(R.id.edit_email))
        verify(exactly = 0) { auth.signInWithEmailAndPassword(any(), any()) }
    }

    @Test
    fun malformedEmail_isRejected() = withFragment { f, v ->
        v.type(R.id.edit_email, "not-an-email")
        v.click(R.id.btn_login)
        assertEquals(f.getString(R.string.error_email_invalid), v.error(R.id.edit_email))
        verify(exactly = 0) { auth.signInWithEmailAndPassword(any(), any()) }
    }

    @Test
    fun emptyPassword_isRejected() = withFragment { f, v ->
        v.type(R.id.edit_email, "rider@example.com")
        v.click(R.id.btn_login)
        assertEquals(f.getString(R.string.error_password_required), v.error(R.id.edit_password))
        verify(exactly = 0) { auth.signInWithEmailAndPassword(any(), any()) }
    }

    // ---- email + password sign-in ----

    @Test
    fun verifiedUser_isSignedIn_andSentToTheMainScreen() = withFragment { _, v ->
        signIn(user(verified = true))
        v.type(R.id.edit_email, "  rider@example.com ")
        v.type(R.id.edit_password, "Secret1!")

        v.click(R.id.btn_login)
        idle()

        verify { auth.signInWithEmailAndPassword("rider@example.com", "Secret1!") }
        verify { nav.navigate(LoginScreenFragmentDirections.actionLoginScreenFragmentToMainScreenFragment()) }
        assertTrue(v.findViewById<android.widget.TextView>(R.id.btn_login).isEnabled)
    }

    @Test
    fun unverifiedUser_isSignedOutAgain_withAnExplanation() = withFragment { f, v ->
        signIn(user(verified = false))
        v.type(R.id.edit_email, "rider@example.com")
        v.type(R.id.edit_password, "Secret1!")

        v.click(R.id.btn_login)
        idle()

        verify { auth.signOut() }
        assertEquals(f.getString(R.string.error_email_not_verified), ShadowToast.getTextOfLatestToast())
        verify(exactly = 0) { nav.navigate(any<androidx.navigation.NavDirections>()) }
    }

    @Test
    fun resultWithoutAUser_justStopsLoading() = withFragment { _, v ->
        signIn(null)
        v.type(R.id.edit_email, "rider@example.com")
        v.type(R.id.edit_password, "Secret1!")

        v.click(R.id.btn_login)
        idle()

        assertTrue(v.findViewById<android.widget.TextView>(R.id.btn_login).isEnabled)
        verify(exactly = 0) { nav.navigate(any<androidx.navigation.NavDirections>()) }
    }

    @Test
    fun failedSignIn_showsTheFirebaseMessage_andReenablesTheButton() = withFragment { f, v ->
        every { auth.signInWithEmailAndPassword(any(), any()) } returns Tasks.forException(RuntimeException("bad password"))
        v.type(R.id.edit_email, "rider@example.com")
        v.type(R.id.edit_password, "wrong")

        v.click(R.id.btn_login)
        idle()

        assertEquals("bad password", ShadowToast.getTextOfLatestToast())
        val button = v.findViewById<android.widget.TextView>(R.id.btn_login)
        assertTrue(button.isEnabled)
        assertEquals(f.getString(R.string.btn_login), button.text.toString())
    }

    @Test
    fun whileSigningIn_theButtonIsDisabledAndEmpty() = withFragment { _, v ->
        val pending = TaskCompletionSource<AuthResult>()
        every { auth.signInWithEmailAndPassword(any(), any()) } returns pending.task
        v.type(R.id.edit_email, "rider@example.com")
        v.type(R.id.edit_password, "Secret1!")

        v.click(R.id.btn_login)

        val button = v.findViewById<android.widget.TextView>(R.id.btn_login)
        assertFalse(button.isEnabled)
        assertEquals("", button.text.toString())
    }

    // ---- navigation ----

    @Test
    fun signUpLink_opensTheRegisterScreen() = withFragment { _, v ->
        v.click(R.id.sign_up)
        verify { nav.navigate(LoginScreenFragmentDirections.actionLoginScreenFragmentToRegisterScreenFragment()) }
    }

    @Test
    fun forgotPasswordLink_opensTheResetScreen() = withFragment { _, v ->
        v.click(R.id.forgot_password)
        verify { nav.navigate(LoginScreenFragmentDirections.actionLoginScreenFragmentToForgotPasswordScreenFragment()) }
    }

    // ---- password visibility ----

    @Test
    fun eyeIcon_togglesThePasswordVisibility() = withFragment { _, v ->
        val password = v.findViewById<EditText>(R.id.edit_password)
        val hidden = password.inputType

        v.click(R.id.toggle_password)
        assertEquals(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, password.inputType)

        v.click(R.id.toggle_password)
        assertEquals(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD, password.inputType)
        assertNotNull(hidden)
    }

    // ---- social buttons ----

    @Test
    fun googleButton_startsTheGoogleFlow_andShowsLoading() = withFragment { _, v ->
        mockkObject(LoginManager.Companion)
        every { LoginManager.getInstance() } returns mockk(relaxed = true)
        val client = mockk<GoogleSignInClient> { every { signInIntent } returns android.content.Intent("google") }
        every { GoogleSignIn.getClient(any<Context>(), any()) } returns client

        v.click(R.id.btn_google)

        assertFalse(v.findViewById<android.widget.TextView>(R.id.btn_login).isEnabled)
        verify { GoogleSignIn.getClient(any<Context>(), any()) }
    }

    @Test
    fun facebookButton_startsTheFacebookFlow_andShowsLoading() = withFragment { f, v ->
        val manager = mockk<LoginManager>(relaxed = true)
        mockkObject(LoginManager.Companion)
        every { LoginManager.getInstance() } returns manager

        v.click(R.id.btn_facebook)

        assertFalse(v.findViewById<android.widget.TextView>(R.id.btn_login).isEnabled)
        verify { manager.logInWithReadPermissions(f, listOf("email", "public_profile")) }
    }

    @Test
    fun startsWithNoErrors() = withFragment { _, v ->
        assertNull(v.error(R.id.edit_email))
        assertNull(v.error(R.id.edit_password))
        assertNotNull(ApplicationProvider.getApplicationContext<Context>())
    }
}
