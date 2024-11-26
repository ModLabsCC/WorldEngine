package cc.modlabs.worldengine.commands

import cc.modlabs.worldengine.extensions.sendMessagePrefixed
import com.mojang.brigadier.Command
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.entity.Player

fun createWorldInfoCommand(): LiteralCommandNode<CommandSourceStack> {

    return Commands.literal("worldinfo")
        .executes { ctx ->
            val sender = ctx.source.sender
            if (sender !is Player) return@executes Command.SINGLE_SUCCESS

            sender.sendMessagePrefixed("commands.worldinfo.info.currentWorld", mapOf("world" to sender.world.name), default = "You are in the world {world}")


            return@executes Command.SINGLE_SUCCESS
        }
        .build()

}