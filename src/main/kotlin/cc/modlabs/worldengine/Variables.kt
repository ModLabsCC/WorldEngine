package cc.modlabs.worldengine

import cc.modlabs.worldengine.cache.MessageCache

val PREFIX: String
    get() = MessageCache.getMessage("general.prefix", default = "<gradient:#4a69bd:#6a89cc>WorldEngine</gradient> <color:#4a628f>>></color> <color:#b2c2d4>")