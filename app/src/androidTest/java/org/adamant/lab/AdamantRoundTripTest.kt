package org.adamant.lab

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdamantRoundTripTest {
    @Test fun tilesPredictAndHuffmanDecodeRgbaByteExactly() {
        val width = 131
        val height = 77
        val pixels = ByteArray(width * height * 4) { i -> ((i * 31 + i / 11) and 255).toByte() }
        val container = AdamantNative.nativeEncode(pixels, width, height)
        assertArrayEquals(pixels, AdamantNative.nativeDecode(container))
    }

    @Test fun constantImageRoundTripsSingleHuffmanSymbol() {
        val pixels = ByteArray(16 * 9 * 4) { 42 }
        assertArrayEquals(pixels, AdamantNative.nativeDecode(AdamantNative.nativeEncode(pixels, 16, 9)))
    }

    @Test fun chimeraResidualIsExactAndBoundedErrorModeHonorsDeclaredSampleBound() {
        val width = 131
        val height = 77
        val pixels = ByteArray(width * height * 4) { i -> ((i * 13 + i / 7) and 255).toByte() }
        val exact = AdamantNative.nativeChimeraEncode(pixels, width, height, true, 6)
        assertArrayEquals(pixels, AdamantNative.nativeChimeraDecode(exact))

        val bounded = AdamantNative.nativeChimeraDecode(
            AdamantNative.nativeChimeraEncode(pixels, width, height, false, 6)
        )
        for (i in pixels.indices) {
            val error = kotlin.math.abs((pixels[i].toInt() and 255) - (bounded[i].toInt() and 255))
            assertTrue("sample error exceeds declared bound at byte $i: $error", error <= 6)
        }
    }
}
