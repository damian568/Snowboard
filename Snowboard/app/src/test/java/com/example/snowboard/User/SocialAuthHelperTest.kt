package com.example.snowboard.User

import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import androidx.test.core.app.ApplicationProvider
import com.example.snowboard.User.Model.UserProfile
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class SocialAuthHelperTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val fragment = mockk<Fragment>()
    private val launcher = mockk<ActivityResultLauncher<Intent>>(relaxed = true)
    private val auth = mockk<FirebaseAuth>()
    private val userDoc = mockk<DocumentReference>()
    private val loginManager = mockk<LoginManager>(relaxed = true)
    private val facebookCallback = slot<FacebookCallback<LoginResult>>()
    private val credential = mockk<AuthCredential>()

    private val loading = mutableListOf<Boolean>()
    private val errors = mutableListOf<String?>()
    private var successes = 0

    @Before
    fun setUp() {
        mockkStatic(
            FirebaseAuth::class, FirebaseFirestore::class, GoogleSignIn::class,
            GoogleAuthProvider::class, FacebookAuthProvider::class
        )
        every { FirebaseAuth.getInstance() } returns auth

        val users = mockk<CollectionReference>()
        val firestore = mockk<FirebaseFirestore>()
        every { FirebaseFirestore.getInstance() } returns firestore
        every { firestore.collection("users") } returns users
        every { users.document(any()) } returns userDoc

        mockkObject(LoginManager.Companion)
        every { LoginManager.getInstance() } returns loginManager
        every { loginManager.registerCallback(any<CallbackManager>(), capture(facebookCallback)) } returns Unit

        every { GoogleAuthProvider.getCredential(any(), any()) } returns credential
        every { FacebookAuthProvider.getCredential(any()) } returns credential

        every { fragment.getString(any<Int>()) } returns "web-client-id"
        every { fragment.requireContext() } returns context
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun helper() = SocialAuthHelper(
        fragment = fragment,
        googleSignInLauncher = launcher,
        onLoading = { loading += it },
        onError = { errors += it },
        onSuccess = { successes++ }
    )

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun firebaseUser(name: String? = "Rider", email: String? = "rider@example.com") = mockk<FirebaseUser> {
        every { uid } returns "uid-1"
        every { displayName } returns name
        every { this@mockk.email } returns email
    }

    private fun signsInAs(user: FirebaseUser?) {
        val result = mockk<AuthResult> { every { this@mockk.user } returns user }
        every { auth.signInWithCredential(credential) } returns Tasks.forResult(result)
    }

    private fun profileExists(exists: Boolean) {
        val snapshot = mockk<DocumentSnapshot> { every { exists() } returns exists }
        every { userDoc.get() } returns Tasks.forResult(snapshot)
    }

    private fun facebookLogin(token: String = "fb-token") {
        val result = mockk<LoginResult> {
            every { accessToken } returns mockk<AccessToken> { every { this@mockk.token } returns token }
        }
        facebookCallback.captured.onSuccess(result)
        idle()
    }

    // ---- Facebook ----

    @Test
    fun signInWithFacebook_showsLoading_andAsksForEmailAndProfile() {
        helper().signInWithFacebook()

        assertEquals(listOf(true), loading)
        verify { loginManager.logInWithReadPermissions(fragment, listOf("email", "public_profile")) }
    }

    @Test
    fun facebook_cancel_hidesLoadingWithoutAnError() {
        helper()

        facebookCallback.captured.onCancel()

        assertEquals(listOf(false), loading)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun facebook_error_hidesLoading_andReportsTheMessage() {
        helper()

        facebookCallback.captured.onError(FacebookException("no network"))

        assertEquals(listOf(false), loading)
        assertEquals(listOf<String?>("no network"), errors)
    }

    @Test
    fun facebook_success_forAnExistingProfile_goesStraightIn() {
        helper()
        signsInAs(firebaseUser())
        profileExists(true)

        facebookLogin()

        verify { FacebookAuthProvider.getCredential("fb-token") }
        assertEquals(1, successes)
        assertEquals(listOf(false), loading)
        verify(exactly = 0) { userDoc.set(any()) }
    }

    @Test
    fun facebook_success_forANewUser_createsABeginnerProfileFirst() {
        helper()
        signsInAs(firebaseUser(name = "Rider", email = "rider@example.com"))
        profileExists(false)
        val profile = slot<Any>()
        every { userDoc.set(capture(profile)) } returns Tasks.forResult<Void>(null)

        facebookLogin()

        assertEquals(UserProfile("Rider", "rider@example.com", "BEGINNER"), profile.captured)
        assertEquals(1, successes)
        assertEquals(listOf(false), loading)
    }

    @Test
    fun newUser_withoutNameOrEmail_getsEmptyProfileFields() {
        helper()
        signsInAs(firebaseUser(name = null, email = null))
        profileExists(false)
        val profile = slot<Any>()
        every { userDoc.set(capture(profile)) } returns Tasks.forResult<Void>(null)

        facebookLogin()

        assertEquals(UserProfile("", "", "BEGINNER"), profile.captured)
    }

    @Test
    fun newUser_whenSavingTheProfileFails_reportsTheError() {
        helper()
        signsInAs(firebaseUser())
        profileExists(false)
        every { userDoc.set(any()) } returns Tasks.forException(RuntimeException("write denied"))

        facebookLogin()

        assertEquals(0, successes)
        assertEquals(listOf<String?>("write denied"), errors)
        assertEquals(listOf(false), loading)
    }

    @Test
    fun whenLoadingTheProfileFails_reportsTheError() {
        helper()
        signsInAs(firebaseUser())
        every { userDoc.get() } returns Tasks.forException(RuntimeException("offline"))

        facebookLogin()

        assertEquals(0, successes)
        assertEquals(listOf<String?>("offline"), errors)
    }

    @Test
    fun whenFirebaseRejectsTheCredential_reportsTheError() {
        helper()
        every { auth.signInWithCredential(credential) } returns Tasks.forException(RuntimeException("account exists"))

        facebookLogin()

        assertEquals(0, successes)
        assertEquals(listOf<String?>("account exists"), errors)
        assertEquals(listOf(false), loading)
    }

    @Test
    fun whenFirebaseReturnsNoUser_reportsAnErrorWithoutMessage() {
        helper()
        signsInAs(null)

        facebookLogin()

        assertEquals(0, successes)
        assertEquals(listOf<String?>(null), errors)
        assertEquals(listOf(false), loading)
    }

    // ---- Google ----

    private fun googleResult(task: com.google.android.gms.tasks.Task<GoogleSignInAccount>) {
        every { GoogleSignIn.getSignedInAccountFromIntent(any()) } returns task
    }

    @Test
    fun signInWithGoogle_showsLoading_andLaunchesTheSignInIntent() {
        val intent = Intent("google-sign-in")
        val client = mockk<GoogleSignInClient> { every { signInIntent } returns intent }
        every { GoogleSignIn.getClient(any<Context>(), any()) } returns client

        helper().signInWithGoogle()

        assertEquals(listOf(true), loading)
        verify { launcher.launch(intent) }
    }

    @Test
    fun google_success_signsInWithTheIdToken() {
        val helper = helper()
        googleResult(Tasks.forResult(mockk { every { idToken } returns "id-token" }))
        signsInAs(firebaseUser())
        profileExists(true)

        helper.handleGoogleSignInResult(Intent())
        idle()

        verify { GoogleAuthProvider.getCredential("id-token", null) }
        assertEquals(1, successes)
    }

    @Test
    fun google_missingIdToken_reportsAnError() {
        val helper = helper()
        googleResult(Tasks.forResult(mockk { every { idToken } returns null }))

        helper.handleGoogleSignInResult(Intent())

        assertEquals(listOf(false), loading)
        assertEquals(listOf<String?>("Google sign-in failed: missing ID token"), errors)
        assertEquals(0, successes)
    }

    @Test
    fun google_cancelled_hidesLoadingQuietly() {
        val helper = helper()
        googleResult(Tasks.forException(ApiException(Status(CommonStatusCodes.CANCELED))))

        helper.handleGoogleSignInResult(Intent())

        assertEquals(listOf(false), loading)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun google_otherFailure_reportsTheStatusCode() {
        val helper = helper()
        googleResult(Tasks.forException(ApiException(Status(CommonStatusCodes.NETWORK_ERROR))))

        helper.handleGoogleSignInResult(Intent())

        assertEquals(listOf(false), loading)
        assertEquals(listOf<String?>("Google sign-in failed (${CommonStatusCodes.NETWORK_ERROR})"), errors)
    }

    @Test
    fun onActivityResult_forwardsToTheFacebookSdkWithoutCrashing() {
        helper().onActivityResult(1234, 0, null)
        assertNull(errors.firstOrNull())
    }
}
