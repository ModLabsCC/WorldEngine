package cc.modlabs.worldengine

import cc.modlabs.kpaper.main.Feature
import cc.modlabs.worldengine.commands.arguments.ChunkGeneratorArgumentType
import cc.modlabs.worldengine.dimensions.DimensionDatapack
import cc.modlabs.worldengine.presets.flat.FlatWorldGenerator
import cc.modlabs.worldengine.world.ChunkGenerators
import cc.modlabs.worldengine.world.generatorConfigSpec
import cc.modlabs.worldengine.world.isValidWorldName
import cc.modlabs.worldengine.world.matchesWorldPermission
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import rufus.lzstring4java.LZString
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
        val custom = ChunkGenerators.resolveForWorld("flat:64", "low_flat")

        assertEquals(128, (resolved.generator as FlatWorldGenerator).baseHeight)
        assertEquals("WorldEngine:flat:128", resolved.configSpec)
        assertEquals(64, (custom.generator as FlatWorldGenerator).baseHeight)
        assertEquals("WorldEngine:flat:64", custom.configSpec)
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

    @Test
    fun `Misode dimensions are validated and staged as a restart datapack`() {
        assertEquals(
            "tSSaLTUQ9R",
            DimensionDatapack.extractShareId("https://misode.github.io/dimension-type/?share=tSSaLTUQ9R")
        )
        assertFailsWith<IllegalArgumentException> {
            DimensionDatapack.extractShareId("https://example.com/dimension-type/?share=tSSaLTUQ9R")
        }

        val shareJson = JsonObject().apply {
            addProperty("min_y", -512)
            addProperty("height", 1024)
            addProperty("skybox", "none")
            add("attributes", JsonObject().apply {
                add("minecraft:audio/background_music", JsonObject().apply {
                    add("default", JsonObject().apply { addProperty("sound", "bedrockia:music.void") })
                })
            })
        }
        val snippet = JsonObject().apply {
            addProperty("id", "example123")
            addProperty("type", "dimension_type")
            addProperty("version", "26.2")
            addProperty("data", LZString.compressToBase64(shareJson.toString()))
        }
        val decoded = DimensionDatapack.decodeSnippet(snippet.toString(), "example123")
        assertEquals(1024, decoded["height"].asInt)
        assertEquals("none", decoded["skybox"].asString)
        val sound = decoded["attributes"].asJsonObject["minecraft:audio/background_music"]
            .asJsonObject["default"].asJsonObject["sound"].asJsonObject
        assertEquals("bedrockia:music.void", sound["sound_id"].asString)

        val directory = Files.createTempDirectory("worldengine-dimension-test")
        try {
            DimensionDatapack.stage(directory, "void", decoded)
            val pack = directory.resolve("dimension-datapack")
            assertTrue(Files.isRegularFile(pack.resolve("pack.mcmeta")))
            val stagedType = JsonParser.parseString(
                Files.readString(pack.resolve("data/worldengine/dimension_type/void.json"))
            ).asJsonObject
            assertEquals(1024, stagedType["height"].asInt)
            assertFalse(stagedType.has("skybox"))
            val dimension = Files.readString(pack.resolve("data/worldengine/dimension/void.json"))
            assertTrue(dimension.contains("\"type\": \"worldengine:void\""))
            assertTrue(dimension.contains("\"block\": \"minecraft:air\""))
            assertTrue(DimensionDatapack.hasDimension(directory, "void"))

            val typeFile = pack.resolve("data/worldengine/dimension_type/void.json")
            val oldType = JsonObject().apply {
                addProperty("min_y", -512)
                addProperty("height", 1024)
                addProperty("skybox", "none")
            }
            Files.writeString(typeFile, oldType.toString())
            assertEquals(1, DimensionDatapack.normalizeStagedDimensions(directory))
            assertFalse(JsonParser.parseString(Files.readString(typeFile)).asJsonObject.has("skybox"))
            assertEquals(0, DimensionDatapack.normalizeStagedDimensions(directory))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
