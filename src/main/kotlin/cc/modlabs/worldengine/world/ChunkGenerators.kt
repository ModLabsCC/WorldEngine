package cc.modlabs.worldengine.world

import cc.modlabs.worldengine.presets.empty.EmptyWorldGenerator
import cc.modlabs.worldengine.presets.flat.FlatWorldGenerator
import cc.modlabs.worldengine.presets.oceanworld.OceanWorldChunkGenerator
import org.bukkit.Bukkit
import org.bukkit.generator.ChunkGenerator

internal data class ResolvedGenerator(val generator: ChunkGenerator, val configSpec: String)

internal fun generatorConfigSpec(pluginName: String, id: String?): String =
    pluginName + id?.let { ":$it" }.orEmpty()

object ChunkGenerators {

    val presets: Map<String, ChunkGenerator> = mapOf(
        "empty" to EmptyWorldGenerator(),
        "flat" to FlatWorldGenerator(),
        "ocean" to OceanWorldChunkGenerator()
    )

    fun resolve(spec: String, worldName: String? = null): ChunkGenerator =
        resolveForWorld(spec, worldName).generator

    internal fun resolveForWorld(spec: String, worldName: String? = null): ResolvedGenerator {
        resolveWorldEngineId(spec)?.let { generator ->
            val id = if (generator is FlatWorldGenerator) "flat:${generator.baseHeight}" else spec.lowercase()
            return ResolvedGenerator(generator, "WorldEngine:$id")
        }

        val split = spec.split(":", limit = 2)
        val id = split.getOrNull(1)
        val plugin = Bukkit.getPluginManager().getPlugin(split[0])
            ?: throw IllegalArgumentException("Generator plugin '${split[0]}' does not exist")
        require(plugin.isEnabled) { "Generator plugin '${plugin.name}' is not enabled" }
        if (plugin.name.equals("PlotSquared", ignoreCase = true) && worldName == null) {
            throw IllegalArgumentException("PlotSquared requires a target world name")
        }

        val generator = plugin.getDefaultWorldGenerator(worldName ?: "TBD", id)
            ?: throw IllegalArgumentException("Plugin '${plugin.name}' does not provide generator '${id.orEmpty()}'")
        val configSpec = generatorConfigSpec(plugin.name, id)
        return ResolvedGenerator(generator, configSpec)
    }

    fun suggestionStrings(): List<String> = presets.keys.toList() +
        Bukkit.getPluginManager().plugins.filter { it.isEnabled }.map { "${it.name}:" }

    internal fun resolveWorldEngineId(id: String?): ChunkGenerator? {
        if (id.isNullOrBlank()) return null
        val split = id.split(":", limit = 2)
        val preset = presets[split[0].lowercase()] ?: return null
        val option = split.getOrNull(1) ?: return preset
        require(split[0].equals("flat", ignoreCase = true)) { "Preset '${split[0]}' does not accept options" }
        val height = option.toIntOrNull() ?: throw IllegalArgumentException("Flat height '$option' is not a number")
        return FlatWorldGenerator(baseHeight = height)
    }
}
