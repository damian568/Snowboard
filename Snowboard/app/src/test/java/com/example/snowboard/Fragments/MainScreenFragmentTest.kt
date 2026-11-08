package com.example.snowboard.Fragments

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.navigation.NavController
import androidx.test.core.app.ApplicationProvider
import com.example.snowboard.HostedFragment
import com.example.snowboard.Interface.WeatherApiService
import com.example.snowboard.Lists.NearbySlopeData
import com.example.snowboard.R
import com.example.snowboard.User.Settings.AppPreferences
import com.example.snowboard.fakeFusedLocation
import com.example.snowboard.grantLocationPermission
import com.example.snowboard.hostInAppCompat
import com.example.snowboard.location
import com.example.snowboard.replaceWeatherService
import com.example.snowboard.weatherResponse
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@RunWith(RobolectricTestRunner::class)
class MainScreenFragmentTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val nav = mockk<NavController>(relaxed = true)
    private val api = mockk<WeatherApiService>()
    private val auth = mockk<FirebaseAuth>()
    private val userDoc = mockk<DocumentReference>()

    @Before
    fun setUp() {
        mockkStatic(FirebaseAuth::class, FirebaseFirestore::class, Jsoup::class)
        every { FirebaseAuth.getInstance() } returns auth
        every { auth.currentUser } returns null

        val users = mockk<CollectionReference>()
        val firestore = mockk<FirebaseFirestore>()
        every { FirebaseFirestore.getInstance() } returns firestore
        every { firestore.collection("users") } returns users
        every { users.document(any()) } returns userDoc

        coEvery { api.getCurrentWeather(any(), any(), any(), any(), any()) } returns weatherResponse()
        ratingPage("4.7")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    /** Live ratings run on a background thread, so wait for them to land on the main thread. */
    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition() && System.currentTimeMillis() < deadline) {
            idle()
            Thread.sleep(10)
        }
        assertTrue("condition was never met", condition())
    }

    private fun ratingPage(rank: String?) {
        val html = if (rank == null) "<div></div>"
        else "<div class=\"js-star-ranking stars-big\" data-rank=\"$rank\"></div>"
        val connection = mockk<Connection>()
        every { Jsoup.connect(any()) } returns connection
        every { connection.userAgent(any()) } returns connection
        every { connection.timeout(any()) } returns connection
        every { connection.get() } returns Jsoup.parse(html)
    }

    private fun screen(): HostedFragment<MainScreenFragment> {
        val fragment = MainScreenFragment()
        replaceWeatherService(fragment, api)
        return hostInAppCompat(fragment, nav)
    }

    private fun View.text(id: Int) = findViewById<TextView>(id).text.toString()

    private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 6371.0 * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    // ---- quick actions ----

    @Test
    fun quickActions_showTheFourDefaultShortcuts() {
        val v = screen().fragment.requireView()

        listOf(R.id.btn_quick_slot_1, R.id.btn_quick_slot_2, R.id.btn_quick_slot_3, R.id.btn_quick_slot_4).forEach {
            assertEquals(View.VISIBLE, v.findViewById<View>(it).visibility)
        }
        assertEquals(context.getString(R.string.menu_ski_slopes), v.text(R.id.txt_quick_slot_1))
        assertEquals(context.getString(R.string.menu_equipment), v.text(R.id.txt_quick_slot_2))
        assertEquals(context.getString(R.string.menu_tips), v.text(R.id.txt_quick_slot_3))
        assertEquals(context.getString(R.string.menu_videos), v.text(R.id.txt_quick_slot_4))
    }

    @Test
    fun quickActions_unusedSlotsAreHidden() {
        AppPreferences.setQuickActions(context, listOf("weather", "history"))

        val v = screen().fragment.requireView()

        assertEquals(context.getString(R.string.menu_weather), v.text(R.id.txt_quick_slot_1))
        assertEquals(context.getString(R.string.menu_history), v.text(R.id.txt_quick_slot_2))
        assertEquals(View.GONE, v.findViewById<View>(R.id.btn_quick_slot_3).visibility)
        assertEquals(View.GONE, v.findViewById<View>(R.id.btn_quick_slot_4).visibility)
    }

    @Test
    fun quickActions_openTheirScreens() {
        AppPreferences.setQuickActions(context, listOf("weather", "history", "skiSlopes", "videos"))
        val v = screen().fragment.requireView()

        v.findViewById<View>(R.id.btn_quick_slot_1).performClick()
        verify { nav.navigate(R.id.weatherScreenFragment) }
        v.findViewById<View>(R.id.btn_quick_slot_2).performClick()
        verify { nav.navigate(R.id.historyScreenFragment) }
        v.findViewById<View>(R.id.btn_quick_slot_3).performClick()
        verify { nav.navigate(R.id.skiSlopesScreenFragment) }
        v.findViewById<View>(R.id.btn_quick_slot_4).performClick()
        verify { nav.navigate(R.id.videosScreenFragment) }
    }

    @Test
    fun editButton_opensTheShortcutPicker() {
        val hosted = screen()

        hosted.fragment.requireView().findViewById<View>(R.id.btn_edit_quick_actions).performClick()
        hosted.fragment.childFragmentManager.executePendingTransactions()

        assertNotNull(hosted.fragment.childFragmentManager.findFragmentByTag("EditQuickActions"))
    }

    @Test
    fun whenThePickerChangesTheShortcuts_theyAreRedrawn() {
        val hosted = screen()
        val v = hosted.fragment.requireView()
        assertEquals(View.VISIBLE, v.findViewById<View>(R.id.btn_quick_slot_4).visibility)

        AppPreferences.setQuickActions(context, listOf("weather"))
        hosted.fragment.childFragmentManager.setFragmentResult(EditQuickActionsBottomSheet.REQUEST_KEY, Bundle())

        assertEquals(context.getString(R.string.menu_weather), v.text(R.id.txt_quick_slot_1))
        assertEquals(View.GONE, v.findViewById<View>(R.id.btn_quick_slot_2).visibility)
    }

    // ---- other buttons ----

    @Test
    fun menuButton_andQuickActionsViewAll_openTheDrawer() {
        val hosted = screen()
        val v = hosted.fragment.requireView()

        v.findViewById<View>(R.id.btn_menu).performClick()
        shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)
        assertTrue(hosted.drawer.isDrawerOpen(GravityCompat.START))

        hosted.drawer.closeDrawer(GravityCompat.START)
        shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)
        v.findViewById<View>(R.id.txt_quick_actions_view_all).performClick()
        shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)
        assertTrue(hosted.drawer.isDrawerOpen(GravityCompat.START))
    }

    @Test
    fun nearbySlopesViewAll_opensTheSkiSlopes() {
        screen().fragment.requireView().findViewById<View>(R.id.txt_nearby_slopes_view_all).performClick()

        verify { nav.navigate(R.id.skiSlopesScreenFragment) }
    }

    // ---- greeting ----

    @Test
    fun greeting_usesTheProfileName() {
        every { auth.currentUser } returns mockk<FirebaseUser> { every { uid } returns "uid-1" }
        val doc = mockk<DocumentSnapshot> { every { getString("fullName") } returns "Rider One" }
        every { userDoc.get() } returns Tasks.forResult(doc)

        val v = screen().fragment.requireView()
        idle()

        assertEquals(context.getString(R.string.home_user_name_format, "Rider One"), v.text(R.id.txt_user_name))
    }

    @Test
    fun greeting_keepsTheDefaultWhenTheNameIsBlank() {
        every { auth.currentUser } returns mockk<FirebaseUser> { every { uid } returns "uid-1" }
        val doc = mockk<DocumentSnapshot> { every { getString("fullName") } returns " " }
        every { userDoc.get() } returns Tasks.forResult(doc)

        val v = screen().fragment.requireView()
        val before = v.text(R.id.txt_user_name)
        idle()

        assertEquals(before, v.text(R.id.txt_user_name))
    }

    @Test
    fun greeting_signedOut_loadsNothing() {
        screen()
        idle()

        verify(exactly = 0) { userDoc.get() }
    }

    // ---- location, nearby slopes and weather ----

    @Test
    fun withoutThePermission_itAsksForIt() {
        fakeFusedLocation(current = location(41.8377, 23.4867))

        val hosted = screen()

        val request = shadowOf(hosted.activity).lastRequestedPermission
        assertNotNull(request)
        assertTrue(request.requestedPermissions.contains(Manifest.permission.ACCESS_FINE_LOCATION))
        coVerify(exactly = 0) { api.getCurrentWeather(any(), any(), any(), any(), any()) }
    }

    @Test
    fun nearbySlopes_showTheThreeClosestResorts_nearestFirst() {
        grantLocationPermission()
        val here = location(41.8377, 23.4867) // Bansko
        fakeFusedLocation(current = here)
        val v = screen().fragment.requireView()
        idle()

        val expected = NearbySlopeData.all
            .sortedBy { distanceKm(here.latitude, here.longitude, it.latitude, it.longitude) }
            .take(3)
        val titles = listOf(R.id.txt_slope_title_1, R.id.txt_slope_title_2, R.id.txt_slope_title_3).map { v.text(it) }
        assertEquals(expected.map { context.getString(it.nameRes) }, titles)
        assertEquals("Bansko", expected.first().skiresortSlug.replaceFirstChar { it.uppercase() })
        assertEquals(context.getString(R.string.distance_km_format, 0.0), v.text(R.id.txt_slope_distance_1))
        assertEquals(context.getString(expected[0].difficultyRes), v.text(R.id.txt_slope_difficulty_1))
        assertEquals(context.getString(expected[0].locationRes), v.text(R.id.txt_slope_subtitle_1))
    }

    @Test
    fun nearbySlopes_showTheLiveRating_whenTheSiteAnswers() {
        grantLocationPermission()
        fakeFusedLocation(current = location(41.8377, 23.4867))
        ratingPage("4.7")
        val v = screen().fragment.requireView()
        idle()

        waitUntil { v.text(R.id.txt_slope_rating_1) == "4.7" }
    }

    @Test
    fun nearbySlopes_keepTheBuiltInRating_whenTheSiteHasNoRating() {
        grantLocationPermission()
        fakeFusedLocation(current = location(41.8377, 23.4867))
        ratingPage(null)
        val v = screen().fragment.requireView()
        idle()
        Thread.sleep(300)
        idle()

        assertEquals(String.format(Locale.getDefault(), "%.1f", NearbySlopeData.all.first().rating), v.text(R.id.txt_slope_rating_1))
    }

    @Test
    fun nearbySlopes_keepTheBuiltInRating_whenTheSiteCannotBeReached() {
        grantLocationPermission()
        fakeFusedLocation(current = location(41.8377, 23.4867))
        val connection = mockk<Connection>()
        every { Jsoup.connect(any()) } returns connection
        every { connection.userAgent(any()) } returns connection
        every { connection.timeout(any()) } returns connection
        every { connection.get() } throws IOException("offline")
        val v = screen().fragment.requireView()
        idle()
        Thread.sleep(300)
        idle()

        assertEquals(String.format(Locale.getDefault(), "%.1f", NearbySlopeData.all.first().rating), v.text(R.id.txt_slope_rating_1))
    }

    @Test
    fun weather_isShownInTheGreetingCard() {
        grantLocationPermission()
        fakeFusedLocation(current = location(42.27, 23.62))
        val v = screen().fragment.requireView()
        idle()

        assertEquals("-3°C", v.text(R.id.txt_temp))
        assertEquals("Light snow", v.text(R.id.txt_condition))
        assertEquals("Bansko", v.text(R.id.txt_location_home))
        coVerify { api.getCurrentWeather(42.27, 23.62, any(), "metric", any()) }
    }

    @Test
    fun weather_inImperialUnits() {
        grantLocationPermission()
        AppPreferences.setMetricUnits(context, false)
        fakeFusedLocation(current = location(42.27, 23.62))
        val v = screen().fragment.requireView()
        idle()

        assertEquals("-3°F", v.text(R.id.txt_temp))
        coVerify { api.getCurrentWeather(any(), any(), any(), "imperial", any()) }
    }

    @Test
    fun weatherIcon_followsTheCondition() {
        val icons = mapOf(
            "Clear" to R.drawable.ic_weather_sun, "Sunny" to R.drawable.ic_weather_sun,
            "Rain" to R.drawable.ic_weather_rain, "Drizzle" to R.drawable.ic_weather_rain,
            "Snow" to R.drawable.ic_weather_snow, "Squall" to R.drawable.ic_weather_snow,
            "Thunderstorm" to R.drawable.ic_weather_storm,
            "Clouds" to R.drawable.ic_cloud, "Haze" to R.drawable.ic_cloud
        )
        grantLocationPermission()
        fakeFusedLocation(current = location(42.27, 23.62))
        icons.forEach { (condition, icon) ->
            coEvery { api.getCurrentWeather(any(), any(), any(), any(), any()) } returns weatherResponse(condition = condition)
            val v = screen().fragment.requireView()
            idle()

            val drawable = v.findViewById<android.widget.ImageView>(R.id.img_weather_icon).drawable
            assertEquals(condition, icon, shadowOf(drawable).createdFromResId)
        }
    }

    @Test
    fun aFailedWeatherRequest_leavesTheCardAlone() {
        grantLocationPermission()
        fakeFusedLocation(current = location(42.27, 23.62))
        coEvery { api.getCurrentWeather(any(), any(), any(), any(), any()) } throws RuntimeException("offline")

        val v = screen().fragment.requireView()
        idle()

        coVerify(exactly = 1) { api.getCurrentWeather(any(), any(), any(), any(), any()) }
        assertTrue(v.text(R.id.txt_location_home) != "Bansko")
    }

    @Test
    fun withoutACurrentFix_theLastLocationIsUsed() {
        grantLocationPermission()
        fakeFusedLocation(current = null, last = location(41.0, 24.0))
        screen()
        idle()

        coVerify { api.getCurrentWeather(41.0, 24.0, any(), any(), any()) }
    }

    @Test
    fun grantingThePermissionLater_loadsEverything() {
        fakeFusedLocation(current = location(42.27, 23.62))
        val hosted = screen()
        grantLocationPermission()

        hosted.fragment.onRequestPermissionsResult(
            100, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), intArrayOf(android.content.pm.PackageManager.PERMISSION_GRANTED)
        )
        idle()

        assertEquals("Bansko", hosted.fragment.requireView().text(R.id.txt_location_home))
    }

    @Test
    fun denyingThePermission_loadsNothing() {
        fakeFusedLocation(current = location(42.27, 23.62))
        val hosted = screen()

        hosted.fragment.onRequestPermissionsResult(
            100, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), intArrayOf(android.content.pm.PackageManager.PERMISSION_DENIED)
        )
        idle()

        coVerify(exactly = 0) { api.getCurrentWeather(any(), any(), any(), any(), any()) }
    }
}
