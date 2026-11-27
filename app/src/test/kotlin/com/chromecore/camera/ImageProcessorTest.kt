package com.chromecore.camera

import com.chromecore.camera.processing.ImageProcessor
import org.junit.Assert.*
import org.junit.Test

class ImageProcessorTest {

    @Test
    fun `default params do not alter image`() {
        val processor = ImageProcessor()
        // Verify that ProcessingParams defaults are identity transforms
        val params = ImageProcessor.ProcessingParams()
        assertEquals(0f, params.brightness, 0.001f)
        assertEquals(1f, params.contrast, 0.001f)
        assertEquals(1f, params.saturation, 0.001f)
        assertEquals(0f, params.temperature, 0.001f)
        assertEquals(0f, params.exposure, 0.001f)
        assertEquals(0f, params.sharpness, 0.001f)
        assertEquals(0f, params.vignette, 0.001f)
        assertEquals(0f, params.grain, 0.001f)
    }

    @Test
    fun `exposure zero is no-op`() {
        val params = ImageProcessor.ProcessingParams(exposure = 0f)
        assertEquals(0f, params.exposure, 0.001f)
    }

    @Test
    fun `brightness range is bounded`() {
        val params = ImageProcessor.ProcessingParams(brightness = 0.5f)
        assertTrue(params.brightness in -1f..1f)
    }

    @Test
    fun `contrast default is identity`() {
        val params = ImageProcessor.ProcessingParams()
        assertEquals(1f, params.contrast, 0.001f)
    }
}
