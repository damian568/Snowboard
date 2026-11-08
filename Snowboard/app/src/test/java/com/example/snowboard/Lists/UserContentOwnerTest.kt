package com.example.snowboard.Lists

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UserContentOwnerTest {

    private val auth = mockk<FirebaseAuth>()

    @Before
    fun setUp() {
        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns auth
    }

    @After
    fun tearDown() {
        unmockkStatic(FirebaseAuth::class)
    }

    private fun content(owner: String) =
        UserContent("1", ContentType.TIP, owner, "t", "d")

    @Test
    fun isMine_whenSignedInUserIsTheOwner() {
        every { auth.currentUser } returns mockk<FirebaseUser> { every { uid } returns "me" }
        assertTrue(content("me").isMine)
    }

    @Test
    fun isMine_falseForSomeoneElsesContent() {
        every { auth.currentUser } returns mockk<FirebaseUser> { every { uid } returns "me" }
        assertFalse(content("other").isMine)
    }

    @Test
    fun isMine_falseWhenSignedOut() {
        every { auth.currentUser } returns null
        assertFalse(content("me").isMine)
    }
}
