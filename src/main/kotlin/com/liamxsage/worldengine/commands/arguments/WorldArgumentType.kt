package com.liamxsage.worldengine.commands.arguments

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
        worlds.forEach { builder.suggest(it) }
        return builder.buildFuture()
    }


    private fun getAllBukkitWorlds(): List<String> {
        val worlds = mutableListOf<String>()
        for (world in Bukkit.getWorlds()) {
            worlds.add(world.name)
        }
        worlds.addAll(getAllFolderWorlds())
        return worlds.distinct()
    }

    private fun getAllFolderWorlds(): MutableList<String> {
        val worlds = mutableListOf<String>()
        val worldFolder = Bukkit.getWorldContainer()
        for (world in worldFolder.listFiles()!!) {
            if (world.isDirectory && world.listFiles()?.any { it.name == "level.dat" } == true) {
                worlds.add(world.name)
            }
        }
        return worlds
    }
}