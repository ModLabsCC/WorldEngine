package cc.modlabs.worldengine.api

import cc.modlabs.worldengine.WorldEngine
import cc.modlabs.worldengine.cache.MessageCache
import cc.modlabs.worldengine.world.WorldOperations
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.generator.ChunkGenerator
import org.bukkit.plugin.Plugin

class DefaultWorldEngineApi : WorldEngineApi {

    override fun plugin(): Plugin = WorldEngine.instance

    override fun reloadMessages() {
        MessageCache.loadCache()
    }

    override fun isWorldLoaded(name: String): Boolean = WorldOperations.isWorldLoaded(name)

    override fun getWorld(name: String): World? = WorldOperations.getWorld(name)

    override fun getOrLoadWorld(name: String): World? = WorldOperations.getOrLoadWorld(name)

    override fun createWorld(name: String, generator: ChunkGenerator?): World? =
        WorldOperations.createWorld(name, generator)

    override fun registerGeneratorInBukkitConfig(worldName: String, generator: ChunkGenerator) {
        WorldOperations.registerGeneratorInBukkitConfig(worldName, generator)
    }

    override fun copyWorldGeneratorConfig(sourceWorldName: String, newWorldName: String) {
        WorldOperations.copyWorldGeneratorConfig(sourceWorldName, newWorldName)
    }

    override fun teleportToWorldSpawn(player: Player, world: World) {
        WorldOperations.teleportToWorldSpawn(player, world)
    }

    override fun scheduleWorldCopy(sourceWorld: World, newName: String, callback: (Result<World>) -> Unit) {
        WorldOperations.scheduleWorldCopy(WorldEngine.instance, sourceWorld, newName, callback)
    }

    override fun hasWorldAccess(player: Player, worldName: String): Boolean =
        WorldOperations.hasWorldPermission(player, worldName)
}
