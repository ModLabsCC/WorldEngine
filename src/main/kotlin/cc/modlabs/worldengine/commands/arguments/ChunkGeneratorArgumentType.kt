package cc.modlabs.worldengine.commands.arguments

import cc.modlabs.worldengine.presets.empty.EmptyWorldGenerator
import cc.modlabs.worldengine.presets.flat.FlatWorldGenerator
import cc.modlabs.worldengine.presets.oceanworld.OceanWorldChunkGenerator
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import org.bukkit.Bukkit
import org.bukkit.WorldCreator
import org.bukkit.generator.ChunkGenerator
import java.util.concurrent.CompletableFuture

class ChunkGeneratorArgumentType : CustomArgumentType.Converted<ChunkGenerator, String> {

    private val generators = mapOf(
        "empty" to EmptyWorldGenerator(),
        "flat" to FlatWorldGenerator(),
        "ocean" to OceanWorldChunkGenerator()
    )

    override fun convert(nativeType: String): ChunkGenerator {
        return generators[nativeType.lowercase()] ?: run {
            val split = nativeType.split(":", limit = 2)
            val id = if (split.size > 1) split[1] else null
            val plugin = Bukkit.getPluginManager().getPlugin(split[0])

            when {
                plugin == null -> {
                    throw IllegalArgumentException("Could not set generator: Plugin '${split[0]}' does not exist")
                }
                !plugin.isEnabled -> {
                    throw IllegalArgumentException("Could not set generator: Plugin '${plugin.name}' is not enabled")
                }
                else -> {
                    return@run plugin.getDefaultWorldGenerator("TBD", id) ?: throw IllegalArgumentException("Could not set generator: Plugin '${plugin.name}' does not provide a default world generator")
                }
            }
        }
    }

    override fun getNativeType(): ArgumentType<String> {
        return StringArgumentType.greedyString()
    }

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val suggestions = mutableListOf<String>()
        generators.keys.forEach { suggestions.add(it) }
        suggestions.addAll(getCustomPluginGenerators())
        suggestions.forEach { builder.suggest(it) }
        return builder.buildFuture()
    }

    private fun getCustomPluginGenerators(): List<String> {
        val generators = mutableListOf<String>()
        Bukkit.getPluginManager().plugins.forEach { plugin ->
            if (plugin.isEnabled) {
                plugin.getDefaultWorldGenerator("TBD", null)?.let { generators.add(plugin.name + ":" ) }
            }
        }
        return generators
    }

}