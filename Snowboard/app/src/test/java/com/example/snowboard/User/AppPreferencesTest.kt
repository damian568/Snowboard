package com.example.snowboard.User

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.snowboard.User.Settings.AppPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppPreferencesTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun defaults() {
        assertFalse(AppPreferences.isDarkMode(context))
        assertTrue(AppPreferences.isMetricUnits(context))
        assertTrue(AppPreferences.isNotificationsEnabled(context))
        assertEquals(listOf("skiSlopes", "equipment", "tips", "videos"), AppPreferences.getQuickActions(context))
    }

    @Test
    fun darkMode_roundTrips() {
        AppPreferences.setDarkMode(context, true)
        assertTrue(AppPreferences.isDarkMode(context))
        AppPreferences.setDarkMode(context, false)
        assertFalse(AppPreferences.isDarkMode(context))
    }

    @Test
    fun metricUnits_roundTrips() {
        AppPreferences.setMetricUnits(context, false)
        assertFalse(AppPreferences.isMetricUnits(context))
        AppPreferences.setMetricUnits(context, true)
        assertTrue(AppPreferences.isMetricUnits(context))
    }

    @Test
    fun notifications_roundTrips() {
        AppPreferences.setNotificationsEnabled(context, false)
        assertFalse(AppPreferences.isNotificationsEnabled(context))
        AppPreferences.setNotificationsEnabled(context, true)
        assertTrue(AppPreferences.isNotificationsEnabled(context))
    }

    @Test
    fun settings_doNotAffectEachOther() {
        AppPreferences.setDarkMode(context, true)
        assertTrue(AppPreferences.isMetricUnits(context))
        assertTrue(AppPreferences.isNotificationsEnabled(context))
    }

    @Test
    fun quickActions_roundTripInOrder() {
        AppPreferences.setQuickActions(context, listOf("weather", "history", "tips"))
        assertEquals(listOf("weather", "history", "tips"), AppPreferences.getQuickActions(context))
    }

    @Test
    fun quickActions_areCappedAtTheMaximum() {
        AppPreferences.setQuickActions(context, listOf("a", "b", "c", "d", "e", "f"))
        assertEquals(listOf("a", "b", "c", "d"), AppPreferences.getQuickActions(context))
        assertEquals(4, AppPreferences.MAX_QUICK_ACTIONS)
    }

    @Test
    fun quickActions_canBeEmptied() {
        AppPreferences.setQuickActions(context, emptyList())
        assertEquals(emptyList<String>(), AppPreferences.getQuickActions(context))
    }

    @Test
    fun quickActions_blankEntriesAreDropped() {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit().putString("quick_actions", "a,,b, ,c").commit()
        assertEquals(listOf("a", "b", "c"), AppPreferences.getQuickActions(context))
    }

    @Test
    fun values_arePersistedInTheAppSettingsFile() {
        AppPreferences.setDarkMode(context, true)
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean("dark_mode", false))
    }
}
