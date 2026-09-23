package com.onestack.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.onestack.OneStackMod;
import com.onestack.data.ItemListManager;
import com.onestack.data.ProgressManager;
import com.onestack.menu.StackListMenu;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
	}

	private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("stack")
				.requires(source -> source.isPlayer())
				.executes(context -> submitStack(context.getSource()))
				.then(Commands.literal("list")
						.executes(context -> openList(context.getSource()))));
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

	private static int openList(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		if (OneStackMod.getItemListManager() == null || OneStackMod.getProgressManager() == null) {
			source.sendFailure(Component.literal("One Stack is not ready yet."));
			return 0;
		}
		StackListMenu.open(player, 0);
		return Command.SINGLE_SUCCESS;
	}
}
