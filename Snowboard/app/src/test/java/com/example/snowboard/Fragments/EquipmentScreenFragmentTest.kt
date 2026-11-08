package com.example.snowboard.Fragments

import android.os.Bundle
import android.os.Looper
import android.view.View
import androidx.core.view.GravityCompat
import androidx.navigation.NavController
import androidx.navigation.Navigator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.snowboard.HostedFragment
import com.example.snowboard.R
import com.example.snowboard.hostInAppCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class EquipmentScreenFragmentTest {

    private val nav = mockk<NavController>(relaxed = true)

    private fun screen() = hostInAppCompat(EquipmentScreenFragment(), nav)

    private fun recycler(hosted: HostedFragment<EquipmentScreenFragment>) =
        hosted.fragment.requireView().findViewById<RecyclerView>(R.id.recycler_view)

    private fun RecyclerView.laidOut(): RecyclerView {
        measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(6000, View.MeasureSpec.EXACTLY)
        )
        layout(0, 0, 1080, 6000)
        return this
    }

    @Test
    fun showsTheTenPiecesOfGear_inATwoColumnGrid() {
        val rv = recycler(screen())

        assertEquals(10, rv.adapter!!.itemCount)
        assertEquals(2, (rv.layoutManager as GridLayoutManager).spanCount)
    }

    @Test
    fun tappingAnItem_opensItsDetails_withItsCategory() {
        val hosted = screen()
        val rv = recycler(hosted).laidOut()
        val args = slot<Bundle>()

        rv.findViewHolderForAdapterPosition(2)!!.itemView.findViewById<View>(R.id.equipment_cardView).performClick()

        verify { nav.navigate(eq(R.id.equipmentDetailFragment), capture(args), isNull(), any<Navigator.Extras>()) }
        val title = hosted.fragment.getString(R.string.equipment_title_3)
        assertEquals(title, args.captured.getString("EQUIPMENT_TITLE"))
        assertEquals("boots", args.captured.getString("EQUIPMENT_CATEGORY"))
        assertEquals(hosted.fragment.getString(R.string.quote_detail_equipment_3), args.captured.getString("EQUIPMENT_QUOTE"))
        assertEquals(R.drawable.boots_pic_equipment, args.captured.getInt("EQUIPMENT_IMAGE"))
        assertEquals(R.drawable.ic_gear_boots, args.captured.getInt("EQUIPMENT_ICON"))
        assertEquals("card_transform_$title", args.captured.getString("TRANSITION_NAME"))
    }

    @Test
    fun categories_areStableKeysInDisplayOrder() {
        val hosted = screen()
        val rv = recycler(hosted).laidOut()
        val args = mutableListOf<Bundle>()
        every { nav.navigate(eq(R.id.equipmentDetailFragment), capture(args), isNull(), any<Navigator.Extras>()) } returns Unit
        (0 until 10).forEach { position ->
            rv.findViewHolderForAdapterPosition(position)!!.itemView.findViewById<View>(R.id.equipment_cardView).performClick()
        }
        val categories = args.map { it.getString("EQUIPMENT_CATEGORY") }

        assertEquals(
            listOf("snowboard", "bindings", "boots", "helmet", "goggles", "jacket", "pants", "gloves", "base_layers", "socks"),
            categories
        )
    }

    @Test
    fun menuButton_opensTheDrawer() {
        val hosted = screen()

        hosted.fragment.requireView().findViewById<View>(R.id.btn_menu).performClick()
        shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)

        assertTrue(hosted.drawer.isDrawerOpen(GravityCompat.START))
    }
}
