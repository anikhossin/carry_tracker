package dev.carrytracker.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dev.carrytracker.CarryTracker;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Mth;

public final class HudSettings {
	public static final int DEFAULT_X = 8;
	public static final int DEFAULT_Y = 8;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public int x = DEFAULT_X;
	public int y = DEFAULT_Y;
	public boolean visible = true;

	private static Path savePath() {
		return FabricLoader.getInstance().getConfigDir().resolve("carrytracker-hud.json");
	}

	public void load() {
		Path path = savePath();
		if (!Files.exists(path)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(path)) {
			HudSettings loaded = GSON.fromJson(reader, HudSettings.class);
			if (loaded != null) {
				this.x = loaded.x;
				this.y = loaded.y;
				this.visible = loaded.visible;
			}
		} catch (IOException exception) {
			CarryTracker.LOGGER.error("Failed to load HUD settings", exception);
		}
	}

	public void save() {
		try {
			Path path = savePath();
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException exception) {
			CarryTracker.LOGGER.error("Failed to save HUD settings", exception);
		}
	}

	public void reset() {
		x = DEFAULT_X;
		y = DEFAULT_Y;
		save();
	}

	public void toggleVisible() {
		visible = !visible;
		save();
	}

	public void setPosition(int newX, int newY, int screenWidth, int screenHeight, int boxWidth, int boxHeight) {
		x = Mth.clamp(newX, 0, Math.max(0, screenWidth - boxWidth));
		y = Mth.clamp(newY, 0, Math.max(0, screenHeight - boxHeight));
	}

	public void nudge(int dx, int dy, int screenWidth, int screenHeight, int boxWidth, int boxHeight) {
		setPosition(x + dx, y + dy, screenWidth, screenHeight, boxWidth, boxHeight);
		save();
	}
}
