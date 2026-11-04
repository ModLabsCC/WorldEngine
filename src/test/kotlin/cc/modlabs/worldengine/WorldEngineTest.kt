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
            addProperty("version", "26.3")
            addProperty("data", LZString.compressToBase64(shareJson.toString()))
        }
        val decoded = DimensionDatapack.decodeSnippet(snippet.toString(), "example123")
        val outdatedSnippet = snippet.deepCopy().apply { addProperty("version", "26.2") }
        assertFailsWith<IllegalArgumentException> {
            DimensionDatapack.decodeSnippet(outdatedSnippet.toString(), "example123")
        }
        assertEquals(1024, decoded["height"].asInt)
        assertEquals("none", decoded["skybox"].asString)
        val sound = decoded["attributes"].asJsonObject["minecraft:audio/background_music"]
            .asJsonObject["default"].asJsonObject["sound"].asJsonObject
        assertEquals("bedrockia:music.void", sound["sound_id"].asString)

        val directory = Files.createTempDirectory("worldengine-dimension-test")
        try {
            DimensionDatapack.stage(directory, "void", decoded)
            val pack = directory.resolve("dimension-datapack")
            val metadata = JsonParser.parseString(Files.readString(pack.resolve("pack.mcmeta"))).asJsonObject["pack"].asJsonObject
            assertEquals(121, metadata["pack_format"].asInt)
            assertEquals("[121,0]", metadata["min_format"].toString())
            assertEquals("[121,0]", metadata["max_format"].toString())
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

    @Test
    fun `existing dimension packs migrate to 26_3 without losing custom metadata`() {
        val directory = Files.createTempDirectory("worldengine-legacy-dimension-test")
        try {
            val pack = directory.resolve("dimension-datapack")
            val types = pack.resolve("data/worldengine/dimension_type")
            Files.createDirectories(types)
            Files.writeString(pack.resolve("pack.mcmeta"), """
                {"pack":{"description":"Custom description","pack_format":107,
                "min_format":[107,1],"max_format":[107,1]},"custom":{"keep":true}}
            """.trimIndent())
            Files.writeString(types.resolve("legacy.json"), """
                {"min_y":0,"height":256,"attributes":{
                  "minecraft:gameplay/bed_rule":{"can_sleep":"never","explodes":true}
                }}
            """.trimIndent())
            Files.writeString(types.resolve("explicit.json"), """
                {"min_y":0,"height":256,"attributes":{
                  "minecraft:gameplay/bed_rule":{"explodes":true,"destroy_on_use":false}
                }}
            """.trimIndent())

            assertEquals(2, DimensionDatapack.normalizeStagedDimensions(directory))
            val metadata = JsonParser.parseString(Files.readString(pack.resolve("pack.mcmeta"))).asJsonObject
            assertEquals(121, metadata["pack"].asJsonObject["pack_format"].asInt)
            assertEquals("[121,0]", metadata["pack"].asJsonObject["min_format"].toString())
            assertEquals("[121,0]", metadata["pack"].asJsonObject["max_format"].toString())
            assertEquals("Custom description", metadata["pack"].asJsonObject["description"].asString)
            assertTrue(metadata["custom"].asJsonObject["keep"].asBoolean)
            val legacy = JsonParser.parseString(Files.readString(types.resolve("legacy.json")))
                .asJsonObject["attributes"].asJsonObject["minecraft:gameplay/bed_rule"].asJsonObject
            assertTrue(legacy["destroy_on_use"].asBoolean)
            assertEquals("never", legacy["can_sleep"].asString)
            assertFalse(legacy.has("explodes"))
            val explicit = JsonParser.parseString(Files.readString(types.resolve("explicit.json")))
                .asJsonObject["attributes"].asJsonObject["minecraft:gameplay/bed_rule"].asJsonObject
            assertFalse(explicit["destroy_on_use"].asBoolean)
            assertFalse(explicit.has("explodes"))
            val migratedMetadata = Files.readString(pack.resolve("pack.mcmeta"))
            assertEquals(0, DimensionDatapack.normalizeStagedDimensions(directory))
            assertEquals(migratedMetadata, Files.readString(pack.resolve("pack.mcmeta")))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `staging a dimension refreshes existing pack metadata`() {
        val directory = Files.createTempDirectory("worldengine-staging-migration-test")
        try {
            val pack = directory.resolve("dimension-datapack")
            Files.createDirectories(pack)
            Files.writeString(pack.resolve("pack.mcmeta"), """
                {"pack":{"description":"WorldEngine dimensions","pack_format":107,
                "min_format":[107,1],"max_format":[107,1]}}
            """.trimIndent())
            DimensionDatapack.stage(directory, "new_void", JsonObject().apply {
                addProperty("min_y", 0)
                addProperty("height", 256)
            })
            val metadata = JsonParser.parseString(Files.readString(pack.resolve("pack.mcmeta"))).asJsonObject
            assertEquals(121, metadata["pack"].asJsonObject["pack_format"].asInt)
            assertEquals("[121,0]", metadata["pack"].asJsonObject["min_format"].toString())
            assertEquals("[121,0]", metadata["pack"].asJsonObject["max_format"].toString())
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
