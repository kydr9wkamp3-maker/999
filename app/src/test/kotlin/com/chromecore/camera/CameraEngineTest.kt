package com.chromecore.camera

import com.chromecore.camera.engine.CameraEngine
import org.junit.Assert.*
import org.junit.Test

class CameraEngineTest {

    @Test
    fun `default camera state is not open`() {
        // CameraState defaults — verifies data class structure
        val state = CameraEngine.CameraState()
        assertFalse(state.isOpen)
        assertFalse(state.isCapturing)
        assertFalse(state.isRecording)
        assertEquals(1.0f, state.zoomLevel, 0.001f)
        assertEquals(0, state.exposureCompensation)
        assertNull(state.iso)
        assertNull(state.shutterSpeed)
    }

    @Test
    fun `flash mode enum has all values`() {
        val modes = CameraEngine.FlashMode.entries
        assertEquals(4, modes.size)
        assertTrue(modes.contains(CameraEngine.FlashMode.AUTO))
        assertTrue(modes.contains(CameraEngine.FlashMode.ON))
        assertTrue(modes.contains(CameraEngine.FlashMode.OFF))
        assertTrue(modes.contains(CameraEngine.FlashMode.TORCH))
    }

    @Test
    fun `focus mode enum has all values`() {
        val modes = CameraEngine.FocusMode.entries
        assertEquals(4, modes.size)
        assertTrue(modes.contains(CameraEngine.FocusMode.AUTO))
        assertTrue(modes.contains(CameraEngine.FocusMode.MANUAL))
        assertTrue(modes.contains(CameraEngine.FocusMode.CONTINUOUS))
        assertTrue(modes.contains(CameraEngine.FocusMode.LOCKED))
    }
}
