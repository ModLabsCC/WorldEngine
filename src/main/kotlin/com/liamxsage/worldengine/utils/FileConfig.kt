package com.liamxsage.worldengine.utils

import com.liamxsage.worldengine.WorldEngine
import dev.fruxz.ascend.extension.createFileAndDirectories
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException
import java.nio.file.FileSystems

class FileConfig(fileName: String, fromRoot: Boolean = false) : YamlConfiguration() {

    private var seperator: String = FileSystems.getDefault().separator ?: "/"
    private val path: String = if (fromRoot) {
        fileName
    } else {
        "plugins${seperator}${WorldEngine.instance.name}$seperator$fileName"
    }

    fun saveConfig() {
        try {
            save(path)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    init {
        File(path).createFileAndDirectories()
        val file = File(path)
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