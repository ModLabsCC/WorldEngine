package cc.modlabs.worldengine

import cc.modlabs.worldengine.commands.createWorldCommand
import cc.modlabs.worldengine.commands.createWorldEngineCommand
import cc.modlabs.worldengine.commands.createWorldInfoCommand
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents

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

            commands.register(
                createWorldEngineCommand(),
                "Plugin management",
            )

            commands.register(
                createWorldInfoCommand(),
                "World information",
                listOf("wi")
            )
        }
    }

}
