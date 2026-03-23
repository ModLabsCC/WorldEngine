package cc.modlabs.worldengine.api

import cc.modlabs.worldengine.world.ChunkGenerators
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.generator.ChunkGenerator
import org.bukkit.plugin.Plugin

/**
 * Stable entry point for other plugins. Obtain via [cc.modlabs.worldengine.WorldEngine.api]
 * or `Bukkit.getServicesManager().getRegistration(WorldEngineApi::class.java)?.provider`.
 *
 * Calls do not enforce player-facing permission checks; authorize callers in your own code.
 */
interface WorldEngineApi {

    fun plugin(): Plugin

    /** Reload `messages.yml` into memory (same as `/worldengine reload`). */
    fun reloadMessages()

    /** Whether a world with this name is currently loaded. */
    fun isWorldLoaded(name: String): Boolean

    fun getWorld(name: String): World?

    /** Load from folder or create if missing (same as `/world <name>` create path). */
    fun getOrLoadWorld(name: String): World?

    /**
     * Create a world with an optional chunk generator.
     * When [generator] is non-null, registers it in `bukkit.yml` like `/world generate`.
     */
    fun createWorld(name: String, generator: ChunkGenerator? = null): World?

    fun registerGeneratorInBukkitConfig(worldName: String, generator: ChunkGenerator)

    fun copyWorldGeneratorConfig(sourceWorldName: String, newWorldName: String)

    fun teleportToWorldSpawn(player: Player, world: World)

    /**
     * Async-ish copy on the main thread after a short delay (same behavior as `/world copy`).
     * [callback] runs on the main thread.
     */
    fun scheduleWorldCopy(sourceWorld: World, newName: String, callback: (Result<World>) -> Unit)

    /** Same rules as command permission checks for `/world`. */
    fun hasWorldAccess(player: Player, worldName: String): Boolean

    /** Built-in preset names (`empty`, `flat`, `ocean`) to generators. */
    fun presetGenerators(): Map<String, ChunkGenerator> = ChunkGenerators.presets

    /** Parse preset id or `PluginName:id` like the `/world generate` argument. */
    fun resolveChunkGenerator(spec: String): ChunkGenerator = ChunkGenerators.resolve(spec)
}
