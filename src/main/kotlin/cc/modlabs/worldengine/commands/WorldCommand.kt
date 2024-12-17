package cc.modlabs.worldengine.commands

import cc.modlabs.worldengine.WorldEngine
import cc.modlabs.worldengine.commands.arguments.ChunkGeneratorArgumentType
import cc.modlabs.worldengine.commands.arguments.WorldArgumentType
import cc.modlabs.worldengine.extensions.sendMessagePrefixed
import cc.modlabs.worldengine.utils.FileConfig
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
                if (!hasWorldPermission(player, worldName)) {
                    player.sendMessagePrefixed("commands.world.errors.no-permission", placeholders = mapOf("world" to worldName), default = "<red>You do not have permission to access world {world}")
                    return@executes Command.SINGLE_SUCCESS
                }

                val world = Bukkit.getWorld(worldName) ?: Bukkit.createWorld(WorldCreator(worldName)) ?: return@executes run {
                    player.sendMessagePrefixed("commands.world.errors.failed-to-create", placeholders = mapOf("world" to worldName), default = "<red>Failed to create world {world}")
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

    if (testIfWorldExists(worldName)) {
        return player.sendMessagePrefixed("commands.world.errors.world-already-exists", placeholders = mapOf("world" to worldName), default = "<red>World {world} already exists")
    }

    if (!hasWorldPermission(player, worldName)) {
        player.sendMessagePrefixed("commands.world.errors.no-permission", placeholders = mapOf("world" to worldName), default = "<red>You do not have permission to generate world {world}")
        return
    }

    player.sendMessagePrefixed("commands.world.info.creating", placeholders = mapOf("world" to worldName), default = "<green>Creating world {world}")

    val world = Bukkit.createWorld(WorldCreator(worldName).generator(generator)) ?: return player.sendMessagePrefixed("commands.world.errors.failed-to-create", placeholders = mapOf("world" to worldName), default = "<red>Failed to create world {world}")

    if (generator != null) {
        addWorldWithGeneratorToBukkitYML(worldName, generator)
    }

    Bukkit.getScheduler().runTaskLater(WorldEngine.instance, Runnable {
        teleportPlayer(player, world)
    }, 1L)
}

private fun testIfWorldExists(worldName: String): Boolean {
    return Bukkit.getWorld(worldName) != null
}

private fun teleportPlayer(player: Player, world: World) {
    player.teleport(world.spawnLocation.add(0.5, 0.0, 0.5))
    player.sendMessagePrefixed("commands.world.info.teleported", placeholders = mapOf("world" to world.name), default = "<green>Teleported to world {world}")
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

private fun hasWorldPermission(player: Player, worldName: String): Boolean {
    val basePermission = "worldengine.world"
    val worldPermission = "$basePermission.$worldName"
    val wildcardPermission = "$basePermission.*"
    val prefixWildcardPermission = "$basePermission.${worldName.split("-")[0]}-*"

    return player.hasPermission(worldPermission) || player.hasPermission(wildcardPermission) || player.hasPermission(prefixWildcardPermission)
}
