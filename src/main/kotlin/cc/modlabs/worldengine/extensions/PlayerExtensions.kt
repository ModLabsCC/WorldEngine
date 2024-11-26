package cc.modlabs.worldengine.extensions

import cc.modlabs.worldengine.PREFIX
import cc.modlabs.worldengine.cache.MessageCache
import dev.fruxz.stacked.text
import org.bukkit.*
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@Deprecated("Use sendMessagePrefixed(key: String, placeholders: Map<String, Any> = emptyMap<String, String>(), default: String = key) instead")
fun Player.sendMessagePrefixed(message: String) {
    this.sendMessage(text(PREFIX + message))
}

fun Player.sendMessagePrefixed(key: String, placeholders: Map<String, Any> = emptyMap<String, String>(), default: String = key) {
    this.sendMessage(text(PREFIX + MessageCache.getMessage(key, this, placeholders, default)))
}

@Deprecated("Use sendMessagePrefixed(key: String, placeholders: Map<String, Any> = emptyMap<String, String>(), default: String = key) instead")
fun CommandSender.sendMessagePrefixed(message: String) = sendMessage(text(PREFIX + message))

fun CommandSender.sendMessagePrefixed(key: String, placeholders: Map<String, Any> = emptyMap<String, String>(), default: String = key) = sendMessage(text(PREFIX + MessageCache.getMessage(key, this, placeholders, default)))

@Deprecated("Use broadcastPrefixed(key: String, placeholders: Map<String, Any> = emptyMap<String, String>(), default: String = key) instead")
fun broadcastPrefixed(message: String) = Bukkit.broadcast(text(PREFIX + message))

fun broadcastPrefixed(key: String, placeholders: Map<String, Any> = emptyMap<String, String>(), default: String = key) = Bukkit.broadcast(text(PREFIX + MessageCache.getMessage(key, Bukkit.getConsoleSender(), placeholders, default)))

fun CommandSender.sendEmtpyLine() = sendMessage(text(" "))

fun Player.soundExecution() {
    playSound(location, Sound.ENTITY_ITEM_PICKUP, .75F, 2F)
    playSound(location, Sound.ITEM_ARMOR_EQUIP_LEATHER, .25F, 2F)
    playSound(location, Sound.ITEM_ARMOR_EQUIP_CHAIN, .1F, 2F)
}


fun Player.sendDeniedSound() = playSound(location, "minecraft:block.note_block.bass", 1f, 1f)

fun Player.sendSuccessSound() = playSound(location, "minecraft:block.note_block.pling", 1f, 1f)

fun Player.sendTeleportSound() = playSound(location, "minecraft:block.note_block.harp", 1f, 1f)

fun Player.sendOpenSound() = playSound(location, "minecraft:block.note_block.chime", 1f, 1f)