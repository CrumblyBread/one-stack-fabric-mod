package com.onestack.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.onestack.OneStackMod;
import com.onestack.data.ItemListManager;
import com.onestack.data.ProgressManager;
import com.onestack.menu.StackListMenu;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class StackCommands {
	private StackCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher, registryAccess));
	}

	private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
		dispatcher.register(Commands.literal("stack")
				.executes(context -> submitStack(context.getSource()))
				.then(Commands.literal("list")
						.executes(context -> openList(context.getSource(), 1))
						.then(Commands.argument("page", IntegerArgumentType.integer(1))
								.executes(context -> openList(context.getSource(), IntegerArgumentType.getInteger(context, "page"))))
						.then(Commands.literal("add")
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.then(Commands.argument("item", ItemArgument.item(registryAccess))
										.executes(context -> addItem(context.getSource(), ItemArgument.getItem(context, "item")))))
						.then(Commands.literal("remove")
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.then(Commands.argument("item", ItemArgument.item(registryAccess))
										.executes(context -> removeItem(context.getSource(), ItemArgument.getItem(context, "item")))))
						.then(Commands.literal("reload")
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.executes(context -> reloadList(context.getSource())))));
	}

	private static int submitStack(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		ItemListManager listManager = OneStackMod.getItemListManager();
		ProgressManager progressManager = OneStackMod.getProgressManager();

		if (listManager == null || progressManager == null) {
			source.sendFailure(Component.literal("One Stack is not ready yet."));
			return 0;
		}

		ItemStack hand = player.getMainHandItem();
		if (hand.isEmpty()) {
			source.sendFailure(Component.literal("Hold a full stack of an item in your main hand."));
			return 0;
		}

		Identifier id = BuiltInRegistries.ITEM.getKey(hand.getItem());
		if (!listManager.contains(id)) {
			source.sendFailure(Component.literal("not on the list"));
			return 0;
		}

		if (progressManager.isCompleted(id)) {
			source.sendFailure(Component.literal("Already completed: " + id));
			return 0;
		}

		int required = hand.getMaxStackSize();
		if (hand.getCount() < required) {
			source.sendFailure(Component.literal("Need a full stack (" + hand.getCount() + "/" + required + ")."));
			return 0;
		}

		hand.shrink(required);
		if (hand.isEmpty()) {
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		}

		progressManager.markCompleted(id);
		int done = progressManager.completedCount();
		int total = listManager.getItems().size();
		source.sendSuccess(() -> Component.literal("Completed " + id + " (" + done + "/" + total + ")"), true);
		return Command.SINGLE_SUCCESS;
	}

	private static int openList(CommandSourceStack source, int pageNumber) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		ItemListManager listManager = OneStackMod.getItemListManager();
		if (listManager == null || OneStackMod.getProgressManager() == null) {
			source.sendFailure(Component.literal("One Stack is not ready yet."));
			return 0;
		}

		int maxPage = Math.max(0, (listManager.getItems().size() - 1) / StackListMenu.PAGE_SIZE) + 1;
		if (pageNumber > maxPage) {
			source.sendFailure(Component.literal("Page " + pageNumber + " is out of range (1-" + maxPage + ")."));
			return 0;
		}

		StackListMenu.open(player, pageNumber - 1);
		return Command.SINGLE_SUCCESS;
	}

	private static int addItem(CommandSourceStack source, ItemInput itemInput) {
		ItemListManager listManager = OneStackMod.getItemListManager();
		if (listManager == null) {
			source.sendFailure(Component.literal("One Stack is not ready yet."));
			return 0;
		}

		Identifier id = BuiltInRegistries.ITEM.getKey(itemInput.item().value());
		if (!listManager.addItem(id)) {
			source.sendFailure(Component.literal(id + " is already on the list."));
			return 0;
		}

		int total = listManager.getItems().size();
		OneStackMod.LOGGER.info("{} added {} to the One Stack list ({} items)", source.getTextName(), id, total);
		source.sendSuccess(() -> Component.literal("Added " + id + " to the list (" + total + " items)."), true);
		return Command.SINGLE_SUCCESS;
	}

	private static int removeItem(CommandSourceStack source, ItemInput itemInput) {
		ItemListManager listManager = OneStackMod.getItemListManager();
		ProgressManager progressManager = OneStackMod.getProgressManager();
		if (listManager == null || progressManager == null) {
			source.sendFailure(Component.literal("One Stack is not ready yet."));
			return 0;
		}

		Identifier id = BuiltInRegistries.ITEM.getKey(itemInput.item().value());
		if (!listManager.removeItem(id)) {
			source.sendFailure(Component.literal(id + " is not on the list."));
			return 0;
		}
		progressManager.unmark(id);

		int total = listManager.getItems().size();
		OneStackMod.LOGGER.info("{} removed {} from the One Stack list ({} items)", source.getTextName(), id, total);
		source.sendSuccess(() -> Component.literal("Removed " + id + " from the list (" + total + " items)."), true);
		return Command.SINGLE_SUCCESS;
	}

	private static int reloadList(CommandSourceStack source) {
		ItemListManager listManager = OneStackMod.getItemListManager();
		if (listManager == null) {
			source.sendFailure(Component.literal("One Stack is not ready yet."));
			return 0;
		}

		listManager.reload();
		int total = listManager.getItems().size();
		OneStackMod.LOGGER.info("{} reloaded the One Stack list from disk ({} items)", source.getTextName(), total);
		source.sendSuccess(() -> Component.literal("Reloaded the list from disk (" + total + " items)."), true);
		return Command.SINGLE_SUCCESS;
	}
}
