package cc.modlabs.worldengine

import cc.modlabs.worldengine.cache.MessageCache
import org.bukkit.generator.ChunkGenerator
import org.bukkit.plugin.java.JavaPlugin
import kotlin.system.measureTimeMillis

class WorldEngine : JavaPlugin() {

    companion object {
        lateinit var instance: WorldEngine
            private set
    }

    init {
        instance = this
    }

    override fun onEnable() {
        logger.info("Enabling WorldEngine...")

        // Copy the messages file to the plugins folder
        saveResource("messages.yml", false)

        // Plugin startup logic
        val time = measureTimeMillis {
            MessageCache.loadCache()
        }
        logger.info("Plugin enabled in $time ms")
        logger.info("WorldEngine is now managing your world!")
    }

    override fun getDefaultWorldGenerator(worldName: String, id: String?): ChunkGenerator? {
        if (id == null) return null
        val clazz = Class.forName(id)
        return clazz.getDeclaredConstructor().newInstance() as ChunkGenerator
    }
}