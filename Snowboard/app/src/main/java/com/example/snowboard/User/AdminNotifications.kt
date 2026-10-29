package com.example.snowboard.User

import android.os.Build
import com.example.snowboard.BuildConfig
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** Emails the app owner (the "Admin - account deletion" EmailJS template) about account actions. */
object AdminNotifications {

    /**
     * Who asked to delete their account, how they sign in and on what device. Sent when their
     * verification code goes out; never includes the code. Must be called from a coroutine.
     */
    suspend fun accountDeletionRequested(user: FirebaseUser, providerId: String) {
        withContext(Dispatchers.IO) {
            EmailJs.send(
                BuildConfig.EMAILJS_ADMIN_TEMPLATE_ID,
                mapOf(
                    "user_name" to fullName(user),
                    "user_email" to user.email.orEmpty().ifEmpty { "—" },
                    "user_id" to user.uid,
                    "provider" to providerName(providerId),
                    "device" to deviceName(),
                    "android_version" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                    "app_version" to "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    "time" to DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG, Locale.UK).format(Date())
                )
            )
        }
    }

    // The name from their profile, falling back to the one from Google / Facebook
    private fun fullName(user: FirebaseUser): String {
        val profileName = runCatching {
            Tasks.await(FirebaseFirestore.getInstance().collection("users").document(user.uid).get())
                .getString("fullName")
        }.getOrNull()
        return profileName?.takeIf { it.isNotBlank() } ?: user.displayName?.takeIf { it.isNotBlank() } ?: "—"
    }

    private fun providerName(providerId: String) = when (providerId) {
        EmailAuthProvider.PROVIDER_ID -> "Email & password"
        GoogleAuthProvider.PROVIDER_ID -> "Google"
        FacebookAuthProvider.PROVIDER_ID -> "Facebook"
        else -> providerId
    }

    // "Google Pixel 7", without repeating the maker when the model already starts with it
    private fun deviceName(): String {
        val maker = Build.MANUFACTURER.replaceFirstChar { it.titlecase(Locale.UK) }
        return if (Build.MODEL.startsWith(Build.MANUFACTURER, ignoreCase = true)) Build.MODEL else "$maker ${Build.MODEL}"
    }
}
