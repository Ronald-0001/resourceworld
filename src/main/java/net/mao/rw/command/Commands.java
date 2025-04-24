package net.mao.rw.command;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.mao.rw.ResourceWorld;
import net.mao.rw.world.dimension.ModDimensions;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.dimension.DimensionTypes;

public class Commands {
    public static void register() {
        RegisterGoToWorlds();
    }

    private static void RegisterGoToWorlds() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    CommandManager.literal("goto")
                            .requires(source -> source.hasPermissionLevel(2)) // Only ops
                            .then(CommandManager.literal("resourceworld")
                                    .executes(context -> teleportToWorld(context, "resourceworld"))
                            )
                            .then(CommandManager.literal("overworld")
                                    .executes(context -> teleportToWorld(context, "overworld"))
                            )
                            .then(CommandManager.literal("rw")  // Alias for resourceworld
                                    .executes(context -> teleportToWorld(context, "resourceworld"))
                            )
                            .then(CommandManager.literal("ow")  // Alias for overworld
                                    .executes(context -> teleportToWorld(context, "overworld"))
                            )
            );
        });
    }

    private static int teleportToWorld(CommandContext<ServerCommandSource> context, String worldName) {
        ServerWorld targetWorld = null;

        // Fetch the world based on the worldName
        if ("resourceworld".equalsIgnoreCase(worldName)) {
            targetWorld = ModDimensions.getTempWorld();  // Get resource world
        } else if ("overworld".equalsIgnoreCase(worldName)) {
            targetWorld = context.getSource().getServer().getWorld(World.OVERWORLD);  // Get overworld
        }

        // If the target world doesn't exist, return an error
        if (targetWorld == null) {
            context.getSource().sendError(Text.literal("The world '" + worldName + "' is not available!"));
            return 0;
        }

        // prep general variables
        ServerPlayerEntity player = context.getSource().getPlayer();
        BlockPos targetPos = targetWorld.getSpawnPos();  // Fallback coordinates where we want to teleport

        // Check if the player’s spawn dimension is the same as the target world
        RegistryKey<World> spawnDimension = player.getSpawnPointDimension();
        if (spawnDimension.equals(targetWorld.getRegistryKey())) {
            BlockPos bedSpawnPos = player.getSpawnPointPosition();
            if (bedSpawnPos != null && targetWorld.getBlockState(bedSpawnPos).isIn(BlockTags.BEDS)) {
                targetPos = bedSpawnPos;
            }
        }

        // Ensure the chunk is loaded before getting the surface position
        targetWorld.getChunkManager().getChunk(targetPos.getX() >> 4, targetPos.getZ() >> 4, ChunkStatus.FULL, true);


        // Get the nearest safe upwards position
        BlockPos safePos = targetPos;
        while (!isSpawnPosSafe(player, targetWorld, safePos)) {
            ResourceWorld.LOGGER.info("moving position " + safePos + " up!");
            safePos = new BlockPos(safePos.getX(), safePos.getY() + 1, safePos.getZ());
            if (safePos.getY() >= targetWorld.getTopY()) {
                // Get the surface position (highest block) at x=0, z=0
                safePos = targetWorld.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, targetPos);
                ResourceWorld.LOGGER.info("failed finding safe position using world top position!");
                break;
            }
        }

        // Teleport the player to the determined position (either bed or surface)
        player.teleport(targetWorld, safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5, player.getYaw(), player.getPitch());

        // Provide feedback to the player
        context.getSource().sendFeedback(() -> Text.literal("").append(player.getName()).append(" Teleported to " + worldName + "!"), true);

        return 1;
    }

    private static boolean isSpawnPosSafe(ServerPlayerEntity player, World world, BlockPos pos) {
        // ensure there a solid block or bed under the player
        BlockState surface = world.getBlockState(pos.down());
        if (!surface.isSolidBlock(world, pos.down()) && !surface.isIn(BlockTags.BEDS)) return false;
        // ensure no blocks inside player!
        for (int i = 0; i < player.getHeight(); i++) {
            if (!world.getBlockState(new BlockPos(pos.getX(), pos.getY() + i, pos.getZ())).isAir()) {
                return false;
            }
        }
        return true;
    }
}
