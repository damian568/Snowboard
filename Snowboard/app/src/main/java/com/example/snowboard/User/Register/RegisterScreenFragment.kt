package com.example.snowboard.User.Register

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.snowboard.R
import com.example.snowboard.User.Model.UserProfile
import com.example.snowboard.User.PasswordRequirements
import com.example.snowboard.User.PasswordValidator
import com.example.snowboard.User.SocialAuthHelper
import com.example.snowboard.databinding.FragmentRegisterScreenBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore

class RegisterScreenFragment : Fragment() {

    private enum class SkillLevel { BEGINNER, ADVANCE }

    private lateinit var binding: FragmentRegisterScreenBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var passwordVisible = false
    private var confirmPasswordVisible = false
    private var selectedLevel = SkillLevel.BEGINNER

    private val googleSignInLauncher: androidx.activity.result.ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            socialAuthHelper.handleGoogleSignInResult(result.data)
        }

    private val socialAuthHelper: SocialAuthHelper by lazy {
        SocialAuthHelper(
            fragment = this,
            googleSignInLauncher = googleSignInLauncher,
            onLoading = { setLoading(it) },
            onError = { showError(it) },
            onSuccess = { goToMainScreen() }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegisterScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.togglePassword.setOnClickListener {
            passwordVisible = !passwordVisible
            val cursorPosition = binding.editPassword.selectionStart
            binding.editPassword.inputType = if (passwordVisible) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            binding.togglePassword.setImageResource(
                if (passwordVisible) R.drawable.ic_eye else R.drawable.ic_eye_off
            )
            binding.editPassword.setSelection(cursorPosition)
        }

        binding.toggleConfirmPassword.setOnClickListener {
            confirmPasswordVisible = !confirmPasswordVisible
            val cursorPosition = binding.editConfirmPassword.selectionStart
            binding.editConfirmPassword.inputType = if (confirmPasswordVisible) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            binding.toggleConfirmPassword.setImageResource(
                if (confirmPasswordVisible) R.drawable.ic_eye else R.drawable.ic_eye_off
            )
            binding.editConfirmPassword.setSelection(cursorPosition)
        }

        PasswordRequirements.attach(
            binding.editPassword, binding.passwordRequirements, binding.passwordStrongIcon
        )
        PasswordRequirements.attachMatch(
            binding.editPassword, binding.editConfirmPassword, binding.passwordMatchIcon
        )

        binding.levelBeginner.setOnClickListener { selectLevel(SkillLevel.BEGINNER) }
        binding.levelAdvance.setOnClickListener { selectLevel(SkillLevel.ADVANCE) }

        binding.logIn.setOnClickListener { goToLoginScreen() }

        binding.btnRegister.setOnClickListener { attemptRegister() }

        binding.btnGoogle.setOnClickListener { socialAuthHelper.signInWithGoogle() }

        binding.btnFacebook.setOnClickListener { socialAuthHelper.signInWithFacebook() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        socialAuthHelper.onActivityResult(requestCode, resultCode, data)
    }

    private fun attemptRegister() {
        val fullName = binding.editFullName.text.toString().trim()
        val email = binding.editEmail.text.toString().trim()
        val password = binding.editPassword.text.toString()
        val confirmPassword = binding.editConfirmPassword.text.toString()

        if (fullName.isEmpty()) {
            binding.editFullName.error = getString(R.string.error_full_name_required)
            return
        }
        if (email.isEmpty()) {
            binding.editEmail.error = getString(R.string.error_email_required)
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editEmail.error = getString(R.string.error_email_invalid)
            return
        }
        if (password.isEmpty()) {
            binding.editPassword.error = getString(R.string.error_password_required)
            return
        }
        PasswordValidator.validate(password)?.let { errorRes ->
            binding.editPassword.error = getString(errorRes)
            return
        }
        if (password != confirmPassword) {
            binding.editConfirmPassword.error = getString(R.string.error_password_mismatch)
            return
        }

        setLoading(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                val uid = user?.uid
                if (uid == null) {
                    setLoading(false)
                    return@addOnSuccessListener
                }
                val profile = UserProfile(
                    fullName = fullName,
                    email = email,
                    level = selectedLevel.name
                )
                firestore.collection("users").document(uid).set(profile)
                    .addOnSuccessListener {
                        user.sendEmailVerification()
                            .addOnCompleteListener {
                                auth.signOut()
                                setLoading(false)
                                if (isAdded) {
                                    Toast.makeText(
                                        requireContext(),
                                        getString(R.string.msg_verification_email_sent, email),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                goToLoginScreen()
                            }
                    }
                    .addOnFailureListener { e ->
                        setLoading(false)
                        showError(e.message)
                    }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                if (e is FirebaseAuthUserCollisionException) {
                    binding.editEmail.error = getString(R.string.error_email_already_in_use)
                } else {
                    showError(e.message)
                }
            }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnRegister.isEnabled = !loading
        binding.btnRegister.text = if (loading) "" else getString(R.string.btn_register)
    }

    private fun showError(message: String?) {
        if (isAdded) {
            Toast.makeText(requireContext(), message ?: return, Toast.LENGTH_LONG).show()
        }
    }

    private fun selectLevel(level: SkillLevel) {
        selectedLevel = level
        val beginnerSelected = level == SkillLevel.BEGINNER

        binding.levelBeginner.setBackgroundResource(
            if (beginnerSelected) R.drawable.bg_level_selected_left else android.R.color.transparent
        )
        binding.levelAdvance.setBackgroundResource(
            if (beginnerSelected) android.R.color.transparent else R.drawable.bg_level_selected_right
        )

        val activeColor = ContextCompat.getColor(requireContext(), R.color.white)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.black)

        binding.iconBeginner.imageTintList =
            ColorStateList.valueOf(if (beginnerSelected) activeColor else inactiveColor)
        binding.textBeginner.setTextColor(if (beginnerSelected) activeColor else inactiveColor)

        binding.iconAdvance.imageTintList =
            ColorStateList.valueOf(if (beginnerSelected) inactiveColor else activeColor)
        binding.textAdvance.setTextColor(if (beginnerSelected) inactiveColor else activeColor)
    }

    private fun goToLoginScreen() {
        val action = RegisterScreenFragmentDirections.actionRegisterScreenFragmentToLoginScreenFragment()
        findNavController().navigate(action)
    }

    private fun goToMainScreen() {
        if (isAdded) {
            val action =
                RegisterScreenFragmentDirections.actionRegisterScreenFragmentToMainScreenFragment()
            findNavController().navigate(action)
        }
    }
}
