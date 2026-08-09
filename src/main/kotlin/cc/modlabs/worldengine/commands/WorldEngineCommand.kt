package cc.modlabs.worldengine.commands

import cc.modlabs.worldengine.WorldEngine
import cc.modlabs.worldengine.cache.MessageCache
import cc.modlabs.worldengine.dimensions.DimensionDatapack
import cc.modlabs.worldengine.extensions.sendMessagePrefixed
import cc.modlabs.worldengine.world.WorldOperations
import cc.modlabs.worldengine.world.isValidWorldName
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.Bukkit

fun createWorldEngineCommand(): LiteralCommandNode<CommandSourceStack> {
    return Commands.literal("worldengine")
        .requires { it.sender.hasPermission("worldengine.manage") }
        .then(Commands.literal("reload")
            .executes { ctx ->
                val sender = ctx.source.sender

                sender.sendMessagePrefixed("commands.worldengine.info.reloading", default = "<yellow>Reloading messages...")
                MessageCache.loadCache()
                sender.sendMessagePrefixed("commands.worldengine.info.reloaded", default = "<green>Messages reloaded!")

                return@executes Command.SINGLE_SUCCESS
            }
        )
        .then(Commands.literal("dimension")
            .then(Commands.literal("create")
                .then(Commands.argument<String>("world", StringArgumentType.word())
                    .then(Commands.argument<String>("share-link", StringArgumentType.greedyString())
                        .executes { context ->
                            val sender = context.source.sender
                            val worldName = context.getArgument<String>("world", String::class.java)
                            val shareLink = context.getArgument<String>("share-link", String::class.java)

                            if (!isValidWorldName(worldName)) {
                                sender.sendMessagePrefixed(
                                    "commands.world.errors.invalid-name",
                                    mapOf("world" to worldName),
                                    "<red>Invalid world name {world}. Use letters, numbers, dots, dashes, or underscores."
                                )
                                return@executes Command.SINGLE_SUCCESS
                            }
                            if (WorldOperations.worldExists(worldName)) {
                                sender.sendMessagePrefixed(
                                    "commands.world.errors.world-already-exists",
                                    mapOf("world" to worldName),
                                    "<red>World {world} already exists."
                                )
                                return@executes Command.SINGLE_SUCCESS
                            }

                            sender.sendMessagePrefixed(
                                "commands.worldengine.info.dimension-importing",
                                mapOf("world" to worldName),
                                "<yellow>Importing dimension type for {world}..."
                            )
                            DimensionDatapack.importVoidDimension(
                                WorldEngine.instance.dataFolder.toPath(),
                                worldName,
                                shareLink
                            ).whenComplete { _, failure ->
                                val plugin = WorldEngine.instance
                                if (!plugin.isEnabled) return@whenComplete
                                Bukkit.getScheduler().runTask(plugin, Runnable {
                                    if (failure == null) {
                                        sender.sendMessagePrefixed(
                                            "commands.worldengine.info.dimension-staged",
                                            mapOf("world" to worldName),
                                            "<green>Dimension {world} is ready. Restart the server to load it."
                                        )
                                    } else {
                                        val cause = failure.cause ?: failure
                                        plugin.logger.warning("Could not stage dimension '$worldName': ${cause.message}")
                                        sender.sendMessagePrefixed(
                                            "commands.worldengine.errors.dimension-import-failed",
                                            mapOf("world" to worldName),
                                            "<red>Could not prepare dimension {world}. Check the link and server log."
                                        )
                                    }
                                })
                            }
                            Command.SINGLE_SUCCESS
                        }
                    )
                )
            )
        )
        .build()
}
