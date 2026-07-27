package cc.modlabs.worldengine

import cc.modlabs.kpaper.main.Feature
import cc.modlabs.worldengine.world.isValidWorldName
import cc.modlabs.worldengine.world.matchesWorldPermission
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorldEngineTest {
    @Test
    fun `optional KPaper features stay disabled`() {
        assertTrue(Feature.entries.none(disabledKPaperFeatures()::isEnabled))
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