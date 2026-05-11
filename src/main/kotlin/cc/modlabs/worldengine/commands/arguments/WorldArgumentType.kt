package cc.modlabs.worldengine.commands.arguments

import cc.modlabs.worldengine.world.WorldOperations
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import dev.fruxz.stacked.text
import io.papermc.paper.command.brigadier.MessageComponentSerializer
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import org.bukkit.Bukkit
import java.util.concurrent.CompletableFuture

class WorldArgumentType : CustomArgumentType.Converted<String, String> {

    override fun getNativeType(): ArgumentType<String> {
        return StringArgumentType.word()
    }

    override fun convert(nativeType: String): String {
        WorldOperations.canonicalWorldArgument(nativeType)?.let { return it }
        val message = MessageComponentSerializer.message().serialize(text("Unknown world $nativeType"))
        throw CommandSyntaxException(SimpleCommandExceptionType(message), message)
    }

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val worlds = getAllBukkitWorlds()

        if (worlds.isEmpty()) return Suggestions.empty()

        // Do not filter by permission here: an empty list hides every name and feels broken; execution still checks
        // [WorldOperations.hasWorldPermission].
        worlds.forEach { builder.suggest(it) }
        return builder.buildFuture()
    }


    private fun getAllBukkitWorlds(): List<String> {
        val worlds = mutableListOf<String>()
        for (world in Bukkit.getWorlds()) {
            worlds.add(WorldOperations.userFacingWorldName(world))
            if (WorldOperations.isManagedSeparateLevel(world)) {
                worlds.add(world.name)
            }
        }
        worlds.addAll(WorldOperations.collectDiscoveredWorldNames())
        return worlds.distinct()
    }
}