package cc.modlabs.worldengine.presets.empty

import org.bukkit.HeightMap
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.generator.BlockPopulator
import org.bukkit.generator.ChunkGenerator
import org.bukkit.generator.WorldInfo
import java.util.*

class EmptyWorldGenerator : ChunkGenerator() {

    override fun getFixedSpawnLocation(world: World, random: Random): Location {
        return Location(world, 0.5, (64 + 1).toDouble(), 0.5)
    }

    override fun getBaseHeight(worldInfo: WorldInfo, random: Random, x: Int, z: Int, heightMap: HeightMap): Int {
        return 64
    }

    override fun shouldGenerateNoise(): Boolean = false

    override fun shouldGenerateCaves(): Boolean = false

    override fun shouldGenerateDecorations(): Boolean = false

    override fun shouldGenerateMobs(): Boolean = false

    override fun shouldGenerateStructures(): Boolean = false

    override fun getDefaultPopulators(world: World): List<BlockPopulator> = listOf(EmptyLevelPopulator())

    override fun shouldGenerateStructures(worldInfo: WorldInfo, random: Random, chunkX: Int, chunkZ: Int): Boolean = false


}