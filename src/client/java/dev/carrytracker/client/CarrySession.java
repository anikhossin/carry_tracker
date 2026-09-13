package dev.carrytracker.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dev.carrytracker.CarryTracker;
import net.fabricmc.loader.api.FabricLoader;

public final class CarrySession {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static Path savePath() {
		return FabricLoader.getInstance().getConfigDir().resolve("carrytracker.json");
	}

	private final List<CarrySlot> slots = new ArrayList<>();
	private boolean autoDetect = true;

	public List<CarrySlot> slots() {
		return List.copyOf(slots);
	}

	public boolean isEmpty() {
		return slots.isEmpty();
	}

	public boolean autoDetect() {
		return autoDetect;
	}

	public void setAutoDetect(boolean autoDetect) {
		this.autoDetect = autoDetect;
		save();
	}

	public List<CarrySlot> incompleteSlots() {
		return slots.stream().filter(slot -> !slot.isComplete()).toList();
	}

	public CarrySlot addGoal(String username, int amount) {
		Optional<CarrySlot> existing = findByUsername(username);
		if (existing.isPresent()) {
			CarrySlot slot = existing.get();
			slot.goal = Math.max(1, slot.goal) + amount;
			save();
			return slot;
		}

		int nextId = slots.stream().mapToInt(slot -> slot.id).max().orElse(0) + 1;
		CarrySlot slot = new CarrySlot(nextId, username, amount);
		slots.add(slot);
		save();
		return slot;
	}

	public Optional<CarrySlot> get(int id) {
		return slots.stream().filter(slot -> slot.id == id).findFirst();
	}

	public Optional<CarrySlot> findByUsername(String username) {
		return slots.stream()
			.filter(slot -> slot.username.equalsIgnoreCase(username))
			.findFirst();
	}

	public Optional<CarrySlot> remove(int id) {
		Optional<CarrySlot> found = get(id);
		if (found.isEmpty()) {
			return Optional.empty();
		}
		slots.removeIf(slot -> slot.id == id);
		save();
		return found;
	}

	public void clear() {
		slots.clear();
		save();
	}

	public CarrySlot addKills(CarrySlot slot, int amount) {
		slot.kills = Math.max(0, slot.kills + amount);
		save();
		return slot;
	}

	public void load() {
		slots.clear();

		Path savePath = savePath();
		if (!Files.exists(savePath)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(savePath)) {
			SaveData data = GSON.fromJson(reader, SaveData.class);
			if (data != null) {
				autoDetect = data.autoDetect() == null || data.autoDetect();
				if (data.slots() != null) {
					for (CarrySlot slot : data.slots()) {
						if (slot.goal < 1) {
							slot.goal = slot.carries > 0 ? slot.carries : 1;
						}
						slots.add(slot);
					}
				}
			}
		} catch (IOException exception) {
			CarryTracker.LOGGER.error("Failed to load carry session", exception);
		}
	}

	public void save() {
		try {
			Path savePath = savePath();
			Files.createDirectories(savePath.getParent());
			try (Writer writer = Files.newBufferedWriter(savePath)) {
				GSON.toJson(new SaveData(slots, autoDetect), writer);
			}
		} catch (IOException exception) {
			CarryTracker.LOGGER.error("Failed to save carry session", exception);
		}
	}

	private record SaveData(List<CarrySlot> slots, Boolean autoDetect) {
	}
}
