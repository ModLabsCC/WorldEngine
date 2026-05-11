package cc.modlabs.worldengine.world

import cc.modlabs.worldengine.WorldEngine
import cc.modlabs.worldengine.utils.FileConfig
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.entity.Player
import org.bukkit.generator.ChunkGenerator
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.Locale

object WorldOperations {

    private fun managedPlugin(): Plugin = WorldEngine.instance

    private fun managedNamespace(): String = managedPlugin().name.lowercase(Locale.ROOT)

    /**
     * Paper 26.1+: [WorldCreator] with a plain string assigns [NamespacedKey.minecraft] keys, which store
     * custom worlds as dimensions *inside* the main level folder. Separate level roots require a non-minecraft key.
     */
    fun separateLevelKey(plugin: Plugin, worldName: String): NamespacedKey =
        NamespacedKey(plugin, sanitizeWorldKey(worldName))

    private fun sanitizeWorldKey(worldName: String): String {
        val s = worldName.lowercase(Locale.ROOT)
            .replace(' ', '_')
            .replace(Regex("[^a-z0-9._-]"), "_")
            .trim('_')
        return s.ifEmpty { "world" }
    }

    /** Matches [org.bukkit.WorldCreator] `nameFromKey` — on-disk folder name for a level key. */
    fun NamespacedKey.toLegacyBukkitLevelFolderName(): String =
        if (namespace == NamespacedKey.MINECRAFT) key else "${namespace}_${key}"

    fun isManagedSeparateLevel(world: World): Boolean =
        world.key.namespace.equals(managedNamespace(), ignoreCase = true)

    /** Command / permission name: plugin-key worlds use the key; others use Bukkit legacy name. */
    fun userFacingWorldName(world: World): String =
        if (isManagedSeparateLevel(world)) world.key.key else world.name

    fun levelRootDirectoryForUserWorldName(userWorldName: String): File {
        val key = separateLevelKey(managedPlugin(), userWorldName)
        return Bukkit.getWorldContainer().resolve(key.toLegacyBukkitLevelFolderName())
    }

    /**
     * True if [dir] is the on-disk root of a level save Bukkit can open.
     * Legacy saves use `level.dat` at the root; Paper 26+ may place it under `dimensions/...`.
     */
    fun looksLikeWorldSaveDirectory(dir: File): Boolean {
        if (!dir.isDirectory) return false
        if (dir.resolve("level.dat").isFile) return true
        val dimensions = dir.resolve("dimensions")
        if (!dimensions.isDirectory) return false
        return dimensions.walkTopDown().maxDepth(8).any { it.isFile && it.name == "level.dat" }
    }

    fun resolveWorld(name: String): World? {
        Bukkit.getWorld(name)?.let { return it }
        val key = separateLevelKey(managedPlugin(), name)
        Bukkit.getWorld(key)?.let { return it }
        return null
    }

    fun isWorldLoaded(name: String): Boolean = resolveWorld(name) != null

    fun getWorld(name: String): World? = resolveWorld(name)

    fun getOrLoadWorld(name: String): World? {
        resolveWorld(name)?.let { return it }
        val container = Bukkit.getWorldContainer()
        val key = separateLevelKey(managedPlugin(), name)
        val legacyFolderName = key.toLegacyBukkitLevelFolderName()
        val dirPlain = container.resolve(name)
        val dirKeyed = container.resolve(legacyFolderName)
        if (dirPlain.isDirectory && looksLikeWorldSaveDirectory(dirPlain)) {
            return Bukkit.createWorld(WorldCreator.name(name))
        }
        if (dirKeyed.isDirectory && looksLikeWorldSaveDirectory(dirKeyed)) {
            return Bukkit.createWorld(WorldCreator.ofKey(key))
        }
        return Bukkit.createWorld(WorldCreator.ofKey(key))
    }

    fun createWorld(name: String, generator: ChunkGenerator? = null): World? {
        val key = separateLevelKey(managedPlugin(), name)
        val creator = WorldCreator.ofKey(key).generator(generator)
        return Bukkit.createWorld(creator)
    }

    private fun legacyBukkitConfigSectionKey(userFacingWorldName: String): String =
        resolveWorld(userFacingWorldName)?.name
            ?: separateLevelKey(managedPlugin(), userFacingWorldName).toLegacyBukkitLevelFolderName()

    fun registerGeneratorInBukkitConfig(worldName: String, generator: ChunkGenerator) {
        val bukkitYml = FileConfig("bukkit.yml", true)
        val worlds = bukkitYml.getConfigurationSection("worlds") ?: bukkitYml.createSection("worlds")
        val sectionKey = legacyBukkitConfigSectionKey(worldName)
        val world = worlds.getConfigurationSection(sectionKey) ?: worlds.createSection(sectionKey)
        world["generator"] = WorldEngine.instance.name + ":" + generator.javaClass.name
        worlds[sectionKey] = world
        bukkitYml["worlds"] = worlds
        bukkitYml.saveConfig()
    }

    fun copyWorldGeneratorConfig(sourceWorldName: String, newWorldName: String) {
        val bukkitYml = FileConfig("bukkit.yml", true)
        val worlds = bukkitYml.getConfigurationSection("worlds") ?: return
        val sourceKey = legacyBukkitConfigSectionKey(sourceWorldName)
        val world = worlds.getConfigurationSection(sourceKey)
            ?: worlds.getConfigurationSection(sourceWorldName)
            ?: return
        if (world["generator"] == null) return
        val destKey = legacyBukkitConfigSectionKey(newWorldName)
        worlds[destKey] = world
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
                    val key = separateLevelKey(plugin, newName)
                    val destinationFolder = Bukkit.getWorldContainer().resolve(key.toLegacyBukkitLevelFolderName())
                    sourceFolder.copyRecursively(destinationFolder, true)
                    destinationFolder.resolve("uid.dat").delete()
                    copyCleanupRelativePaths.forEach { relative ->
                        destinationFolder.resolve(relative).takeIf { it.exists() }?.delete()
                    }
                    copyWorldGeneratorConfig(sourceWorld.name, newName)
                    val created = Bukkit.createWorld(WorldCreator.ofKey(key).copy(sourceWorld))
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
