package cc.modlabs.worldengine.world

import cc.modlabs.worldengine.WorldEngine
import cc.modlabs.worldengine.utils.FileConfig
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.generator.ChunkGenerator
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.Locale

object WorldOperations {

    private fun managedPlugin(): Plugin = WorldEngine.instance

    private fun managedNamespace(): String = managedPlugin().name.lowercase(Locale.ROOT)

    private fun managedLegacyFolderPrefix(): String = "${managedNamespace()}_"

    /**
     * Bukkit `worlds:` section uses keys like `worldengine_pinewood`; commands and permissions use the short
     * dimension key `pinewood` when it is our namespace.
     */
    fun preferShortManagedWorldName(sectionOrFolderName: String): String {
        val prefix = managedLegacyFolderPrefix()
        if (sectionOrFolderName.length > prefix.length &&
            sectionOrFolderName.startsWith(prefix, ignoreCase = true)
        ) {
            return sectionOrFolderName.substring(prefix.length)
        }
        return sectionOrFolderName
    }

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

    /**
     * Dimension / extra-level folders often have no `level.dat` at the folder root (only chunk data).
     * Paper writes `paper-world.yml` per dimension; treat it as a reliable marker alongside chunk stores.
     */
    private fun looksLikeDimensionOrWorldData(dir: File): Boolean {
        if (!dir.isDirectory) return false
        if (looksLikeWorldSaveDirectory(dir)) return true
        if (dir.resolve("paper-world.yml").isFile) return true
        if (dir.resolve("region").isDirectory) return true
        if (dir.resolve("entities").isDirectory) return true
        if (dir.resolve("poi").isDirectory) return true
        return false
    }

    /** True if this directory is probably a main level folder we should scan for `dimensions/`. */
    private fun looksLikeLevelRootForScan(levelRoot: File): Boolean {
        if (!levelRoot.isDirectory) return false
        if (looksLikeWorldSaveDirectory(levelRoot)) return true
        val pluginDims = levelRoot.resolve("dimensions").resolve(managedNamespace())
        if (pluginDims.isDirectory && !pluginDims.listFiles().isNullOrEmpty()) return true
        return false
    }

    /**
     * Names from disk: top-level level folders and `dimensions/&lt;ns&gt;/&lt;name&gt;` (Paper 26+).
     */
    private fun collectDiscoveredWorldNamesFromDisk(): Set<String> {
        val out = linkedSetOf<String>()
        val container = Bukkit.getWorldContainer()
        val pluginPrefix = managedLegacyFolderPrefix()

        fun considerLevelRoot(levelRoot: File) {
            if (!levelRoot.isDirectory || !looksLikeLevelRootForScan(levelRoot)) return
            out.add(levelRoot.name)
            if (levelRoot.name.startsWith(pluginPrefix)) {
                val short = levelRoot.name.removePrefix(pluginPrefix)
                if (short.isNotEmpty()) out.add(short)
            }
            val dimRoot = levelRoot.resolve("dimensions")
            if (!dimRoot.isDirectory) return
            dimRoot.listFiles()?.forEach { namespaceDir ->
                if (!namespaceDir.isDirectory) return@forEach
                val isOurNamespace = namespaceDir.name.equals(managedNamespace(), ignoreCase = true)
                namespaceDir.listFiles()?.forEach { worldDir ->
                    if (!worldDir.isDirectory) return@forEach
                    val nonEmpty = !worldDir.listFiles().isNullOrEmpty()
                    if (looksLikeDimensionOrWorldData(worldDir) || (isOurNamespace && nonEmpty)) {
                        out.add(worldDir.name)
                    }
                }
            }
        }

        container.listFiles()?.forEach { considerLevelRoot(it) }
        return out
    }

    /**
     * World names listed under `worlds:` in `bukkit.yml` (server root), including `worldengine_*` keys.
     */
    private fun collectWorldNamesFromBukkitYml(): Set<String> {
        val out = linkedSetOf<String>()
        val file = File("bukkit.yml")
        if (!file.isFile) return out
        val yaml = YamlConfiguration.loadConfiguration(file)
        val worlds = yaml.getConfigurationSection("worlds") ?: return out
        val prefix = managedLegacyFolderPrefix()
        for (key in worlds.getKeys(false)) {
            if (key.isBlank()) continue
            out.add(key)
            if (key.length > prefix.length && key.startsWith(prefix, ignoreCase = true)) {
                val short = key.substring(prefix.length)
                if (short.isNotEmpty()) out.add(short)
            }
        }
        return out
    }

    /**
     * Union of on-disk discovery and `bukkit.yml` `worlds:` keys (so configured worlds always tab-complete).
     */
    fun collectDiscoveredWorldNames(): List<String> {
        val out = linkedSetOf<String>()
        out.addAll(collectDiscoveredWorldNamesFromDisk())
        out.addAll(collectWorldNamesFromBukkitYml())
        return out.toList()
    }

    /**
     * Normalizes a `/world` argument to the canonical name we use elsewhere (permissions, messages).
     * Returns null if no loaded world and no matching on-disk save was found.
     */
    fun canonicalWorldArgument(input: String): String? {
        resolveWorld(input)?.let { return userFacingWorldName(it) }
        for (candidate in collectDiscoveredWorldNames()) {
            if (candidate.equals(input, ignoreCase = true)) {
                return preferShortManagedWorldName(candidate)
            }
        }
        return null
    }

    fun resolveWorld(name: String): World? {
        Bukkit.getWorld(name)?.let { return it }
        val key = separateLevelKey(managedPlugin(), name)
        Bukkit.getWorld(key)?.let { return it }
        for (w in Bukkit.getWorlds()) {
            if (w.name.equals(name, ignoreCase = true)) return w
            if (userFacingWorldName(w).equals(name, ignoreCase = true)) return w
        }
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
        val legacySectionPermission = "$basePermission.${managedLegacyFolderPrefix()}$worldName"

        player.effectivePermissions.forEach { perm ->
            if (!perm.permission.startsWith(basePermission)) return@forEach
            if (!perm.permission.contains("*")) return@forEach
            val worldWildcard = perm.permission.substring(basePermission.length + 1)
            val regex = worldWildcard.replace("*", "[a-zA-Z0-9_-]*")
            if (worldName.matches(Regex(regex))) return true
            val legacyKey = "${managedLegacyFolderPrefix()}$worldName"
            if (legacyKey.matches(Regex(regex))) return true
        }

        if (!worldName.startsWith(managedLegacyFolderPrefix(), ignoreCase = true) &&
            player.hasPermission(legacySectionPermission)
        ) {
            return true
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
