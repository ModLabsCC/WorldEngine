package cc.modlabs.worldengine

import cc.modlabs.worldengine.api.DefaultWorldEngineApi
import cc.modlabs.worldengine.api.WorldEngineApi
import cc.modlabs.worldengine.cache.MessageCache
import cc.modlabs.kpaper.main.Feature
import cc.modlabs.kpaper.main.KPlugin
import cc.modlabs.kpaper.main.featureConfig
import org.bukkit.Bukkit
import org.bukkit.generator.ChunkGenerator
import org.bukkit.plugin.ServicePriority
import kotlin.system.measureTimeMillis

internal fun disabledKPaperFeatures() = featureConfig {
    Feature.entries.forEach { feature(it, false) }
}

class WorldEngine : KPlugin() {

    override val featureConfig = disabledKPaperFeatures()

    companion object {
        lateinit var instance: WorldEngine
            private set

        val api: WorldEngineApi
            get() = Bukkit.getServicesManager().getRegistration(WorldEngineApi::class.java)?.provider
                ?: error("WorldEngine API is not registered; is the plugin enabled?")
    }

    override fun load() {
        instance = this
    }

    override fun startup() {
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

    override fun shutdown() {
        Bukkit.getServicesManager().unregisterAll(this)
    }

    override fun getDefaultWorldGenerator(worldName: String, id: String?): ChunkGenerator? {
        if (id == null) return null
        val clazz = Class.forName(id, false, javaClass.classLoader).asSubclass(ChunkGenerator::class.java)
        return clazz.getDeclaredConstructor().newInstance()
    }
}