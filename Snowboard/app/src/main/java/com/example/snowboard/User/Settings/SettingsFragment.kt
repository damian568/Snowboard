package com.example.snowboard.User.Settings

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.snowboard.Content.DIALOG_WIDTH_RATIO
import com.example.snowboard.Content.HEADER_ART_TOP
import com.example.snowboard.Content.cropToBand
import com.example.snowboard.R
import com.example.snowboard.User.AccountRepository
import com.example.snowboard.User.AdminNotifications
import com.example.snowboard.User.EmailVerification
import com.example.snowboard.User.ReauthHelper
import com.example.snowboard.databinding.DialogDeleteAccountBinding
import com.example.snowboard.databinding.FragmentSettingsBinding
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutionException

class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding

    // The open "Delete account" dialog, while it is showing
    private var deleteDialog: Dialog? = null
    private var deleteBinding: DialogDeleteAccountBinding? = null
    private var deleteProvider = EmailAuthProvider.PROVIDER_ID
    private var emailVerification = EmailVerification()
    private var codeConfirmed = false
    private var adminNotified = false
    private var resendCountdown: Job? = null

    // Google / Facebook users sign in again before their account can be deleted
    private val reauthHelper = ReauthHelper(
        this,
        onCredential = { credential -> deleteAccount(credential) },
        onCancelled = { setDeleting(false) },
        onError = { message ->
            setDeleting(false)
            showToast(message ?: getString(R.string.msg_account_delete_failed))
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        selectUnits(AppPreferences.isMetricUnits(requireContext()))
        binding.unitsMetric.setOnClickListener { selectUnits(true) }
        binding.unitsImperial.setOnClickListener { selectUnits(false) }

        binding.switchNotifications.isChecked = AppPreferences.isNotificationsEnabled(requireContext())
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            AppPreferences.setNotificationsEnabled(requireContext(), isChecked)
        }

        binding.switchDarkMode.isChecked = AppPreferences.isDarkMode(requireContext())
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            AppPreferences.setDarkMode(requireContext(), isChecked)
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        binding.rowChangePassword.setOnClickListener {
            findNavController().navigate(
                SettingsFragmentDirections.actionSettingsFragmentToChangePasswordScreenFragment()
            )
        }

        binding.rowChangeEmail.setOnClickListener {
            findNavController().navigate(
                SettingsFragmentDirections.actionSettingsFragmentToChangeEmailScreenFragment()
            )
        }

        binding.rowDeleteAccount.setOnClickListener { confirmDeleteAccount() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        reauthHelper.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        deleteDialog?.dismiss()
    }

    /**
     * Two steps: a code emailed to the account (sent as soon as the dialog opens), then the
     * password, or signing in again with Google / Facebook. Only then is anything deleted.
     */
    private fun confirmDeleteAccount() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val email = user.email
        deleteProvider = signInProvider(user)
        emailVerification = EmailVerification()
        adminNotified = false
        // No address to send a code to (possible with Facebook): straight to signing in again
        codeConfirmed = email == null

        val dialogBinding = DialogDeleteAccountBinding.inflate(layoutInflater)
        if (deleteProvider == GoogleAuthProvider.PROVIDER_ID) {
            dialogBinding.txtReauthNote.setText(R.string.delete_account_reauth_google)
        } else if (deleteProvider == FacebookAuthProvider.PROVIDER_ID) {
            dialogBinding.txtReauthNote.setText(R.string.delete_account_reauth_facebook)
        }

        val dialog = Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(dialogBinding.root)
            setOnDismissListener {
                resendCountdown?.cancel()
                deleteDialog = null
                deleteBinding = null
            }
        }
        deleteDialog = dialog
        deleteBinding = dialogBinding

        dialogBinding.imgHeader.doOnLayout { (it as ImageView).cropToBand(HEADER_ART_TOP) }
        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnResendCode.setOnClickListener { email?.let { sendCode(it) } }
        dialogBinding.btnDelete.setOnClickListener {
            if (codeConfirmed) reauthenticateAndDelete(user) else verifyCode()
        }
        // An error goes away as soon as the user starts fixing it
        dialogBinding.editCode.doOnTextChanged { _, _, _, _ ->
            clearFieldError(dialogBinding.fieldCode, dialogBinding.txtCodeError)
        }
        dialogBinding.editPassword.doOnTextChanged { _, _, _, _ ->
            clearFieldError(dialogBinding.fieldPassword, dialogBinding.txtPasswordError)
        }
        showDeleteStep()

        dialog.show()
        // Centered card like the other delete dialog; the transparent window lets the rounded corners show
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout((resources.displayMetrics.widthPixels * DIALOG_WIDTH_RATIO).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        }

        if (email != null) sendCode(email)
    }

    // Step 1 shows the code field, step 2 the password field (or the Google / Facebook note)
    private fun showDeleteStep() {
        val dialogBinding = deleteBinding ?: return
        val usesPassword = deleteProvider == EmailAuthProvider.PROVIDER_ID
        dialogBinding.fieldCode.isVisible = !codeConfirmed
        dialogBinding.btnResendCode.isVisible = !codeConfirmed
        dialogBinding.fieldPassword.isVisible = codeConfirmed && usesPassword
        dialogBinding.txtReauthNote.isVisible = codeConfirmed && !usesPassword
        dialogBinding.btnDelete.setText(if (codeConfirmed) R.string.btn_delete_account else R.string.btn_verify)
        if (codeConfirmed) {
            dialogBinding.txtStepInfo.setText(
                if (usesPassword) R.string.delete_account_code_confirmed else R.string.delete_account_code_confirmed_social
            )
        }
    }

    private fun sendCode(email: String) {
        val dialogBinding = deleteBinding ?: return
        dialogBinding.txtStepInfo.text = getString(R.string.delete_account_code_sending, email)
        dialogBinding.btnDelete.isEnabled = false
        dialogBinding.btnResendCode.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                emailVerification.sendCode(email)
                deleteBinding?.txtStepInfo?.text = getString(R.string.delete_account_code_sent, email)
                startResendCountdown()
                notifyAdminOnce()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't send the verification code", e)
                deleteBinding?.txtStepInfo?.setText(R.string.msg_code_send_failed)
                deleteBinding?.btnResendCode?.isEnabled = true
            } finally {
                deleteBinding?.btnDelete?.isEnabled = true
            }
        }
    }

    // Emails the owner who is deleting their account, once per dialog (not on every resent code).
    // Only logged if it fails: it must never stop the user from deleting their account.
    private fun notifyAdminOnce() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        if (adminNotified) return
        adminNotified = true
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                AdminNotifications.accountDeletionRequested(user, deleteProvider)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't email the admin about the deletion request", e)
            }
        }
    }

    // Stops the code being resent over and over
    private fun startResendCountdown() {
        resendCountdown?.cancel()
        resendCountdown = viewLifecycleOwner.lifecycleScope.launch {
            for (seconds in RESEND_DELAY_SECONDS downTo 1) {
                deleteBinding?.btnResendCode?.apply {
                    isEnabled = false
                    text = getString(R.string.btn_resend_code_in, seconds)
                }
                delay(1000)
            }
            deleteBinding?.btnResendCode?.apply {
                isEnabled = true
                setText(R.string.btn_resend_code)
            }
        }
    }

    private fun verifyCode() {
        val dialogBinding = deleteBinding ?: return
        val error = when (emailVerification.check(dialogBinding.editCode.text.toString())) {
            EmailVerification.Result.CORRECT -> {
                codeConfirmed = true
                resendCountdown?.cancel()
                showDeleteStep()
                dialogBinding.editPassword.requestFocus()
                return
            }
            EmailVerification.Result.WRONG -> R.string.error_code_wrong
            EmailVerification.Result.EXPIRED -> R.string.error_code_expired
        }
        showFieldError(dialogBinding.fieldCode, dialogBinding.txtCodeError, error)
        dialogBinding.editCode.requestFocus()
    }

    // Red border plus a red message under the field, in place of the system's setError() popup
    private fun showFieldError(field: View, errorText: TextView, messageId: Int) {
        field.setBackgroundResource(R.drawable.bg_login_input_error)
        errorText.setText(messageId)
        errorText.isVisible = true
    }

    private fun clearFieldError(field: View, errorText: TextView) {
        field.setBackgroundResource(R.drawable.bg_login_input)
        errorText.isVisible = false
    }

    private fun reauthenticateAndDelete(user: FirebaseUser) {
        when (deleteProvider) {
            GoogleAuthProvider.PROVIDER_ID -> {
                setDeleting(true)
                reauthHelper.requestGoogle()
            }
            FacebookAuthProvider.PROVIDER_ID -> {
                setDeleting(true)
                reauthHelper.requestFacebook()
            }
            else -> {
                val dialogBinding = deleteBinding ?: return
                val password = dialogBinding.editPassword.text.toString()
                val email = user.email
                if (password.isEmpty() || email == null) {
                    showFieldError(dialogBinding.fieldPassword, dialogBinding.txtPasswordError, R.string.error_current_password_required)
                } else {
                    deleteAccount(EmailAuthProvider.getCredential(email, password))
                }
            }
        }
    }

    // How the user signs in, so we know how to re-authenticate them
    private fun signInProvider(user: FirebaseUser): String {
        val providers = user.providerData.map { it.providerId }
        return listOf(EmailAuthProvider.PROVIDER_ID, GoogleAuthProvider.PROVIDER_ID, FacebookAuthProvider.PROVIDER_ID)
            .firstOrNull { it in providers } ?: EmailAuthProvider.PROVIDER_ID
    }

    private fun deleteAccount(credential: AuthCredential) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        setDeleting(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                AccountRepository.deleteAccount(user, credential)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't delete the account", e)
                setDeleting(false)
                when ((e as? ExecutionException)?.cause ?: e) {
                    is FirebaseAuthInvalidCredentialsException -> deleteBinding?.let {
                        showFieldError(it.fieldPassword, it.txtPasswordError, R.string.error_current_password_incorrect)
                        it.editPassword.requestFocus()
                    }
                    // Picked a different Google / Facebook account than the one being deleted
                    is FirebaseAuthInvalidUserException -> showToast(getString(R.string.error_reauth_wrong_account))
                    else -> showToast(getString(R.string.msg_account_delete_failed))
                }
                return@launch
            }

            deleteDialog?.dismiss()
            signOutOfProviders()
            showToast(getString(R.string.msg_account_deleted))
            // Back to Login with nothing behind it, like logging out
            findNavController().navigate(
                R.id.loginScreenFragment,
                null,
                NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
            )
        }
    }

    // So the next Google / Facebook sign-in asks which account to use instead of reusing the deleted one
    private fun signOutOfProviders() {
        FirebaseAuth.getInstance().signOut()
        GoogleSignIn.getClient(requireContext(), GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
        LoginManager.getInstance().logOut()
    }

    private fun setDeleting(deleting: Boolean) {
        val dialogBinding = deleteBinding ?: return
        deleteDialog?.setCancelable(!deleting)
        dialogBinding.btnDelete.isEnabled = !deleting
        dialogBinding.btnCancel.isEnabled = !deleting
        dialogBinding.editPassword.isEnabled = !deleting
        dialogBinding.btnDelete.setText(if (deleting) R.string.btn_deleting else R.string.btn_delete_account)    }

    private fun showToast(message: String) {
        context?.let { Toast.makeText(it, message, Toast.LENGTH_LONG).show() }
    }

    private fun selectUnits(metric: Boolean) {
        AppPreferences.setMetricUnits(requireContext(), metric)

        binding.unitsMetric.setBackgroundResource(
            if (metric) R.drawable.bg_level_selected_left else android.R.color.transparent
        )
        binding.unitsImperial.setBackgroundResource(
            if (metric) android.R.color.transparent else R.drawable.bg_level_selected_right
        )

        val activeColor = ContextCompat.getColor(requireContext(), R.color.white)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.black)

        binding.textUnitsMetric.setTextColor(if (metric) activeColor else inactiveColor)
        binding.textUnitsImperial.setTextColor(if (metric) inactiveColor else activeColor)
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    override fun onStop() {
        super.onStop()
        (activity as? AppCompatActivity)?.supportActionBar?.show()
    }

    companion object {
        private const val TAG = "SettingsFragment"
        private const val RESEND_DELAY_SECONDS = 60
    }
}
