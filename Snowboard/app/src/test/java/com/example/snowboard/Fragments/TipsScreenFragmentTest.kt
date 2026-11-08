package com.example.snowboard.Fragments

import android.os.Bundle
import android.os.Looper
import android.view.View
import androidx.core.view.GravityCompat
import androidx.navigation.Navigator
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.example.snowboard.Content.UserContentRepository
import com.example.snowboard.Lists.ContentType
import com.example.snowboard.Lists.UserContent
import com.example.snowboard.R
import com.example.snowboard.hostInAppCompat
import com.google.firebase.auth.FirebaseAuth
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class TipsScreenFragmentTest {

    private val nav = mockk<NavController>(relaxed = true)
    private val onChange = slot<(List<UserContent>) -> Unit>()

    @Before
    fun setUp() {
        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns mockk(relaxed = true)
        mockkObject(UserContentRepository)
        every { UserContentRepository.listen(any(), any(), any(), capture(onChange)) } returns mockk<ListenerRegistration>(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun userTip(id: String, title: String) =
        UserContent(id, ContentType.TIP, "other", title, "$title description\n\nMore", quote = "$title quote")

    private fun screen() = hostInAppCompat(TipsScreenFragment(), nav)

    private fun RecyclerView.laidOut(): RecyclerView {
        measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(4000, View.MeasureSpec.EXACTLY)
        )
        layout(0, 0, 1080, 4000)
        return this
    }

    private fun recycler(hosted: com.example.snowboard.HostedFragment<TipsScreenFragment>) =
        hosted.fragment.requireView().findViewById<RecyclerView>(R.id.recycler_view)

    @Test
    fun showsTheTenBuiltInTips() {
        val hosted = screen()

        assertEquals(10, recycler(hosted).adapter!!.itemCount)
    }

    @Test
    fun listensForUserTips() {
        screen()

        verify { UserContentRepository.listen(ContentType.TIP, null, any(), any()) }
    }

    @Test
    fun userTips_goAboveTheBuiltInOnes() {
        val hosted = screen()

        onChange.captured(listOf(userTip("a", "First"), userTip("b", "Second")))
        idle()

        assertEquals(12, recycler(hosted).adapter!!.itemCount)
        val rv = recycler(hosted).laidOut()
        val firstTitle = rv.findViewHolderForAdapterPosition(0)!!.itemView.findViewById<android.widget.TextView>(R.id.title)
        assertEquals("First", firstTitle.text.toString())
    }

    @Test
    fun anUpdate_replacesThePreviousUserTips_insteadOfAddingToThem() {
        val hosted = screen()
        onChange.captured(listOf(userTip("a", "First"), userTip("b", "Second")))

        onChange.captured(listOf(userTip("c", "Third")))

        assertEquals(11, recycler(hosted).adapter!!.itemCount)
    }

    @Test
    fun deletingEveryUserTip_leavesOnlyTheBuiltInOnes() {
        val hosted = screen()
        onChange.captured(listOf(userTip("a", "First")))

        onChange.captured(emptyList())

        assertEquals(10, recycler(hosted).adapter!!.itemCount)
    }

    @Test
    fun tappingATip_opensItsDetails_withItsContentAndTransitionName() {
        val hosted = screen()
        val rv = recycler(hosted).laidOut()
        val args = slot<Bundle>()

        val card = rv.findViewHolderForAdapterPosition(0)!!.itemView.findViewById<View>(R.id.tip_cardView)
        card.performClick()

        verify { nav.navigate(eq(R.id.tipDetailFragment), capture(args), isNull(), any<Navigator.Extras>()) }
        val title = hosted.fragment.getString(R.string.title_detail_tip_1)
        assertEquals(title, args.captured.getString("TIP_TITLE"))
        assertEquals(hosted.fragment.getString(R.string.description_detail_tip_1), args.captured.getString("TIP_DESC"))
        assertEquals(hosted.fragment.getString(R.string.quote_detail_tip_1), args.captured.getString("TIP_QUOTE"))
        assertEquals(R.drawable.ic_snowboarding, args.captured.getInt("TIP_ICON"))
        assertEquals(R.drawable.ic_snowboard_logo, args.captured.getInt("TIP_IMAGE"))
        assertEquals("card_transform_$title", args.captured.getString("TRANSITION_NAME"))
    }

    @Test
    fun tappingAUserTip_opensItsDetails() {
        val hosted = screen()
        onChange.captured(listOf(userTip("a", "My tip")))
        val rv = recycler(hosted).laidOut()
        val args = slot<Bundle>()

        rv.findViewHolderForAdapterPosition(0)!!.itemView.findViewById<View>(R.id.tip_cardView).performClick()

        verify { nav.navigate(eq(R.id.tipDetailFragment), capture(args), isNull(), any<Navigator.Extras>()) }
        assertEquals("My tip", args.captured.getString("TIP_TITLE"))
        assertEquals("My tip quote", args.captured.getString("TIP_QUOTE"))
        assertEquals("card_transform_a", args.captured.getString("TRANSITION_NAME"))
    }

    @Test
    fun menuButton_opensTheDrawer() {
        val hosted = screen()

        hosted.fragment.requireView().findViewById<View>(R.id.btn_menu).performClick()
        shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)

        assertTrue(hosted.drawer.isDrawerOpen(GravityCompat.START))
    }
}
