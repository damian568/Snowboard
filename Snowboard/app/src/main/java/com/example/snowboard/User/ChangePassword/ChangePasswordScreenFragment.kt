package com.example.snowboard.User.ChangePassword

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.snowboard.R
import com.example.snowboard.User.PasswordRequirements
import com.example.snowboard.User.PasswordValidator
import com.example.snowboard.databinding.FragmentChangePasswordScreenBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException

class ChangePasswordScreenFragment : Fragment() {

    private lateinit var binding: FragmentChangePasswordScreenBinding
    private val auth = FirebaseAuth.getInstance()
    private var currentPasswordVisible = false
    private var newPasswordVisible = false
    private var confirmNewPasswordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChangePasswordScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        binding.toggleCurrentPassword.setOnClickListener {
            currentPasswordVisible = togglePasswordVisibility(
                binding.editCurrentPassword, binding.toggleCurrentPassword, currentPasswordVisible
            )
        }
        binding.toggleNewPassword.setOnClickListener {
            newPasswordVisible = togglePasswordVisibility(
                binding.editNewPassword, binding.toggleNewPassword, newPasswordVisible
            )
        }
        binding.toggleConfirmNewPassword.setOnClickListener {
            confirmNewPasswordVisible = togglePasswordVisibility(
                binding.editConfirmNewPassword, binding.toggleConfirmNewPassword, confirmNewPasswordVisible
            )
        }

        PasswordRequirements.attach(
            binding.editNewPassword, binding.passwordRequirements, binding.passwordStrongIcon
        )
        PasswordRequirements.attachMatch(
            binding.editNewPassword, binding.editConfirmNewPassword, binding.passwordMatchIcon
        )

        binding.btnChangePassword.setOnClickListener { attemptChangePassword() }
    }

    private fun togglePasswordVisibility(editText: EditText, toggle: ImageView, visible: Boolean): Boolean {
        val newVisible = !visible
        val cursorPosition = editText.selectionStart
        editText.inputType = if (newVisible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        toggle.setImageResource(if (newVisible) R.drawable.ic_eye else R.drawable.ic_eye_off)
        editText.setSelection(cursorPosition)
        return newVisible
    }

    private fun attemptChangePassword() {
        val user = auth.currentUser ?: return
        val email = user.email ?: return
        val currentPassword = binding.editCurrentPassword.text.toString()
        val newPassword = binding.editNewPassword.text.toString()
        val confirmNewPassword = binding.editConfirmNewPassword.text.toString()

        if (currentPassword.isEmpty()) {
            binding.editCurrentPassword.error = getString(R.string.error_current_password_required)
            return
        }
        if (newPassword.isEmpty()) {
            binding.editNewPassword.error = getString(R.string.error_new_password_required)
            return
        }
        PasswordValidator.validate(newPassword)?.let { errorRes ->
            binding.editNewPassword.error = getString(errorRes)
            return
        }
        if (newPassword == currentPassword) {
            binding.editNewPassword.error = getString(R.string.error_new_password_same_as_old)
            return
        }
        if (newPassword != confirmNewPassword) {
            binding.editConfirmNewPassword.error = getString(R.string.error_password_mismatch)
            return
        }

        setLoading(true)
        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        setLoading(false)
                        if (isAdded) {
                            Toast.makeText(requireContext(), getString(R.string.msg_password_changed), Toast.LENGTH_LONG).show()
                            findNavController().popBackStack()
                        }
                    }
                    .addOnFailureListener { e ->
                        setLoading(false)
                        showError(e.message)
                    }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                if (e is FirebaseAuthInvalidCredentialsException) {
                    binding.editCurrentPassword.error = getString(R.string.error_current_password_incorrect)
                } else {
                    showError(e.message)
                }
            }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnChangePassword.isEnabled = !loading
        binding.btnChangePassword.text = if (loading) "" else getString(R.string.btn_change_password)
    }

    private fun showError(message: String?) {
        if (isAdded) {
            Toast.makeText(requireContext(), message ?: return, Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    override fun onStop() {
        super.onStop()
        (activity as? AppCompatActivity)?.supportActionBar?.show()
    }
}
