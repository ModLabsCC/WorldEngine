package com.liamxsage.worldengine.commands

import com.liamxsage.worldengine.WorldEngine
import com.liamxsage.worldengine.commands.arguments.ChunkGeneratorArgumentType
import com.liamxsage.worldengine.commands.arguments.WorldArgumentType
import com.liamxsage.worldengine.extensions.sendMessagePrefixed
import com.liamxsage.worldengine.utils.FileConfig
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.entity.Player
import org.bukkit.generator.ChunkGenerator

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
                val world = Bukkit.getWorld(worldName) ?: Bukkit.createWorld(WorldCreator(worldName)) ?: return@executes run {
                    player.sendMessagePrefixed("<red>Failed to create world $worldName")
                    Command.SINGLE_SUCCESS
                }

                logger.info("Found world $worldName - teleporting player")
                teleportPlayer(player, world)
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
                .then(Commands.argument<ChunkGenerator>("preset", ChunkGeneratorArgumentType())
                    .executes { context ->
                        if (context.source.sender !is Player) return@executes 0
                        val player = context.source.sender as Player

                        val worldName = context.getArgument<String>("world", String::class.java)
                        val generator = context.getArgument<ChunkGenerator>("preset", ChunkGenerator::class.java)
                        generateWorld(player, worldName, generator)
                        Command.SINGLE_SUCCESS
                    }
                )
            )
        )
        .build()
}

private fun generateWorld(player: Player, worldName: String, generator: ChunkGenerator? = null) {
    val world = Bukkit.createWorld(WorldCreator(worldName).generator(generator)) ?: return player.sendMessagePrefixed("<red>Failed to create world $worldName")
    player.sendMessagePrefixed("<green>Created world $worldName")

    if (generator != null) {
        addWorldWithGeneratorToBukkitYML(worldName, generator)
    }

    Bukkit.getScheduler().runTaskLater(WorldEngine.instance, Runnable {
        teleportPlayer(player, world)
    }, 1L)
}

private fun teleportPlayer(player: Player, world: World) {
    player.teleport(world.spawnLocation.add(0.5, 0.0, 0.5))
    player.sendMessagePrefixed("<green>Teleported to world ${world.name}")
}

private fun addWorldWithGeneratorToBukkitYML(worldName: String, generator: ChunkGenerator) {
    val bukkitYml = FileConfig("bukkit.yml", true)
    val worlds = bukkitYml.getConfigurationSection("worlds") ?: bukkitYml.createSection("worlds")
    val world = worlds.getConfigurationSection(worldName) ?: worlds.createSection(worldName)
    world["generator"] = WorldEngine.instance.name + ":" + generator.javaClass.name
    worlds[worldName] = world
    bukkitYml["worlds"] = worlds
    bukkitYml.saveConfig()
}


