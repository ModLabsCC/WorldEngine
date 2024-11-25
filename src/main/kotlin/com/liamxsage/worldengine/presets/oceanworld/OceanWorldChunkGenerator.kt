package com.liamxsage.worldengine.presets.oceanworld

import com.liamxsage.worldengine.presets.SimplexNoise
import org.bukkit.HeightMap
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.generator.BiomeProvider
import org.bukkit.generator.BlockPopulator
import org.bukkit.generator.ChunkGenerator
import org.bukkit.generator.WorldInfo
import java.util.*

class OceanWorldChunkGenerator : ChunkGenerator() {

    override fun shouldGenerateNoise(): Boolean = false

    override fun shouldGenerateCaves(): Boolean = false

    override fun shouldGenerateDecorations(): Boolean = true

    override fun shouldGenerateMobs(): Boolean = false

    override fun shouldGenerateStructures(): Boolean = false

    override fun getDefaultPopulators(world: World): List<BlockPopulator> = Collections.emptyList()

    override fun getDefaultBiomeProvider(worldInfo: WorldInfo): BiomeProvider {
        return OceanWorldBiomeProvider()
    }

    override fun getBaseHeight(worldInfo: WorldInfo, random: Random, x: Int, z: Int, heightMap: HeightMap): Int {
        // Return the base height for the ocean floor (y=32)
        return when (heightMap) {
            HeightMap.OCEAN_FLOOR -> 32
            else -> 64
        }
    }

    override fun generateBedrock(worldInfo: WorldInfo, random: Random, chunkX: Int, chunkZ: Int, chunkData: ChunkData) {
        // Bedrock at y=-63 (the bottom layer)
        for (x in 0..15) {
            for (z in 0..15) {
                chunkData.setBlock(x, -63, z, Material.BEDROCK)
            }
        }
    }

    override fun generateNoise(worldInfo: WorldInfo, random: Random, chunkX: Int, chunkZ: Int, chunkData: ChunkData) {
        generateOceanFloor(chunkX, chunkZ, chunkData)
    }

    override fun generateSurface(worldInfo: WorldInfo, random: Random, chunkX: Int, chunkZ: Int, chunkData: ChunkData) {
        // No need for separate surface generation as it's handled in the noise generation
    }

    private fun generateOceanFloor(chunkX: Int, chunkZ: Int, chunkData: ChunkData) {
        for (x in 0..15) {
            for (z in 0..15) {
                val worldX = chunkX * 16 + x
                val worldZ = chunkZ * 16 + z
                val height = getHeight(worldX, worldZ)

                // Fülle das Chunk mit SAND bis zur generierten Höhe
                for (y in chunkData.minHeight..height) {
                    chunkData.setBlock(x, y, z, Material.SAND)
                }

                // Fülle mit Wasser bis y=64
                for (y in height + 1..64) {
                    chunkData.setBlock(x, y, z, Material.WATER)
                }
            }
        }
    }

    companion object {
        fun getHeight(worldX: Int, worldZ: Int): Int {
            // Verwende die Simplex-Noise Funktion, um die Höhe zu bestimmen
            val noiseValue = SimplexNoise.noise(worldX * 0.001, worldZ * 0.001)
            val baseHeight = 11
            val heightOffset = (noiseValue * 51).toInt()  // Kontrolliere die Höhe der Hügel

            val height = baseHeight + heightOffset
            return height
        }
    }
}