package com.example.snowboard.Fragments

import android.os.Looper
import android.view.View
import androidx.core.view.GravityCompat
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.example.snowboard.Adapters.VideosAdapter
import com.example.snowboard.Content.UserContentRepository
import com.example.snowboard.HostedFragment
import com.example.snowboard.Lists.ContentType
import com.example.snowboard.Lists.UserContent
import com.example.snowboard.Lists.VideosList
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
class VideosScreenFragmentTest {

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

    private fun screen() = hostInAppCompat(VideosScreenFragment(), nav)

    private fun recycler(hosted: HostedFragment<VideosScreenFragment>) =
        hosted.fragment.requireView().findViewById<RecyclerView>(R.id.recycler_view)

    @Suppress("UNCHECKED_CAST")
    private fun videos(hosted: HostedFragment<VideosScreenFragment>): List<VideosList> {
        val field = VideosScreenFragment::class.java.getDeclaredField("videosArrayList")
        field.isAccessible = true
        return field.get(hosted.fragment) as List<VideosList>
    }

    private fun userVideo(id: String, link: String) =
        UserContent(id, ContentType.VIDEO, "other", "Video $id", "About $id", link = link)

    @Test
    fun showsTheTenBuiltInVideos() {
        assertEquals(10, recycler(screen()).adapter!!.itemCount)
    }

    @Test
    fun listensForUserVideos() {
        screen()

        verify { UserContentRepository.listen(ContentType.VIDEO, null, any(), any()) }
    }

    @Test
    fun userVideos_goFirst_withTheYouTubeIdTakenFromTheirLink() {
        val hosted = screen()

        onChange.captured(
            listOf(
                userVideo("a", "https://youtu.be/dQw4w9WgXcQ"),
                userVideo("b", "https://www.youtube.com/watch?v=abcdefghijk&t=3")
            )
        )

        assertEquals(12, recycler(hosted).adapter!!.itemCount)
        assertEquals(listOf("dQw4w9WgXcQ", "abcdefghijk"), videos(hosted).take(2).map { it.webVideos })
        assertEquals("Video a", videos(hosted)[0].title)
        assertEquals("About a", videos(hosted)[0].description)
    }

    @Test
    fun aVideoWhoseLinkIsNotYouTube_isLeftOut() {
        val hosted = screen()

        onChange.captured(listOf(userVideo("a", "https://vimeo.com/1"), userVideo("b", "dQw4w9WgXcQ")))

        assertEquals(11, recycler(hosted).adapter!!.itemCount)
        assertEquals("Video b", videos(hosted)[0].title)
    }

    @Test
    fun anUpdate_replacesThePreviousUserVideos() {
        val hosted = screen()
        onChange.captured(listOf(userVideo("a", "dQw4w9WgXcQ"), userVideo("b", "abcdefghijk")))

        onChange.captured(emptyList())

        assertEquals(10, recycler(hosted).adapter!!.itemCount)
    }

    @Test
    fun back_whileAVideoIsFullscreen_exitsFullscreenInsteadOfLeaving() {
        val hosted = screen()
        val adapter = recycler(hosted).adapter as VideosAdapter
        var exited = false
        adapter.isAnyVideoFullscreen = true
        adapter.exitCurrentFullscreen = { exited = true }

        hosted.activity.onBackPressedDispatcher.onBackPressed()

        assertTrue(exited)
        assertFalse(hosted.activity.isFinishing)
    }

    @Test
    fun back_withNoFullscreenVideo_leavesTheScreen() {
        val hosted = screen()

        hosted.activity.onBackPressedDispatcher.onBackPressed()

        assertTrue(hosted.activity.isFinishing)
    }

    @Test
    fun menuButton_opensTheDrawer() {
        val hosted = screen()

        hosted.fragment.requireView().findViewById<View>(R.id.btn_menu).performClick()
        shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)

        assertTrue(hosted.drawer.isDrawerOpen(GravityCompat.START))
    }
}
