package com.example.snowboard.Adapters

import android.view.View
import android.widget.FrameLayout
import com.example.snowboard.Lists.ContentType
import com.example.snowboard.Lists.TipsList
import com.example.snowboard.Lists.UserContent
import com.example.snowboard.R
import com.example.snowboard.themedContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TipsAdapterTest {

    private val userTip = UserContent("u-1", ContentType.TIP, "owner", "Mine", "d")
    private val builtIn = TipsList("Built-in", "Built-in desc", 0, R.drawable.ic_tips, "q")
    private val added = TipsList("Added", "Added desc", 0, R.drawable.ic_tips, "q", userTip)

    private class Recorder {
        val calls = mutableListOf<Pair<View, UserContent?>>()
        var show = false
        val bind: (View, UserContent?) -> Unit = { view, content ->
            calls += view to content
            view.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    private fun holderFor(adapter: TipsAdapter, position: Int): TipsAdapter.ViewHolder {
        val holder = adapter.onCreateViewHolder(FrameLayout(themedContext()), 0)
        adapter.onBindViewHolder(holder, position)
        return holder
    }

    @Test
    fun itemCount_matchesTheList() {
        assertEquals(2, TipsAdapter(arrayListOf(builtIn, added), { _, _ -> }, { _, _ -> }).itemCount)
    }

    @Test
    fun bind_showsTitleAndDescription() {
        val holder = holderFor(TipsAdapter(arrayListOf(builtIn), { _, _ -> }, { _, _ -> }), 0)

        assertEquals("Built-in", holder.textTitle.text.toString())
        assertEquals("Built-in desc", holder.textDescription.text.toString())
    }

    @Test
    fun transitionName_usesTheTitleForBuiltInTips_andTheIdForUserTips() {
        val adapter = TipsAdapter(arrayListOf(builtIn, added), { _, _ -> }, { _, _ -> })

        assertEquals("card_transform_Built-in", holderFor(adapter, 0).cardView.transitionName)
        assertEquals("card_transform_u-1", holderFor(adapter, 1).cardView.transitionName)
    }

    @Test
    fun bind_asksForOwnerActions_passingTheUserContent() {
        val recorder = Recorder()
        val adapter = TipsAdapter(arrayListOf(builtIn, added), recorder.bind) { _, _ -> }

        val first = holderFor(adapter, 0)
        val second = holderFor(adapter, 1)

        assertSame(first.ownerActions, recorder.calls[0].first)
        assertNull(recorder.calls[0].second)
        assertSame(second.ownerActions, recorder.calls[1].first)
        assertSame(userTip, recorder.calls[1].second)
    }

    @Test
    fun chevron_isVisibleWhenThereAreNoOwnerActions() {
        val recorder = Recorder().apply { show = false }
        val holder = holderFor(TipsAdapter(arrayListOf(builtIn), recorder.bind) { _, _ -> }, 0)

        assertEquals(View.VISIBLE, holder.chevron.visibility)
    }

    @Test
    fun chevron_isHiddenInFavourOfTheOwnerActions() {
        val recorder = Recorder().apply { show = true }
        val holder = holderFor(TipsAdapter(arrayListOf(added), recorder.bind) { _, _ -> }, 0)

        assertEquals(View.INVISIBLE, holder.chevron.visibility)
    }

    @Test
    fun click_reportsTheTipAndTheClickedCard() {
        var clicked: TipsList? = null
        var clickedView: View? = null
        val holder = holderFor(
            TipsAdapter(arrayListOf(added), { _, _ -> }, { tip, view -> clicked = tip; clickedView = view }), 0
        )

        holder.cardView.performClick()

        assertSame(added, clicked)
        assertSame(holder.cardView, clickedView)
    }
}
