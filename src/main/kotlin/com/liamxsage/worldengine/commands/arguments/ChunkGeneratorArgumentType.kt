package com.liamxsage.worldengine.commands.arguments

import com.liamxsage.worldengine.presets.empty.EmptyWorldGenerator
import com.liamxsage.worldengine.presets.flat.FlatWorldGenerator
import com.liamxsage.worldengine.presets.oceanworld.OceanWorldChunkGenerator
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import org.bukkit.generator.ChunkGenerator
import java.util.concurrent.CompletableFuture

class ChunkGeneratorArgumentType : CustomArgumentType.Converted<ChunkGenerator, String> {

    private val generators = mapOf(
        "vanilla" to null,
        "empty" to EmptyWorldGenerator(),
        "flat" to FlatWorldGenerator(),
        "ocean" to OceanWorldChunkGenerator()
    )

    override fun convert(nativeType: String): ChunkGenerator {
        return generators[nativeType.lowercase()] ?: throw IllegalArgumentException("Unknown generator $nativeType")
    }

    override fun getNativeType(): ArgumentType<String> {
        return StringArgumentType.word()
    }

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val suggestions = mutableListOf<String>()
        generators.keys.forEach { suggestions.add(it) }
        suggestions.forEach { builder.suggest(it) }
        return builder.buildFuture()
    }
}