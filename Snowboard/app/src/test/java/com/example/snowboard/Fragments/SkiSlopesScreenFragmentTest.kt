package com.example.snowboard.Fragments

import android.os.Looper
import android.view.View
import androidx.core.view.GravityCompat
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.example.snowboard.Content.UserContentRepository
import com.example.snowboard.HostedFragment
import com.example.snowboard.Lists.ContentType
import com.example.snowboard.Lists.SkiSlopesList
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class SkiSlopesScreenFragmentTest {

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

    private fun screen() = hostInAppCompat(SkiSlopesScreenFragment(), nav)

    private fun recycler(hosted: HostedFragment<SkiSlopesScreenFragment>) =
        hosted.fragment.requireView().findViewById<RecyclerView>(R.id.recycler_view)

    @Suppress("UNCHECKED_CAST")
    private fun slopes(hosted: HostedFragment<SkiSlopesScreenFragment>): List<SkiSlopesList> {
        val field = SkiSlopesScreenFragment::class.java.getDeclaredField("skiSlopesArrayList")
        field.isAccessible = true
        return field.get(hosted.fragment) as List<SkiSlopesList>
    }

    private fun userSlope(id: String, title: String = "Resort $id", link: String = "https://resort.example/$id") =
        UserContent(id, ContentType.SKI_SLOPE, "other", title, "Description of $title", link = link)

    @Test
    fun showsTheBuiltInSlopes_collapsed() {
        val hosted = screen()

        assertEquals(10, recycler(hosted).adapter!!.itemCount)
        assertTrue(slopes(hosted).none { it.isExpanded })
    }

    @Test
    fun listensForUserSlopes() {
        screen()

        verify { UserContentRepository.listen(ContentType.SKI_SLOPE, null, any(), any()) }
    }

    @Test
    fun userSlopes_goAboveTheBuiltInOnes() {
        val hosted = screen()

        onChange.captured(listOf(userSlope("a"), userSlope("b")))

        assertEquals(12, recycler(hosted).adapter!!.itemCount)
        assertEquals(listOf("a", "b"), slopes(hosted).take(2).map { it.userContent?.id })
        assertEquals("Resort a", slopes(hosted)[0].title)
        assertEquals("Description of Resort a", slopes(hosted)[0].description)
    }

    @Test
    fun userSlopeLink_isBuiltAsAnHtmlWebsiteLink() {
        val hosted = screen()

        onChange.captured(listOf(userSlope("a", title = "A & B", link = "https://x.example/p?a=1&b=2")))

        val website = hosted.fragment.getString(R.string.ski_slope_website)
        assertEquals(
            "A &amp; B: <a href=\"https://x.example/p?a=1&amp;b=2\">$website</a>",
            slopes(hosted)[0].link
        )
    }

    @Test
    fun userSlopeWithoutAWebsite_hasNoLink() {
        val hosted = screen()

        onChange.captured(listOf(userSlope("a", link = "")))

        assertEquals("", slopes(hosted)[0].link)
    }

    @Test
    fun anUpdate_keepsOpenSlopesOpen_andReplacesTheRest() {
        val hosted = screen()
        onChange.captured(listOf(userSlope("a"), userSlope("b")))
        slopes(hosted).first { it.userContent?.id == "a" }.isExpanded = true

        onChange.captured(listOf(userSlope("a"), userSlope("c")))

        val byId = slopes(hosted).filter { it.userContent != null }.associateBy { it.userContent!!.id }
        assertEquals(setOf("a", "c"), byId.keys)
        assertTrue(byId.getValue("a").isExpanded)
        assertFalse(byId.getValue("c").isExpanded)
        assertEquals(12, recycler(hosted).adapter!!.itemCount)
    }

    @Test
    fun deletingEveryUserSlope_leavesTheBuiltInOnes() {
        val hosted = screen()
        onChange.captured(listOf(userSlope("a")))

        onChange.captured(emptyList())

        assertEquals(10, recycler(hosted).adapter!!.itemCount)
    }

    @Test
    fun menuButton_opensTheDrawer() {
        val hosted = screen()

        hosted.fragment.requireView().findViewById<View>(R.id.btn_menu).performClick()
        shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)

        assertTrue(hosted.drawer.isDrawerOpen(GravityCompat.START))
    }
}
