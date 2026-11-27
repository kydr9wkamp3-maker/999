package org.adamant.lab

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QualityMetricsTest {
    @Test fun identicalRgbaHasPerfectGlobalSsimAndUndefinedFinitePsnr() {
        val pixels = byteArrayOf(20, 40, 60, -1, 80, 100, 120, -1)
        assertNull(QualityMetrics.psnr(pixels, pixels.copyOf()))
        assertEquals(1.0, QualityMetrics.globalSsim(pixels, pixels.copyOf()), 1e-12)
    }

    @Test fun maximumSampleErrorIsMeasuredAcrossUnsignedRgbaBytes() {
        assertEquals(8, QualityMetrics.maxSampleError(byteArrayOf(0, 20, -1, 0), byteArrayOf(8, 12, -1, 0)))
    }

    @Test fun csvQuotesCommaAndQuoteCharacters() {
        val record = ExperimentRecord(
            "2026-01-01T00:00:00Z", "Device, Model", "Android 15", "a,\"b.png", 1, 1,
            "PNG", "Exact", 4, 5, 0.8, 1.0, 2.0, null, 1.0, 0, true, "note, \"x\"",
            "filehash", "source-rgba", "decoded-rgba", true
        )
        val csv = ExperimentLog.toCsv(listOf(record))
        assertTrue(csv.contains("\"Device, Model\""))
        assertTrue(csv.contains("\"a,\"\"b.png\""))
        assertTrue(csv.contains("\"note, \"\"x\"\"\""))
        assertTrue(csv.lineSequence().first().contains("max_sample_error"))
    }
}
