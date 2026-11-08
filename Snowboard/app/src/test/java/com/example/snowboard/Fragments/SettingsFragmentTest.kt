package com.example.snowboard.Fragments

import android.app.Dialog
import android.content.Context
import android.os.Looper
import android.view.View
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.test.core.app.ApplicationProvider
import com.example.snowboard.R
import com.example.snowboard.User.AccountRepository
import com.example.snowboard.User.AdminNotifications
import com.example.snowboard.User.EmailJs
import com.example.snowboard.User.Settings.AppPreferences
import com.example.snowboard.User.Settings.SettingsFragment
import com.example.snowboard.User.Settings.SettingsFragmentDirections
import com.example.snowboard.launchWithNav
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowToast
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class SettingsFragmentTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val auth = mockk<FirebaseAuth>(relaxed = true)
    private val nav = mockk<NavController>(relaxed = true)
    private val loginManager = mockk<LoginManager>(relaxed = true)
    private val emailParams = slot<Map<String, String>>()

    @Before
    fun setUp() {
        mockkStatic(FirebaseAuth::class, GoogleSignIn::class)
        mockkObject(LoginManager.Companion, EmailJs, AdminNotifications, AccountRepository)
        every { FirebaseAuth.getInstance() } returns auth
        every { LoginManager.getInstance() } returns loginManager
        every { EmailJs.send(any(), capture(emailParams)) } just runs
        coEvery { AdminNotifications.accountDeletionRequested(any(), any()) } returns Unit
        coEvery { AccountRepository.deleteAccount(any(), any()) } returns Unit
        every { auth.currentUser } returns user()
    }

    @After
    fun tearDown() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        unmockkAll()
    }

    private fun user(email: String? = "rider@example.com", provider: String = "password") = mockk<FirebaseUser> {
        every { this@mockk.email } returns email
        every { uid } returns "uid-1"
        every { providerData } returns listOf(mockk<UserInfo> { every { providerId } returns provider })
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    /** Real work (the code email) happens on a background thread, so wait for it to land on the main thread. */
    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition() && System.currentTimeMillis() < deadline) {
            idle()
            Thread.sleep(10)
        }
        assertTrue("condition was never met", condition())
    }

    private fun withFragment(block: (SettingsFragment, View) -> Unit) {
        launchWithNav<SettingsFragment>(nav).onFragment { block(it, it.requireView()) }
    }

    private fun dialog(): Dialog = ShadowDialog.getLatestDialog()
    private fun Dialog.view(id: Int) = findViewById<View>(id)
    private fun Dialog.text(id: Int) = findViewById<TextView>(id).text.toString()

    private fun openDeleteDialog(v: View): Dialog {
        v.findViewById<View>(R.id.row_delete_account).performClick()
        return dialog()
    }

    // ---- preferences ----

    @Test
    fun units_defaultToMetric_andCanBeSwitched() = withFragment { _, v ->
        assertTrue(AppPreferences.isMetricUnits(context))

        v.findViewById<View>(R.id.units_imperial).performClick()
        assertFalse(AppPreferences.isMetricUnits(context))

        v.findViewById<View>(R.id.units_metric).performClick()
        assertTrue(AppPreferences.isMetricUnits(context))
    }

    @Test
    fun units_showTheSavedChoice() {
        AppPreferences.setMetricUnits(context, false)
        withFragment { _, _ -> assertFalse(AppPreferences.isMetricUnits(context)) }
    }

    @Test
    fun notificationsSwitch_reflectsAndSavesTheSetting() = withFragment { _, v ->
        val toggle = v.findViewById<android.widget.CompoundButton>(R.id.switch_notifications)
        assertTrue(toggle.isChecked)

        toggle.isChecked = false

        assertFalse(AppPreferences.isNotificationsEnabled(context))
    }

    @Test
    fun darkModeSwitch_savesTheSetting_andSwitchesTheAppTheme() = withFragment { _, v ->
        val toggle = v.findViewById<android.widget.CompoundButton>(R.id.switch_dark_mode)
        assertFalse(toggle.isChecked)

        toggle.isChecked = true
        assertTrue(AppPreferences.isDarkMode(context))
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode())

        toggle.isChecked = false
        assertFalse(AppPreferences.isDarkMode(context))
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.getDefaultNightMode())
    }

    @Test
    fun savedDarkMode_isShownAsOn() {
        AppPreferences.setDarkMode(context, true)
        withFragment { _, v ->
            assertTrue(v.findViewById<android.widget.CompoundButton>(R.id.switch_dark_mode).isChecked)
        }
    }

    // ---- navigation ----

    @Test
    fun rows_openChangePasswordAndChangeEmail() = withFragment { _, v ->
        v.findViewById<View>(R.id.row_change_password).performClick()
        verify { nav.navigate(SettingsFragmentDirections.actionSettingsFragmentToChangePasswordScreenFragment()) }

        v.findViewById<View>(R.id.row_change_email).performClick()
        verify { nav.navigate(SettingsFragmentDirections.actionSettingsFragmentToChangeEmailScreenFragment()) }
    }

    @Test
    fun backButton_popsTheBackStack() = withFragment { _, v ->
        v.findViewById<View>(R.id.btn_back).performClick()
        verify { nav.popBackStack() }
    }

    // ---- delete account: step 1, the emailed code ----

    @Test
    fun deleteAccount_whenSignedOut_doesNothing() = withFragment { _, v ->
        every { auth.currentUser } returns null
        v.findViewById<View>(R.id.row_delete_account).performClick()
        assertNull(ShadowDialog.getLatestDialog())
    }

    @Test
    fun deleteAccount_opensTheDialogOnTheCodeStep_andEmailsTheCode() = withFragment { f, v ->
        val d = openDeleteDialog(v)

        assertTrue(d.isShowing)
        assertEquals(View.VISIBLE, d.view(R.id.field_code).visibility)
        assertEquals(View.VISIBLE, d.view(R.id.btn_resend_code).visibility)
        assertEquals(View.GONE, d.view(R.id.field_password).visibility)
        assertEquals(f.getString(R.string.btn_verify), d.text(R.id.btn_delete))

        waitUntil { d.text(R.id.txt_step_info) == f.getString(R.string.delete_account_code_sent, "rider@example.com") }
        assertEquals("rider@example.com", emailParams.captured["email"])
        assertTrue(emailParams.captured.getValue("passcode").matches(Regex("[0-9]{6}")))
        coVerify(exactly = 1) { AdminNotifications.accountDeletionRequested(any(), "password") }
    }

    @Test
    fun afterTheCodeIsSent_resendIsLockedBehindACountdown() = withFragment { f, v ->
        val d = openDeleteDialog(v)
        waitUntil { d.text(R.id.txt_step_info) == f.getString(R.string.delete_account_code_sent, "rider@example.com") }

        val resend = d.view(R.id.btn_resend_code) as TextView
        assertFalse(resend.isEnabled)
        assertEquals(f.getString(R.string.btn_resend_code_in, 60), resend.text.toString())

        shadowOf(Looper.getMainLooper()).idleFor(61, TimeUnit.SECONDS)
        assertTrue(resend.isEnabled)
        assertEquals(f.getString(R.string.btn_resend_code), resend.text.toString())
    }

    @Test
    fun emailingTheCodeFails_showsTheErrorAndAllowsAResend() = withFragment { f, v ->
        every { EmailJs.send(any(), any()) } throws java.io.IOException("offline")
        val d = openDeleteDialog(v)

        waitUntil { d.text(R.id.txt_step_info) == f.getString(R.string.msg_code_send_failed) }

        assertTrue(d.view(R.id.btn_resend_code).isEnabled)
        assertTrue(d.view(R.id.btn_delete).isEnabled)
    }

    @Test
    fun adminIsNotToldAgainWhenTheCodeIsResent() = withFragment { f, v ->
        val d = openDeleteDialog(v)
        waitUntil { d.text(R.id.txt_step_info) == f.getString(R.string.delete_account_code_sent, "rider@example.com") }
        shadowOf(Looper.getMainLooper()).idleFor(61, TimeUnit.SECONDS)

        d.view(R.id.btn_resend_code).performClick()
        waitUntil { d.text(R.id.txt_step_info) == f.getString(R.string.delete_account_code_sent, "rider@example.com") }

        verify(exactly = 2) { EmailJs.send(any(), any()) }
        coVerify(exactly = 1) { AdminNotifications.accountDeletionRequested(any(), any()) }
    }

    @Test
    fun wrongCode_isShownAsAFieldError_andClearedWhenTyping() = withFragment { f, v ->
        val d = openDeleteDialog(v)
        waitUntil { d.text(R.id.txt_step_info) == f.getString(R.string.delete_account_code_sent, "rider@example.com") }
        val typed = if (emailParams.captured.getValue("passcode") == "111111") "222222" else "111111"
        d.findViewById<android.widget.EditText>(R.id.edit_code).setText(typed)

        d.view(R.id.btn_delete).performClick()

        assertEquals(View.VISIBLE, d.view(R.id.txt_code_error).visibility)
        assertEquals(f.getString(R.string.error_code_wrong), d.text(R.id.txt_code_error))

        d.findViewById<android.widget.EditText>(R.id.edit_code).setText("1")
        assertEquals(View.GONE, d.view(R.id.txt_code_error).visibility)
    }

    @Test
    fun cancel_closesTheDialog() = withFragment { _, v ->
        val d = openDeleteDialog(v)

        d.view(R.id.btn_cancel).performClick()

        assertFalse(d.isShowing)
    }

    // ---- delete account: step 2, confirm and delete ----

    /** Opens the dialog and enters the correct code, leaving it on the password step. */
    private fun reachPasswordStep(f: SettingsFragment, v: View): Dialog {
        val d = openDeleteDialog(v)
        waitUntil { d.text(R.id.txt_step_info) == f.getString(R.string.delete_account_code_sent, "rider@example.com") }
        d.findViewById<android.widget.EditText>(R.id.edit_code).setText(emailParams.captured.getValue("passcode"))
        d.view(R.id.btn_delete).performClick()
        return d
    }

    @Test
    fun correctCode_movesToThePasswordStep() = withFragment { f, v ->
        val d = reachPasswordStep(f, v)

        assertEquals(View.GONE, d.view(R.id.field_code).visibility)
        assertEquals(View.VISIBLE, d.view(R.id.field_password).visibility)
        assertEquals(f.getString(R.string.btn_delete_account), d.text(R.id.btn_delete))
        assertEquals(f.getString(R.string.delete_account_code_confirmed), d.text(R.id.txt_step_info))
    }

    @Test
    fun emptyPassword_isRejected_andNothingIsDeleted() = withFragment { f, v ->
        val d = reachPasswordStep(f, v)

        d.view(R.id.btn_delete).performClick()

        assertEquals(f.getString(R.string.error_current_password_required), d.text(R.id.txt_password_error))
        coVerify(exactly = 0) { AccountRepository.deleteAccount(any(), any()) }
    }

    @Test
    fun correctPassword_deletesTheAccount_signsOut_andReturnsToLogin() = withFragment { f, v ->
        val d = reachPasswordStep(f, v)
        d.findViewById<android.widget.EditText>(R.id.edit_password).setText("Secret1!")

        d.view(R.id.btn_delete).performClick()
        waitUntil { !d.isShowing }
        idle()

        coVerify(exactly = 1) { AccountRepository.deleteAccount(any(), any()) }
        verify { auth.signOut() }
        assertEquals(f.getString(R.string.msg_account_deleted), ShadowToast.getTextOfLatestToast())
        verify { nav.navigate(R.id.loginScreenFragment, null, any<NavOptions>()) }
    }

    @Test
    fun wrongPassword_isShownOnThePasswordField_andTheDialogStays() = withFragment { f, v ->
        coEvery { AccountRepository.deleteAccount(any(), any()) } throws
            FirebaseAuthInvalidCredentialsException("ERROR_WRONG_PASSWORD", "bad")
        val d = reachPasswordStep(f, v)
        d.findViewById<android.widget.EditText>(R.id.edit_password).setText("wrong")

        d.view(R.id.btn_delete).performClick()
        waitUntil { d.view(R.id.txt_password_error).visibility == View.VISIBLE }

        assertEquals(f.getString(R.string.error_current_password_incorrect), d.text(R.id.txt_password_error))
        assertTrue(d.isShowing)
        assertTrue(d.view(R.id.btn_delete).isEnabled)
    }

    @Test
    fun anotherSocialAccount_isExplainedWithAToast() = withFragment { f, v ->
        coEvery { AccountRepository.deleteAccount(any(), any()) } throws
            FirebaseAuthInvalidUserException("ERROR_USER_MISMATCH", "other account")
        val d = reachPasswordStep(f, v)
        d.findViewById<android.widget.EditText>(R.id.edit_password).setText("Secret1!")

        d.view(R.id.btn_delete).performClick()
        waitUntil { ShadowToast.getTextOfLatestToast() == f.getString(R.string.error_reauth_wrong_account) }
    }

    @Test
    fun anyOtherFailure_toastsAGenericMessage_andKeepsTheAccount() = withFragment { f, v ->
        coEvery { AccountRepository.deleteAccount(any(), any()) } throws RuntimeException("boom")
        val d = reachPasswordStep(f, v)
        d.findViewById<android.widget.EditText>(R.id.edit_password).setText("Secret1!")

        d.view(R.id.btn_delete).performClick()
        waitUntil { ShadowToast.getTextOfLatestToast() == f.getString(R.string.msg_account_delete_failed) }

        verify(exactly = 0) { auth.signOut() }
        assertTrue(d.isShowing)
    }

    // ---- delete account: Google / Facebook / no email ----

    @Test
    fun accountWithoutAnEmail_skipsStraightToReauthentication() {
        every { auth.currentUser } returns user(email = null, provider = "facebook.com")
        withFragment { f, v ->
            val d = openDeleteDialog(v)

            assertEquals(View.GONE, d.view(R.id.field_code).visibility)
            assertEquals(f.getString(R.string.btn_delete_account), d.text(R.id.btn_delete))
            assertEquals(f.getString(R.string.delete_account_reauth_facebook), d.text(R.id.txt_reauth_note))
            verify(exactly = 0) { EmailJs.send(any(), any()) }
        }
    }

    @Test
    fun facebookUser_confirmingAsksFacebookToSignInAgain() {
        every { auth.currentUser } returns user(email = null, provider = "facebook.com")
        withFragment { f, v ->
            val d = openDeleteDialog(v)

            d.view(R.id.btn_delete).performClick()

            verify { loginManager.logInWithReadPermissions(f, listOf("email", "public_profile")) }
            assertFalse(d.view(R.id.btn_delete).isEnabled)
            assertEquals(f.getString(R.string.btn_deleting), d.text(R.id.btn_delete))
        }
    }

    @Test
    fun googleUser_confirmingAsksGoogleToSignInAgain() {
        every { auth.currentUser } returns user(email = null, provider = "google.com")
        val client = mockk<GoogleSignInClient> {
            every { signOut() } returns TaskCompletionSource<Void>().task
        }
        every { GoogleSignIn.getClient(any<Context>(), any()) } returns client
        withFragment { f, v ->
            val d = openDeleteDialog(v)
            assertEquals(f.getString(R.string.delete_account_reauth_google), d.text(R.id.txt_reauth_note))

            d.view(R.id.btn_delete).performClick()

            verify { client.signOut() }
            assertFalse(d.view(R.id.btn_cancel).isEnabled)
            assertNotNull(d)
        }
    }
}
