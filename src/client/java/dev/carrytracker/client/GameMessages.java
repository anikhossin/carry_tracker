package dev.carrytracker.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class GameMessages {
	private GameMessages() {
	}

	public static Component prefix() {
		return Component.literal("[CarryTracker] ").withStyle(ChatFormatting.GOLD);
	}

	public static void local(String message) {
		local(Component.literal(message).withStyle(ChatFormatting.GRAY));
	}

	public static void local(Component message) {
		Minecraft client = Minecraft.getInstance();
		if (client.gui != null) {
			client.gui.getChat().addClientSystemMessage(prefix().copy().append(message));
		}
	}

	public static void localError(String message) {
		local(Component.literal(message).withStyle(ChatFormatting.RED));
	}

	private static volatile boolean sendingPartyAnnounce;

	public static boolean isSendingPartyAnnounce() {
		return sendingPartyAnnounce;
	}

	public static void party(String message) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}

		sendingPartyAnnounce = true;
		client.execute(() -> client.execute(() -> {
			try {
				if (client.player != null) {
					client.player.connection.sendCommand("pc " + message);
				}
			} finally {
				sendingPartyAnnounce = false;
			}
		}));
	}

	public static void showCompletionTitle(String username, int goal) {
		Minecraft client = Minecraft.getInstance();
		if (client.gui == null) {
			return;
		}

		client.gui.setTimes(10, 70, 20);
		client.gui.setTitle(Component.literal(username + " " + goal + "/" + goal + " DONE! GG").withStyle(ChatFormatting.GREEN));
		client.gui.setSubtitle(Component.literal("Carry complete!").withStyle(ChatFormatting.GOLD));
	}
}
