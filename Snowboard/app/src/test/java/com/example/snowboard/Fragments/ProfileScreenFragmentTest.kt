package com.example.snowboard.Fragments

import android.net.Uri
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.navigation.NavController
import com.example.snowboard.R
import com.example.snowboard.User.AvatarRepository
import com.example.snowboard.User.ProfileScreenFragment
import com.example.snowboard.User.ProfileScreenFragmentDirections
import com.example.snowboard.fakeGlideViewLoading
import com.example.snowboard.hostInAppCompat
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
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
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class ProfileScreenFragmentTest {

    private val auth = mockk<FirebaseAuth>(relaxed = true)
    private val nav = mockk<NavController>(relaxed = true)
    private val userDoc = mockk<DocumentReference>()

    @Before
    fun setUp() {
        mockkStatic(FirebaseAuth::class, FirebaseFirestore::class)
        every { FirebaseAuth.getInstance() } returns auth
        every { auth.currentUser } returns mockk<FirebaseUser> {
            every { uid } returns "uid-1"
            every { email } returns "rider@example.com"
        }
        val users = mockk<CollectionReference>()
        val firestore = mockk<FirebaseFirestore>()
        every { FirebaseFirestore.getInstance() } returns firestore
        every { firestore.collection("users") } returns users
        every { users.document("uid-1") } returns userDoc
        profile()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun profile(name: String? = "Rider One", level: String? = "BEGINNER", avatar: Blob? = null) {
        val doc = mockk<DocumentSnapshot> {
            every { getString("fullName") } returns name
            every { getString("level") } returns level
            every { getBlob("avatar") } returns avatar
        }
        every { userDoc.get() } returns Tasks.forResult(doc)
    }

    private fun show(): Pair<ProfileScreenFragment, View> {
        val hosted = hostInAppCompat(ProfileScreenFragment(), nav)
        idle()
        return hosted.fragment to hosted.fragment.requireView()
    }

    private fun View.text(id: Int) = findViewById<TextView>(id).text.toString()

    // ---- loading the profile ----

    @Test
    fun showsEmailNameAndBeginnerLevel() {
        fakeGlideViewLoading()
        val (f, v) = show()

        assertEquals("rider@example.com", v.text(R.id.user_email))
        assertEquals("Rider One", v.text(R.id.user_name))
        assertEquals(f.getString(R.string.level_beginner_label), v.text(R.id.stat_level_value))
    }

    @Test
    fun anyOtherLevel_isShownAsAdvanced() {
        fakeGlideViewLoading()
        profile(level = "ADVANCE")
        val (f, v) = show()

        assertEquals(f.getString(R.string.level_advanced_label), v.text(R.id.stat_level_value))
    }

    @Test
    fun blankName_keepsTheLayoutsDefault() {
        fakeGlideViewLoading()
        val before = hostInAppCompat(ProfileScreenFragment(), nav).fragment.requireView().text(R.id.user_name)
        profile(name = "  ")
        val (_, v) = show()
        idle()

        assertEquals(before, v.text(R.id.user_name))
    }

    @Test
    fun withoutAnAvatar_showsTheLogo() {
        fakeGlideViewLoading()
        val (_, v) = show()

        val avatar = v.findViewById<ImageView>(R.id.img_avatar)
        assertEquals(R.drawable.ic_snowboard_logo, shadowOf(avatar.drawable).createdFromResId)
    }

    @Test
    fun withAnAvatar_loadsItsBytesThroughGlide() {
        val glide = fakeGlideViewLoading()
        profile(avatar = Blob.fromBytes(byteArrayOf(1, 2, 3)))
        show()

        verify { glide.manager.load(byteArrayOf(1, 2, 3)) }
    }

    @Test
    fun signedOut_loadsNothing() {
        fakeGlideViewLoading()
        every { auth.currentUser } returns null
        val (_, v) = show()

        assertFalse(v.text(R.id.user_email) == "rider.com")
        verify(exactly = 0) { userDoc.get() }
    }

    // ---- navigation ----

    @Test
    fun rows_openTheirScreens() {
        fakeGlideViewLoading()
        val (_, v) = show()

        v.findViewById<View>(R.id.row_personal_info).performClick()
        verify { nav.navigate(ProfileScreenFragmentDirections.actionProfileScreenFragmentToPersonalInformationFragment()) }

        v.findViewById<View>(R.id.row_settings).performClick()
        verify { nav.navigate(ProfileScreenFragmentDirections.actionProfileScreenFragmentToSettingsFragment()) }

        v.findViewById<View>(R.id.row_help_support).performClick()
        verify { nav.navigate(ProfileScreenFragmentDirections.actionProfileScreenFragmentToHelpSupportFragment()) }
    }

    @Test
    fun logOut_signsOut_andReturnsToTheLoginScreen() {
        fakeGlideViewLoading()
        val (_, v) = show()

        v.findViewById<View>(R.id.row_log_out).performClick()

        verify { auth.signOut() }
        verify { nav.navigate(ProfileScreenFragmentDirections.actionProfileScreenFragmentToLoginScreenFragment()) }
    }

    @Test
    fun menuButton_opensTheDrawer() {
        fakeGlideViewLoading()
        val hosted = hostInAppCompat(ProfileScreenFragment(), nav)

        hosted.fragment.requireView().findViewById<View>(R.id.btn_menu).performClick()
        // The drawer slides open, so let the animation run
        shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)

        assertTrue(hosted.drawer.isDrawerOpen(GravityCompat.START))
    }

    // ---- changing the avatar ----

    private fun pickAvatar(fragment: ProfileScreenFragment, uri: Uri) {
        val method = ProfileScreenFragment::class.java.getDeclaredMethod("setAvatarImage", Uri::class.java)
        method.isAccessible = true
        method.invoke(fragment, uri)
    }

    @Test
    fun pickedAvatar_isUploaded_whileTheCameraButtonIsDisabled() {
        fakeGlideViewLoading()
        mockkObject(AvatarRepository)
        val gate = CompletableDeferred<ByteArray>()
        coEvery { AvatarRepository.saveAvatar(any(), "uid-1", any()) } coAnswers { gate.await() }
        val (f, v) = show()

        pickAvatar(f, Uri.parse("content://photo/1"))
        idle()
        assertFalse(v.findViewById<View>(R.id.btn_camera).isEnabled)

        gate.complete(byteArrayOf(7))
        idle()
        assertTrue(v.findViewById<View>(R.id.btn_camera).isEnabled)
    }

    @Test
    fun failedUpload_toastsTheError_andRestoresTheAvatar() {
        fakeGlideViewLoading()
        mockkObject(AvatarRepository)
        coEvery { AvatarRepository.saveAvatar(any(), any(), any()) } throws RuntimeException("offline")
        val (f, v) = show()

        pickAvatar(f, Uri.parse("content://photo/1"))
        idle()

        assertEquals(f.getString(R.string.msg_avatar_upload_failed), ShadowToast.getTextOfLatestToast())
        assertTrue(v.findViewById<View>(R.id.btn_camera).isEnabled)
        assertEquals(R.drawable.ic_snowboard_logo, shadowOf(v.findViewById<ImageView>(R.id.img_avatar).drawable).createdFromResId)
    }
}
