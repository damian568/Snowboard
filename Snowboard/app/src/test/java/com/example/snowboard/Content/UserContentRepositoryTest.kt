package com.example.snowboard.Content

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.snowboard.Lists.ContentType
import com.example.snowboard.Lists.UserContent
import com.example.snowboard.fakeGlideBitmapLoading
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.WriteBatch
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserContentRepositoryTest {

    private val firestore = mockk<FirebaseFirestore>()
    private val auth = mockk<FirebaseAuth>()
    private val collections = ContentType.entries.associateWith { mockk<CollectionReference>() }

    @Before
    fun setUp() {
        mockkStatic(FirebaseFirestore::class, FirebaseAuth::class)
        every { FirebaseFirestore.getInstance() } returns firestore
        every { FirebaseAuth.getInstance() } returns auth
        ContentType.entries.forEach { type ->
            every { firestore.collection(type.collection) } returns collections.getValue(type)
        }
        signedInAs("u1")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun signedInAs(uid: String?) {
        every { auth.currentUser } returns uid?.let { id -> mockk<FirebaseUser> { every { this@mockk.uid } returns id } }
    }

    private fun draft(
        id: String = "",
        type: ContentType = ContentType.TIP,
        category: String = ""
    ) = UserContent(
        id = id, type = type, ownerId = "u1", title = "Title", description = "Desc",
        quote = "Quote", link = "https://example.com", category = category
    )

    private fun context() = ApplicationProvider.getApplicationContext<android.content.Context>()

    // ---- save: add ----

    private fun captureAdd(type: ContentType = ContentType.TIP): () -> Map<String, Any> {
        val added = slot<Any>()
        every { collections.getValue(type).add(capture(added)) } returns Tasks.forResult(mockk<DocumentReference>())
        @Suppress("UNCHECKED_CAST")
        return { added.captured as Map<String, Any> }
    }

    @Test
    fun save_newContent_addsItOwnedByTheSignedInUser() {
        val fields = captureAdd()

        runBlocking { UserContentRepository.save(context(), draft(), null, false) }

        val saved = fields()
        assertEquals("Title", saved["title"])
        assertEquals("Desc", saved["description"])
        assertEquals("Quote", saved["quote"])
        assertEquals("https://example.com", saved["link"])
        assertEquals("u1", saved["ownerId"])
        assertEquals(FieldValue.serverTimestamp(), saved["createdAt"])
        assertFalse(saved.containsKey("image"))
        assertFalse(saved.containsKey("category"))
    }

    @Test
    fun save_newContent_withACategory_storesIt() {
        val fields = captureAdd(ContentType.EQUIPMENT)

        runBlocking { UserContentRepository.save(context(), draft(type = ContentType.EQUIPMENT, category = "boards"), null, false) }

        assertEquals("boards", fields()["category"])
    }

    @Test
    fun save_newContent_withAPhoto_storesACompressedBlob() {
        fakeGlideBitmapLoading()
        val fields = captureAdd(ContentType.SKI_SLOPE)

        runBlocking {
            UserContentRepository.save(context(), draft(type = ContentType.SKI_SLOPE), Uri.parse("content://photo/1"), false)
        }

        val image = fields()["image"] as Blob
        assertTrue(image.toBytes().isNotEmpty())
    }

    @Test
    fun save_newContent_whenSignedOut_failsWithoutWriting() {
        signedInAs(null)
        val fields = captureAdd()

        try {
            runBlocking { UserContentRepository.save(context(), draft(), null, false) }
            fail("expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("signed in"))
        }
        verify(exactly = 0) { collections.getValue(ContentType.TIP).add(any()) }
        assertTrue(runCatching { fields() }.isFailure)
    }

    // ---- save: update ----

    private fun captureUpdate(id: String = "doc1", type: ContentType = ContentType.TIP): () -> Map<String, Any> {
        val updated = slot<Map<String, Any>>()
        val doc = mockk<DocumentReference>()
        every { collections.getValue(type).document(id) } returns doc
        every { doc.update(capture(updated)) } returns Tasks.forResult<Void>(null)
        return { updated.captured }
    }

    @Test
    fun save_existingContent_updatesOnlyTheEditableFields() {
        val fields = captureUpdate()

        runBlocking { UserContentRepository.save(context(), draft(id = "doc1"), null, false) }

        val updated = fields()
        assertEquals(setOf("title", "description", "quote", "link"), updated.keys)
    }

    @Test
    fun save_existingContent_removeImage_deletesThePhotoField() {
        val fields = captureUpdate(type = ContentType.EQUIPMENT)

        runBlocking {
            UserContentRepository.save(context(), draft(id = "doc1", type = ContentType.EQUIPMENT), null, true)
        }

        assertEquals(FieldValue.delete(), fields()["image"])
    }

    @Test
    fun save_existingContent_newImageWinsOverRemove() {
        fakeGlideBitmapLoading()
        val fields = captureUpdate(type = ContentType.EQUIPMENT)

        runBlocking {
            UserContentRepository.save(
                context(), draft(id = "doc1", type = ContentType.EQUIPMENT), Uri.parse("content://photo/2"), true
            )
        }

        assertTrue(fields()["image"] is Blob)
    }

    // ---- delete ----

    @Test
    fun delete_removesTheDocumentFromItsCollection() {
        val doc = mockk<DocumentReference>()
        every { collections.getValue(ContentType.VIDEO).document("v1") } returns doc
        every { doc.delete() } returns Tasks.forResult<Void>(null)

        runBlocking { UserContentRepository.delete(draft(id = "v1", type = ContentType.VIDEO)) }

        verify(exactly = 1) { doc.delete() }
    }

    // ---- deleteAllOwnedBy ----

    private fun querySnapshotOf(count: Int): QuerySnapshot {
        val docs = List(count) { mockk<DocumentSnapshot> { every { reference } returns mockk<DocumentReference>() } }
        return mockk { every { documents } returns docs }
    }

    @Test
    fun deleteAllOwnedBy_deletesInBatchesOfAtMost500_acrossEveryCollection() {
        val deletesPerBatch = mutableListOf<Int>()
        val committed = mutableListOf<Int>()
        every { firestore.batch() } answers {
            val index = deletesPerBatch.size
            deletesPerBatch += 0
            mockk<WriteBatch> {
                every { delete(any<DocumentReference>()) } answers {
                    deletesPerBatch[index] = deletesPerBatch[index] + 1
                    self as WriteBatch
                }
                every { commit() } answers {
                    committed += index
                    Tasks.forResult<Void>(null)
                }
            }
        }
        ContentType.entries.forEach { type ->
            val query = mockk<Query>()
            every { collections.getValue(type).whereEqualTo("ownerId", "u1") } returns query
            val count = if (type == ContentType.TIP) 501 else 1
            every { query.get(Source.SERVER) } returns Tasks.forResult(querySnapshotOf(count))
        }

        runBlocking { UserContentRepository.deleteAllOwnedBy("u1") }

        // TIP: 500 + 1, then one single-document batch for each of the three other collections
        assertEquals(listOf(500, 1, 1, 1, 1), deletesPerBatch)
        assertEquals(listOf(0, 1, 2, 3, 4), committed)
    }

    @Test
    fun deleteAllOwnedBy_withNothingToDelete_commitsNothing() {
        every { firestore.batch() } answers { fail("no batch expected"); mockk() }
        ContentType.entries.forEach { type ->
            val query = mockk<Query>()
            every { collections.getValue(type).whereEqualTo("ownerId", "u1") } returns query
            every { query.get(Source.SERVER) } returns Tasks.forResult(querySnapshotOf(0))
        }

        runBlocking { UserContentRepository.deleteAllOwnedBy("u1") }

        verify(exactly = 0) { firestore.batch() }
    }

    // ---- listen ----

    private val registration = mockk<ListenerRegistration>()
    private val listener = slot<EventListener<QuerySnapshot>>()

    private fun document(
        id: String,
        owner: String?,
        createdAt: Long?,
        title: String = "t-$id",
        image: Blob? = null,
        category: String? = null
    ) = mockk<DocumentSnapshot> {
        every { this@mockk.id } returns id
        every { getString("ownerId") } returns owner
        every { getString("title") } returns title
        every { getString("description") } returns "d-$id"
        every { getString("quote") } returns null
        every { getString("link") } returns null
        every { getString("category") } returns category
        every { getBlob("image") } returns image
        every { getTimestamp("createdAt", ServerTimestampBehavior.ESTIMATE) } returns createdAt?.let { Timestamp(it, 0) }
    }

    private fun snapshotOf(vararg docs: DocumentSnapshot) = mockk<QuerySnapshot> { every { documents } returns docs.toList() }

    @Test
    fun listen_withoutACategory_listensToTheWholeCollection() {
        every { collections.getValue(ContentType.TIP).addSnapshotListener(capture(listener)) } returns registration

        val result = UserContentRepository.listen(ContentType.TIP, null, onError = {}, onChange = {})

        assertSame(registration, result)
    }

    @Test
    fun listen_withACategory_filtersOnIt() {
        val query = mockk<Query>()
        every { collections.getValue(ContentType.EQUIPMENT).whereEqualTo("category", "boards") } returns query
        every { query.addSnapshotListener(capture(listener)) } returns registration

        val result = UserContentRepository.listen(ContentType.EQUIPMENT, "boards", onError = {}, onChange = {})

        assertSame(registration, result)
    }

    @Test
    fun listen_deliversNewestFirst_andMapsEveryField() {
        every { collections.getValue(ContentType.EQUIPMENT).addSnapshotListener(capture(listener)) } returns registration
        var delivered: List<UserContent>? = null
        UserContentRepository.listen(ContentType.EQUIPMENT, null, onError = {}, onChange = { delivered = it })

        val photo = Blob.fromBytes(byteArrayOf(9, 8, 7))
        listener.captured.onEvent(
            snapshotOf(
                document("old", "u1", 100),
                document("new", "u2", 300, image = photo, category = "boards"),
                document("mid", "u1", 200)
            ),
            null
        )

        val items = delivered!!
        assertEquals(listOf("new", "mid", "old"), items.map { it.id })
        val newest = items.first()
        assertEquals(ContentType.EQUIPMENT, newest.type)
        assertEquals("u2", newest.ownerId)
        assertEquals("t-new", newest.title)
        assertEquals("d-new", newest.description)
        assertEquals("", newest.quote)
        assertEquals("", newest.link)
        assertEquals("boards", newest.category)
        assertArrayEquals(byteArrayOf(9, 8, 7), newest.image)
        assertNull(items.last().image)
    }

    @Test
    fun listen_skipsDocumentsWithoutAnOwner() {
        every { collections.getValue(ContentType.TIP).addSnapshotListener(capture(listener)) } returns registration
        var delivered: List<UserContent>? = null
        UserContentRepository.listen(ContentType.TIP, null, onError = {}, onChange = { delivered = it })

        listener.captured.onEvent(snapshotOf(document("a", null, 1), document("b", "u1", 2)), null)

        assertEquals(listOf("b"), delivered!!.map { it.id })
    }

    @Test
    fun listen_documentsWithoutATimestampGoLast() {
        every { collections.getValue(ContentType.TIP).addSnapshotListener(capture(listener)) } returns registration
        var delivered: List<UserContent>? = null
        UserContentRepository.listen(ContentType.TIP, null, onError = {}, onChange = { delivered = it })

        listener.captured.onEvent(snapshotOf(document("none", "u1", null), document("dated", "u1", 5)), null)

        assertEquals(listOf("dated", "none"), delivered!!.map { it.id })
    }

    @Test
    fun listen_nullSnapshot_deliversAnEmptyList() {
        every { collections.getValue(ContentType.TIP).addSnapshotListener(capture(listener)) } returns registration
        var delivered: List<UserContent>? = null
        UserContentRepository.listen(ContentType.TIP, null, onError = {}, onChange = { delivered = it })

        listener.captured.onEvent(null, null)

        assertEquals(emptyList<UserContent>(), delivered)
    }

    @Test
    fun listen_error_isReportedAndNothingIsDelivered() {
        every { collections.getValue(ContentType.TIP).addSnapshotListener(capture(listener)) } returns registration
        var error: Exception? = null
        var delivered = false
        UserContentRepository.listen(ContentType.TIP, null, onError = { error = it }, onChange = { delivered = true })

        val failure = FirebaseFirestoreException("denied", FirebaseFirestoreException.Code.PERMISSION_DENIED)
        listener.captured.onEvent(snapshotOf(document("a", "u1", 1)), failure)

        assertSame(failure, error)
        assertFalse(delivered)
    }
}
