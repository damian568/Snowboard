package com.example.snowboard.Fragments

import android.os.Looper
import android.view.WindowManager
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import com.example.snowboard.Constants.Constants
import com.example.snowboard.hostInAppCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class SplashScreenFragmentTest {

    private val auth = mockk<FirebaseAuth>()
    private val nav = mockk<NavController>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns auth
        every { auth.currentUser } returns null
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun waitFor(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(millis, TimeUnit.MILLISECONDS)

    @Test
    fun staysOnTheSplashUntilTheDelayHasPassed() {
        hostInAppCompat(SplashScreenFragment(), nav)

        waitFor(Constants.DelayMills_Splash - 100)

        verify(exactly = 0) { nav.navigate(any<NavDirections>()) }
    }

    @Test
    fun signedInUser_goesToTheMainScreen() {
        every { auth.currentUser } returns mockk<FirebaseUser>()
        hostInAppCompat(SplashScreenFragment(), nav)

        waitFor(Constants.DelayMills_Splash + 100)

        verify(exactly = 1) { nav.navigate(SplashScreenFragmentDirections.actionSplashScreenFragmentToMainScreenFragment()) }
        verify(exactly = 0) { nav.navigate(SplashScreenFragmentDirections.actionSplashScreenFragmentToLoginScreenFragment()) }
    }

    @Test
    fun signedOutUser_goesToTheLoginScreen() {
        hostInAppCompat(SplashScreenFragment(), nav)

        waitFor(Constants.DelayMills_Splash + 100)

        verify(exactly = 1) { nav.navigate(SplashScreenFragmentDirections.actionSplashScreenFragmentToLoginScreenFragment()) }
        verify(exactly = 0) { nav.navigate(SplashScreenFragmentDirections.actionSplashScreenFragmentToMainScreenFragment()) }
    }

    @Test
    fun leavingBeforeTheDelayEnds_neverNavigates() {
        val hosted = hostInAppCompat(SplashScreenFragment(), nav)

        waitFor(Constants.DelayMills_Splash / 2)
        hosted.activity.supportFragmentManager.beginTransaction().remove(hosted.fragment).commitNow()
        waitFor(Constants.DelayMills_Splash)

        verify(exactly = 0) { nav.navigate(any<NavDirections>()) }
    }

    @Test
    fun showsFullScreen() {
        val hosted = hostInAppCompat(SplashScreenFragment(), nav)

        val flags = hosted.activity.window.attributes.flags
        assertTrue(flags and WindowManager.LayoutParams.FLAG_FULLSCREEN != 0)
    }
}
