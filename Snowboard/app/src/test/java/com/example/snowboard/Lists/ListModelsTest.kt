package com.example.snowboard.Lists

import com.example.snowboard.User.Model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListModelsTest {

    private fun content(type: ContentType = ContentType.TIP) =
        UserContent(id = "1", type = type, ownerId = "o", title = "t", description = "d")

    @Test
    fun tipsList_userContentDefaultsToNull() {
        assertNull(TipsList("t", "d", 1, 2, "q").userContent)
    }

    @Test
    fun tipsList_keepsUserContent() {
        val c = content()
        assertEquals(c, TipsList("t", "d", 1, 2, "q", c).userContent)
    }

    @Test
    fun videosList_userContentDefaultsToNull() {
        val v = VideosList("id", "title", "desc")
        assertNull(v.userContent)
        assertEquals("id", v.webVideos)
    }

    @Test
    fun skiSlopesList_isCollapsedByDefault_andToggleable() {
        val s = SkiSlopesList(1, "t", "d", 2, "<a href=\"x\">x</a>")
        assertFalse(s.isExpanded)
        assertNull(s.userContent)
        s.isExpanded = true
        assertTrue(s.isExpanded)
    }

    @Test
    fun equipmentList_copyChangesOnlyTheGivenField() {
        val e = EquipmentList(1, "Board", "desc", 2, "quote", "boards")
        val copy = e.copy(equipmentTitle = "Boots")
        assertEquals("Boots", copy.equipmentTitle)
        assertEquals(e.category, copy.category)
        assertNotEquals(e, copy)
    }

    @Test
    fun userContent_defaults() {
        val c = content()
        assertEquals("", c.quote)
        assertEquals("", c.link)
        assertNull(c.image)
        assertEquals("", c.category)
    }

    @Test
    fun userProfile_defaultsToEmptyStrings() {
        val p = UserProfile()
        assertEquals("", p.fullName)
        assertEquals("", p.email)
        assertEquals("", p.level)
    }

    @Test
    fun userProfile_equalityIsByValue() {
        assertEquals(UserProfile("A", "a@b.c", "BEGINNER"), UserProfile("A", "a@b.c", "BEGINNER"))
        assertNotEquals(UserProfile("A", "a@b.c", "BEGINNER"), UserProfile("A", "a@b.c", "ADVANCE"))
    }
}
