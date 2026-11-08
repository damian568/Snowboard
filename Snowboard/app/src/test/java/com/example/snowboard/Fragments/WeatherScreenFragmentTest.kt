package com.example.snowboard.Fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.fragment.app.testing.FragmentScenario
import com.example.snowboard.BuildConfig
import com.example.snowboard.Interface.WeatherApiService
import com.example.snowboard.R
import com.example.snowboard.User.Settings.AppPreferences
import com.example.snowboard.fakeFusedLocation
import com.example.snowboard.grantLocationPermission
import com.example.snowboard.location
import com.example.snowboard.replaceWeatherService
import com.example.snowboard.weatherResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class WeatherScreenFragmentTest {

    private val api = mockk<WeatherApiService>()

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    /** Opens the screen with the fake weather API installed before the first request is made. */
    private fun open(block: (WeatherScreenFragment, View) -> Unit) {
        val scenario = FragmentScenario.launchInContainer(
            WeatherScreenFragment::class.java, null, R.style.Theme_SnowBoard,
            object : androidx.fragment.app.FragmentFactory() {
                override fun instantiate(classLoader: ClassLoader, className: String): androidx.fragment.app.Fragment =
                    super.instantiate(classLoader, className).also { replaceWeatherService(it, api) }
            }
        )
        scenario.onFragment {
            idle()
            block(it, it.requireView())
        }
    }

    private fun View.text(id: Int) = findViewById<TextView>(id).text.toString()
    private fun clock(seconds: Long) = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(seconds * 1000))

    private fun givenWeather(condition: String = "Snow") {
        grantLocationPermission()
        fakeFusedLocation(current = location(42.27, 23.62))
        coEvery { api.getCurrentWeather(any(), any(), any(), any(), any()) } returns weatherResponse(condition = condition)
    }

    // ---- showing the weather ----

    @Test
    fun showsTheCurrentWeather_inMetricUnits() {
        givenWeather("Snow")
        open { f, v ->
            assertEquals("Bansko", v.text(R.id.my_location))
            assertEquals("-3°C", v.text(R.id.temp))
            assertEquals(f.getString(R.string.weather_min, -5, "C"), v.text(R.id.min_temp))
            assertEquals(f.getString(R.string.weather_max, -1, "C"), v.text(R.id.max_temp))
            assertEquals("80%", v.text(R.id.humidity))
            assertEquals("4.2 m/s", v.text(R.id.wind_speed))
            assertEquals("Snow", v.text(R.id.conditions))
            assertEquals("1012 hPa", v.text(R.id.sea))
            assertEquals(clock(1_700_000_000L), v.text(R.id.sunrise))
            assertEquals(clock(1_700_030_000L), v.text(R.id.sunset))
            assertEquals(SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date()), v.text(R.id.date))
        }
    }

    @Test
    fun requestsTheWeatherForTheUsersLocation_withTheApiKeyAndLanguage() {
        givenWeather()
        open { _, _ ->
            coVerify {
                api.getCurrentWeather(42.27, 23.62, BuildConfig.WEATHER_API_KEY, "metric", Locale.getDefault().language)
            }
        }
    }

    @Test
    fun imperialUnits_areRequestedAndShown() {
        givenWeather()
        AppPreferences.setMetricUnits(androidx.test.core.app.ApplicationProvider.getApplicationContext(), false)
        open { f, v ->
            coVerify { api.getCurrentWeather(any(), any(), any(), "imperial", any()) }
            assertEquals("-3°F", v.text(R.id.temp))
            assertEquals("4.2 mph", v.text(R.id.wind_speed))
            assertEquals(f.getString(R.string.weather_min, -5, "F"), v.text(R.id.min_temp))
        }
    }

    // ---- the headline for each condition ----

    private fun headlineFor(condition: String, expected: Int) {
        givenWeather(condition)
        open { f, v -> assertEquals(condition, f.getString(expected), v.text(R.id.weather)) }
        unmockkAll()
    }

    @Test
    fun theHeadline_matchesTheCondition() {
        headlineFor("Clouds", R.string.weather_condition_1)
        headlineFor("Windy", R.string.weather_condition_2)
        headlineFor("Rain", R.string.weather_condition_3)
        headlineFor("Snow", R.string.weather_condition_4)
        headlineFor("Clear", R.string.weather_condition_5)
        headlineFor("Sunny", R.string.weather_condition_6)
        headlineFor("Haze", R.string.weather_condition_7)
        headlineFor("Squall", R.string.weather_condition_8)
        headlineFor("Drizzle", R.string.weather_condition_9)
        headlineFor("Thunderstorm", R.string.weather_condition_10)
    }

    @Test
    fun anUnknownCondition_isShownAsClear() = headlineFor("Tornado", R.string.weather_condition_5)

    @Test
    @Config(qualifiers = "bg")
    fun inBulgarian_theHeadlineIsStillChosenByCondition() {
        headlineFor("Snow", R.string.weather_condition_4)
        headlineFor("Thunderstorm", R.string.weather_condition_10)
        headlineFor("Tornado", R.string.weather_condition_5)
    }

    // ---- location and permission ----

    @Test
    fun withoutThePermission_itAsks_andFetchesNothing() {
        fakeFusedLocation(current = location(1.0, 1.0))
        open { f, _ ->
            val request = shadowOf(f.requireActivity()).lastRequestedPermission
            assertNotNull(request)
            assertTrue(request.requestedPermissions.contains(Manifest.permission.ACCESS_FINE_LOCATION))
            coVerify(exactly = 0) { api.getCurrentWeather(any(), any(), any(), any(), any()) }
        }
    }

    @Test
    fun grantingThePermission_loadsTheWeather() {
        fakeFusedLocation(current = location(42.27, 23.62))
        coEvery { api.getCurrentWeather(any(), any(), any(), any(), any()) } returns weatherResponse()
        open { f, v ->
            grantLocationPermission()

            f.onRequestPermissionsResult(100, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), intArrayOf(PackageManager.PERMISSION_GRANTED))
            idle()

            assertEquals("Bansko", v.text(R.id.my_location))
        }
    }

    @Test
    fun denyingThePermission_loadsNothing() {
        fakeFusedLocation(current = location(42.27, 23.62))
        open { f, _ ->
            f.onRequestPermissionsResult(100, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), intArrayOf(PackageManager.PERMISSION_DENIED))
            idle()

            coVerify(exactly = 0) { api.getCurrentWeather(any(), any(), any(), any(), any()) }
        }
    }

    @Test
    fun otherPermissionRequests_areIgnored() {
        grantLocationPermission()
        fakeFusedLocation(current = null, last = null)
        open { f, _ ->
            f.onRequestPermissionsResult(7, arrayOf("x"), intArrayOf(PackageManager.PERMISSION_GRANTED))
            idle()

            coVerify(exactly = 0) { api.getCurrentWeather(any(), any(), any(), any(), any()) }
        }
    }

    @Test
    fun withoutACurrentFix_theLastKnownLocationIsUsed() {
        grantLocationPermission()
        fakeFusedLocation(current = null, last = location(41.0, 24.0))
        coEvery { api.getCurrentWeather(any(), any(), any(), any(), any()) } returns weatherResponse(name = "Smolyan")
        open { _, v ->
            coVerify { api.getCurrentWeather(41.0, 24.0, any(), any(), any()) }
            assertEquals("Smolyan", v.text(R.id.my_location))
        }
    }

    @Test
    fun withNoLocationAtAll_nothingIsFetched() {
        grantLocationPermission()
        fakeFusedLocation(current = null, last = null)
        open { _, _ ->
            coVerify(exactly = 0) { api.getCurrentWeather(any(), any(), any(), any(), any()) }
        }
    }

    @Test
    fun aFailedRequest_leavesTheScreenAsItWas() {
        grantLocationPermission()
        fakeFusedLocation(current = location(42.27, 23.62))
        coEvery { api.getCurrentWeather(any(), any(), any(), any(), any()) } throws RuntimeException("offline")
        open { _, v ->
            coVerify(exactly = 1) { api.getCurrentWeather(any(), any(), any(), any(), any()) }
            // The request failed, so none of its data made it onto the screen
            assertNotEquals("Bansko", v.text(R.id.my_location))
            assertNotEquals("-3°C", v.text(R.id.temp))
        }
    }
}
