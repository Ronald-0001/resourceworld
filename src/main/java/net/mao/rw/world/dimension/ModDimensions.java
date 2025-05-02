package net.mao.rw.world.dimension;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
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

import java.util.*;
import java.util.concurrent.CompletableFuture;

public abstract class ModDimensions {
    public static Map<Identifier, ModDimensions> Dimensions = new HashMap<>();
    public static final String DimensionHandle = "resource";
    public Identifier DimensionIdentifier = new Identifier(DimensionHandle, this.GetDimensionName());
    private RuntimeWorldHandle WorldHandle;

    // Suggest only dimensions from registry (you can filter here)
    public static final SuggestionProvider<ServerCommandSource> DIMENSION_SUGGESTIONS = ModDimensions::suggestDimensionIds;
    public static CompletableFuture<Suggestions> suggestDimensionIds(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        return CompletableFuture.supplyAsync(() -> {
            for (Identifier identifier : Dimensions.keySet()) {
                builder.suggest(identifier.toString());
            }
            return builder.build();
        });
    }

    public static void register() {
        new ResourceOverworld();
    }

    ModDimensions() {
        ResourceWorld.LOGGER.info("Initializing ModDimension {}:{}!", DimensionHandle, this.GetDimensionName());
        ServerLifecycleEvents.SERVER_STARTED.register(this::createDimension);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::quickDeleteWorld);
    }

    public String GetDimensionName() {
        return "world";
    }

    protected long GetSeed(MinecraftServer server) {
        return new Random().nextLong();
    }

    protected BiomeSource GetBiomeSource(MinecraftServer server) {
        DynamicRegistryManager registryManager = server.getRegistryManager();

        Registry<MultiNoiseBiomeSourceParameterList> multiNoiseBiomeSourceParameterListRegistry =
                registryManager.get(RegistryKeys.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST);
        RegistryEntry<MultiNoiseBiomeSourceParameterList> multiNoiseBiomeSourceParameterListEntry =
                multiNoiseBiomeSourceParameterListRegistry.getEntry(MultiNoiseBiomeSourceParameterLists.OVERWORLD)
                        .orElseThrow(() -> new IllegalStateException("Overworld parameter list not found"));

        return MultiNoiseBiomeSource.create(multiNoiseBiomeSourceParameterListEntry);
    }

    protected RegistryEntry<ChunkGeneratorSettings> GetChunkGenSettings(MinecraftServer server) {
        DynamicRegistryManager registryManager = server.getRegistryManager();

        Registry<ChunkGeneratorSettings> chunkGeneratorSettingsRegistry =
                registryManager.get(RegistryKeys.CHUNK_GENERATOR_SETTINGS);
        return chunkGeneratorSettingsRegistry.getEntry(ChunkGeneratorSettings.OVERWORLD)
                .orElseThrow(() -> new IllegalStateException("Missing Overworld parameter list"));
    }

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

    public void quickDeleteWorld(MinecraftServer server) {
        if (Dimensions.containsKey(this.DimensionIdentifier)) {
            Dimensions.remove(this.DimensionIdentifier);
            String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8); // 8 random chars
            this.DimensionIdentifier = new Identifier(DimensionHandle, this.GetDimensionName() + "-" + uuid);
        }
        if (this.WorldHandle != null) {
            this.WorldHandle.delete();  // Deletes world data from disk
            this.WorldHandle = null;
            ResourceWorld.LOGGER.info("Deleted {}:{}!", DimensionHandle, this.GetDimensionName());
        }
    }

    private void createDimension(MinecraftServer server) {
        this.quickDeleteWorld(server);

        Fantasy fantasy = Fantasy.get(server);
        RuntimeWorldConfig worldConfig = this.GetWorldConfig(server);

        this.WorldHandle = fantasy.getOrOpenPersistentWorld(this.DimensionIdentifier, worldConfig);

        ServerWorld world = this.WorldHandle.asWorld();
        if (world == null) {
            ResourceWorld.LOGGER.error("World handle did not return a valid ServerWorld!");
        }

        Dimensions.put(this.DimensionIdentifier, this);
        ResourceWorld.LOGGER.info("Created {}:{}!", DimensionHandle, this.GetDimensionName());
    }

    public void reset(MinecraftServer server) {
        try {
            this.createDimension(server);
        } catch (Exception e) {
            ResourceWorld.LOGGER.info(e.toString());
        }
    }

    public ServerWorld getWorld() {
        return this.WorldHandle != null ? this.WorldHandle.asWorld() : null;
    }

}