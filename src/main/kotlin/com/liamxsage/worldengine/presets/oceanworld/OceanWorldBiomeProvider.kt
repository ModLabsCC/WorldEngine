package com.liamxsage.worldengine.presets.oceanworld;

import org.bukkit.block.Biome
import org.bukkit.generator.BiomeProvider
import org.bukkit.generator.WorldInfo

class OceanWorldBiomeProvider : BiomeProvider() {
    override fun getBiome(worldInfo: WorldInfo, x: Int, y: Int, z: Int): Biome {
        return when(OceanWorldChunkGenerator.getHeight(x, z)) {
            in 45..63 -> Biome.WARM_OCEAN
            in 35..45 -> Biome.LUKEWARM_OCEAN
            in 26..35 -> Biome.DEEP_LUKEWARM_OCEAN
            in 22..26 -> Biome.OCEAN
            in 0..22 -> Biome.DEEP_OCEAN
            else -> Biome.OCEAN
        }
    }

    override fun getBiomes(worldInfo: WorldInfo): MutableList<Biome> {
        return mutableListOf(Biome.WARM_OCEAN, Biome.OCEAN, Biome.DEEP_OCEAN, Biome.DEEP_LUKEWARM_OCEAN, Biome.LUKEWARM_OCEAN)
    }
}