package com.example.snowboard.Fragments

import android.app.Application
import android.content.Intent
import android.view.View
import android.widget.TextView
import androidx.navigation.NavController
import androidx.test.core.app.ApplicationProvider
import com.example.snowboard.R
import com.example.snowboard.User.HelpSupport.HelpSupportFragment
import com.example.snowboard.launchWithNav
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
class HelpSupportFragmentTest {

    private val nav = mockk<NavController>(relaxed = true)

    private fun withFragment(block: (HelpSupportFragment, View) -> Unit) {
        launchWithNav<HelpSupportFragment>(nav).onFragment { block(it, it.requireView()) }
    }

    @Test
    fun showsTheAppVersion() = withFragment { f, v ->
        val versionName = f.requireContext().packageManager.getPackageInfo(f.requireContext().packageName, 0).versionName
        assertEquals(
            f.getString(R.string.help_app_version_value, versionName),
            v.findViewById<TextView>(R.id.text_app_version).text.toString()
        )
    }

    @Test
    fun contactSupport_opensAnEmailComposerAddressedToSupport() = withFragment { f, v ->
        v.findViewById<View>(R.id.row_contact_support).performClick()

        val started: Intent = shadowOf(f.requireActivity()).nextStartedActivity
        assertNotNull(started)
        assertEquals(Intent.ACTION_SENDTO, started.action)
        assertEquals("mailto:", started.data.toString())
        assertArrayEquals(arrayOf(f.getString(R.string.support_email)), started.getStringArrayExtra(Intent.EXTRA_EMAIL))
        assertEquals(f.getString(R.string.help_email_subject), started.getStringExtra(Intent.EXTRA_SUBJECT))
    }

    @Test
    fun contactSupport_withoutAnEmailApp_toastsAnExplanation() = withFragment { f, v ->
        shadowOf(ApplicationProvider.getApplicationContext<Application>()).checkActivities(true)

        v.findViewById<View>(R.id.row_contact_support).performClick()

        assertEquals(f.getString(R.string.error_no_email_app), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun backButton_popsTheBackStack() = withFragment { _, v ->
        v.findViewById<View>(R.id.btn_back).performClick()
        verify { nav.popBackStack() }
    }
}
