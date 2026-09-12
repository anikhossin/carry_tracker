package dev.carrytracker.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class HudEditorScreen extends Screen {
	private final CarryHud hud;
	private final HudSettings settings;
	private boolean dragging;
	private int dragOffsetX;
	private int dragOffsetY;
	private Button toggleButton;

	public HudEditorScreen(CarryHud hud) {
		super(Component.literal("CarryTracker HUD Editor"));
		this.hud = hud;
		this.settings = hud.settings();
	}

	@Override
	protected void init() {
		hud.setEditorOpen(true);

		int buttonWidth = 100;
		int buttonHeight = 20;
		int gap = 8;
		int totalWidth = buttonWidth * 3 + gap * 2;
		int startX = (this.width - totalWidth) / 2;
		int y = this.height - 36;

		toggleButton = addRenderableWidget(Button.builder(toggleLabel(settings.visible), button -> {
			settings.toggleVisible();
			button.setMessage(toggleLabel(settings.visible));
		}).bounds(startX, y, buttonWidth, buttonHeight).build());

		addRenderableWidget(Button.builder(Component.literal("Reset"), button -> {
			settings.reset();
		}).bounds(startX + buttonWidth + gap, y, buttonWidth, buttonHeight).build());

		addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
			.bounds(startX + (buttonWidth + gap) * 2, y, buttonWidth, buttonHeight).build());
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(0, 0, this.width, this.height, 0x66000000);
		graphics.fill(0, this.height - 52, this.width, this.height, 0xCC000000);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		graphics.text(this.font, "HUD Editor", 8, 8, 0xFFFFAA00, true);
		graphics.text(this.font, "Drag the HUD box to move it.", 8, 20, 0xFFFFFFFF, true);
		graphics.text(this.font, "Arrow keys nudge 1px. Hold Shift for 10px.", 8, 32, 0xFFCCCCCC, true);
		graphics.text(this.font, "Position: " + settings.x + ", " + settings.y, 8, 44, 0xFFFFFF55, true);

		if (hud.contains(mouseX, mouseY) || dragging) {
			graphics.text(this.font, "Dragging...", settings.x, settings.y - 12, 0xFFFFAA00, true);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0 && hud.contains(event.x(), event.y())) {
			dragging = true;
			dragOffsetX = (int) event.x() - settings.x;
			dragOffsetY = (int) event.y() - settings.y;
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging) {
			settings.setPosition(
				(int) event.x() - dragOffsetX,
				(int) event.y() - dragOffsetY,
				this.width,
				this.height,
				hud.boxWidth(),
				hud.boxHeight()
			);
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging) {
			dragging = false;
			settings.save();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int step = isShiftHeld() ? 10 : 1;
		int key = event.key();

		if (key == InputConstants.KEY_LEFT) {
			settings.nudge(-step, 0, this.width, this.height, hud.boxWidth(), hud.boxHeight());
			return true;
		}
		if (key == InputConstants.KEY_RIGHT) {
			settings.nudge(step, 0, this.width, this.height, hud.boxWidth(), hud.boxHeight());
			return true;
		}
		if (key == InputConstants.KEY_UP) {
			settings.nudge(0, -step, this.width, this.height, hud.boxWidth(), hud.boxHeight());
			return true;
		}
		if (key == InputConstants.KEY_DOWN) {
			settings.nudge(0, step, this.width, this.height, hud.boxWidth(), hud.boxHeight());
			return true;
		}

		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		hud.setEditorOpen(false);
		settings.save();
		this.minecraft.setScreen(null);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private boolean isShiftHeld() {
		return InputConstants.isKeyDown(this.minecraft.getWindow(), InputConstants.KEY_LSHIFT)
			|| InputConstants.isKeyDown(this.minecraft.getWindow(), InputConstants.KEY_RSHIFT);
	}

	private static Component toggleLabel(boolean visible) {
		return Component.literal(visible ? "HUD: On" : "HUD: Off");
	}

	public static void open(CarryHud hud) {
		var client = net.minecraft.client.Minecraft.getInstance();
		client.execute(() -> client.setScreen(new HudEditorScreen(hud)));
		GameMessages.local(Component.literal("Opened HUD editor.").withStyle(ChatFormatting.GREEN));
	}
}
