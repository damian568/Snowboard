package com.example.snowboard.Fragments

import android.content.Context
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.CheckBox
import androidx.fragment.app.testing.FragmentScenario
import androidx.test.core.app.ApplicationProvider
import com.example.snowboard.R
import com.example.snowboard.User.Settings.AppPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowToast
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class EditQuickActionsBottomSheetTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun launch(block: (EditQuickActionsBottomSheet, View) -> Unit) {
        FragmentScenario.launch(EditQuickActionsBottomSheet::class.java, null, R.style.Theme_SnowBoard, null)
            .onFragment { block(it, it.requireView()) }
    }

    private fun View.checkbox(id: Int) = findViewById<CheckBox>(id)

    @Test
    fun checkboxes_reflectTheSavedShortcuts() = launch { _, v ->
        // Default shortcuts: ski slopes, equipment, tips, videos
        assertTrue(v.checkbox(R.id.chk_ski_slopes).isChecked)
        assertTrue(v.checkbox(R.id.chk_equipment).isChecked)
        assertTrue(v.checkbox(R.id.chk_tips).isChecked)
        assertTrue(v.checkbox(R.id.chk_videos).isChecked)
        assertFalse(v.checkbox(R.id.chk_weather).isChecked)
        assertFalse(v.checkbox(R.id.chk_history).isChecked)
    }

    @Test
    fun checkboxes_reflectCustomisedShortcuts() {
        AppPreferences.setQuickActions(context, listOf("weather", "history"))
        launch { _, v ->
            assertTrue(v.checkbox(R.id.chk_weather).isChecked)
            assertTrue(v.checkbox(R.id.chk_history).isChecked)
            assertFalse(v.checkbox(R.id.chk_ski_slopes).isChecked)
        }
    }

    @Test
    fun unknownSavedIds_areIgnored() {
        AppPreferences.setQuickActions(context, listOf("bogus", "tips"))
        launch { _, v ->
            assertTrue(v.checkbox(R.id.chk_tips).isChecked)
            assertFalse(v.checkbox(R.id.chk_videos).isChecked)
        }
    }

    @Test
    fun unchecking_savesTheRemainingShortcuts_andNotifiesTheHost() {
        var notified = false
        FragmentScenario.launch(EditQuickActionsBottomSheet::class.java, null, R.style.Theme_SnowBoard, null).onFragment { f ->
            f.parentFragmentManager.setFragmentResultListener(
                EditQuickActionsBottomSheet.REQUEST_KEY, f
            ) { _, _: Bundle -> notified = true }

            f.requireView().checkbox(R.id.chk_tips).isChecked = false

            assertEquals(listOf("skiSlopes", "equipment", "videos"), AppPreferences.getQuickActions(context))
        }
        assertTrue(notified)
    }

    @Test
    fun checkingAnotherOne_whileUnderTheLimit_savesIt() = launch { _, v ->
        v.checkbox(R.id.chk_tips).isChecked = false

        v.checkbox(R.id.chk_weather).isChecked = true

        assertEquals(listOf("skiSlopes", "equipment", "videos", "weather"), AppPreferences.getQuickActions(context))
    }

    @Test
    fun checkingAFifth_isRefused_withAMessage() = launch { f, v ->
        v.checkbox(R.id.chk_weather).isChecked = true

        assertFalse(v.checkbox(R.id.chk_weather).isChecked)
        assertEquals(f.getString(R.string.quick_actions_max_reached), ShadowToast.getTextOfLatestToast())
        assertEquals(listOf("skiSlopes", "equipment", "tips", "videos"), AppPreferences.getQuickActions(context))
    }

    @Test
    fun uncheckingEverything_savesAnEmptyList() = launch { _, v ->
        listOf(R.id.chk_ski_slopes, R.id.chk_equipment, R.id.chk_tips, R.id.chk_videos).forEach {
            v.checkbox(it).isChecked = false
        }

        assertEquals(emptyList<String>(), AppPreferences.getQuickActions(context))
    }

    @Test
    fun closeButton_dismissesTheSheet() = launch { f, v ->
        assertTrue(f.isAdded)

        v.findViewById<View>(R.id.img_close).performClick()
        // The sheet slides out before it is removed
        shadowOf(Looper.getMainLooper()).idleFor(2, TimeUnit.SECONDS)

        assertFalse(f.isAdded)
    }
}
