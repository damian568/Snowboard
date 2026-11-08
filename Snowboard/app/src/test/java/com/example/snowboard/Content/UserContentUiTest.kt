package com.example.snowboard.Content

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.example.snowboard.Fragments.AddContentDialog
import com.example.snowboard.Lists.ContentType
import com.example.snowboard.Lists.UserContent
import com.example.snowboard.R
import com.example.snowboard.fakeGlideViewLoading
import com.example.snowboard.themedContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ListenerRegistration
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowToast

/** A bare Fragment showing one tip row, so the Fragment extension functions have a real host. */
class ContentHostFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.list_tips, container, false)
}

@RunWith(RobolectricTestRunner::class)
class UserContentUiTest {

    private val auth = mockk<FirebaseAuth>()

    @Before
    fun setUp() {
        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns auth
        every { auth.currentUser } returns mockk<FirebaseUser> { every { uid } returns "me" }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun content(owner: String = "me") =
        UserContent("c1", ContentType.TIP, owner, "My tip", "desc")

    private fun launch(): FragmentScenario<ContentHostFragment> =
        FragmentScenario.launchInContainer(ContentHostFragment::class.java, null, R.style.Theme_SnowBoard, null)

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    // ---- cropToBand ----

    private fun imageView(widthPx: Int, bitmapW: Int, bitmapH: Int) = ImageView(themedContext()).apply {
        scaleType = ImageView.ScaleType.MATRIX
        setImageBitmap(Bitmap.createBitmap(bitmapW, bitmapH, Bitmap.Config.ARGB_8888))
        layout(0, 0, widthPx, 100)
    }

    private fun values(matrix: Matrix) = FloatArray(9).also { matrix.getValues(it) }

    @Test
    fun cropToBand_scalesToTheWidth_andShiftsUpByTheTopFraction() {
        val view = imageView(widthPx = 200, bitmapW = 100, bitmapH = 400)

        view.cropToBand(0.25f)

        val v = values(view.imageMatrix)
        assertEquals(2f, v[Matrix.MSCALE_X], 0.001f)
        assertEquals(2f, v[Matrix.MSCALE_Y], 0.001f)
        // 400px tall * 0.25 * scale 2
        assertEquals(-200f, v[Matrix.MTRANS_Y], 0.001f)
        assertEquals(0f, v[Matrix.MTRANS_X], 0.001f)
    }

    @Test
    fun cropToBand_withZeroTopFraction_doesNotShift() {
        val view = imageView(widthPx = 50, bitmapW = 100, bitmapH = 100)

        view.cropToBand(0f)

        val v = values(view.imageMatrix)
        assertEquals(0.5f, v[Matrix.MSCALE_X], 0.001f)
        assertEquals(0f, v[Matrix.MTRANS_Y], 0.001f)
    }

    @Test
    fun cropToBand_withoutADrawable_leavesTheMatrixAlone() {
        val view = ImageView(themedContext()).apply { layout(0, 0, 200, 100) }
        val before = values(view.imageMatrix)

        view.cropToBand(0.17f)

        assertTrue(before.contentEquals(values(view.imageMatrix)))
    }

    @Test
    fun headerArtTop_isAFractionOfTheImage() {
        assertTrue(HEADER_ART_TOP in 0f..1f)
        assertTrue(DIALOG_WIDTH_RATIO in 0f..1f)
    }

    // ---- showContentImage ----

    @Test
    fun showContentImage_withoutBytes_clearsGlideAndShowsTheFallback() {
        val glide = fakeGlideViewLoading()
        val view = ImageView(themedContext())

        view.showContentImage(null, R.drawable.ic_tips)

        verify { glide.manager.clear(view) }
        assertEquals(R.drawable.ic_tips, shadowOf(view.drawable).createdFromResId)
    }

    @Test
    fun showContentImage_withBytes_loadsThemThroughGlide() {
        val glide = fakeGlideViewLoading()
        val view = ImageView(themedContext())
        val bytes = byteArrayOf(1, 2, 3)

        view.showContentImage(bytes, R.drawable.ic_tips)

        verify { glide.manager.load(bytes) }
        verify(exactly = 0) { glide.manager.clear(view) }
    }

    // ---- bindOwnerActions ----

    @Test
    fun bindOwnerActions_builtInItem_hidesTheButtons() {
        launch().onFragment { fragment ->
            val actions = fragment.requireView().findViewById<View>(R.id.owner_actions)
            actions.visibility = View.VISIBLE
            fragment.bindOwnerActions(actions, null)
            assertEquals(View.GONE, actions.visibility)
        }
    }

    @Test
    fun bindOwnerActions_someoneElsesItem_hidesTheButtons() {
        launch().onFragment { fragment ->
            val actions = fragment.requireView().findViewById<View>(R.id.owner_actions)
            actions.visibility = View.VISIBLE
            fragment.bindOwnerActions(actions, content(owner = "other"))
            assertEquals(View.GONE, actions.visibility)
        }
    }

    @Test
    fun bindOwnerActions_ownItem_showsTheButtons() {
        launch().onFragment { fragment ->
            val actions = fragment.requireView().findViewById<View>(R.id.owner_actions)
            actions.visibility = View.GONE
            fragment.bindOwnerActions(actions, content())
            assertEquals(View.VISIBLE, actions.visibility)
        }
    }

    @Test
    fun bindOwnerActions_edit_opensTheEditDialog() {
        fakeGlideViewLoading()
        launch().onFragment { fragment ->
            val actions = fragment.requireView().findViewById<View>(R.id.owner_actions)
            fragment.bindOwnerActions(actions, content())

            actions.findViewById<View>(R.id.btn_edit).performClick()
            fragment.childFragmentManager.executePendingTransactions()
            idle()

            val dialog = fragment.childFragmentManager.findFragmentByTag(AddContentDialog.TAG)
            assertNotNull(dialog)
            assertTrue(dialog is AddContentDialog)
        }
    }

    @Test
    fun bindOwnerActions_delete_asksForConfirmation_andCancelDeletesNothing() {
        mockkObject(UserContentRepository)
        launch().onFragment { fragment ->
            val actions = fragment.requireView().findViewById<View>(R.id.owner_actions)
            fragment.bindOwnerActions(actions, content())

            actions.findViewById<View>(R.id.btn_delete).performClick()
            val dialog: Dialog = ShadowDialog.getLatestDialog()

            assertTrue(dialog.isShowing)
            assertEquals(
                fragment.getString(R.string.delete_content_title, "My tip"),
                dialog.findViewById<TextView>(R.id.txt_title).text.toString()
            )
            dialog.findViewById<View>(R.id.btn_cancel).performClick()
            idle()

            assertTrue(!dialog.isShowing)
            coVerify(exactly = 0) { UserContentRepository.delete(any()) }
        }
    }

    @Test
    fun bindOwnerActions_confirmDelete_deletesAndToasts() {
        mockkObject(UserContentRepository)
        coEvery { UserContentRepository.delete(any()) } returns Unit
        val item = content()
        launch().onFragment { fragment ->
            val actions = fragment.requireView().findViewById<View>(R.id.owner_actions)
            fragment.bindOwnerActions(actions, item)

            actions.findViewById<View>(R.id.btn_delete).performClick()
            val dialog: Dialog = ShadowDialog.getLatestDialog()
            dialog.findViewById<View>(R.id.btn_delete).performClick()
            idle()

            assertTrue(!dialog.isShowing)
            coVerify(exactly = 1) { UserContentRepository.delete(item) }
            assertEquals(fragment.getString(R.string.msg_content_deleted), ShadowToast.getTextOfLatestToast())
        }
    }

    @Test
    fun bindOwnerActions_failedDelete_toastsTheError() {
        mockkObject(UserContentRepository)
        coEvery { UserContentRepository.delete(any()) } throws RuntimeException("offline")
        launch().onFragment { fragment ->
            val actions = fragment.requireView().findViewById<View>(R.id.owner_actions)
            fragment.bindOwnerActions(actions, content())

            actions.findViewById<View>(R.id.btn_delete).performClick()
            ShadowDialog.getLatestDialog().findViewById<View>(R.id.btn_delete).performClick()
            idle()

            assertEquals(fragment.getString(R.string.msg_content_delete_failed), ShadowToast.getTextOfLatestToast())
        }
    }

    // ---- observeUserContent ----

    @Test
    fun observeUserContent_listensForTheTypeAndCategory_andForwardsChanges() {
        mockkObject(UserContentRepository)
        val onChange = slot<(List<UserContent>) -> Unit>()
        val onError = slot<(Exception) -> Unit>()
        val registration = mockk<ListenerRegistration>(relaxed = true)
        every {
            UserContentRepository.listen(ContentType.EQUIPMENT, "boards", capture(onError), capture(onChange))
        } returns registration

        val received = mutableListOf<List<UserContent>>()
        launch().onFragment { fragment ->
            fragment.observeUserContent(ContentType.EQUIPMENT, "boards") { received += it }
        }

        onChange.captured(listOf(content()))
        assertEquals(listOf(listOf(content())), received)
        // A failed listener is only logged
        onError.captured(RuntimeException("denied"))
    }

    @Test
    fun observeUserContent_stopsListeningWhenTheViewIsDestroyed() {
        mockkObject(UserContentRepository)
        val registration = mockk<ListenerRegistration>()
        every { registration.remove() } just runs
        every { UserContentRepository.listen(any(), any(), any(), any()) } returns registration

        val scenario = launch()
        scenario.onFragment { it.observeUserContent(ContentType.TIP) {} }
        verify(exactly = 0) { registration.remove() }

        scenario.moveToState(Lifecycle.State.DESTROYED)

        verify(exactly = 1) { registration.remove() }
    }
}
