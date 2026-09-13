package dev.carrytracker.client;

import java.util.Locale;

import dev.carrytracker.CarryTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

public class CarryTrackerClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		CarrySession session = new CarrySession();
		session.load();
		HudSettings hudSettings = new HudSettings();
		hudSettings.load();
		CarryHud hud = new CarryHud(session, hudSettings);
		CarryCommands commands = new CarryCommands(session, hud);
		KillDetector detector = new KillDetector(session, commands);

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> commands.register(dispatcher));
		ClientTickEvents.END_CLIENT_TICK.register(client -> detector.onTick());

		ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
			String value = command.trim().toLowerCase(Locale.ROOT);
			if (value.equals("pc") || value.startsWith("pc ") || value.startsWith("pchat") || value.startsWith("party ")) {
				PartyTracker.INSTANCE.markInParty();
			}
			return true;
		});

		ClientReceiveMessageEvents.CHAT.register((message, playerChatMessage, sender, boundChatType, timeStamp) -> {
			PartyTracker.INSTANCE.onMessage(message);
		});
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			PartyTracker.INSTANCE.onMessage(message);
		});

		HudElementRegistry.attachElementBefore(
			VanillaHudElements.CHAT,
			CarryTracker.id("slots"),
			hud::render
		);

		CarryTracker.LOGGER.info("CarryTracker client ready ({} slot(s) loaded)", session.slots().size());
	}
}
