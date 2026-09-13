package dev.carrytracker.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
	private static final long DEBOUNCE_MS = 2000L;
	private static final double SEARCH_RANGE = 96d;

	private final CarrySession session;
	private final CarryCommands commands;
	private final Map<Integer, TrackedBoss> tracked = new HashMap<>();
	private ClientLevel currentWorld;
	private String lastSpawner;
	private long lastCountAt;

	public KillDetector(CarrySession session, CarryCommands commands) {
		this.session = session;
		this.commands = commands;
	}

	public void onTick() {
		if (!session.autoDetect() || session.isEmpty()) {
			tracked.clear();
			return;
		}

		Minecraft client = Minecraft.getInstance();
		ClientLevel level = client.level;
		if (level == null || client.player == null) {
			tracked.clear();
			currentWorld = null;
			return;
		}

		if (currentWorld != level) {
			tracked.clear();
			currentWorld = level;
			lastSpawner = null;
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
			if (spawner != null) {
				lastSpawner = spawner;
			}

			int id = entity.getId();
			seen.add(id);
			tracked.putIfAbsent(id, new TrackedBoss(spawner));
			if (spawner != null) {
				tracked.get(id).spawner = spawner;
			}

			if (entity.isDeadOrDying()) {
				countBoss(tracked.remove(id));
			}
		}
	}

	public void onMessage(Component message) {
		if (!session.autoDetect() || session.isEmpty()) {
			return;
		}

		String text = ChatFormatting.stripFormatting(message.getString());
		if (text == null || text.isBlank()) {
			return;
		}

		String lower = text.toLowerCase(Locale.ROOT);
		if (!lower.contains("slayer boss slain") && !lower.contains("slayer quest complete")) {
			return;
		}

		countKill(lastSpawner);
	}

	private void countBoss(TrackedBoss boss) {
		if (boss == null) {
			return;
		}
		countKill(boss.spawner != null ? boss.spawner : lastSpawner);
	}

	private void countKill(String spawner) {
		long now = System.currentTimeMillis();
		if (now - lastCountAt < DEBOUNCE_MS) {
			return;
		}

		Optional<CarrySlot> slot = resolveSlot(spawner);
		if (slot.isEmpty()) {
			if (!session.incompleteSlots().isEmpty()) {
				GameMessages.localError("Slayer kill detected, but more than one slot is active. Use /k <slot>.");
			}
			return;
		}

		lastCountAt = now;
		commands.recordKill(slot.get().id, true);
	}

	private Optional<CarrySlot> resolveSlot(String spawner) {
		if (spawner != null && !spawner.isBlank()) {
			Optional<CarrySlot> named = session.findByUsername(spawner);
			if (named.isPresent() && !named.get().isComplete()) {
				return named;
			}
		}

		List<CarrySlot> incomplete = session.incompleteSlots();
		if (incomplete.size() == 1) {
			return Optional.of(incomplete.getFirst());
		}
		return Optional.empty();
	}

	private static boolean isSlayerBoss(List<ArmorStand> stands) {
		for (ArmorStand stand : stands) {
			String name = stand.getName().getString();
			if (!name.contains("❤") && !name.toLowerCase(Locale.ROOT).contains("hit")) {
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
			int index = name.toLowerCase(Locale.ROOT).indexOf("spawned by:");
			if (index >= 0) {
				String spawner = name.substring(index + "spawned by:".length()).trim();
				if (!spawner.isEmpty()) {
					return spawner;
				}
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
			stands.add(stand);
		}
		return stands;
	}

	private static final class TrackedBoss {
		String spawner;

		TrackedBoss(String spawner) {
			this.spawner = spawner;
		}
	}
}
