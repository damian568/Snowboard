package com.example.snowboard.User

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.bumptech.glide.Glide
import com.example.snowboard.fakeGlideBitmapLoading
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AvatarRepositoryTest {

    private val userDoc = mockk<DocumentReference>()
    private val data = slot<Map<String, Any>>()
    private val options = slot<SetOptions>()

    @Before
    fun setUp() {
        val users = mockk<CollectionReference>()
        val firestore = mockk<FirebaseFirestore>()
        mockkStatic(FirebaseFirestore::class)
        every { FirebaseFirestore.getInstance() } returns firestore
        every { firestore.collection("users") } returns users
        every { users.document("uid-1") } returns userDoc
        every { userDoc.set(capture(data), capture(options)) } returns Tasks.forResult<Void>(null)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun avatarBytes_returnsTheStoredBlob() {
        val document = mockk<DocumentSnapshot> {
            every { getBlob("avatar") } returns Blob.fromBytes(byteArrayOf(1, 2, 3))
        }
        assertArrayEquals(byteArrayOf(1, 2, 3), AvatarRepository.avatarBytes(document))
    }

    @Test
    fun avatarBytes_isNullWithoutAnAvatar() {
        val document = mockk<DocumentSnapshot> { every { getBlob("avatar") } returns null }
        assertNull(AvatarRepository.avatarBytes(document))
    }

    @Test
    fun saveAvatar_storesACompressedJpegInTheProfile_andReturnsIt() {
        fakeGlideBitmapLoading()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        val saved = runBlocking { AvatarRepository.saveAvatar(context, "uid-1", Uri.parse("content://photo/1")) }

        assertTrue(saved.isNotEmpty())
        assertEquals(setOf("avatar"), data.captured.keys)
        assertArrayEquals(saved, (data.captured["avatar"] as Blob).toBytes())
        // merge, so the rest of the profile (name, email, level) is kept
        assertEquals(SetOptions.merge(), options.captured)
        unmockkStaticGlide()
    }

    private fun unmockkStaticGlide() = io.mockk.unmockkStatic(Glide::class)
}
