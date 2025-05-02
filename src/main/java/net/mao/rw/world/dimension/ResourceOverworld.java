package net.mao.rw.world.dimension;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.MultiNoiseBiomeSource;
import net.minecraft.world.biome.source.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.biome.source.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;

import java.util.Random;

public class ResourceOverworld extends ModDimensions {
    @Override
    public String GetDimensionName() {
        return "overworld";
    }

    @Override
    protected long GetSeed(MinecraftServer server) {
        return new Random().nextLong();
    }

    @Override
    protected BiomeSource GetBiomeSource(MinecraftServer server) {
        DynamicRegistryManager registryManager = server.getRegistryManager();

        Registry<MultiNoiseBiomeSourceParameterList> multiNoiseBiomeSourceParameterListRegistry =
                registryManager.get(RegistryKeys.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST);
        RegistryEntry<MultiNoiseBiomeSourceParameterList> multiNoiseBiomeSourceParameterListEntry =
                multiNoiseBiomeSourceParameterListRegistry.getEntry(MultiNoiseBiomeSourceParameterLists.OVERWORLD)
                        .orElseThrow(() -> new IllegalStateException("Overworld parameter list not found"));

        return MultiNoiseBiomeSource.create(multiNoiseBiomeSourceParameterListEntry);
    }

    @Override
    protected RegistryEntry<ChunkGeneratorSettings> GetChunkGenSettings(MinecraftServer server) {
        DynamicRegistryManager registryManager = server.getRegistryManager();

        Registry<ChunkGeneratorSettings> chunkGeneratorSettingsRegistry =
                registryManager.get(RegistryKeys.CHUNK_GENERATOR_SETTINGS);
        return chunkGeneratorSettingsRegistry.getEntry(ChunkGeneratorSettings.OVERWORLD)
                .orElseThrow(() -> new IllegalStateException("Missing Overworld parameter list"));
    }

    @Override
    protected RuntimeWorldConfig GetWorldConfig(MinecraftServer server) {
        long seed = this.GetSeed(server);
        var biomeSource = this.GetBiomeSource(server);
        var chunkSettings = this.GetChunkGenSettings(server);

        return new RuntimeWorldConfig()
                .setDifficulty(Difficulty.HARD)
                .setShouldTickTime(true)
                .setDimensionType(DimensionTypes.OVERWORLD) // Set as Overworld dimension
                .setSeed(seed)
                .setGenerator(new NoiseChunkGenerator(biomeSource, chunkSettings)); // Use NoiseChunkGenerator
    }
}
