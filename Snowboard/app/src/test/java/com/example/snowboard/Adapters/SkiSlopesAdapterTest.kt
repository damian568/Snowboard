package com.example.snowboard.Adapters

import android.content.Intent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import org.robolectric.Robolectric
import android.widget.FrameLayout
import com.example.snowboard.Lists.ContentType
import com.example.snowboard.Lists.SkiSlopesList
import com.example.snowboard.Lists.UserContent
import com.example.snowboard.R
import com.example.snowboard.fakeGlideViewLoading
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class SkiSlopesAdapterTest {

    private val link = "<a href=\"https://bansko.example/\">bansko.example</a>"

    private fun slope(
        link: String = this.link,
        userContent: UserContent? = null,
        expanded: Boolean = false
    ) = SkiSlopesList(R.drawable.bansko_resort, "Bansko", "Big resort", R.drawable.bansko_resort, link, expanded, userContent)

    private fun userSlope(image: ByteArray?) =
        UserContent("s1", ContentType.SKI_SLOPE, "owner", "Mine", "d", link = "x", image = image)

    private val activity = Robolectric.buildActivity(AppCompatActivity::class.java).setup().get()
    private val ownerCalls = mutableListOf<Pair<View, UserContent?>>()

    @Before
    fun setUp() {
        fakeGlideViewLoading()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun holderFor(item: SkiSlopesList): Pair<SkiSlopesAdapter, SkiSlopesAdapter.ViewHolder> {
        val adapter = SkiSlopesAdapter(listOf(item)) { view, content -> ownerCalls += view to content }
        val holder = adapter.onCreateViewHolder(FrameLayout(activity), 0)
        adapter.onBindViewHolder(holder, 0)
        return adapter to holder
    }

    @Test
    fun itemCount_matchesTheList() {
        assertEquals(2, SkiSlopesAdapter(listOf(slope(), slope())) { _, _ -> }.itemCount)
    }

    @Test
    fun bind_showsTitle_andRendersTheLinkAsHtml() {
        val (_, holder) = holderFor(slope())

        assertEquals("Bansko", holder.textTitle.text.toString())
        assertEquals("bansko.example", holder.textLink.text.toString())
    }

    @Test
    fun collapsed_hidesTheDetails_andKeepsTheArrowDown() {
        val (_, holder) = holderFor(slope())

        assertEquals(View.GONE, holder.textDescription.visibility)
        assertEquals(View.GONE, holder.imageSkiSlopes.visibility)
        assertEquals(View.GONE, holder.linkRow.visibility)
        assertEquals(0f, holder.arrowButton.rotation, 0f)
    }

    @Test
    fun expanded_showsTheDetails_andFlipsTheArrow() {
        val (_, holder) = holderFor(slope(expanded = true))

        assertEquals(View.VISIBLE, holder.textDescription.visibility)
        assertEquals(View.VISIBLE, holder.imageSkiSlopes.visibility)
        assertEquals(View.VISIBLE, holder.linkRow.visibility)
        assertEquals(180f, holder.arrowButton.rotation, 0f)
    }

    @Test
    fun clickingTheCard_togglesExpanded() {
        val item = slope()
        val (_, holder) = holderFor(item)

        holder.cardView.performClick()
        assertTrue(item.isExpanded)
        assertEquals(View.VISIBLE, holder.textDescription.visibility)

        holder.cardView.performClick()
        assertFalse(item.isExpanded)
        assertEquals(View.GONE, holder.textDescription.visibility)
    }

    @Test
    fun blankLink_hidesTheLinkRowEvenWhenExpanded() {
        val (_, holder) = holderFor(slope(link = "   ", expanded = true))

        assertEquals(View.GONE, holder.linkRow.visibility)
        assertEquals(View.VISIBLE, holder.textDescription.visibility)
    }

    @Test
    fun userSlopeWithoutAPhoto_hidesThePhotoWhenExpanded() {
        val (_, holder) = holderFor(slope(userContent = userSlope(null), expanded = true))

        assertEquals(View.GONE, holder.imageSkiSlopes.visibility)
    }

    @Test
    fun userSlopeWithAPhoto_showsThePhotoWhenExpanded() {
        val (_, holder) = holderFor(slope(userContent = userSlope(byteArrayOf(1, 2)), expanded = true))

        assertEquals(View.VISIBLE, holder.imageSkiSlopes.visibility)
    }

    @Test
    fun bind_asksForOwnerActions_withTheUserContent() {
        val content = userSlope(null)
        val (_, holder) = holderFor(slope(userContent = content))

        assertSame(holder.ownerActions, ownerCalls.last().first)
        assertSame(content, ownerCalls.last().second)
    }

    @Test
    fun builtInSlope_passesNullToOwnerActions() {
        holderFor(slope())

        assertNull(ownerCalls.last().second)
    }

    @Test
    fun clickingTheLinkRow_opensTheWebsiteInTheBrowser() {
        val (_, holder) = holderFor(slope(expanded = true))

        holder.linkRow.performClick()

        val started: Intent = shadowOf(activity).nextStartedActivity
        assertNotNull(started)
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertEquals("https://bansko.example/", started.data.toString())
    }

    @Test
    fun clickingTheLinkRow_withoutAUrlInTheLink_doesNothing() {
        val (_, holder) = holderFor(slope(link = "just text", expanded = true))

        holder.linkRow.performClick()

        assertNull(shadowOf(activity).nextStartedActivity)
    }
}
