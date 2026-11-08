package com.example.snowboard.Constants

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConstantsTest {

    @Test
    fun durations_arePositive() {
        assertTrue(Constants.SkiRotation > 0)
        assertTrue(Constants.DetailDoration > 0)
        assertTrue(Constants.DelayMills_Splash > 0)
    }

    @Test
    fun values_matchWhatTheScreensExpect() {
        assertEquals(200L, Constants.SkiRotation)
        assertEquals(300L, Constants.DetailDoration)
        assertEquals(3000L, Constants.DelayMills_Splash)
        assertEquals(0, Constants.Detail_IMG)
    }
}
