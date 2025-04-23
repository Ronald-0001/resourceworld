package net.mao.rw.world.dimension;

import net.mao.rw.ResourceWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

public class ResourceDimension {
    public static final RegistryKey<World> RESOURCE_DIMENSION_KEY = RegistryKey.of(Registry.WORLD_KEY,
            new Identifier(TutorialMod.MOD_ID, "resource-world"));
    public static final RegistryKey<DimensionType> RESOURCE_TYPE_KEY = RegistryKey.of(Registry.DIMENSION_TYPE_KEY,
            RESOURCE_DIMENSION_KEY.getValue());


    public static void register() {
        ResourceWorld.LOGGER.debug("Registering ModDimensions for " + ResourceWorld.MOD_ID);
    }
}
