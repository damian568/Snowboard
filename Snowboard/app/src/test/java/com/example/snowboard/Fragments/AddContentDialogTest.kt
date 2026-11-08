package com.example.snowboard.Fragments

import android.content.Intent
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.testing.FragmentScenario
import com.example.snowboard.Content.UserContentRepository
import com.example.snowboard.Lists.ContentType
import com.example.snowboard.Lists.UserContent
import com.example.snowboard.R
import com.example.snowboard.fakeGlideViewLoading
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
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
import io.mockk.slot

@RunWith(RobolectricTestRunner::class)
class AddContentDialogTest {

    private val draft = slot<UserContent>()
    private val removeImage = slot<Boolean>()

    @Before
    fun setUp() {
        fakeGlideViewLoading()
        mockkObject(UserContentRepository)
        coEvery { UserContentRepository.save(any(), capture(draft), any(), capture(removeImage)) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    // A dismissed dialog is detached, so it can no longer resolve strings itself
    private fun str(id: Int) = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>().getString(id)

    private fun launch(
        type: ContentType,
        category: String? = null,
        content: UserContent? = null,
        block: (AddContentDialog, View) -> Unit
    ) {
        FragmentScenario.launch(
            AddContentDialog::class.java,
            AddContentDialog.newInstance(type, category, content).arguments,
            R.style.Theme_SnowBoard,
            null
        ).onFragment { block(it, it.requireView()) }
    }

    private fun View.type(id: Int, text: String) = findViewById<EditText>(id).setText(text)
    private fun View.error(id: Int) = findViewById<EditText>(id).error?.toString()
    private fun View.save() = findViewById<View>(R.id.btn_save).performClick()
    private fun View.visible(id: Int) = findViewById<View>(id).visibility == View.VISIBLE
    private fun View.text(id: Int) = findViewById<TextView>(id).text.toString()

    private fun existing(type: ContentType, image: ByteArray? = null, category: String = "boards") = UserContent(
        id = "doc1", type = type, ownerId = "me", title = "Old title", description = "Old desc",
        quote = "Old quote", link = "https://old.example", image = image, category = category
    )

    // ---- which fields each type shows ----

    @Test
    fun tip_showsQuote_noLink_noPhoto() = launch(ContentType.TIP) { f, v ->
        assertEquals(f.getString(R.string.add_tip), v.text(R.id.txt_dialog_title))
        assertTrue(v.visible(R.id.field_quote))
        assertFalse(v.visible(R.id.field_link))
        assertFalse(v.visible(R.id.photo_box))
    }

    @Test
    fun equipment_showsPhoto_noLink_noQuote() = launch(ContentType.EQUIPMENT, "boards") { f, v ->
        assertEquals(f.getString(R.string.add_equipment), v.text(R.id.txt_dialog_title))
        assertTrue(v.visible(R.id.photo_box))
        assertFalse(v.visible(R.id.field_link))
        assertFalse(v.visible(R.id.field_quote))
    }

    @Test
    fun skiSlope_showsLinkAndPhoto_withTheWebsiteHint() = launch(ContentType.SKI_SLOPE) { f, v ->
        assertTrue(v.visible(R.id.field_link))
        assertTrue(v.visible(R.id.photo_box))
        assertEquals(f.getString(R.string.hint_website_optional), v.findViewById<EditText>(R.id.edit_link).hint.toString())
        assertEquals(f.getString(R.string.hint_description), v.findViewById<EditText>(R.id.edit_description).hint.toString())
    }

    @Test
    fun video_showsLink_noPhoto_withTheYouTubeHint_andOptionalDescription() = launch(ContentType.VIDEO) { f, v ->
        assertTrue(v.visible(R.id.field_link))
        assertFalse(v.visible(R.id.photo_box))
        assertEquals(f.getString(R.string.hint_youtube_link), v.findViewById<EditText>(R.id.edit_link).hint.toString())
        assertEquals(
            f.getString(R.string.hint_description_optional),
            v.findViewById<EditText>(R.id.edit_description).hint.toString()
        )
    }

    @Test
    fun adding_showsTheAddButtonLabel() = launch(ContentType.TIP) { f, v ->
        assertEquals(f.getString(R.string.btn_add), v.text(R.id.txt_save))
    }

    // ---- editing ----

    @Test
    fun editing_prefillsTheFields_andUsesTheEditTitleAndButton() = launch(ContentType.TIP, content = existing(ContentType.TIP)) { f, v ->
        assertEquals(f.getString(R.string.edit_tip), v.text(R.id.txt_dialog_title))
        assertEquals("Old title", v.text(R.id.edit_title))
        assertEquals("Old desc", v.text(R.id.edit_description))
        assertEquals("Old quote", v.text(R.id.edit_quote))
        assertEquals("https://old.example", v.text(R.id.edit_link))
        assertEquals(f.getString(R.string.btn_save_changes), v.text(R.id.txt_save))
    }

    @Test
    fun editing_keepsTheIdOwnerAndCategory_andToastsUpdated() {
        val item = existing(ContentType.EQUIPMENT)
        launch(ContentType.EQUIPMENT, category = "ignored", content = item) { f, v ->
            v.type(R.id.edit_title, "New title")
            v.save()
            idle()

            coVerify(exactly = 1) { UserContentRepository.save(any(), any(), null, false) }
            assertEquals("doc1", draft.captured.id)
            assertEquals("me", draft.captured.ownerId)
            assertEquals("boards", draft.captured.category)
            assertEquals("New title", draft.captured.title)
            assertEquals(str(R.string.msg_content_updated), ShadowToast.getTextOfLatestToast())
        }
    }

    // ---- validation ----

    @Test
    fun emptyTitle_isRejected() = launch(ContentType.TIP) { f, v ->
        v.type(R.id.edit_description, "desc")
        v.save()
        assertEquals(f.getString(R.string.error_field_required), v.error(R.id.edit_title))
        coVerify(exactly = 0) { UserContentRepository.save(any(), any(), any(), any()) }
    }

    @Test
    fun tip_withoutADescription_isRejected() = launch(ContentType.TIP) { f, v ->
        v.type(R.id.edit_title, "Title")
        v.save()
        assertEquals(f.getString(R.string.error_field_required), v.error(R.id.edit_description))
        coVerify(exactly = 0) { UserContentRepository.save(any(), any(), any(), any()) }
    }

    @Test
    fun video_withoutAYouTubeLink_isRejected() = launch(ContentType.VIDEO) { f, v ->
        v.type(R.id.edit_title, "Title")
        v.type(R.id.edit_link, "https://vimeo.com/123")
        v.save()
        assertEquals(f.getString(R.string.error_invalid_youtube_link), v.error(R.id.edit_link))
        coVerify(exactly = 0) { UserContentRepository.save(any(), any(), any(), any()) }
    }

    @Test
    fun video_descriptionIsOptional_andTheLinkIsKept() = launch(ContentType.VIDEO) { _, v ->
        v.type(R.id.edit_title, "Carving")
        v.type(R.id.edit_link, "https://youtu.be/dQw4w9WgXcQ")
        v.save()
        idle()

        assertEquals("https://youtu.be/dQw4w9WgXcQ", draft.captured.link)
        assertEquals("", draft.captured.description)
        assertEquals("", draft.captured.quote)
    }

    @Test
    fun skiSlope_withAnInvalidWebsite_isRejected() = launch(ContentType.SKI_SLOPE) { f, v ->
        v.type(R.id.edit_title, "Resort")
        v.type(R.id.edit_description, "Nice")
        v.type(R.id.edit_link, "not a website")
        v.save()
        assertEquals(f.getString(R.string.error_invalid_website), v.error(R.id.edit_link))
        coVerify(exactly = 0) { UserContentRepository.save(any(), any(), any(), any()) }
    }

    @Test
    fun skiSlope_websiteWithoutAScheme_getsHttps() = launch(ContentType.SKI_SLOPE) { _, v ->
        v.type(R.id.edit_title, "Resort")
        v.type(R.id.edit_description, "Nice")
        v.type(R.id.edit_link, "resort.example.com/info")
        v.save()
        idle()

        assertEquals("https://resort.example.com/info", draft.captured.link)
    }

    @Test
    fun skiSlope_websiteIsOptional() = launch(ContentType.SKI_SLOPE) { _, v ->
        v.type(R.id.edit_title, "Resort")
        v.type(R.id.edit_description, "Nice")
        v.save()
        idle()

        assertEquals("", draft.captured.link)
    }

    // ---- saving ----

    @Test
    fun newTip_isSavedTrimmed_withItsQuote_andToastsAdded_thenCloses() = launch(ContentType.TIP) { f, v ->
        v.type(R.id.edit_title, "  Wax often ")
        v.type(R.id.edit_description, " Keeps you fast ")
        v.type(R.id.edit_quote, " Glide on ")

        v.save()
        idle()

        val saved = draft.captured
        assertEquals("", saved.id)
        assertEquals(ContentType.TIP, saved.type)
        assertEquals("Wax often", saved.title)
        assertEquals("Keeps you fast", saved.description)
        assertEquals("Glide on", saved.quote)
        assertEquals("", saved.link)
        assertFalse(removeImage.captured)
        assertEquals(str(R.string.msg_content_added), ShadowToast.getTextOfLatestToast())
        assertFalse(f.dialog?.isShowing ?: false)
    }

    @Test
    fun newEquipment_isFiledUnderTheCategoryItWasAddedIn() = launch(ContentType.EQUIPMENT, "boots") { _, v ->
        v.type(R.id.edit_title, "Boots")
        v.type(R.id.edit_description, "Warm")
        v.save()
        idle()

        assertEquals("boots", draft.captured.category)
    }

    @Test
    fun whileSaving_theDialogCannotBeClosed_andTheButtonShowsProgress() {
        val gate = CompletableDeferred<Unit>()
        coEvery { UserContentRepository.save(any(), any(), any(), any()) } coAnswers { gate.await() }
        launch(ContentType.TIP) { f, v ->
            v.type(R.id.edit_title, "Title")
            v.type(R.id.edit_description, "Desc")

            v.save()
            idle()

            assertFalse(f.isCancelable)
            assertFalse(v.findViewById<View>(R.id.btn_save).isEnabled)
            assertFalse(v.findViewById<View>(R.id.img_close).isEnabled)
            assertEquals(f.getString(R.string.btn_saving), v.text(R.id.txt_save))

            gate.complete(Unit)
            idle()
            assertEquals(str(R.string.msg_content_added), ShadowToast.getTextOfLatestToast())
        }
    }

    @Test
    fun failedSave_toastsTheError_andLetsTheUserTryAgain() {
        coEvery { UserContentRepository.save(any(), any(), any(), any()) } throws RuntimeException("offline")
        launch(ContentType.TIP) { f, v ->
            v.type(R.id.edit_title, "Title")
            v.type(R.id.edit_description, "Desc")

            v.save()
            idle()

            assertEquals(f.getString(R.string.msg_content_save_failed), ShadowToast.getTextOfLatestToast())
            assertTrue(v.findViewById<View>(R.id.btn_save).isEnabled)
            assertTrue(f.isCancelable)
            assertEquals(f.getString(R.string.btn_add), v.text(R.id.txt_save))
        }
    }

    // ---- photo ----

    @Test
    fun editingEquipmentWithAPhoto_showsIt_andRemoveClearsIt() {
        val item = existing(ContentType.EQUIPMENT, image = byteArrayOf(1, 2, 3))
        launch(ContentType.EQUIPMENT, content = item) { _, v ->
            assertTrue(v.visible(R.id.img_photo))
            assertTrue(v.visible(R.id.btn_remove_photo))
            assertFalse(v.visible(R.id.photo_placeholder))

            v.findViewById<View>(R.id.btn_remove_photo).performClick()
            assertFalse(v.visible(R.id.img_photo))
            assertFalse(v.visible(R.id.btn_remove_photo))
            assertTrue(v.visible(R.id.photo_placeholder))

            v.save()
            idle()
            assertTrue(removeImage.captured)
        }
    }

    @Test
    fun equipmentWithoutAPhoto_showsThePlaceholder() = launch(ContentType.EQUIPMENT, "boards") { _, v ->
        assertFalse(v.visible(R.id.img_photo))
        assertFalse(v.visible(R.id.btn_remove_photo))
        assertTrue(v.visible(R.id.photo_placeholder))
    }

    @Test
    fun tappingThePhotoBox_opensThePicker() = launch(ContentType.EQUIPMENT, "boards") { f, v ->
        v.findViewById<View>(R.id.photo_box).performClick()

        val started: Intent? = shadowOf(f.requireActivity()).nextStartedActivity
        assertNotNull(started)
        assertEquals(Intent.ACTION_GET_CONTENT, started!!.action)
        assertEquals("image/*", started.type)
    }

    // ---- closing ----

    @Test
    fun closeButton_dismissesTheDialog() = launch(ContentType.TIP) { f, v ->
        v.findViewById<View>(R.id.img_close).performClick()
        idle()

        assertFalse(f.isAdded)
        assertNull(f.dialog?.takeIf { it.isShowing })
    }
}
