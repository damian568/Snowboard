package com.example.snowboard.Fragments

import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.navigation.NavController
import com.example.snowboard.R
import com.example.snowboard.User.PersonalInformation.PersonalInformationFragment
import com.example.snowboard.launchWithNav
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
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
class PersonalInformationFragmentTest {

    private val auth = mockk<FirebaseAuth>()
    private val nav = mockk<NavController>(relaxed = true)
    private val userDoc = mockk<DocumentReference>()
    private val update = slot<Map<String, Any>>()

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
        every { userDoc.update(capture(update)) } returns Tasks.forResult<Void>(null)
        profile()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun profile(name: String? = "Rider One", level: String? = "BEGINNER") {
        val doc = mockk<DocumentSnapshot> {
            every { getString("fullName") } returns name
            every { getString("level") } returns level
        }
        every { userDoc.get() } returns Tasks.forResult(doc)
    }

    private fun withFragment(block: (PersonalInformationFragment, View) -> Unit) {
        launchWithNav<PersonalInformationFragment>(nav).onFragment {
            idle()
            block(it, it.requireView())
        }
    }

    private fun View.save() = findViewById<View>(R.id.btn_save).performClick()

    @Test
    fun showsTheEmailAndTheStoredName() = withFragment { _, v ->
        assertEquals("rider@example.com", v.findViewById<TextView>(R.id.text_email).text.toString())
        assertEquals("Rider One", v.findViewById<EditText>(R.id.edit_full_name).text.toString())
    }

    @Test
    fun beginnerProfile_savesBeginner() = withFragment { _, v ->
        v.save()
        idle()
        assertEquals("BEGINNER", update.captured["level"])
    }

    @Test
    fun advanceProfile_isPreselected() {
        profile(level = "ADVANCE")
        withFragment { _, v ->
            v.save()
            idle()
            assertEquals("ADVANCE", update.captured["level"])
        }
    }

    @Test
    fun unknownLevel_fallsBackToBeginner() {
        profile(level = null)
        withFragment { _, v ->
            v.save()
            idle()
            assertEquals("BEGINNER", update.captured["level"])
        }
    }

    @Test
    fun choosingALevel_isWhatGetsSaved() = withFragment { _, v ->
        v.findViewById<View>(R.id.level_advance).performClick()
        v.save()
        idle()
        assertEquals("ADVANCE", update.captured["level"])

        v.findViewById<View>(R.id.level_beginner).performClick()
        v.save()
        idle()
        assertEquals("BEGINNER", update.captured["level"])
    }

    @Test
    fun emptyName_isRejected() = withFragment { f, v ->
        v.findViewById<EditText>(R.id.edit_full_name).setText("   ")

        v.save()

        assertEquals(f.getString(R.string.error_full_name_required), v.findViewById<EditText>(R.id.edit_full_name).error.toString())
        verify(exactly = 0) { userDoc.update(any<Map<String, Any>>()) }
    }

    @Test
    fun save_updatesTheProfile_confirms_andGoesBack() = withFragment { f, v ->
        v.findViewById<EditText>(R.id.edit_full_name).setText("  New Name ")

        v.save()
        idle()

        assertEquals("New Name", update.captured["fullName"])
        assertEquals(f.getString(R.string.msg_profile_updated), ShadowToast.getTextOfLatestToast())
        verify { nav.popBackStack() }
        assertTrue(v.findViewById<TextView>(R.id.btn_save).isEnabled)
    }

    @Test
    fun failedSave_showsTheError_andStays() = withFragment { _, v ->
        every { userDoc.update(any<Map<String, Any>>()) } returns Tasks.forException(RuntimeException("denied"))

        v.save()
        idle()

        assertEquals("denied", ShadowToast.getTextOfLatestToast())
        verify(exactly = 0) { nav.popBackStack() }
        assertTrue(v.findViewById<TextView>(R.id.btn_save).isEnabled)
    }

    @Test
    fun signedOut_neitherLoadsNorSaves() {
        every { auth.currentUser } returns null
        withFragment { _, v ->
            v.save()
            idle()
            verify(exactly = 0) { userDoc.get() }
            verify(exactly = 0) { userDoc.update(any<Map<String, Any>>()) }
        }
    }

    @Test
    fun backButton_popsTheBackStack() = withFragment { _, v ->
        v.findViewById<View>(R.id.btn_back).performClick()
        verify { nav.popBackStack() }
    }
}
