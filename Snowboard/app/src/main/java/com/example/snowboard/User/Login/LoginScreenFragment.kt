package com.example.snowboard.User.Login

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.snowboard.R
import com.example.snowboard.User.SocialAuthHelper
import com.example.snowboard.databinding.FragmentLoginScreenBinding
import com.google.firebase.auth.FirebaseAuth

class LoginScreenFragment : Fragment() {

    private lateinit var binding: FragmentLoginScreenBinding
    private val auth = FirebaseAuth.getInstance()
    private var passwordVisible = false

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
        binding = FragmentLoginScreenBinding.inflate(inflater, container, false)
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

        binding.signUp.setOnClickListener { goToRegisterScreen() }

        binding.forgotPassword.setOnClickListener { goToForgotPasswordScreen() }

        binding.btnLogin.setOnClickListener { attemptLogin() }

        binding.btnGoogle.setOnClickListener { socialAuthHelper.signInWithGoogle() }

        binding.btnFacebook.setOnClickListener { socialAuthHelper.signInWithFacebook() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        socialAuthHelper.onActivityResult(requestCode, resultCode, data)
    }

    private fun attemptLogin() {
        val email = binding.editEmail.text.toString().trim()
        val password = binding.editPassword.text.toString()

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

        setLoading(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    setLoading(false)
                    return@addOnSuccessListener
                }
                user.reload().addOnCompleteListener {
                    setLoading(false)
                    if (user.isEmailVerified) {
                        goToMainScreen()
                    } else {
                        auth.signOut()
                        if (isAdded) {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.error_email_not_verified),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                showError(e.message)
            }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnLogin.isEnabled = !loading
        binding.btnLogin.text = if (loading) "" else getString(R.string.btn_login)
    }

    private fun showError(message: String?) {
        if (isAdded) {
            Toast.makeText(requireContext(), message ?: return, Toast.LENGTH_LONG).show()
        }
    }

    private fun goToMainScreen() {
        if (isAdded) {
            val action =
                LoginScreenFragmentDirections.actionLoginScreenFragmentToMainScreenFragment()
            findNavController().navigate(action)
        }
    }

    private fun goToRegisterScreen(){
        val action = LoginScreenFragmentDirections.actionLoginScreenFragmentToRegisterScreenFragment()
        findNavController().navigate(action)
    }

    private fun goToForgotPasswordScreen() {
        val action =
            LoginScreenFragmentDirections.actionLoginScreenFragmentToForgotPasswordScreenFragment()
        findNavController().navigate(action)
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }
}
