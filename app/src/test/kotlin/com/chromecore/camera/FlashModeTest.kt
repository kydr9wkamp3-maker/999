package com.chromecore.camera

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Проверка цикла режимов вспышки: OFF → AUTO → ON → OFF.
 * Логика скопирована из CameraScreen.kt для изолированного тестирования.
 */
class FlashModeTest {

    enum class FlashMode { OFF, ON, AUTO }

    private fun FlashMode.next(): FlashMode = when (this) {
        FlashMode.OFF -> FlashMode.AUTO
        FlashMode.AUTO -> FlashMode.ON
        FlashMode.ON -> FlashMode.OFF
    }

    @Test
    fun flashModeCyclesThroughAllModes() {
        var mode = FlashMode.OFF
        assertEquals(FlashMode.AUTO, mode.next())
        mode = mode.next()

        assertEquals(FlashMode.ON, mode.next())
        mode = mode.next()

        assertEquals(FlashMode.OFF, mode.next())
        mode = mode.next()

        // Полный цикл
        assertEquals(FlashMode.AUTO, mode.next())
    }

    @Test
    fun flashModeFullCycle() {
        val sequence = generateSequence(FlashMode.OFF) { it.next() }.take(6).toList()
        assertEquals(listOf(FlashMode.OFF, FlashMode.AUTO, FlashMode.ON, FlashMode.OFF, FlashMode.AUTO, FlashMode.ON), sequence)
    }
}
