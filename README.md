<br />
<div align="center">
  <a href="https://discord.com/users/216487432667791360">
    <img src="https://github.com/ModLabsCC/WorldEngine/blob/main/.github/assets/worldengine.png" alt="Logo" width="200" height="200">
</a>

<h3 align="center">WorldEngine</h3>

  <p align="center">
    WorldManagement kept ahead of the curve, always planning a step further.
    <br />
    <br />
    <a href="https://liamxsage.com">Website</a>
    ·
    <a href="https://discord.com/users/216487432667791360"><strong>Contact</strong></a>
  </p>
</div>

## API usage

WorldEngine exposes a small Bukkit service API for other plugins:

- Service interface: `cc.modlabs.worldengine.api.WorldEngineApi`
- Convenience accessor: `cc.modlabs.worldengine.WorldEngine.api`

### Kotlin example

```kotlin
import cc.modlabs.worldengine.WorldEngine

val worldEngine = Bukkit.getPluginManager().getPlugin("WorldEngine") as? WorldEngine
    ?: return
val api = worldEngine.api

// Load or create world (same behavior as /world <name>)
val world = api.getOrLoadWorld("event_world") ?: return

// Teleport player to world spawn
api.teleportToWorldSpawn(player, world)

// Create world with a preset generator
val flatGenerator = api.resolveChunkGenerator("flat")
api.createWorld("flat_event", flatGenerator)

// Copy world asynchronously-ish on main thread callback
api.scheduleWorldCopy(world, "event_world_backup") { result ->
    result.onSuccess { copied ->
        player.sendMessage("Copied world: ${copied.name}")
    }.onFailure { error ->
        player.sendMessage("Copy failed: ${error.message}")
    }
}
```

### Java example

```java
import cc.modlabs.worldengine.api.WorldEngineApi;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

RegisteredServiceProvider<WorldEngineApi> registration =
        Bukkit.getServicesManager().getRegistration(WorldEngineApi.class);
if (registration == null) {
    return;
}

WorldEngineApi api = registration.getProvider();
var world = api.getOrLoadWorld("event_world");
if (world != null) {
    api.teleportToWorldSpawn(player, world);
}
```

### Notes

- API methods are designed to mirror existing command/startup features.
- Permission enforcement is not automatic for API callers; handle authorization in your plugin.
- Built-in generator ids are `empty`, `flat`, and `ocean`.

## Dependency (for plugin developers)

WorldEngine is published to ModLabs Nexus.

### Gradle Kotlin DSL (`build.gradle.kts`)

```kotlin
repositories {
    maven("https://nexus.modlabs.cc/repository/maven-mirrors/")
}

dependencies {
    compileOnly("cc.modlabs.worldengine:WorldEngine:<version>")
}
```
