package cc.modlabs.worldengine.cache

import cc.modlabs.worldengine.extensions.getLogger
import cc.modlabs.worldengine.utils.FileConfig
import dev.fruxz.stacked.extension.asStyledString
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

object MessageCache {

    private val cacheLock: ReadWriteLock = ReentrantReadWriteLock()
    private var cache: Map<String, String> = mapOf()

    fun getMessage(key: String, commandSender: CommandSender = Bukkit.getConsoleSender(), placeholders: Map<String, Any> = emptyMap<String, String>(), default: String = key): String {
        cacheLock.readLock().lock()
        var message = cache[key]
        cacheLock.readLock().unlock()
        message = message ?: createMessage(key, default)

        for ((placeholder, value) in placeholders) {
            message = message!!.replace("{$placeholder}", value.toString())
        }
        message = commandSender.replaceSenderPlaceholders(message)


        return message
    }

    /**
     * This is a developer function that creates a message with the specified key and default value.
     * It is used to create messages that are not present in the messages.yml file.
     * This should only be used for development purposes.
     */
    private fun createMessage(key: String, default: String): String {
        val messagesFile = FileConfig("messages.yml")

        if(messagesFile.contains(key)) {
            return messagesFile.getString(key) ?: default
        }

        messagesFile[key] = default
        messagesFile.saveConfig()

        cacheLock.writeLock().lock()
        cache = cache.plus(key to default)
        cacheLock.writeLock().unlock()

        getLogger().info("Created message $key with default value $default")
        return default
    }

    fun loadCache() {
        cacheLock.writeLock().lock()
        cache = mapOf()

        val tempCache = mutableMapOf<String, String>()
        val messages = FileConfig("messages.yml")
        messages.getKeys(true).forEach {
            val message = messages.getString(it) ?: return@forEach
            tempCache[it] = message
        }

        cache = tempCache
        cacheLock.writeLock().unlock()
    }

    private fun CommandSender.replaceSenderPlaceholders(inputMessage: String): String {
        var message = inputMessage
        message = message.replace("{player}", this.name)

        if (this is Player) {
            message = message.replace("{displayname}", this.displayName().asStyledString)

            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                message = PlaceholderAPI.setPlaceholders(player, message)
            }
        }
        return message
    }
}