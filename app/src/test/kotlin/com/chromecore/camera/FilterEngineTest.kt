package com.chromecore.camera

import com.chromecore.camera.filters.FilterEngine
import org.junit.Assert.*
import org.junit.Test

class FilterEngineTest {

    @Test
    fun `preset library has at least 25 filters`() {
        val engine = FilterEngine()
        val presets = engine.getPresets()
        assertTrue("Expected at least 25 presets, got ${presets.size}", presets.size >= 25)
    }

    @Test
    fun `all presets have unique IDs`() {
        val engine = FilterEngine()
        val presets = engine.getPresets()
        val ids = presets.map { it.id }
        assertEquals("Duplicate preset IDs found", ids.size, ids.toSet().size)
    }

    @Test
    fun `all presets have non-empty names`() {
        val engine = FilterEngine()
        val presets = engine.getPresets()
        presets.forEach { preset ->
            assertTrue("Preset '${preset.id}' has empty name", preset.name.isNotEmpty())
        }
    }

    @Test
    fun `chromecore filters exist`() {
        val engine = FilterEngine()
        val presets = engine.getPresets()
        val chromeFilters = presets.filter { it.category == FilterEngine.FilterCategory.CHROME }
        assertTrue("Expected Chrome filters", chromeFilters.isNotEmpty())
    }

    @Test
    fun `metalheart filters exist`() {
        val engine = FilterEngine()
        val presets = engine.getPresets()
        val metalFilters = presets.filter { it.category == FilterEngine.FilterCategory.METALHEART }
        assertTrue("Expected Metalheart filters", metalFilters.isNotEmpty())
    }

    @Test
    fun `filter categories are diverse`() {
        val engine = FilterEngine()
        val presets = engine.getPresets()
        val categories = presets.map { it.category }.toSet()
        assertTrue(
            "Expected at least 8 categories, got ${categories.size}: $categories",
            categories.size >= 8
        )
    }

    @Test
    fun `custom applier filters are defined`() {
        val engine = FilterEngine()
        val presets = engine.getPresets()
        val customFilters = presets.filter { it.customApplier != null }
        assertTrue("Expected custom applier filters (RGB Split, Scanlines, etc.)", customFilters.isNotEmpty())
    }
}
