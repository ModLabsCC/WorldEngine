package cc.modlabs.worldengine.commands.arguments

import cc.modlabs.worldengine.world.ChunkGenerators
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import java.util.concurrent.CompletableFuture

class ChunkGeneratorArgumentType : CustomArgumentType.Converted<String, String> {

    override fun convert(nativeType: String): String = nativeType

    override fun getNativeType(): ArgumentType<String> = StringArgumentType.greedyString()

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        ChunkGenerators.suggestionStrings().forEach { builder.suggest(it) }
        return builder.buildFuture()
    }
}
