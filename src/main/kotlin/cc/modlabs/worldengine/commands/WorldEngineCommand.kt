package cc.modlabs.worldengine.commands

import cc.modlabs.worldengine.cache.MessageCache
import cc.modlabs.worldengine.extensions.sendMessagePrefixed
import com.mojang.brigadier.Command
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands

fun createWorldEngineCommand(): LiteralCommandNode<CommandSourceStack> {
    return Commands.literal("worldengine")
        .requires { it.sender.hasPermission("worldengine.manage") }
        .then(Commands.literal("reload")
            .executes { ctx ->
                val sender = ctx.source.sender

                sender.sendMessagePrefixed("<yellow>Reloading config...")
                MessageCache.loadCache()
                sender.sendMessagePrefixed("<green>Config reloaded!")

                return@executes Command.SINGLE_SUCCESS
            }
        )
        .build()
}