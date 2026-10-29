package com.example.snowboard.Content

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.bumptech.glide.Glide
import com.example.snowboard.Lists.ContentType
import com.example.snowboard.Lists.UserContent
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object UserContentRepository {

    private const val FIELD_OWNER_ID = "ownerId"
    private const val FIELD_CREATED_AT = "createdAt"
    private const val FIELD_TITLE = "title"
    private const val FIELD_DESCRIPTION = "description"
    private const val FIELD_QUOTE = "quote"
    private const val FIELD_LINK = "link"
    private const val FIELD_IMAGE = "image"
    private const val FIELD_CATEGORY = "category"

    // No Firebase Storage on the Spark plan, so photos live in the document like avatars do.
    // A 640px JPEG is roughly 40-80 KB, well under Firestore's 1 MB document limit.
    private const val IMAGE_SIZE_PX = 640
    private const val JPEG_QUALITY = 75

    /** Streams every user's content of [type] (only [category] when given), newest first. Remove the registration when done. */
    fun listen(
        type: ContentType,
        category: String?,
        onError: (Exception) -> Unit,
        onChange: (List<UserContent>) -> Unit
    ): ListenerRegistration {
        val query: Query = if (category == null) collection(type) else collection(type).whereEqualTo(FIELD_CATEGORY, category)
        return query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }
            // Sorted here rather than with orderBy so a just-added item (pending server timestamp) still lands on top
            val items = snapshot?.documents.orEmpty()
                .sortedByDescending { it.getTimestamp(FIELD_CREATED_AT, ServerTimestampBehavior.ESTIMATE) }
                .mapNotNull { toContent(type, it) }
            onChange(items)
        }
    }

    /**
     * Adds [draft] for the signed-in user when its id is empty, otherwise updates it.
     * [newImage] replaces the stored photo, [removeImage] clears it. Must be called from a coroutine.
     */
    suspend fun save(context: Context, draft: UserContent, newImage: Uri?, removeImage: Boolean) =
        withContext(Dispatchers.IO) {
            val fields = mutableMapOf<String, Any>(
                FIELD_TITLE to draft.title,
                FIELD_DESCRIPTION to draft.description,
                FIELD_QUOTE to draft.quote,
                FIELD_LINK to draft.link
            )
            if (newImage != null) {
                fields[FIELD_IMAGE] = Blob.fromBytes(compress(context.applicationContext, newImage))
            }

            if (draft.id.isEmpty()) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw IllegalStateException("Must be signed in to add content")
                fields[FIELD_OWNER_ID] = uid
                fields[FIELD_CREATED_AT] = FieldValue.serverTimestamp()
                if (draft.category.isNotEmpty()) fields[FIELD_CATEGORY] = draft.category
                Tasks.await(collection(draft.type).add(fields))
            } else {
                if (newImage == null && removeImage) {
                    fields[FIELD_IMAGE] = FieldValue.delete()
                }
                Tasks.await(collection(draft.type).document(draft.id).update(fields))
            }
        }

    suspend fun delete(content: UserContent) {
        withContext(Dispatchers.IO) {
            Tasks.await(collection(content.type).document(content.id).delete())
        }
    }

    private fun collection(type: ContentType) =
        FirebaseFirestore.getInstance().collection(type.collection)

    private fun toContent(type: ContentType, document: DocumentSnapshot): UserContent? {
        val ownerId = document.getString(FIELD_OWNER_ID) ?: return null
        return UserContent(
            id = document.id,
            type = type,
            ownerId = ownerId,
            title = document.getString(FIELD_TITLE).orEmpty(),
            description = document.getString(FIELD_DESCRIPTION).orEmpty(),
            quote = document.getString(FIELD_QUOTE).orEmpty(),
            link = document.getString(FIELD_LINK).orEmpty(),
            image = document.getBlob(FIELD_IMAGE)?.toBytes(),
            category = document.getString(FIELD_CATEGORY).orEmpty()
        )
    }

    private fun compress(context: Context, imageUri: Uri): ByteArray {
        // Glide decodes with EXIF rotation applied and scales the photo to fit within the box
        val bitmap = Glide.with(context)
            .asBitmap()
            .load(imageUri)
            .fitCenter()
            .submit(IMAGE_SIZE_PX, IMAGE_SIZE_PX)
            .get()

        return ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
            stream.toByteArray()
        }
    }
}
