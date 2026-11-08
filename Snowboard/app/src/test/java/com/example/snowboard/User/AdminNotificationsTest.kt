package com.example.snowboard.User

import android.os.Build
import com.example.snowboard.BuildConfig
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class AdminNotificationsTest {

    private val templateId = slot<String>()
    private val params = slot<Map<String, String>>()
    private val userDoc = mockk<DocumentReference>()

    @Before
    fun setUp() {
        mockkObject(EmailJs)
        every { EmailJs.send(capture(templateId), capture(params)) } just runs

        val users = mockk<CollectionReference>()
        val firestore = mockk<FirebaseFirestore>()
        mockkStatic(FirebaseFirestore::class)
        every { FirebaseFirestore.getInstance() } returns firestore
        every { firestore.collection("users") } returns users
        every { users.document(any()) } returns userDoc
        profileName(null)

        setDevice("Google", "Pixel 7")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun profileName(name: String?) {
        val snapshot = mockk<DocumentSnapshot> { every { getString("fullName") } returns name }
        every { userDoc.get() } returns Tasks.forResult(snapshot)
    }

    private fun setDevice(manufacturer: String, model: String) {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", manufacturer)
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", model)
    }

    private fun user(
        uid: String = "uid-1",
        email: String? = "rider@example.com",
        displayName: String? = null
    ) = mockk<FirebaseUser> {
        every { this@mockk.uid } returns uid
        every { this@mockk.email } returns email
        every { this@mockk.displayName } returns displayName
    }

    private fun notify(user: FirebaseUser = user(), provider: String = "password"): Map<String, String> {
        runBlocking { AdminNotifications.accountDeletionRequested(user, provider) }
        return params.captured
    }

    @Test
    fun usesTheAdminTemplate_andNeverSendsACode() {
        val sent = notify()
        assertEquals(BuildConfig.EMAILJS_ADMIN_TEMPLATE_ID, templateId.captured)
        assertTrue(sent.keys.none { it.contains("code", true) || it.contains("passcode", true) })
    }

    @Test
    fun describesTheUserAndTheApp() {
        val sent = notify(user(uid = "abc", email = "me@x.com"))
        assertEquals("abc", sent["user_id"])
        assertEquals("me@x.com", sent["user_email"])
        assertEquals("${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", sent["android_version"])
        assertEquals("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", sent["app_version"])
        assertTrue(sent.getValue("time").isNotBlank())
    }

    @Test
    fun missingEmail_isShownAsADash() {
        assertEquals("—", notify(user(email = null))["user_email"])
        assertEquals("—", notify(user(email = ""))["user_email"])
    }

    @Test
    fun name_prefersTheProfileName() {
        profileName("Profile Name")
        assertEquals("Profile Name", notify(user(displayName = "Google Name"))["user_name"])
    }

    @Test
    fun name_fallsBackToTheSocialDisplayName() {
        profileName(null)
        assertEquals("Google Name", notify(user(displayName = "Google Name"))["user_name"])
        profileName("   ")
        assertEquals("Google Name", notify(user(displayName = "Google Name"))["user_name"])
    }

    @Test
    fun name_isADashWhenNothingIsKnown() {
        profileName(null)
        assertEquals("—", notify(user(displayName = null))["user_name"])
        assertEquals("—", notify(user(displayName = "  "))["user_name"])
    }

    @Test
    fun name_survivesAFirestoreFailure() {
        every { userDoc.get() } returns Tasks.forException(RuntimeException("offline"))
        assertEquals("Google Name", notify(user(displayName = "Google Name"))["user_name"])
    }

    @Test
    fun provider_isNamedForHumans() {
        assertEquals("Email & password", notify(provider = "password")["provider"])
        assertEquals("Google", notify(provider = "google.com")["provider"])
        assertEquals("Facebook", notify(provider = "facebook.com")["provider"])
        assertEquals("github.com", notify(provider = "github.com")["provider"])
    }

    @Test
    fun device_isMakerAndModel() {
        setDevice("Google", "Pixel 7")
        assertEquals("Google Pixel 7", notify()["device"])
    }

    @Test
    fun device_capitalisesTheMaker() {
        setDevice("samsung", "SM-G991B")
        assertEquals("Samsung SM-G991B", notify()["device"])
    }

    @Test
    fun device_doesNotRepeatTheMakerWhenTheModelStartsWithIt() {
        setDevice("Xiaomi", "Xiaomi 13")
        assertEquals("Xiaomi 13", notify()["device"])
        setDevice("HUAWEI", "huawei P30")
        assertEquals("huawei P30", notify()["device"])
    }
}
