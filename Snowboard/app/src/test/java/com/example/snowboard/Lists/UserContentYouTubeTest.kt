package com.example.snowboard.Lists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserContentYouTubeTest {

    private val id = "dQw4w9WgXcQ"

    @Test
    fun bareId_isAccepted() = assertEquals(id, UserContent.youTubeId(id))

    @Test
    fun bareId_isTrimmed() = assertEquals(id, UserContent.youTubeId("  $id \n"))

    @Test
    fun bareId_withDashAndUnderscore() = assertEquals("a-b_c-d_e-f", UserContent.youTubeId("a-b_c-d_e-f"))

    @Test
    fun watchLink() = assertEquals(id, UserContent.youTubeId("https://www.youtube.com/watch?v=$id"))

    @Test
    fun watchLink_withExtraParametersBefore() =
        assertEquals(id, UserContent.youTubeId("https://www.youtube.com/watch?feature=share&v=$id"))

    @Test
    fun watchLink_withExtraParametersAfter() =
        assertEquals(id, UserContent.youTubeId("https://www.youtube.com/watch?v=$id&t=42s"))

    @Test
    fun shortLink() = assertEquals(id, UserContent.youTubeId("https://youtu.be/$id"))

    @Test
    fun shortLink_withTimestamp() = assertEquals(id, UserContent.youTubeId("https://youtu.be/$id?t=10"))

    @Test
    fun shortsLink() = assertEquals(id, UserContent.youTubeId("https://www.youtube.com/shorts/$id"))

    @Test
    fun embedLink() = assertEquals(id, UserContent.youTubeId("https://www.youtube.com/embed/$id"))

    @Test
    fun liveLink() = assertEquals(id, UserContent.youTubeId("https://www.youtube.com/live/$id"))

    @Test
    fun mobileLink() = assertEquals(id, UserContent.youTubeId("https://m.youtube.com/watch?v=$id"))

    @Test
    fun linkWithoutScheme() = assertEquals(id, UserContent.youTubeId("youtube.com/watch?v=$id"))

    @Test
    fun emptyAndBlank_returnNull() {
        assertNull(UserContent.youTubeId(""))
        assertNull(UserContent.youTubeId("   "))
    }

    @Test
    fun otherSite_returnsNull() {
        assertNull(UserContent.youTubeId("https://vimeo.com/12345678901"))
        assertNull(UserContent.youTubeId("https://example.com/watch?v=$id"))
    }

    @Test
    fun wrongLengthId_returnsNull() {
        assertNull(UserContent.youTubeId("short"))
        assertNull(UserContent.youTubeId("dQw4w9WgXcQQ"))
    }

    @Test
    fun youtubeLinkWithoutVideoId_returnsNull() {
        assertNull(UserContent.youTubeId("https://www.youtube.com/"))
        assertNull(UserContent.youTubeId("https://www.youtube.com/watch?v=short"))
    }

    @Test
    fun idWithIllegalCharacters_returnsNull() {
        assertNull(UserContent.youTubeId("dQw4w9WgX!Q"))
    }
}
