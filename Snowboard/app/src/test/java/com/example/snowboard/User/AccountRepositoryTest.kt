package com.example.snowboard.User

import com.example.snowboard.Content.UserContentRepository
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.ExecutionException

@RunWith(RobolectricTestRunner::class)
class AccountRepositoryTest {

    private val credential = mockk<AuthCredential>()
    private val user = mockk<FirebaseUser>()
    private val profileDoc = mockk<DocumentReference>()

    @Before
    fun setUp() {
        every { user.uid } returns "uid-1"
        every { user.reauthenticate(credential) } returns Tasks.forResult<Void>(null)
        every { user.delete() } returns Tasks.forResult<Void>(null)

        val users = mockk<CollectionReference>()
        val firestore = mockk<FirebaseFirestore>()
        mockkStatic(FirebaseFirestore::class)
        every { FirebaseFirestore.getInstance() } returns firestore
        every { firestore.collection("users") } returns users
        every { users.document("uid-1") } returns profileDoc
        every { profileDoc.delete() } returns Tasks.forResult<Void>(null)

        mockkObject(UserContentRepository)
        coEvery { UserContentRepository.deleteAllOwnedBy(any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // Tasks.await wraps the failure, sometimes more than once; look at the whole cause chain
    private fun rootMessages(e: Throwable) = generateSequence(e) { it.cause }.mapNotNull { it.message }.joinToString(" | ")

    @Test
    fun deleteAccount_reauthenticates_thenDeletesContentProfileAndFinallyTheAuthAccount() {
        runBlocking { AccountRepository.deleteAccount(user, credential) }

        coVerifyOrder {
            user.reauthenticate(credential)
            UserContentRepository.deleteAllOwnedBy("uid-1")
            profileDoc.delete()
            user.delete()
        }
    }

    @Test
    fun deleteAccount_failedReauth_deletesNothing() {
        every { user.reauthenticate(credential) } returns Tasks.forException(RuntimeException("wrong password"))

        try {
            runBlocking { AccountRepository.deleteAccount(user, credential) }
            fail("expected the reauthentication failure")
        } catch (e: ExecutionException) {
            assertTrue(rootMessages(e).contains("wrong password"))
        }

        coVerify(exactly = 0) { UserContentRepository.deleteAllOwnedBy(any()) }
        verify(exactly = 0) { profileDoc.delete() }
        verify(exactly = 0) { user.delete() }
    }

    @Test
    fun deleteAccount_failureWhileDeletingContent_keepsTheAuthAccount() {
        coEvery { UserContentRepository.deleteAllOwnedBy(any()) } throws IllegalStateException("boom")

        try {
            runBlocking { AccountRepository.deleteAccount(user, credential) }
            fail("expected the failure to propagate")
        } catch (e: IllegalStateException) {
            assertTrue(e.message == "boom")
        }

        verify(exactly = 0) { profileDoc.delete() }
        verify(exactly = 0) { user.delete() }
    }

    @Test
    fun deleteAccount_failureWhileDeletingTheProfile_keepsTheAuthAccount() {
        every { profileDoc.delete() } returns Tasks.forException(RuntimeException("denied"))

        try {
            runBlocking { AccountRepository.deleteAccount(user, credential) }
            fail("expected the failure to propagate")
        } catch (e: ExecutionException) {
            assertTrue(rootMessages(e).contains("denied"))
        }

        verify(exactly = 0) { user.delete() }
    }
}
