package com.liamxsage.worldengine

import com.liamxsage.worldengine.commands.createWorldCommand
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.jetbrains.annotations.ApiStatus

class CommandBootstrapper : PluginBootstrap {

    override fun bootstrap(context: BootstrapContext) {
        val manager = context.lifecycleManager

        manager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val commands = event.registrar()

            commands.register(
                createWorldCommand(),
                "Manage your worlds",
                listOf("w")
            )
        }
    }

}
