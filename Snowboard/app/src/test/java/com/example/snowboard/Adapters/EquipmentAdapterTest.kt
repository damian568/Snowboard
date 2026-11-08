package com.example.snowboard.Adapters

import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.example.snowboard.Lists.EquipmentList
import com.example.snowboard.R
import com.example.snowboard.themedContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class EquipmentAdapterTest {

    private val items = listOf(
        EquipmentList(R.drawable.ic_equipment, "Board", "A board", R.drawable.ic_equipment, "q1", "boards"),
        EquipmentList(R.drawable.ic_equipment, "Boots", "Some boots", R.drawable.ic_equipment, "q2", "boots")
    )

    private fun holderFor(adapter: EquipmentAdapter, position: Int): EquipmentAdapter.ViewHolder {
        val holder = adapter.onCreateViewHolder(FrameLayout(themedContext()), 0)
        adapter.onBindViewHolder(holder, position)
        return holder
    }

    @Test
    fun itemCount_matchesTheList() {
        assertEquals(2, EquipmentAdapter(items) { _, _ -> }.itemCount)
        assertEquals(0, EquipmentAdapter(emptyList()) { _, _ -> }.itemCount)
    }

    @Test
    fun bind_showsTitleAndDescription() {
        val holder = holderFor(EquipmentAdapter(items) { _, _ -> }, 1)

        assertEquals("Boots", holder.textTitle.text.toString())
        assertEquals("Some boots", holder.textInstruction.text.toString())
        assertNotNull(holder.imageView.drawable)
        assertNotNull(holder.iconView.drawable)
    }

    @Test
    fun bind_givesEachCardATransitionNameFromItsTitle() {
        val adapter = EquipmentAdapter(items) { _, _ -> }

        assertEquals("card_transform_Board", holderFor(adapter, 0).cardView.transitionName)
        assertEquals("card_transform_Boots", holderFor(adapter, 1).cardView.transitionName)
    }

    @Test
    fun click_reportsTheItemAndTheClickedCard() {
        var clicked: EquipmentList? = null
        var clickedView: View? = null
        val holder = holderFor(EquipmentAdapter(items) { item, view -> clicked = item; clickedView = view }, 0)

        holder.cardView.performClick()

        assertSame(items[0], clicked)
        assertSame(holder.cardView, clickedView)
    }

    @Test
    fun rebinding_replacesTheContent() {
        val adapter = EquipmentAdapter(items) { _, _ -> }
        val holder = adapter.onCreateViewHolder(FrameLayout(themedContext()), 0)

        adapter.onBindViewHolder(holder, 0)
        adapter.onBindViewHolder(holder, 1)

        assertEquals("Boots", holder.textTitle.text.toString())
        assertEquals("card_transform_Boots", holder.cardView.transitionName)
    }

    @Test
    fun iconAndImage_useTheItemsResources() {
        val holder = holderFor(EquipmentAdapter(items) { _, _ -> }, 0)

        assertEquals(R.drawable.ic_equipment, shadowOf(holder.imageView.drawable).createdFromResId)
        assertEquals(R.drawable.ic_equipment, shadowOf(holder.iconView.drawable).createdFromResId)
        assertEquals("Board", holder.itemView.findViewById<TextView>(R.id.equipment_title).text.toString())
    }
}
