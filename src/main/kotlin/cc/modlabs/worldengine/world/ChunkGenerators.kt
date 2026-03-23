package cc.modlabs.worldengine.world

import cc.modlabs.worldengine.presets.empty.EmptyWorldGenerator
import cc.modlabs.worldengine.presets.flat.FlatWorldGenerator
import cc.modlabs.worldengine.presets.oceanworld.OceanWorldChunkGenerator
import org.bukkit.Bukkit
import org.bukkit.generator.ChunkGenerator

object ChunkGenerators {

    val presets: Map<String, ChunkGenerator> = mapOf(
        "empty" to EmptyWorldGenerator(),
        "flat" to FlatWorldGenerator(),
        "ocean" to OceanWorldChunkGenerator()
    )

    fun resolve(spec: String): ChunkGenerator {
        return presets[spec.lowercase()] ?: run {
            val split = spec.split(":", limit = 2)
            val id = if (split.size > 1) split[1] else null
            val plugin = Bukkit.getPluginManager().getPlugin(split[0])
            when {
                plugin == null ->
                    throw IllegalArgumentException("Could not set generator: Plugin '${split[0]}' does not exist")
                !plugin.isEnabled ->
                    throw IllegalArgumentException("Could not set generator: Plugin '${plugin.name}' is not enabled")
                else ->
                    plugin.getDefaultWorldGenerator("TBD", id)
                        ?: throw IllegalArgumentException("Could not set generator: Plugin '${plugin.name}' does not provide a default world generator")
            }
        }
    }

    fun suggestionStrings(): List<String> {
        val fromPlugins = Bukkit.getPluginManager().plugins.mapNotNull { plugin ->
            if (plugin.isEnabled) {
                plugin.getDefaultWorldGenerator("TBD", null)?.let { plugin.name + ":" }
            } else null
        }
        return presets.keys.toList() + fromPlugins
    }
}
