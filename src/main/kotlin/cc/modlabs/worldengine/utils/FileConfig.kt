package cc.modlabs.worldengine.utils

import cc.modlabs.worldengine.WorldEngine
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException
import java.nio.file.FileSystems

class FileConfig(fileName: String, fromRoot: Boolean = false) : YamlConfiguration() {

    private val separator: String = FileSystems.getDefault().separator
    private val path: String = if (fromRoot) {
        fileName
    } else {
        "plugins${separator}${WorldEngine.instance.name}$separator$fileName"
    }

    fun saveConfig() {
        try {
            save(path)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    init {
        val file = File(path)
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        try {
            if (!file.exists()) {
                file.createNewFile()
            }
            load(path)
        } catch (_: IOException) {
            // Do nothing
        } catch (e: InvalidConfigurationException) {
            e.printStackTrace()
        }
    }

}