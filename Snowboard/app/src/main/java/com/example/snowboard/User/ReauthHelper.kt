package com.example.snowboard.User

import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.snowboard.R
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.GoogleAuthProvider

/**
 * Asks a Google or Facebook user to sign in again and hands back the fresh credential, for actions
 * Firebase only allows right after signing in (deleting the account). Password users don't need
 * this; their credential comes from EmailAuthProvider.
 * Create it as a property of the Fragment, so the Google launcher is registered before the Fragment starts.
 */
class ReauthHelper(
    private val fragment: Fragment,
    private val onCredential: (AuthCredential) -> Unit,
    private val onCancelled: () -> Unit,
    private val onError: (String?) -> Unit
) {
    private val facebookCallbackManager = CallbackManager.Factory.create()

    private val googleSignInLauncher =
        fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleGoogleSignInResult(result.data)
        }

    init {
        LoginManager.getInstance().registerCallback(
            facebookCallbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    onCredential(FacebookAuthProvider.getCredential(result.accessToken.token))
                }

                override fun onCancel() = onCancelled()

                override fun onError(error: FacebookException) = onError(error.message)
            }
        )
    }

    /** Forward from the hosting Fragment's onActivityResult (Facebook SDK). */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        facebookCallbackManager.onActivityResult(requestCode, resultCode, data)
    }

    fun requestGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(fragment.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(fragment.requireContext(), gso)
        // Signed out first so the account picker shows instead of silently reusing the last account
        client.signOut().addOnCompleteListener { googleSignInLauncher.launch(client.signInIntent) }
    }

    fun requestFacebook() {
        LoginManager.getInstance().logInWithReadPermissions(fragment, listOf("email", "public_profile"))
    }

    private fun handleGoogleSignInResult(data: Intent?) {
        try {
            val idToken = GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(ApiException::class.java).idToken
            if (idToken == null) {
                onError("Google sign-in failed: missing ID token")
            } else {
                onCredential(GoogleAuthProvider.getCredential(idToken, null))
            }
        } catch (e: ApiException) {
            // Backing out of the account picker reports SIGN_IN_CANCELLED, not CANCELED
            if (e.statusCode == CommonStatusCodes.CANCELED || e.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                onCancelled()
            } else {
                onError("Google sign-in failed (${e.statusCode})")
            }
        }
    }
}
