package com.onestack.menu;

import com.onestack.OneStackMod;
import com.onestack.data.ItemListManager;
import com.onestack.data.ProgressManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Double-chest GUI using vanilla {@link MenuType#GENERIC_9x6} so no client mod is required.
 * Slots 0-44 show challenge items; slot 45 is previous (red concrete), slot 53 is next (green concrete).
 */
public class StackListMenu extends AbstractContainerMenu {
	public static final int ROWS = 6;
	public static final int COLUMNS = 9;
	public static final int PAGE_SIZE = 45; // first 5 rows
	public static final int PREV_SLOT = 45;
	public static final int NEXT_SLOT = 53;

	private final SimpleContainer display;
	private final ServerPlayer player;
	private int page;

	public StackListMenu(int containerId, Inventory playerInventory, ServerPlayer player, int page) {
		super(MenuType.GENERIC_9x6, containerId);
		this.display = new SimpleContainer(ROWS * COLUMNS);
		this.player = player;
		this.page = Math.max(0, page);

		for (int row = 0; row < ROWS; row++) {
			for (int col = 0; col < COLUMNS; col++) {
				int index = col + row * COLUMNS;
				this.addSlot(new DisplaySlot(display, index, 8 + col * 18, 18 + row * 18));
			}
		}

		int playerInvY = 84 + (ROWS - 4) * 18;
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerInvY + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(playerInventory, col, 8 + col * 18, playerInvY + 58));
		}

		refresh();
	}

	public static void open(ServerPlayer player, int page) {
		player.openMenu(new net.minecraft.world.SimpleMenuProvider(
				(id, inv, p) -> new StackListMenu(id, inv, player, page),
				Component.literal("One Stack List")
		));
	}

	private void refresh() {
		ItemListManager listManager = OneStackMod.getItemListManager();
		ProgressManager progressManager = OneStackMod.getProgressManager();
		List<Identifier> items = listManager.getItems();

		int maxPage = Math.max(0, (items.size() - 1) / PAGE_SIZE);
		if (page > maxPage) {
			page = maxPage;
		}

		for (int i = 0; i < display.getContainerSize(); i++) {
			display.setItem(i, ItemStack.EMPTY);
		}

		int start = page * PAGE_SIZE;
		for (int i = 0; i < PAGE_SIZE; i++) {
			int itemIndex = start + i;
			if (itemIndex >= items.size()) {
				break;
			}
			Identifier id = items.get(itemIndex);
			Item item = BuiltInRegistries.ITEM.getValue(id);
			if (item == null || item == Items.AIR) {
				continue;
			}
			ItemStack stack = new ItemStack(item);
			if (progressManager.isCompleted(id)) {
				stack.setCount(stack.getMaxStackSize());
			} else {
				stack.setCount(1);
			}
			display.setItem(i, stack);
		}

		if (page > 0) {
			display.setItem(PREV_SLOT, new ItemStack(Items.CONCRETE.red()));
		}
		if (page < maxPage) {
			display.setItem(NEXT_SLOT, new ItemStack(Items.CONCRETE.green()));
		}

		broadcastChanges();
	}

	@Override
	public void clicked(int slotId, int button, ContainerInput clickType, Player clickPlayer) {
		if (slotId == PREV_SLOT && page > 0) {
			page--;
			refresh();
			return;
		}
		if (slotId == NEXT_SLOT) {
			ItemListManager listManager = OneStackMod.getItemListManager();
			int maxPage = Math.max(0, (listManager.getItems().size() - 1) / PAGE_SIZE);
			if (page < maxPage) {
				page++;
				refresh();
			}
			return;
		}
		if (slotId >= 0 && slotId < display.getContainerSize()) {
			// Display-only challenge slots: ignore all clicks.
			return;
		}
		super.clicked(slotId, button, clickType, clickPlayer);
	}

	@Override
	public ItemStack quickMoveStack(Player clickPlayer, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player clickPlayer) {
		return clickPlayer == player;
	}

	private static class DisplaySlot extends Slot {
		public DisplaySlot(SimpleContainer container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}

		@Override
		public boolean mayPickup(Player player) {
			return false;
		}
	}
}
