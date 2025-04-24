package net.mao.rw.command;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.mao.rw.world.dimension.ModDimensions;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
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

        // fetch world
        if ("resourceworld".equalsIgnoreCase(worldName)) {
            targetWorld = ModDimensions.getTempWorld();
        } else if ("overworld".equalsIgnoreCase(worldName)) {
            targetWorld = context.getSource().getServer().getWorld(World.OVERWORLD);
        }

        // validate world
        if (targetWorld == null) {
            context.getSource().sendError(Text.literal("The world '" + worldName + "' is not available!"));
            return 0;
        }

        // get and pos player
        ServerPlayerEntity player = context.getSource().getPlayer();
        BlockPos targetPos = new BlockPos(0, 0, 0);

        // Ensure the chunk at pos is loaded before trying to get the surface
        targetWorld.getChunkManager().getChunk(targetPos.getX() >> 4, targetPos.getZ() >> 4, ChunkStatus.FULL, true); // Force loading the chunk

        // Get the surface position (highest block) at x=0, z=0
        BlockPos surfacePos = targetWorld.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, targetPos);

        // Teleport the player to the surface of the selected world
        player.teleport(targetWorld, surfacePos.getX() + 0.5, surfacePos.getY(), surfacePos.getZ() + 0.5, player.getYaw(), player.getPitch());

        // give feedback and return
        context.getSource().sendFeedback(() -> Text.literal("Teleported to surface of " + worldName + "!"), false);
        return 1;
    }
}
