package com.onestack.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onestack.OneStackMod;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Server-wide completion progress stored at {@code config/one_stack/progress.json}.
 */
public class ProgressManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path progressFile;
	private final Set<Identifier> completed = new LinkedHashSet<>();

	public ProgressManager(MinecraftServer server) {
		Path configDir = server.getServerDirectory().resolve("config").resolve(OneStackMod.MOD_ID);
		this.progressFile = configDir.resolve("progress.json");
	}

	public void load() {
		completed.clear();
		if (!Files.exists(progressFile)) {
			save();
			return;
		}

		try (Reader reader = Files.newBufferedReader(progressFile)) {
			JsonElement root = JsonParser.parseReader(reader);
			JsonArray array;
			if (root.isJsonArray()) {
				array = root.getAsJsonArray();
			} else if (root.isJsonObject() && root.getAsJsonObject().has("completed")) {
				array = root.getAsJsonObject().getAsJsonArray("completed");
			} else {
				throw new IllegalStateException("progress.json must be a JSON array or an object with a \"completed\" array");
			}

			for (JsonElement element : array) {
				completed.add(Identifier.parse(element.getAsString().toLowerCase(Locale.ROOT)));
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to load One Stack progress from " + progressFile, e);
		}
	}

	public void save() {
		try {
			Files.createDirectories(progressFile.getParent());
			JsonObject root = new JsonObject();
			JsonArray array = new JsonArray();
			for (Identifier id : completed) {
				array.add(id.toString());
			}
			root.add("completed", array);
			try (Writer writer = Files.newBufferedWriter(progressFile)) {
				GSON.toJson(root, writer);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to save One Stack progress to " + progressFile, e);
		}
	}

	public Set<Identifier> getCompleted() {
		return Collections.unmodifiableSet(completed);
	}

	public boolean isCompleted(Identifier id) {
		return completed.contains(id);
	}

	public boolean markCompleted(Identifier id) {
		if (!completed.add(id)) {
			return false;
		}
		save();
		return true;
	}

	public int completedCount() {
		return completed.size();
	}
}
