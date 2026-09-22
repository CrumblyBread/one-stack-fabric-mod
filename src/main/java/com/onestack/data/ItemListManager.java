package com.onestack.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.onestack.OneStackMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Admin-editable list of challenge items stored at {@code config/one_stack/items.json}.
 * Created with a survival-obtainable default on first launch.
 */
public class ItemListManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path configFile;
	private final List<Identifier> items = new ArrayList<>();

	public ItemListManager(MinecraftServer server) {
		Path configDir = server.getServerDirectory().resolve("config").resolve(OneStackMod.MOD_ID);
		this.configFile = configDir.resolve("items.json");
	}

	public void loadOrCreateDefault() {
		try {
			Files.createDirectories(configFile.getParent());
			if (!Files.exists(configFile)) {
				items.clear();
				items.addAll(buildDefaultList());
				save();
				OneStackMod.LOGGER.info("Created default item list at {}", configFile);
				return;
			}

			items.clear();
			try (Reader reader = Files.newBufferedReader(configFile)) {
				JsonElement root = JsonParser.parseReader(reader);
				JsonArray array;
				if (root.isJsonArray()) {
					array = root.getAsJsonArray();
				} else if (root.isJsonObject() && root.getAsJsonObject().has("items")) {
					array = root.getAsJsonObject().getAsJsonArray("items");
				} else {
					throw new IllegalStateException("items.json must be a JSON array or an object with an \"items\" array");
				}

				Set<Identifier> seen = new LinkedHashSet<>();
				for (JsonElement element : array) {
					String raw = element.getAsString().toLowerCase(Locale.ROOT);
					Identifier id = Identifier.parse(raw);
					if (!BuiltInRegistries.ITEM.containsKey(id)) {
						OneStackMod.LOGGER.warn("Skipping unknown item in items.json: {}", id);
						continue;
					}
					seen.add(id);
				}
				items.addAll(seen);
			}

			OneStackMod.LOGGER.info("Loaded item list from {}", configFile);
		} catch (Exception e) {
			throw new RuntimeException("Failed to load One Stack item list from " + configFile, e);
		}
	}

	public void save() {
		try {
			Files.createDirectories(configFile.getParent());
			JsonArray array = new JsonArray();
			for (Identifier id : items) {
				array.add(id.toString());
			}
			try (Writer writer = Files.newBufferedWriter(configFile)) {
				GSON.toJson(array, writer);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to save One Stack item list to " + configFile, e);
		}
	}

	public List<Identifier> getItems() {
		return Collections.unmodifiableList(items);
	}

	public boolean contains(Identifier id) {
		return items.contains(id);
	}

	public boolean contains(Item item) {
		return contains(BuiltInRegistries.ITEM.getKey(item));
	}

	private static List<Identifier> buildDefaultList() {
		Set<Identifier> denylist = defaultDenylist();
		List<Identifier> result = new ArrayList<>();

		for (Item item : BuiltInRegistries.ITEM) {
			Identifier id = BuiltInRegistries.ITEM.getKey(item);
			if (id == null || Items.AIR == item) {
				continue;
			}

			ItemStack stack = new ItemStack(item);
			if (stack.getMaxStackSize() <= 1) {
				continue;
			}

			String path = id.getPath();
			if (path.endsWith("_spawn_egg") || path.contains("command_block") || path.contains("structure_block")) {
				continue;
			}

			if (denylist.contains(id)) {
				continue;
			}

			result.add(id);
		}

		result.sort((a, b) -> a.toString().compareToIgnoreCase(b.toString()));
		return result;
	}

	/**
	 * Survival-unobtainable (or otherwise unsuitable) stackable items left out of the default list.
	 * Admins can still add any of these back by editing items.json.
	 */
	private static Set<Identifier> defaultDenylist() {
		Set<Identifier> deny = new LinkedHashSet<>();
		add(deny, "minecraft:bedrock");
		add(deny, "minecraft:barrier");
		add(deny, "minecraft:light");
		add(deny, "minecraft:structure_void");
		add(deny, "minecraft:jigsaw");
		add(deny, "minecraft:command_block");
		add(deny, "minecraft:chain_command_block");
		add(deny, "minecraft:repeating_command_block");
		add(deny, "minecraft:command_block_minecart");
		add(deny, "minecraft:spawner");
		add(deny, "minecraft:trial_spawner");
		add(deny, "minecraft:vault");
		add(deny, "minecraft:budding_amethyst");
		add(deny, "minecraft:chorus_plant");
		add(deny, "minecraft:farmland");
		add(deny, "minecraft:dirt_path");
		add(deny, "minecraft:frosted_ice");
		add(deny, "minecraft:end_portal_frame");
		add(deny, "minecraft:infested_stone");
		add(deny, "minecraft:infested_cobblestone");
		add(deny, "minecraft:infested_stone_bricks");
		add(deny, "minecraft:infested_mossy_stone_bricks");
		add(deny, "minecraft:infested_cracked_stone_bricks");
		add(deny, "minecraft:infested_chiseled_stone_bricks");
		add(deny, "minecraft:infested_deepslate");
		add(deny, "minecraft:reinforced_deepslate");
		add(deny, "minecraft:petrified_oak_slab");
		add(deny, "minecraft:knowledge_book");
		add(deny, "minecraft:debug_stick");
		return deny;
	}

	private static void add(Set<Identifier> set, String id) {
		set.add(Identifier.parse(id));
	}
}
