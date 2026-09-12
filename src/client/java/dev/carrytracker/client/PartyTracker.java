package dev.carrytracker.client;

import java.util.Locale;

import net.minecraft.network.chat.Component;

public final class PartyTracker {
	public static final PartyTracker INSTANCE = new PartyTracker();

	private boolean inParty;

	private PartyTracker() {
	}

	public boolean inParty() {
		return inParty;
	}

	public void markInParty() {
		inParty = true;
	}

	public void onMessage(Component message) {
		String text = message.getString();
		if (text == null || text.isBlank()) {
			return;
		}

		String lower = text.toLowerCase(Locale.ROOT);
		if (lower.contains("party >") || lower.contains("party chat")) {
			inParty = true;
			return;
		}
		if (lower.contains("you have joined") && lower.contains("party")) {
			inParty = true;
			return;
		}
		if (lower.contains("joined the party") || lower.contains("is now in the party")) {
			inParty = true;
			return;
		}
		if ((lower.contains("you are not") || lower.contains("you're not")) && lower.contains("party")) {
			inParty = false;
			return;
		}
		if (lower.contains("you left the party")
			|| lower.contains("have been kicked from the party")
			|| lower.contains("kicked you from the party")
			|| lower.contains("party was disbanded")
			|| lower.contains("disbanded the party")) {
			inParty = false;
		}
	}
}
