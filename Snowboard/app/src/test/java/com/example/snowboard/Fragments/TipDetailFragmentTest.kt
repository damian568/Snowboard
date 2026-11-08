package com.example.snowboard.Fragments

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.NavController
import com.example.snowboard.Fragments.Detail.TipDetailFragment
import com.example.snowboard.R
import com.example.snowboard.hostInAppCompat
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class TipDetailFragmentTest {

    private val nav = mockk<NavController>(relaxed = true)

    private fun show(
        title: String? = "Warm up",
        desc: String = "Intro paragraph",
        quote: String? = "A quote",
        icon: Int = R.drawable.ic_eye,
        image: Int = R.drawable.ic_snowboard_logo,
        transition: String? = "card_transform_Warm up"
    ): View {
        val fragment = TipDetailFragment().apply {
            arguments = Bundle().apply {
                putString("TIP_TITLE", title)
                putString("TIP_DESC", desc)
                putString("TIP_QUOTE", quote)
                putInt("TIP_ICON", icon)
                putInt("TIP_IMAGE", image)
                putString("TRANSITION_NAME", transition)
            }
        }
        hostInAppCompat(fragment, nav)
        return fragment.requireView()
    }

    private fun View.text(id: Int) = findViewById<TextView>(id).text.toString()

    @Test
    fun showsTheTitle_andTheTransitionNameOnTheRoot() {
        val v = show()

        assertEquals("Warm up", v.text(R.id.tip_detail_title))
        assertEquals("card_transform_Warm up", v.findViewById<View>(R.id.tip_detail_root).transitionName)
    }

    @Test
    fun theFirstParagraphIsTheIntro() {
        val v = show(desc = "Intro paragraph")

        assertEquals("Intro paragraph", v.text(R.id.tip_detail_description))
        assertEquals(View.GONE, v.findViewById<View>(R.id.tip_detail_sections).visibility)
    }

    @Test
    fun theRemainingParagraphsBecomeSections() {
        val v = show(desc = "Intro\n\nFirst section\n\n  Second section  \n \n Third section")

        val sections = v.findViewById<ViewGroup>(R.id.tip_detail_sections)
        assertEquals("Intro", v.text(R.id.tip_detail_description))
        assertEquals(View.VISIBLE, sections.visibility)
        assertEquals(3, sections.childCount)
        assertEquals("First section", sections.getChildAt(0).findViewById<TextView>(R.id.section_body).text.toString())
        assertEquals("Second section", sections.getChildAt(1).findViewById<TextView>(R.id.section_body).text.toString())
        assertEquals("Third section", sections.getChildAt(2).findViewById<TextView>(R.id.section_body).text.toString())
    }

    @Test
    fun onlyTheFirstSectionHasNoSeparator() {
        val v = show(desc = "Intro\n\nOne\n\nTwo")
        val sections = v.findViewById<ViewGroup>(R.id.tip_detail_sections)

        assertEquals(View.GONE, sections.getChildAt(0).findViewById<View>(R.id.section_separator).visibility)
        assertEquals(View.VISIBLE, sections.getChildAt(1).findViewById<View>(R.id.section_separator).visibility)
    }

    @Test
    fun sectionIcons_startWithTheTipsOwnIcon_thenCycle() {
        val v = show(desc = "Intro\n\nOne\n\nTwo\n\nThree\n\nFour", icon = R.drawable.ic_eye)
        val sections = v.findViewById<ViewGroup>(R.id.tip_detail_sections)

        val icons = (0 until 4).map {
            shadowOf(sections.getChildAt(it).findViewById<android.widget.ImageView>(R.id.section_icon).drawable).createdFromResId
        }
        assertEquals(listOf(R.drawable.ic_eye, R.drawable.ic_mountain, R.drawable.ic_flag_hill, R.drawable.ic_eye), icons)
    }

    @Test
    fun theQuote_isShownWhenThereIsOne() {
        val v = show(quote = "Keep going")

        assertEquals(View.VISIBLE, v.findViewById<View>(R.id.tip_quote_card).visibility)
        assertEquals("Keep going", v.text(R.id.tip_quote))
    }

    @Test
    fun theQuote_isHiddenWhenBlankOrMissing() {
        assertEquals(View.GONE, show(quote = "   ").findViewById<View>(R.id.tip_quote_card).visibility)
        assertEquals(View.GONE, show(quote = null).findViewById<View>(R.id.tip_quote_card).visibility)
    }

    @Test
    fun closeButton_goesBack() {
        val v = show()

        v.findViewById<View>(R.id.tip_btn_close).performClick()

        verify { nav.navigateUp() }
    }
}
