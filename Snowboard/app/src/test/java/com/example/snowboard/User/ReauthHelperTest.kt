package com.example.snowboard.User

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import androidx.test.core.app.ApplicationProvider
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ReauthHelperTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val fragment = mockk<Fragment>()
    private val launcher = mockk<ActivityResultLauncher<Intent>>(relaxed = true)
    private val loginManager = mockk<LoginManager>(relaxed = true)
    private val facebookCallback = slot<FacebookCallback<LoginResult>>()
    private val googleCallback = slot<ActivityResultCallback<ActivityResult>>()
    private val credential = mockk<AuthCredential>()

    private val credentials = mutableListOf<AuthCredential>()
    private val errors = mutableListOf<String?>()
    private var cancelled = 0

    @Before
    fun setUp() {
        mockkStatic(GoogleSignIn::class, GoogleAuthProvider::class, FacebookAuthProvider::class)
        mockkObject(LoginManager.Companion)
        every { LoginManager.getInstance() } returns loginManager
        every { loginManager.registerCallback(any<CallbackManager>(), capture(facebookCallback)) } returns Unit
        every { GoogleAuthProvider.getCredential(any(), any()) } returns credential
        every { FacebookAuthProvider.getCredential(any()) } returns credential

        every {
            fragment.registerForActivityResult(any<ActivityResultContract<Intent, ActivityResult>>(), capture(googleCallback))
        } returns launcher
        every { fragment.getString(any<Int>()) } returns "web-client-id"
        every { fragment.requireContext() } returns context
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun helper() = ReauthHelper(
        fragment,
        onCredential = { credentials += it },
        onCancelled = { cancelled++ },
        onError = { errors += it }
    )

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun googleResult(task: Task<GoogleSignInAccount>) {
        every { GoogleSignIn.getSignedInAccountFromIntent(any()) } returns task
        googleCallback.captured.onActivityResult(ActivityResult(Activity.RESULT_OK, Intent()))
    }

    // ---- Facebook ----

    @Test
    fun requestFacebook_asksForEmailAndProfile() {
        helper().requestFacebook()

        verify { loginManager.logInWithReadPermissions(fragment, listOf("email", "public_profile")) }
    }

    @Test
    fun facebook_success_handsBackAFreshCredential() {
        helper()
        val result = mockk<LoginResult> {
            every { accessToken } returns mockk<AccessToken> { every { token } returns "fb-token" }
        }

        facebookCallback.captured.onSuccess(result)

        verify { FacebookAuthProvider.getCredential("fb-token") }
        assertSame(credential, credentials.single())
    }

    @Test
    fun facebook_cancel_isReportedAsCancelled() {
        helper()

        facebookCallback.captured.onCancel()

        assertEquals(1, cancelled)
        assertTrue(credentials.isEmpty())
    }

    @Test
    fun facebook_error_isReportedWithItsMessage() {
        helper()

        facebookCallback.captured.onError(FacebookException("login failed"))

        assertEquals(listOf<String?>("login failed"), errors)
    }

    // ---- Google ----

    @Test
    fun requestGoogle_signsOutFirst_thenLaunchesTheAccountPicker() {
        val intent = Intent("google-sign-in")
        val client = mockk<GoogleSignInClient> {
            every { signOut() } returns Tasks.forResult<Void>(null)
            every { signInIntent } returns intent
        }
        every { GoogleSignIn.getClient(any<Context>(), any()) } returns client
        val helper = helper()

        helper.requestGoogle()
        idle()

        verify { client.signOut() }
        verify { launcher.launch(intent) }
    }

    @Test
    fun google_success_handsBackAFreshCredential() {
        helper()

        googleResult(Tasks.forResult(mockk { every { idToken } returns "id-token" }))

        verify { GoogleAuthProvider.getCredential("id-token", null) }
        assertSame(credential, credentials.single())
    }

    @Test
    fun google_missingIdToken_isAnError() {
        helper()

        googleResult(Tasks.forResult(mockk { every { idToken } returns null }))

        assertEquals(listOf<String?>("Google sign-in failed: missing ID token"), errors)
        assertTrue(credentials.isEmpty())
    }

    @Test
    fun google_backingOutOfThePicker_isCancelled() {
        helper()

        googleResult(Tasks.forException(ApiException(Status(GoogleSignInStatusCodes.SIGN_IN_CANCELLED))))

        assertEquals(1, cancelled)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun google_canceledStatus_isCancelledToo() {
        helper()

        googleResult(Tasks.forException(ApiException(Status(CommonStatusCodes.CANCELED))))

        assertEquals(1, cancelled)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun google_otherFailure_reportsTheStatusCode() {
        helper()

        googleResult(Tasks.forException(ApiException(Status(CommonStatusCodes.NETWORK_ERROR))))

        assertEquals(listOf<String?>("Google sign-in failed (${CommonStatusCodes.NETWORK_ERROR})"), errors)
        assertEquals(0, cancelled)
    }

    @Test
    fun onActivityResult_forwardsToTheFacebookSdkWithoutCrashing() {
        helper().onActivityResult(1234, 0, null)
        assertTrue(errors.isEmpty())
    }
}
