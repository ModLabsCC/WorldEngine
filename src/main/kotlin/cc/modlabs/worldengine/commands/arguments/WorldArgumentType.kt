package cc.modlabs.worldengine.commands.arguments

import cc.modlabs.worldengine.WorldEngine
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
import org.bukkit.entity.Player
import java.util.Locale
import java.util.concurrent.CompletableFuture

class WorldArgumentType : CustomArgumentType.Converted<String, String> {

    override fun getNativeType(): ArgumentType<String> {
        return StringArgumentType.word()
    }

    override fun convert(nativeType: String): String {
        if (getAllBukkitWorlds().contains(nativeType)) {
            return nativeType
        }
        val message = MessageComponentSerializer.message().serialize(text("Unknown world $nativeType"))
        throw CommandSyntaxException(SimpleCommandExceptionType(message), message)
    }

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val worlds = getAllBukkitWorlds()

        if (worlds.isEmpty()) return Suggestions.empty()

        if (context.source is Player) {
            val player = context.source as Player
            worlds.forEach {
                if (WorldOperations.hasWorldPermission(player, it)) builder.suggest(it)
            }
            return builder.buildFuture()
        }

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
        worlds.addAll(getAllFolderWorlds())
        return worlds.distinct()
    }

    private fun getAllFolderWorlds(): MutableList<String> {
        val worlds = mutableListOf<String>()
        val container = Bukkit.getWorldContainer()
        val entries = container.listFiles() ?: return worlds
        val pluginPrefix = WorldEngine.instance.name.lowercase(Locale.ROOT) + "_"
        for (dir in entries) {
            if (dir.isDirectory && WorldOperations.looksLikeWorldSaveDirectory(dir)) {
                worlds.add(dir.name)
                if (dir.name.startsWith(pluginPrefix)) {
                    val short = dir.name.removePrefix(pluginPrefix)
                    if (short.isNotEmpty()) worlds.add(short)
                }
            }
        }
        return worlds
    }
}