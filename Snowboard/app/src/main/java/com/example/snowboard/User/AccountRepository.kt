package com.example.snowboard.User

import com.example.snowboard.Content.UserContentRepository
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AccountRepository {

    /**
     * Re-authenticates [user] with [credential], then deletes everything they added, their profile
     * (users/{uid}, which also holds the avatar) and finally the Firebase Auth account.
     * The Auth account goes last: once it is gone the security rules no longer let us delete the rest.
     * Must be called from a coroutine.
     */
    suspend fun deleteAccount(user: FirebaseUser, credential: AuthCredential) {
        withContext(Dispatchers.IO) {
            Tasks.await(user.reauthenticate(credential))
            UserContentRepository.deleteAllOwnedBy(user.uid)
            Tasks.await(FirebaseFirestore.getInstance().collection("users").document(user.uid).delete())
            Tasks.await(user.delete())
        }
    }
}
