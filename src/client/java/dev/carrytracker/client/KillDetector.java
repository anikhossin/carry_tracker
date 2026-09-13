package dev.carrytracker.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import dev.carrytracker.CarryTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;

public final class KillDetector {
	private static final String[] SLAYER_NAMES = {
		"Revenant Horror",
		"Atoned Horror",
		"Tarantula Broodfather",
		"Conjoined Brood",
		"Sven Packmaster",
		"Voidgloom Seraph",
		"Inferno Demonlord",
		"Riftstalker Bloodfiend"
	};
	private static final long DEBOUNCE_MS = 2500L;
	private static final double SEARCH_RANGE = 64d;
	private static final double NAMETAG_RANGE = 4d;
	private static final int MIN_ALIVE_TICKS = 40;

	private final CarrySession session;
	private final CarryCommands commands;
	private final Map<Integer, String> spawners = new HashMap<>();
	private final Map<Integer, Integer> aliveTicks = new HashMap<>();
	private ClientLevel currentWorld;
	private long lastCountAt;
	private boolean disabled;

	public KillDetector(CarrySession session, CarryCommands commands) {
		this.session = session;
		this.commands = commands;
	}

	public void onTick() {
		if (disabled) {
			return;
		}

		try {
			tick();
		} catch (Throwable exception) {
			disabled = true;
			clearTracked();
			CarryTracker.LOGGER.error("Kill detector failed; auto-detect disabled for this session", exception);
			GameMessages.local(Component.literal("Auto-detect crashed and was turned off. Use /k for now.")
				.withStyle(ChatFormatting.RED));
		}
	}

	private void tick() {
		if (!session.autoDetect() || session.incompleteSlots().isEmpty()) {
			clearTracked();
			return;
		}

		Minecraft client = Minecraft.getInstance();
		ClientLevel level = client.level;
		if (level == null || client.player == null) {
			clearTracked();
			currentWorld = null;
			return;
		}

		if (currentWorld != level) {
			clearTracked();
			currentWorld = level;
			return;
		}

		AABB search = client.player.getBoundingBox().inflate(SEARCH_RANGE);
		List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, search, entity -> true);
		Set<Integer> seen = new HashSet<>();

		for (LivingEntity entity : nearby) {
			if (entity instanceof ArmorStand) {
				continue;
			}

			List<ArmorStand> stands = armorStandsAfter(entity);
			if (!isSlayerBoss(stands)) {
				continue;
			}

			String spawner = findSpawner(stands);
			if (spawner == null || session.findByUsername(spawner).filter(slot -> !slot.isComplete()).isEmpty()) {
				continue;
			}

			int id = entity.getId();
			seen.add(id);
			spawners.put(id, spawner);
			int ticks = aliveTicks.getOrDefault(id, 0);

			if (entity.isDeadOrDying()) {
				if (ticks >= MIN_ALIVE_TICKS) {
					countKill(spawner);
				}
				spawners.remove(id);
				aliveTicks.remove(id);
				continue;
			}

			aliveTicks.put(id, ticks + 1);
		}

		spawners.keySet().removeIf(id -> !seen.contains(id));
		aliveTicks.keySet().removeIf(id -> !seen.contains(id));
	}

	private void countKill(String spawner) {
		long now = System.currentTimeMillis();
		if (now - lastCountAt < DEBOUNCE_MS) {
			return;
		}

		Optional<CarrySlot> slot = session.findByUsername(spawner)
			.filter(found -> !found.isComplete());
		if (slot.isEmpty()) {
			return;
		}

		lastCountAt = now;
		commands.recordKill(slot.get().id, true);
	}

	private void clearTracked() {
		spawners.clear();
		aliveTicks.clear();
	}

	private static boolean isSlayerBoss(List<ArmorStand> stands) {
		for (ArmorStand stand : stands) {
			String name = stand.getName().getString();
			if (!name.contains("❤") && !name.contains(" Hit")) {
				continue;
			}
			for (String slayerName : SLAYER_NAMES) {
				if (name.contains(slayerName)) {
					return true;
				}
			}
		}
		return false;
	}

	private static String findSpawner(List<ArmorStand> stands) {
		for (ArmorStand stand : stands) {
			String name = stand.getName().getString();
			int index = name.indexOf("Spawned by:");
			if (index < 0) {
				continue;
			}
			String spawner = name.substring(index + "Spawned by:".length()).trim();
			if (!spawner.isEmpty()) {
				return spawner;
			}
		}
		return null;
	}

	private static List<ArmorStand> armorStandsAfter(Entity entity) {
		List<ArmorStand> stands = new ArrayList<>();
		int id = entity.getId();
		var world = entity.level();
		while (true) {
			Entity next = world.getEntity(++id);
			if (!(next instanceof ArmorStand stand)) {
				break;
			}
			if (stand.distanceTo(entity) > NAMETAG_RANGE) {
				break;
			}
			stands.add(stand);
		}
		return stands;
	}
}
