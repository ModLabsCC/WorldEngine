package cc.modlabs.worldengine

import cc.modlabs.kpaper.main.Feature
import cc.modlabs.worldengine.commands.arguments.ChunkGeneratorArgumentType
import cc.modlabs.worldengine.world.ChunkGenerators
import cc.modlabs.worldengine.world.generatorConfigSpec
import cc.modlabs.worldengine.world.isValidWorldName
import cc.modlabs.worldengine.world.matchesWorldPermission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorldEngineTest {
    @Test
    fun `optional KPaper features stay disabled`() {
        assertTrue(Feature.entries.none(disabledKPaperFeatures()::isEnabled))
    }

    @Test
    fun `built-in generators keep a restart-safe config spec`() {
        val resolved = ChunkGenerators.resolveForWorld("flat", "worldengine_plots")

        assertTrue(resolved.configSpec.startsWith("WorldEngine:"))
        assertEquals("PlotSquared", generatorConfigSpec("PlotSquared", null))
        assertEquals("PlotSquared:single", generatorConfigSpec("PlotSquared", "single"))
        assertEquals("PlotSquared", ChunkGeneratorArgumentType().convert("PlotSquared"))
    }

    @Test
    fun `world names and permission globs are strict`() {
        assertTrue(isValidWorldName("event_2026.2"))
        assertFalse(isValidWorldName(".."))
        assertFalse(isValidWorldName("../world"))
        assertTrue(matchesWorldPermission("event_*", "event_summer"))
        assertFalse(matchesWorldPermission("event.*", "event_summer"))
    }
}
