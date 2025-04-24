package net.mao.rw;

import net.fabricmc.api.ModInitializer;

import net.mao.rw.command.Commands;
import net.mao.rw.world.dimension.ModDimensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceWorld implements ModInitializer {
	public static final String MOD_ID = "resource-world";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		ModDimensions.register();
		Commands.register();
		LOGGER.info("Hello Fabric world!");
	}
}