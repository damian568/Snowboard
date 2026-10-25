package com.example.snowboard.User

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object AvatarRepository {

    private const val FIELD_AVATAR = "avatar"

    // Keeps the stored JPEG around 20-30 KB, well under Firestore's 1 MB document limit
    private const val AVATAR_SIZE_PX = 320
    private const val JPEG_QUALITY = 80

    /** Returns the saved JPEG bytes. Must be called from a coroutine; does its work off the main thread. */
    suspend fun saveAvatar(context: Context, uid: String, imageUri: Uri): ByteArray =
        withContext(Dispatchers.IO) {
            val bytes = compress(context.applicationContext, imageUri)
            Tasks.await(
                FirebaseFirestore.getInstance().collection("users").document(uid)
                    .set(mapOf(FIELD_AVATAR to Blob.fromBytes(bytes)), SetOptions.merge())
            )
            bytes
        }

    fun avatarBytes(document: DocumentSnapshot): ByteArray? =
        document.getBlob(FIELD_AVATAR)?.toBytes()

    private fun compress(context: Context, imageUri: Uri): ByteArray {
        // Glide decodes with EXIF rotation applied and center-crops to a square avatar
        val bitmap = Glide.with(context)
            .asBitmap()
            .load(imageUri)
            .centerCrop()
            .submit(AVATAR_SIZE_PX, AVATAR_SIZE_PX)
            .get()

        return ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
            stream.toByteArray()
        }
    }
}
