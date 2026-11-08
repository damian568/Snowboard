package com.example.snowboard.Fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.NavController
import com.example.snowboard.Content.UserContentRepository
import com.example.snowboard.Fragments.Detail.EquipmentDetailFragment
import com.example.snowboard.Lists.ContentType
import com.example.snowboard.Lists.UserContent
import com.example.snowboard.R
import com.example.snowboard.fakeGlideViewLoading
import com.example.snowboard.hostInAppCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ListenerRegistration
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class EquipmentDetailFragmentTest {

    private val nav = mockk<NavController>(relaxed = true)
    private val onChange = slot<(List<UserContent>) -> Unit>()

    @Before
    fun setUp() {
        fakeGlideViewLoading()
        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns mockk { every { currentUser } returns mockk<FirebaseUser> { every { uid } returns "me" } }
        mockkObject(UserContentRepository)
        every { UserContentRepository.listen(any(), any(), any(), capture(onChange)) } returns mockk<ListenerRegistration>(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun show(title: String = "Boots", category: String? = "boots"): Pair<EquipmentDetailFragment, View> {
        val fragment = EquipmentDetailFragment().apply {
            arguments = Bundle().apply {
                putString("EQUIPMENT_TITLE", title)
                putString("EQUIPMENT_QUOTE", "Good boots matter")
                putInt("EQUIPMENT_IMAGE", R.drawable.boots_pic_equipment)
                putInt("EQUIPMENT_ICON", R.drawable.ic_gear_boots)
                putString("EQUIPMENT_CATEGORY", category)
                putString("TRANSITION_NAME", "card_transform_$title")
            }
        }
        hostInAppCompat(fragment, nav)
        return fragment to fragment.requireView()
    }

    private fun View.text(id: Int) = findViewById<TextView>(id).text.toString()

    private fun item(id: String, title: String, owner: String = "me", image: ByteArray? = null) =
        UserContent(id, ContentType.EQUIPMENT, owner, title, "$title description", image = image, category = "boots")

    // ---- the built-in content ----

    @Test
    fun showsTitleQuote_andTheTransitionName() {
        val (_, v) = show()

        assertEquals("Boots", v.text(R.id.equipment_detail_title))
        assertEquals("Good boots matter", v.text(R.id.equipment_quote))
        assertEquals("card_transform_Boots", v.findViewById<View>(R.id.equipment_detail_root).transitionName)
    }

    @Test
    fun description_dropsTheTitlePrefix() {
        val (f, v) = show("Boots")

        val full = f.getString(R.string.equipment_desc_detail_3)
        assertEquals(full.substringAfter(" – "), v.text(R.id.equipment_detail_description))
    }

    @Test
    fun everyKnownEnglishTitle_getsItsOwnDescription() {
        val expected = listOf(
            "Snowboard" to R.string.equipment_desc_detail_1, "Bindings" to R.string.equipment_desc_detail_2,
            "Boots" to R.string.equipment_desc_detail_3, "Helmet" to R.string.equipment_desc_detail_4,
            "Goggles" to R.string.equipment_desc_detail_5, "Jacket" to R.string.equipment_desc_detail_6,
            "Pants" to R.string.equipment_desc_detail_7, "Gloves" to R.string.equipment_desc_detail_8,
            "Base Layers" to R.string.equipment_desc_detail_9, "Socks" to R.string.equipment_desc_detail_10
        )
        expected.forEach { (title, res) ->
            val (f, v) = show(title)
            assertEquals(title, f.getString(res).substringAfter(" – "), v.text(R.id.equipment_detail_description))
        }
    }

    @Test
    fun everyKnownBulgarianTitle_getsItsOwnDescription() {
        val expected = listOf(
            "Сноуборд" to R.string.equipment_desc_detail_1, "Автомати" to R.string.equipment_desc_detail_2,
            "Обувки" to R.string.equipment_desc_detail_3, "Каска" to R.string.equipment_desc_detail_4,
            "Очила" to R.string.equipment_desc_detail_5, "Яке" to R.string.equipment_desc_detail_6,
            "Панталон" to R.string.equipment_desc_detail_7, "Ръкавици" to R.string.equipment_desc_detail_8,
            "Термо Бельо" to R.string.equipment_desc_detail_9, "Чорапи" to R.string.equipment_desc_detail_10
        )
        expected.forEach { (title, res) ->
            val (f, v) = show(title)
            assertEquals(title, f.getString(res).substringAfter(" – "), v.text(R.id.equipment_detail_description))
        }
    }

    @Test
    fun unknownTitle_fallsBackToTheGenericText() {
        val (f, v) = show("Mystery gear")

        assertEquals(
            f.getString(R.string.equipment_instruction_google).substringAfter(" – "),
            v.text(R.id.equipment_detail_description)
        )
    }

    @Test
    fun brands_becomeTappableRows_thatOpenTheShop() {
        val (f, v) = show("Boots")

        val brands = v.findViewById<ViewGroup>(R.id.equipment_detail_brands)
        assertEquals(View.VISIBLE, v.findViewById<View>(R.id.equipment_brands_card).visibility)
        assertTrue(brands.childCount > 0)

        val row = brands.getChildAt(0)
        assertTrue(row.findViewById<TextView>(R.id.brand_name).text.isNotBlank())
        row.findViewById<View>(R.id.brand_row).performClick()

        val started: Intent = shadowOf(f.requireActivity()).nextStartedActivity
        assertNotNull(started)
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertTrue(started.data.toString().startsWith("http"))
    }

    @Test
    fun closeButton_goesBack() {
        val (_, v) = show()

        v.findViewById<View>(R.id.equipment_btn_close).performClick()

        verify { nav.navigateUp() }
    }

    // ---- equipment added by users ----

    @Test
    fun listensForUserEquipmentInThisCategory() {
        show(category = "boots")

        verify { UserContentRepository.listen(ContentType.EQUIPMENT, "boots", any(), any()) }
    }

    @Test
    fun withoutACategory_doesNotListen() {
        show(category = null)

        verify(exactly = 0) { UserContentRepository.listen(any(), any(), any(), any()) }
    }

    @Test
    fun userEquipment_isListedInItsOwnCard() {
        val (_, v) = show()

        onChange.captured(listOf(item("1", "My boots"), item("2", "Spare boots", image = byteArrayOf(1, 2))))

        val rows = v.findViewById<ViewGroup>(R.id.equipment_user_items)
        assertEquals(View.VISIBLE, v.findViewById<View>(R.id.equipment_user_card).visibility)
        assertEquals(2, rows.childCount)
        assertEquals("My boots", rows.getChildAt(0).findViewById<TextView>(R.id.user_equipment_name).text.toString())
        assertEquals("My boots description", rows.getChildAt(0).findViewById<TextView>(R.id.user_equipment_description).text.toString())
        assertEquals(View.GONE, rows.getChildAt(0).findViewById<View>(R.id.user_equipment_photo).visibility)
        assertEquals(View.VISIBLE, rows.getChildAt(1).findViewById<View>(R.id.user_equipment_photo).visibility)
    }

    @Test
    fun onlyTheOwnerGetsEditAndDelete() {
        val (_, v) = show()

        onChange.captured(listOf(item("1", "Mine", owner = "me"), item("2", "Theirs", owner = "someone-else")))

        val rows = v.findViewById<ViewGroup>(R.id.equipment_user_items)
        assertEquals(View.VISIBLE, rows.getChildAt(0).findViewById<View>(R.id.owner_actions).visibility)
        assertEquals(View.GONE, rows.getChildAt(1).findViewById<View>(R.id.owner_actions).visibility)
    }

    @Test
    fun anUpdate_replacesTheRows_andNoItemsHidesTheCard() {
        val (_, v) = show()
        onChange.captured(listOf(item("1", "A"), item("2", "B")))

        onChange.captured(listOf(item("3", "C")))
        assertEquals(1, v.findViewById<ViewGroup>(R.id.equipment_user_items).childCount)

        onChange.captured(emptyList())
        assertEquals(0, v.findViewById<ViewGroup>(R.id.equipment_user_items).childCount)
        assertEquals(View.GONE, v.findViewById<View>(R.id.equipment_user_card).visibility)
    }
}
