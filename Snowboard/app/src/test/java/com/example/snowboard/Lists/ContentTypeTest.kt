package com.example.snowboard.Lists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentTypeTest {

    @Test
    fun collections_matchFirestoreNames() {
        assertEquals("tips", ContentType.TIP.collection)
        assertEquals("equipment", ContentType.EQUIPMENT.collection)
        assertEquals("skiSlopes", ContentType.SKI_SLOPE.collection)
        assertEquals("videos", ContentType.VIDEO.collection)
    }

    @Test
    fun collections_areUnique() {
        val names = ContentType.entries.map { it.collection }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun everyType_hasResources() {
        ContentType.entries.forEach {
            assertTrue(it.addTitle != 0)
            assertTrue(it.editTitle != 0)
            assertTrue(it.titleHint != 0)
            assertTrue(it.titleIcon != 0)
        }
    }

    @Test
    fun valueOf_roundTripsName() {
        ContentType.entries.forEach { assertEquals(it, ContentType.valueOf(it.name)) }
    }
}
