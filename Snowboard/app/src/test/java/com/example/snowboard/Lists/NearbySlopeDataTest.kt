package com.example.snowboard.Lists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbySlopeDataTest {

    private val all = NearbySlopeData.all

    @Test
    fun hasTenResorts() {
        assertEquals(10, all.size)
    }

    @Test
    fun slugs_areUniqueAndUrlSafe() {
        val slugs = all.map { it.skiresortSlug }
        assertEquals(slugs.size, slugs.toSet().size)
        slugs.forEach { assertTrue("slug '$it'", it.matches(Regex("[a-z0-9-]+"))) }
    }

    @Test
    fun ratings_areOnAFiveStarScale() {
        all.forEach { assertTrue("${it.skiresortSlug}: ${it.rating}", it.rating in 0.0..5.0) }
    }

    @Test
    fun coordinates_areInsideBulgaria() {
        all.forEach {
            assertTrue("${it.skiresortSlug} lat ${it.latitude}", it.latitude in 41.2..44.3)
            assertTrue("${it.skiresortSlug} lon ${it.longitude}", it.longitude in 22.3..28.7)
        }
    }

    @Test
    fun resources_areSet() {
        all.forEach {
            assertTrue(it.imgResort != 0)
            assertTrue(it.nameRes != 0)
            assertTrue(it.locationRes != 0)
            assertTrue(it.difficultyRes != 0)
        }
    }

    @Test
    fun nameResources_areUnique() {
        assertEquals(all.size, all.map { it.nameRes }.toSet().size)
    }

    @Test
    fun banskoIsFirst() {
        assertEquals("bansko", all.first().skiresortSlug)
    }
}
