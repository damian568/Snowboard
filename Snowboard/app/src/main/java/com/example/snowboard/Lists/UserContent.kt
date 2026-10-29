package com.example.snowboard.Lists

import android.os.Parcelable
import com.example.snowboard.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.parcelize.Parcelize

// What the FAB can add on each screen; `collection` is the Firestore collection it is stored in
enum class ContentType(
    val collection: String,
    val addTitle: Int,
    val editTitle: Int,
    val titleHint: Int,
    val titleIcon: Int
) {
    TIP("tips", R.string.add_tip, R.string.edit_tip, R.string.hint_tip_title, R.drawable.ic_tips),
    EQUIPMENT("equipment", R.string.add_equipment, R.string.edit_equipment, R.string.hint_equipment_name, R.drawable.ic_equipment),
    SKI_SLOPE("skiSlopes", R.string.add_ski_slope, R.string.edit_ski_slope, R.string.hint_resort_name, R.drawable.ic_slopes),
    VIDEO("videos", R.string.add_video, R.string.edit_video, R.string.hint_video_title, R.drawable.ic_videocam)
}

// A tip, equipment, ski slope or video added by a user. Everyone can see it, only the owner can edit or delete it.
@Parcelize
data class UserContent(
    val id: String,
    val type: ContentType,
    val ownerId: String,
    val title: String,
    val description: String,
    val quote: String = "",
    // Website for ski slopes, YouTube link for videos
    val link: String = "",
    // Compressed JPEG for equipment and ski slopes
    val image: ByteArray? = null,
    // Equipment only: which built-in category it was added under, see EquipmentList.category
    val category: String = ""
) : Parcelable {

    val isMine: Boolean
        get() = ownerId == FirebaseAuth.getInstance().currentUser?.uid

    companion object {
        private val youTubeIdPattern = Regex("^[A-Za-z0-9_-]{11}$")
        private val youTubeLinkPattern =
            Regex("(?:youtu\\.be/|[?&]v=|/shorts/|/embed/|/live/)([A-Za-z0-9_-]{11})")

        /** The video id from a YouTube link (or a bare id), or null if it isn't one. */
        fun youTubeId(link: String): String? {
            val trimmed = link.trim()
            if (youTubeIdPattern.matches(trimmed)) return trimmed
            if (!trimmed.contains("youtu")) return null
            return youTubeLinkPattern.find(trimmed)?.groupValues?.get(1)
        }
    }
}
