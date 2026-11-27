package org.adamant.lab

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MvpMemoryBudgetTest {
    @Test fun allowedPixelsUsesHeapBudgetAndMvpCap() {
        assertEquals(6_710_886L, MvpMemoryBudget.allowedPixels(256L * 1024L * 1024L))
        assertEquals(12_000_000L, MvpMemoryBudget.allowedPixels(512L * 1024L * 1024L))
    }

    @Test fun sampleSizeFits3280By2460Within256MibBudget() {
        val maxHeap = 256L * 1024L * 1024L
        val limit = MvpMemoryBudget.allowedPixels(maxHeap)
        val sample = MvpMemoryBudget.inSampleSize(3280, 2460, limit)
        val decodedPixels = ((3280L + sample - 1) / sample) * ((2460L + sample - 1) / sample)
        assertEquals(2, sample)
        assertTrue(decodedPixels <= limit)
    }
}
