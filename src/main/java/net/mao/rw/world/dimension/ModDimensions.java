package net.mao.rw.world.dimension;

import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.MultiNoiseBiomeSource;
import net.minecraft.world.biome.source.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.biome.source.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.gen.chunk.*;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.mao.rw.ResourceWorld;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.dimension.DimensionTypes;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.util.Random;

public class ModDimensions {
    private static RuntimeWorldHandle tempWorldHandle;

    public static void register() {
        ResourceWorld.LOGGER.info("Initializing ModDimensions for " + ResourceWorld.MOD_ID);
        // Initialization logic if needed
        // Register event to trigger once the server starts
        ServerLifecycleEvents.SERVER_STARTED.register((MinecraftServer server) -> {
            ModDimensions.createCustomDimension(server);
        });

    }

    public static void createCustomDimension(MinecraftServer server) {
        Fantasy fantasy = Fantasy.get(server);
        DynamicRegistryManager registryManager = server.getRegistryManager();

        // Generate a unique random seed
        long customSeed = new Random().nextLong();

        // Set up BiomeSource (for Overworld-like biomes)
        Registry<MultiNoiseBiomeSourceParameterList> multiNoiseBiomeSourceParameterListRegistry =
                registryManager.get(RegistryKeys.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST);
        RegistryEntry<MultiNoiseBiomeSourceParameterList> multiNoiseBiomeSourceParameterListEntry =
                multiNoiseBiomeSourceParameterListRegistry.getEntry(MultiNoiseBiomeSourceParameterLists.OVERWORLD)
                        .orElseThrow(() -> new IllegalStateException("Overworld parameter list not found"));

        var biomeSource = MultiNoiseBiomeSource.create(multiNoiseBiomeSourceParameterListEntry);

        // Set up ChunkGeneratorSettings (for Overworld-like terrain)
        Registry<ChunkGeneratorSettings> chunkGeneratorSettingsRegistry =
                registryManager.get(RegistryKeys.CHUNK_GENERATOR_SETTINGS);
        RegistryEntry<ChunkGeneratorSettings> chunkGeneratorSettingsEntry =
                chunkGeneratorSettingsRegistry.getEntry(ChunkGeneratorSettings.OVERWORLD)
                        .orElseThrow(() -> new IllegalStateException("Missing Overworld parameter list"));

        // Create a runtime world config for Fantasy
        RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
                .setDifficulty(Difficulty.HARD)
                .setShouldTickTime(true)
                .setDimensionType(DimensionTypes.OVERWORLD) // Set as Overworld dimension
                .setSeed(customSeed)
                .setGenerator(new NoiseChunkGenerator(biomeSource, chunkGeneratorSettingsEntry)); // Use NoiseChunkGenerator

        // Create the dimension
        //RegistryKey<World> dimensionKey = RegistryKey.of(RegistryKeys.WORLD, new Identifier(ResourceWorld.MOD_ID, "dimension"));
        tempWorldHandle = fantasy.openTemporaryWorld(worldConfig);

        // set a block in our created temporary world!
        ServerWorld world = tempWorldHandle.asWorld();
        world.setBlockState(BlockPos.ORIGIN, Blocks.BEDROCK.getDefaultState());

        ResourceWorld.LOGGER.info("Created temporary overworld!");

        // we don't need the world anymore, delete it!
        //worldHandle.delete();
    }

    public static ServerWorld getTempWorld() {
        return tempWorldHandle != null ? tempWorldHandle.asWorld() : null;
    }
}