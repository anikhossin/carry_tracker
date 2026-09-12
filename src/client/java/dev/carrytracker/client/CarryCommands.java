package dev.carrytracker.client;

import java.util.Optional;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class CarryCommands {
	private final CarrySession session;
	private final CarryHud hud;

	public CarryCommands(CarrySession session, CarryHud hud) {
		this.session = session;
		this.hud = hud;
	}

	public void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		registerTree(dispatcher, "ct");
		registerTree(dispatcher, "carrytracker");

		dispatcher.register(ClientCommands.literal("carry")
			.executes(context -> {
				showUsage();
				return 1;
			})
			.then(ClientCommands.literal("list").executes(context -> {
				listSlots();
				return 1;
			}))
			.then(ClientCommands.literal("clear").executes(context -> {
				clearSlots();
				return 1;
			}))
			.then(ClientCommands.literal("remove")
				.then(ClientCommands.argument("slot", IntegerArgumentType.integer(1)).executes(context -> {
					removeSlot(IntegerArgumentType.getInteger(context, "slot"));
					return 1;
				})))
			.then(ClientCommands.argument("username", StringArgumentType.word())
				.then(ClientCommands.argument("amount", IntegerArgumentType.integer(1)).executes(context -> {
					addGoal(
						StringArgumentType.getString(context, "username"),
						IntegerArgumentType.getInteger(context, "amount")
					);
					return 1;
				}))));

		dispatcher.register(ClientCommands.literal("k")
			.executes(context -> {
				incrementKillFromOptionalSlot(Optional.empty());
				return 1;
			})
			.then(ClientCommands.argument("slot", IntegerArgumentType.integer(1)).executes(context -> {
				incrementKillFromOptionalSlot(Optional.of(IntegerArgumentType.getInteger(context, "slot")));
				return 1;
			})));

		dispatcher.register(ClientCommands.literal("check").executes(context -> {
			checkSlots();
			return 1;
		}));

		dispatcher.register(ClientCommands.literal("hud")
			.executes(context -> {
				openHudEditor();
				return 1;
			})
			.then(ClientCommands.literal("toggle").executes(context -> {
				toggleHud();
				return 1;
			}))
			.then(ClientCommands.literal("reset").executes(context -> {
				resetHud();
				return 1;
			})));

		dispatcher.register(ClientCommands.literal("carryremove")
			.then(ClientCommands.argument("slot", IntegerArgumentType.integer(1)).executes(context -> {
				removeSlot(IntegerArgumentType.getInteger(context, "slot"));
				return 1;
			})));
	}

	private void registerTree(CommandDispatcher<FabricClientCommandSource> dispatcher, String name) {
		dispatcher.register(ClientCommands.literal(name)
			.executes(context -> {
				showUsage();
				return 1;
			})
			.then(ClientCommands.literal("list").executes(context -> {
				listSlots();
				return 1;
			}))
			.then(ClientCommands.literal("clear").executes(context -> {
				clearSlots();
				return 1;
			}))
			.then(ClientCommands.literal("remove")
				.then(ClientCommands.argument("slot", IntegerArgumentType.integer(1)).executes(context -> {
					removeSlot(IntegerArgumentType.getInteger(context, "slot"));
					return 1;
				})))
			.then(ClientCommands.literal("check").executes(context -> {
				checkSlots();
				return 1;
			}))
			.then(ClientCommands.literal("carry")
				.then(ClientCommands.argument("username", StringArgumentType.word())
					.then(ClientCommands.argument("amount", IntegerArgumentType.integer(1)).executes(context -> {
						addGoal(
							StringArgumentType.getString(context, "username"),
							IntegerArgumentType.getInteger(context, "amount")
						);
						return 1;
					}))))
			.then(ClientCommands.literal("k")
				.executes(context -> {
					incrementKillFromOptionalSlot(Optional.empty());
					return 1;
				})
				.then(ClientCommands.argument("slot", IntegerArgumentType.integer(1)).executes(context -> {
					incrementKillFromOptionalSlot(Optional.of(IntegerArgumentType.getInteger(context, "slot")));
					return 1;
				})))
			.then(ClientCommands.literal("hud")
				.executes(context -> {
					openHudEditor();
					return 1;
				})
				.then(ClientCommands.literal("toggle").executes(context -> {
					toggleHud();
					return 1;
				}))
				.then(ClientCommands.literal("reset").executes(context -> {
					resetHud();
					return 1;
				}))));
	}

	private void incrementKillFromOptionalSlot(Optional<Integer> slotId) {
		if (slotId.isPresent()) {
			incrementKill(slotId.get());
			return;
		}

		var slots = session.slots();
		if (slots.isEmpty()) {
			GameMessages.localError("No active carry slots.");
			return;
		}
		if (slots.size() == 1) {
			incrementKill(slots.getFirst().id);
			return;
		}

		GameMessages.localError("More than one slot is active. Use /k <slot number>.");
	}

	private void openHudEditor() {
		HudEditorScreen.open(hud);
	}

	private void toggleHud() {
		hud.settings().toggleVisible();
		boolean visible = hud.settings().visible;
		GameMessages.local(Component.literal(visible ? "HUD shown." : "HUD hidden.")
			.withStyle(visible ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
	}

	private void resetHud() {
		hud.settings().reset();
		GameMessages.local(Component.literal("HUD position reset.").withStyle(ChatFormatting.YELLOW));
	}

	private void addGoal(String username, int amount) {
		boolean existed = session.findByUsername(username).isPresent();
		CarrySlot slot = session.addGoal(username, amount);
		int goal = slot.killGoal();

		if (existed) {
			GameMessages.party(slot.username + " added " + amount + " more! Now " + goal + " carries.");
			GameMessages.local(Component.literal("Added " + amount + " to #" + slot.id + " " + slot.username
				+ " (now " + slot.kills + "/" + goal + ")")
				.withStyle(ChatFormatting.GREEN));
			return;
		}

		GameMessages.party(slot.username + " starting your " + goal + " carries!");
		GameMessages.local(Component.literal("Created slot #" + slot.id + " for " + slot.username
			+ " (" + slot.kills + "/" + goal + ")")
			.withStyle(ChatFormatting.GREEN));
	}

	private void listSlots() {
		if (session.isEmpty()) {
			GameMessages.local("No active carry slots.");
			return;
		}

		GameMessages.local(Component.literal("Active carry slots:").withStyle(ChatFormatting.AQUA));
		for (CarrySlot slot : session.slots()) {
			GameMessages.local(formatSlot(slot));
		}
	}

	private void clearSlots() {
		session.clear();
		GameMessages.local(Component.literal("Cleared all carry slots.").withStyle(ChatFormatting.YELLOW));
	}

	private void removeSlot(int slotId) {
		Optional<CarrySlot> removed = session.remove(slotId);
		if (removed.isEmpty()) {
			GameMessages.localError("No carry slot #" + slotId + ".");
			return;
		}

		CarrySlot slot = removed.get();
		GameMessages.local(Component.literal("Removed slot #" + slot.id + " " + slot.username)
			.withStyle(ChatFormatting.YELLOW));
	}

	private void incrementKill(int slotId) {
		Optional<CarrySlot> found = session.get(slotId);
		if (found.isEmpty()) {
			GameMessages.localError("No carry slot #" + slotId + ".");
			return;
		}

		CarrySlot slot = found.get();
		boolean alreadyComplete = slot.isComplete();
		session.addKills(slot, 1);
		int goal = slot.killGoal();

		if (alreadyComplete) {
			GameMessages.local(Component.literal("Slot #" + slot.id + " " + slot.username
				+ " is already finished. Count is now " + slot.kills + ".")
				.withStyle(ChatFormatting.YELLOW));
			return;
		}

		if (slot.kills >= goal) {
			GameMessages.party(slot.username + " " + goal + "/" + goal + " DONE! GG");
			GameMessages.showCompletionTitle(slot.username, goal);
			GameMessages.local(Component.literal("Slot #" + slot.id + " " + slot.username
				+ " finished (" + goal + "/" + goal + ").")
				.withStyle(ChatFormatting.GREEN));
			return;
		}

		int left = goal - slot.kills;
		GameMessages.party(slot.username + " " + slot.kills + "/" + goal + " done! " + left + " left.");
		GameMessages.local(Component.literal("Kill logged on #" + slot.id + " " + slot.username
			+ " (" + slot.kills + "/" + goal + ").")
			.withStyle(ChatFormatting.GREEN));
	}

	private void checkSlots() {
		if (session.isEmpty()) {
			GameMessages.local("No active carry slots.");
			return;
		}

		GameMessages.local(Component.literal("Carry summary:").withStyle(ChatFormatting.AQUA));
		for (CarrySlot slot : session.slots()) {
			GameMessages.local(formatSlot(slot));
		}
	}

	private void showUsage() {
		GameMessages.local(Component.literal("Commands:").withStyle(ChatFormatting.AQUA));
		GameMessages.local("/carry <username> <goal> — create a slot or add to the goal");
		GameMessages.local("/carry list — show slots");
		GameMessages.local("/carry clear — clear all slots");
		GameMessages.local("/carryremove <slot> — remove one slot, e.g. /carryremove 1");
		GameMessages.local("/k — add a kill (or /k <slot> if more than one)");
		GameMessages.local("/check — summary");
		GameMessages.local("/hud — HUD editor    /hud toggle    /hud reset");
		GameMessages.local("Also: /ct carry, /ct k, /ct clear, /ct hud, /ct check");
	}

	private static Component formatSlot(CarrySlot slot) {
		ChatFormatting killColor = slot.isComplete() ? ChatFormatting.GREEN : ChatFormatting.YELLOW;
		return Component.literal("#" + slot.id + " " + slot.username + " ")
			.withStyle(ChatFormatting.WHITE)
			.append(Component.literal(slot.kills + "/" + slot.killGoal())
				.withStyle(killColor));
	}
}
