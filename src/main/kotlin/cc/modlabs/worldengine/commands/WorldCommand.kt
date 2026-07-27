package cc.modlabs.worldengine.commands

import cc.modlabs.worldengine.WorldEngine
import cc.modlabs.worldengine.commands.arguments.ChunkGeneratorArgumentType
import cc.modlabs.worldengine.commands.arguments.WorldArgumentType
import cc.modlabs.worldengine.extensions.sendMessagePrefixed
import cc.modlabs.worldengine.world.ChunkGenerators
import cc.modlabs.worldengine.world.WorldOperations
import cc.modlabs.worldengine.world.isValidWorldName
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.Bukkit
import org.bukkit.entity.Player

fun createWorldCommand(): LiteralCommandNode<CommandSourceStack> {
    return Commands.literal("world")
        .requires { it.sender.hasPermission("worldengine.world") }
        .then(Commands.argument<String>("world", WorldArgumentType())
            .executes { context ->
                if (context.source.sender !is Player) return@executes 0
                val player = context.source.sender as Player
                val logger = WorldEngine.instance.logger

                logger.info("Executing command `world` with argument ${context.getArgument<String>("world", String::class.java)}")

                val worldName = context.getArgument<String>("world", String::class.java)
                if (!WorldOperations.hasWorldPermission(player, worldName)) {
                    player.sendMessagePrefixed("commands.world.errors.no-permission", placeholders = mapOf("world" to worldName), default = "<red>You do not have permission to access world {world}")
                    return@executes Command.SINGLE_SUCCESS
                }

                val world = WorldOperations.getOrLoadWorld(worldName) ?: return@executes run {
                    player.sendMessagePrefixed("commands.world.errors.failed-to-create", placeholders = mapOf("world" to worldName), default = "<red>Failed to create world {world}")
                    Command.SINGLE_SUCCESS
                }

                logger.info("Found world $worldName - teleporting player")
                WorldOperations.teleportToWorldSpawn(player, world)
                player.sendMessagePrefixed("commands.world.info.teleported", placeholders = mapOf("world" to WorldOperations.userFacingWorldName(world)), default = "<green>Teleported to world {world}")
                Command.SINGLE_SUCCESS
            }
        )
        .then(Commands.literal("generate")
            .requires { it.sender.hasPermission("worldengine.world.generate") }
            .then(Commands.argument<String>("world", StringArgumentType.word())
                .executes { context ->
                    if (context.source.sender !is Player) return@executes 0
                    val player = context.source.sender as Player

                    val worldName = context.getArgument<String>("world", String::class.java)
                    generateWorld(player, worldName)
                    Command.SINGLE_SUCCESS
                }
                .then(Commands.argument<String>("preset", ChunkGeneratorArgumentType())
                    .executes { context ->
                        if (context.source.sender !is Player) return@executes 0
                        val player = context.source.sender as Player

                        val worldName = context.getArgument<String>("world", String::class.java)
                        val generatorSpec = context.getArgument<String>("preset", String::class.java)
                        generateWorld(player, worldName, generatorSpec)
                        Command.SINGLE_SUCCESS
                    }
                )
            )
        )
        .then(Commands.literal("copy")
            .requires { it.sender.hasPermission("worldengine.world.generate") }
            .then(Commands.argument<String>("newname", StringArgumentType.word())
                .executes { context ->
                    if (context.source.sender !is Player) return@executes 0
                    val player = context.source.sender as Player
                    val logger = WorldEngine.instance.logger
                    logger.info("Executing command `world copy` with argument ${context.getArgument<String>("newname", String::class.java)}")

                    val newName = context.getArgument<String>("newname", String::class.java)
                    val world = player.world

                    if (!isValidWorldName(newName)) {
                        player.sendMessagePrefixed("commands.world.errors.invalid-name", placeholders = mapOf("world" to newName), default = "<red>Invalid world name {world}. Use letters, numbers, dots, dashes, or underscores.")
                        return@executes Command.SINGLE_SUCCESS
                    }

                    if (WorldOperations.worldExists(newName)) {
                        player.sendMessagePrefixed("commands.world.errors.world-already-exists", placeholders = mapOf("world" to newName), default = "<red>World {world} already exists")
                        return@executes Command.SINGLE_SUCCESS
                    }

                    if (!WorldOperations.hasWorldPermission(player, newName)) {
                        player.sendMessagePrefixed("commands.world.errors.no-permission", placeholders = mapOf("world" to newName), default = "<red>You do not have permission to copy world {world}")
                        return@executes Command.SINGLE_SUCCESS
                    }

                    player.sendMessagePrefixed("commands.world.info.copying", placeholders = mapOf("world" to WorldOperations.userFacingWorldName(world), "newworld" to newName), default = "<green>Copying world {world} to {newworld}")

                    val destinationFolder = WorldOperations.levelRootDirectoryForUserWorldName(newName)
                    WorldOperations.scheduleWorldCopy(WorldEngine.instance, world, newName) { result ->
                        if (destinationFolder.resolve("uid.dat").exists()) {
                            player.sendMessagePrefixed("commands.world.errors.failed-to-delete-uid", placeholders = mapOf("world" to newName), default = "<red>Failed to delete uid.dat - please delete it manually or the world will not load")
                        }
                        result.fold(
                            onSuccess = {
                                player.sendMessagePrefixed("commands.world.info.copied", placeholders = mapOf("world" to WorldOperations.userFacingWorldName(world), "newworld" to newName), default = "<green>Copied world {world} to {newworld}! <click:run_command:'/world {newworld}'><color:#1bff0f>Teleport?</color></click>")
                            },
                            onFailure = {
                                logger.warning("World copy failed: ${it.message}")
                                player.sendMessagePrefixed(
                                    "commands.world.errors.copy-failed",
                                    default = "<red>World copy failed. Check the server log for details."
                                )
                            }
                        )
                    }

                    Command.SINGLE_SUCCESS
                }
            )
            .executes { context ->
                if (context.source.sender !is Player) return@executes 0
                val player = context.source.sender as Player
                player.sendMessagePrefixed("commands.world.info.copy-help", default = "<yellow>Copy your current world to a new world")
                return@executes Command.SINGLE_SUCCESS
            }
        )
        .build()
}

private fun generateWorld(player: Player, worldName: String, generatorSpec: String? = null) {
    if (!isValidWorldName(worldName)) {
        return player.sendMessagePrefixed("commands.world.errors.invalid-name", placeholders = mapOf("world" to worldName), default = "<red>Invalid world name {world}. Use letters, numbers, dots, dashes, or underscores.")
    }

    if (WorldOperations.worldExists(worldName)) {
        return player.sendMessagePrefixed("commands.world.errors.world-already-exists", placeholders = mapOf("world" to worldName), default = "<red>World {world} already exists")
    }

    if (!WorldOperations.hasWorldPermission(player, worldName)) {
        player.sendMessagePrefixed("commands.world.errors.no-permission", placeholders = mapOf("world" to worldName), default = "<red>You do not have permission to generate world {world}")
        return
    }

    val resolved = generatorSpec?.let { spec ->
        runCatching {
            val bukkitWorldName = WorldOperations.levelRootDirectoryForUserWorldName(worldName).name
            ChunkGenerators.resolveForWorld(spec, bukkitWorldName)
        }.getOrElse {
            WorldEngine.instance.logger.warning("Could not resolve generator '$spec' for '$worldName': ${it.message}")
            player.sendMessagePrefixed(
                "commands.world.errors.invalid-generator",
                placeholders = mapOf("generator" to spec),
                default = "<red>Could not use generator {generator}. Check the server log."
            )
            return
        }
    }

    player.sendMessagePrefixed("commands.world.info.creating", placeholders = mapOf("world" to worldName), default = "<green>Creating world {world}")

    val world = WorldOperations.createWorld(worldName, resolved?.generator)
        ?: return player.sendMessagePrefixed("commands.world.errors.failed-to-create", placeholders = mapOf("world" to worldName), default = "<red>Failed to create world {world}")

    resolved?.let { WorldOperations.registerGeneratorInBukkitConfig(worldName, it.configSpec) }

    Bukkit.getScheduler().runTaskLater(WorldEngine.instance, Runnable {
        WorldOperations.teleportToWorldSpawn(player, world)
        player.sendMessagePrefixed("commands.world.info.teleported", placeholders = mapOf("world" to WorldOperations.userFacingWorldName(world)), default = "<green>Teleported to world {world}")
    }, 1L)
}
