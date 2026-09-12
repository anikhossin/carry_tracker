package dev.carrytracker.client;

import java.util.List;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class CarryHud {
	static final int PADDING = 6;
	static final int LINE_HEIGHT = 10;
	private static final int EDITOR_BACKGROUND = 0x33000000;
	private static final int EDITOR_BORDER = 0xFFFFAA00;
	private static final int TITLE_COLOR = 0xFFFFAA00;
	private static final int DONE_COLOR = 0xFF55FF55;
	private static final int PROGRESS_COLOR = 0xFFFFFF55;
	private static final int PLACEHOLDER_COLOR = 0xFFAAAAAA;

	private final CarrySession session;
	private final HudSettings settings;
	private int boxWidth = 120;
	private int boxHeight = 28;
	private boolean editorOpen;

	public CarryHud(CarrySession session, HudSettings settings) {
		this.session = session;
		this.settings = settings;
	}

	public HudSettings settings() {
		return settings;
	}

	public void setEditorOpen(boolean editorOpen) {
		this.editorOpen = editorOpen;
	}

	public int boxWidth() {
		return boxWidth;
	}

	public int boxHeight() {
		return boxHeight;
	}

	public boolean contains(double mouseX, double mouseY) {
		return mouseX >= settings.x
			&& mouseY >= settings.y
			&& mouseX < settings.x + boxWidth
			&& mouseY < settings.y + boxHeight;
	}

	public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft client = Minecraft.getInstance();
		if (client.options.hideGui || client.player == null) {
			return;
		}

		if (!editorOpen && !settings.visible) {
			return;
		}

		List<CarrySlot> slots = session.slots();
		if (!editorOpen && slots.isEmpty()) {
			return;
		}

		Font font = client.font;
		String title = titleText();
		int width = font.width(title);
		if (slots.isEmpty()) {
			width = Math.max(width, font.width("No active slots"));
			width = Math.max(width, font.width("Drag this box"));
		} else {
			for (CarrySlot slot : slots) {
				width = Math.max(width, font.width(lineFor(slot)));
			}
		}

		boxWidth = width + PADDING * 2;
		boxHeight = PADDING * 2 + LINE_HEIGHT * (slots.isEmpty() ? 3 : slots.size() + 1);
		settings.setPosition(settings.x, settings.y, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight(), boxWidth, boxHeight);

		int x = settings.x;
		int y = settings.y;
		if (editorOpen) {
			graphics.fill(x, y, x + boxWidth, y + boxHeight, EDITOR_BACKGROUND);
			graphics.outline(x, y, boxWidth, boxHeight, EDITOR_BORDER);
		}

		graphics.text(font, title, x + PADDING, y + PADDING, TITLE_COLOR, true);

		int lineY = y + PADDING + LINE_HEIGHT;
		if (slots.isEmpty()) {
			graphics.text(font, "No active slots", x + PADDING, lineY, PLACEHOLDER_COLOR, true);
			graphics.text(font, "Drag this box", x + PADDING, lineY + LINE_HEIGHT, PLACEHOLDER_COLOR, true);
			return;
		}

		for (CarrySlot slot : slots) {
			int color = slot.isComplete() ? DONE_COLOR : PROGRESS_COLOR;
			graphics.text(font, lineFor(slot), x + PADDING, lineY, color, true);
			lineY += LINE_HEIGHT;
		}
	}

	private static String titleText() {
		return "CarryTracker";
	}

	private static String lineFor(CarrySlot slot) {
		return "#" + slot.id + " " + slot.username + "  " + slot.kills + "/" + slot.killGoal();
	}
}
