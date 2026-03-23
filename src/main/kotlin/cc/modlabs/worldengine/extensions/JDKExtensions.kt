package cc.modlabs.worldengine.extensions

import cc.modlabs.worldengine.WorldEngine
import org.slf4j.LoggerFactory

fun <T : Any> T.getLogger(): org.slf4j.Logger {
    return LoggerFactory.getLogger(WorldEngine::class.java)
}