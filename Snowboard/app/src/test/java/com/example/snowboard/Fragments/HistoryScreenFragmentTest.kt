package com.example.snowboard.Fragments

import android.graphics.Matrix
import android.os.Looper
import android.view.View
import android.widget.ImageView
import androidx.core.view.GravityCompat
import androidx.navigation.NavController
import com.example.snowboard.R
import com.example.snowboard.hostInAppCompat
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class HistoryScreenFragmentTest {

    private val nav = mockk<NavController>(relaxed = true)

    @Test
    fun menuButton_opensTheDrawer() {
        val hosted = hostInAppCompat(HistoryScreenFragment(), nav)

        hosted.fragment.requireView().findViewById<View>(R.id.btn_menu).performClick()
        // The drawer slides open, so let the animation run
        shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)

        assertTrue(hosted.drawer.isDrawerOpen(GravityCompat.START))
    }

    @Test
    fun headerArt_isScaledToFillTheHeader_andAnchoredToTheTop() {
        val hosted = hostInAppCompat(HistoryScreenFragment(), nav)
        val art = hosted.fragment.requireView().findViewById<ImageView>(R.id.header_art)
        shadowOf(Looper.getMainLooper()).idle()
        val width = art.width.toFloat()
        val height = art.height.toFloat()
        assertTrue("header was never laid out", width > 0 && height > 0)

        val drawable = art.drawable
        val expectedScale = maxOf(width / drawable.intrinsicWidth, height / drawable.intrinsicHeight)
        val values = FloatArray(9).also { art.imageMatrix.getValues(it) }

        assertEquals(expectedScale, values[Matrix.MSCALE_X], 0.0001f)
        assertEquals(expectedScale, values[Matrix.MSCALE_Y], 0.0001f)
        // Centered horizontally, never shifted vertically
        assertEquals((width - drawable.intrinsicWidth * expectedScale) / 2f, values[Matrix.MTRANS_X], 0.01f)
        assertEquals(0f, values[Matrix.MTRANS_Y], 0.0001f)
    }
}
