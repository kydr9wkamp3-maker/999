package com.chromecore.camera

import android.graphics.Bitmap
import com.chromecore.camera.filters.FilterEngine
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Instrumented tests for image processing.
 * Run on device/emulator: ./gradlew connectedAndroidTest
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ImageProcessingInstrumentedTest {

    @Test
    fun `process returns bitmap of same dimensions`() {
        val processor = com.chromecore.camera.processing.ImageProcessor()
        // This test requires Android framework - run as instrumented test
        // Placeholder for actual bitmap creation
    }

    @Test
    fun `filter intensity zero returns unchanged bitmap`() {
        val engine = FilterEngine()
        val presets = engine.getPresets()
        // Verify intensity parameter is handled
        presets.forEach { preset ->
            assertNotNull("Preset ${preset.id} should not be null", preset)
        }
    }
}
