package com.example.snowboard.Lists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickActionTest {

    @Test
    fun fromId_findsEveryAction() {
        QuickAction.entries.forEach { assertEquals(it, QuickAction.fromId(it.id)) }
    }

    @Test
    fun fromId_unknownId_returnsNull() {
        assertNull(QuickAction.fromId("nope"))
        assertNull(QuickAction.fromId(""))
    }

    @Test
    fun fromId_isCaseSensitive() {
        assertNull(QuickAction.fromId("SKISLOPES"))
        assertEquals(QuickAction.SKI_SLOPES, QuickAction.fromId("skiSlopes"))
    }

    @Test
    fun ids_areUnique() {
        val ids = QuickAction.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun everyAction_hasIconLabelAndDestination() {
        QuickAction.entries.forEach {
            assertTrue(it.icon != 0)
            assertTrue(it.label != 0)
            assertTrue(it.destination != 0)
        }
    }

    @Test
    fun ids_matchContentCollections_forContentScreens() {
        assertEquals(ContentType.SKI_SLOPE.collection, QuickAction.SKI_SLOPES.id)
        assertEquals(ContentType.EQUIPMENT.collection, QuickAction.EQUIPMENT.id)
        assertEquals(ContentType.TIP.collection, QuickAction.TIPS.id)
        assertEquals(ContentType.VIDEO.collection, QuickAction.VIDEOS.id)
    }
}
