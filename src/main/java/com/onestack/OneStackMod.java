package com.onestack;

import com.onestack.command.StackCommands;
import com.onestack.data.ItemListManager;
import com.onestack.data.ProgressManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OneStackMod implements ModInitializer {
	public static final String MOD_ID = "one_stack";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static ItemListManager itemListManager;
	private static ProgressManager progressManager;

	@Override
	public void onInitialize() {
		StackCommands.register();

		ServerLifecycleEvents.SERVER_STARTED.register(OneStackMod::onServerStarted);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			if (progressManager != null) {
				progressManager.save();
			}
		});

		LOGGER.info("One Stack initialized");
	}

	private static void onServerStarted(MinecraftServer server) {
		itemListManager = new ItemListManager(server);
		progressManager = new ProgressManager(server);
		itemListManager.loadOrCreateDefault();
		progressManager.load();
		LOGGER.info("Loaded {} challenge items ({} completed)",
				itemListManager.getItems().size(),
				progressManager.getCompleted().size());
	}

	public static ItemListManager getItemListManager() {
		return itemListManager;
	}

	public static ProgressManager getProgressManager() {
		return progressManager;
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
