package cc.modlabs.worldengine.world

import cc.modlabs.worldengine.WorldEngine
import cc.modlabs.worldengine.utils.FileConfig
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.entity.Player
import org.bukkit.generator.ChunkGenerator
import org.bukkit.plugin.Plugin

object WorldOperations {

    fun isWorldLoaded(name: String): Boolean = Bukkit.getWorld(name) != null

    fun getWorld(name: String): World? = Bukkit.getWorld(name)

    fun getOrLoadWorld(name: String): World? =
        Bukkit.getWorld(name) ?: Bukkit.createWorld(WorldCreator(name))

    fun createWorld(name: String, generator: ChunkGenerator? = null): World? =
        Bukkit.createWorld(WorldCreator(name).generator(generator))

    fun registerGeneratorInBukkitConfig(worldName: String, generator: ChunkGenerator) {
        val bukkitYml = FileConfig("bukkit.yml", true)
        val worlds = bukkitYml.getConfigurationSection("worlds") ?: bukkitYml.createSection("worlds")
        val world = worlds.getConfigurationSection(worldName) ?: worlds.createSection(worldName)
        world["generator"] = WorldEngine.instance.name + ":" + generator.javaClass.name
        worlds[worldName] = world
        bukkitYml["worlds"] = worlds
        bukkitYml.saveConfig()
    }

    fun copyWorldGeneratorConfig(sourceWorldName: String, newWorldName: String) {
        val bukkitYml = FileConfig("bukkit.yml", true)
        val worlds = bukkitYml.getConfigurationSection("worlds") ?: return
        val world = worlds.getConfigurationSection(sourceWorldName) ?: return
        if (world["generator"] == null) return
        worlds[newWorldName] = world
        bukkitYml["worlds"] = worlds
        bukkitYml.saveConfig()
    }

    fun teleportToWorldSpawn(player: Player, world: World) {
        player.teleport(world.spawnLocation.add(0.5, 0.0, 0.5))
    }

    fun hasWorldPermission(player: Player, worldName: String): Boolean {
        val basePermission = "worldengine.world"
        val worldPermission = "$basePermission.$worldName"
        val wildcardPermission = "$basePermission.*"

        player.effectivePermissions.forEach { perm ->
            if (!perm.permission.startsWith(basePermission)) return@forEach
            if (!perm.permission.contains("*")) return@forEach
            val worldWildcard = perm.permission.substring(basePermission.length + 1)
            val regex = worldWildcard.replace("*", "[a-zA-Z0-9_-]*")
            if (worldName.matches(Regex(regex))) return true
        }

        return player.hasPermission(worldPermission) || player.hasPermission(wildcardPermission)
    }

    private val copyCleanupRelativePaths = setOf(
        "advancements", "playerdata", "stats", "session.lock", "data/raids.dat"
    )

    fun scheduleWorldCopy(
        plugin: Plugin,
        sourceWorld: World,
        newName: String,
        callback: (Result<World>) -> Unit
    ) {
        sourceWorld.save()
        Bukkit.getScheduler().runTaskLater(
            plugin,
            Runnable {
                try {
                    val sourceFolder = sourceWorld.worldFolder
                    val destinationFolder = Bukkit.getWorldContainer().resolve(newName)
                    sourceFolder.copyRecursively(destinationFolder, true)
                    destinationFolder.resolve("uid.dat").delete()
                    copyCleanupRelativePaths.forEach { relative ->
                        destinationFolder.resolve(relative).takeIf { it.exists() }?.delete()
                    }
                    copyWorldGeneratorConfig(sourceWorld.name, newName)
                    val created = Bukkit.createWorld(WorldCreator(newName).copy(sourceWorld))
                    if (created == null) {
                        callback(Result.failure(IllegalStateException("Failed to load copied world '$newName'")))
                    } else {
                        callback(Result.success(created))
                    }
                } catch (t: Throwable) {
                    callback(Result.failure(t))
                }
            },
            20L
        )
    }
}
