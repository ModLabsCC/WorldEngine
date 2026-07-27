package cc.modlabs.worldengine.utils

import cc.modlabs.worldengine.WorldEngine
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
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
        } catch (e: Exception) {
            throw IllegalStateException("Could not save $path", e)
        }
    }

    init {
        val file = File(path)
        try {
            file.parentFile?.mkdirs()
            if (!file.exists()) file.createNewFile()
            load(file)
        } catch (e: Exception) {
            throw IllegalStateException("Could not load $path", e)
        }
    }
}