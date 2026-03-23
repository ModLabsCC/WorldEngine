package cc.modlabs.worldengine

import cc.modlabs.worldengine.api.DefaultWorldEngineApi
import cc.modlabs.worldengine.api.WorldEngineApi
import cc.modlabs.worldengine.cache.MessageCache
import org.bukkit.Bukkit
import org.bukkit.generator.ChunkGenerator
import org.bukkit.plugin.ServicePriority
import org.bukkit.plugin.java.JavaPlugin
import kotlin.system.measureTimeMillis

class WorldEngine : JavaPlugin() {

    companion object {
        lateinit var instance: WorldEngine
            private set

        val api: WorldEngineApi
            get() = Bukkit.getServicesManager().getRegistration(WorldEngineApi::class.java)?.provider
                ?: error("WorldEngine API is not registered; is the plugin enabled?")
    }

    init {
        instance = this
    }

    override fun onEnable() {
        logger.info("Enabling WorldEngine...")

        Bukkit.getServicesManager().register(
            WorldEngineApi::class.java,
            DefaultWorldEngineApi(),
            this,
            ServicePriority.Normal
        )

        // Copy the messages file to the plugins folder
        saveResource("messages.yml", false)

        // Plugin startup logic
        val time = measureTimeMillis {
            MessageCache.loadCache()
        }
        logger.info("Plugin enabled in $time ms")
        logger.info("WorldEngine is now managing your world!")
    }

    override fun onDisable() {
        Bukkit.getServicesManager().unregisterAll(this)
    }

    override fun getDefaultWorldGenerator(worldName: String, id: String?): ChunkGenerator? {
        if (id == null) return null
        val clazz = Class.forName(id)
        return clazz.getDeclaredConstructor().newInstance() as ChunkGenerator
    }
}