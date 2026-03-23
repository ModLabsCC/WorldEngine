package cc.modlabs.worldengine.commands.arguments

import cc.modlabs.worldengine.world.ChunkGenerators
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import org.bukkit.generator.ChunkGenerator
import java.util.concurrent.CompletableFuture

class ChunkGeneratorArgumentType : CustomArgumentType.Converted<ChunkGenerator, String> {

    override fun convert(nativeType: String): ChunkGenerator = ChunkGenerators.resolve(nativeType)

    override fun getNativeType(): ArgumentType<String> {
        return StringArgumentType.greedyString()
    }

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        ChunkGenerators.suggestionStrings().forEach { builder.suggest(it) }
        return builder.buildFuture()
    }

}