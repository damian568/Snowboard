package com.example.snowboard.Adapters

import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LifecycleOwner
import com.example.snowboard.Lists.ContentType
import com.example.snowboard.Lists.UserContent
import com.example.snowboard.Lists.VideosList
import com.example.snowboard.themedContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VideosAdapterTest {

    private val owner = object : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle get() = registry
    }

    private val userVideo = UserContent("v1", ContentType.VIDEO, "owner", "Mine", "d", link = "dQw4w9WgXcQ")
    private val calls = mutableListOf<Pair<View, UserContent?>>()

    private fun adapterOf(vararg videos: VideosList) =
        VideosAdapter(videos.toList(), owner.lifecycle) { view, content -> calls += view to content }

    private fun holderFor(adapter: VideosAdapter, position: Int): VideosAdapter.ViewHolder {
        val holder = adapter.onCreateViewHolder(FrameLayout(themedContext()), 0)
        adapter.onBindViewHolder(holder, position)
        return holder
    }

    @Test
    fun itemCount_matchesTheList() {
        assertEquals(2, adapterOf(VideosList("a", "A", "d"), VideosList("b", "B", "d")).itemCount)
    }

    @Test
    fun startsWithNoVideoInFullscreen() {
        val adapter = adapterOf()
        assertFalse(adapter.isAnyVideoFullscreen)
        adapter.exitCurrentFullscreen() // the default is a harmless no-op
    }

    @Test
    fun bind_showsTitleAndDescription() {
        val holder = holderFor(adapterOf(VideosList("a", "Carving basics", "How to carve")), 0)

        assertEquals("Carving basics", holder.titleVideos.text.toString())
        assertEquals("How to carve", holder.descriptionVideos.text.toString())
        assertEquals(View.VISIBLE, holder.descriptionVideos.visibility)
    }

    @Test
    fun bind_blankDescription_isHidden() {
        val holder = holderFor(adapterOf(VideosList("a", "Title", "  ")), 0)

        assertEquals(View.GONE, holder.descriptionVideos.visibility)
    }

    @Test
    fun bind_asksForOwnerActions_withTheUserContent() {
        val holder = holderFor(adapterOf(VideosList("a", "Mine", "d", userVideo)), 0)

        assertSame(holder.ownerActions, calls.last().first)
        assertSame(userVideo, calls.last().second)
    }

    @Test
    fun bind_builtInVideo_passesNullToOwnerActions() {
        holderFor(adapterOf(VideosList("a", "Built-in", "d")), 0)

        assertNull(calls.last().second)
    }

    @Test
    fun bind_attachesThePlayerToTheLifecycle() {
        val holder = holderFor(adapterOf(VideosList("a", "T", "d")), 0)

        // The player view is a lifecycle observer, so it is paused / released with the screen
        assertEquals(1, owner.registry.observerCount)
        assertFalse(holder.isInitialized) // becomes true once YouTube reports the player ready
    }
}
