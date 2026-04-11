package com.OGAS.combatflex.client;

import com.OGAS.combatflex.SkillData;
import com.OGAS.combatflex.SpecialSkillFeature;
import com.OGAS.combatflex.SpecialSkillType;
import com.OGAS.combatflex.network.NetworkHandler;
import com.OGAS.combatflex.network.packet.UseSpecialSkillPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class SpecialSkillScreen extends Screen {
	private static final int BUTTON_SIZE = 20;
	private static final int ICON_SIZE = 16;
	private static final int BUTTON_GAP = 8;
	private static final int CROSSHAIR_OFFSET_Y = 34;
	private static final float COOLDOWN_TEXT_SCALE = 0.7F;

	public SpecialSkillScreen() {
		super(Component.translatable("screen.cbtflex.special_skill.title"));
	}

	@Override
	public void tick() {
		super.tick();
		if (ClientKeyMappings.isSpecialSkillsKeyHeld(Minecraft.getInstance())) {
			return;
		}

		Player player = Minecraft.getInstance().player;
		SpecialSkillType hoveredSkill = getHoveredSkill(getMouseX(), getMouseY());
		if (player != null && hoveredSkill != null && SkillData.hasSpecialSkill(player, hoveredSkill)) {
			NetworkHandler.sendToServer(new UseSpecialSkillPacket(hoveredSkill.id()));
		}
		this.onClose();
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		Player player = Minecraft.getInstance().player;

		for (int index = 0; index < SpecialSkillType.values().length; index++) {
			SpecialSkillType skillType = SpecialSkillType.values()[index];
			renderEntry(guiGraphics, skillType, getButtonLeft(index), getButtonTop(), mouseX, mouseY, player);
		}

		renderTooltip(guiGraphics, mouseX, mouseY, player);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (ClientKeyMappings.OPEN_SPECIAL_SKILLS.matches(keyCode, scanCode)) {
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void renderEntry(GuiGraphics guiGraphics, SpecialSkillType skillType, int x, int y, int mouseX,
			int mouseY, Player player) {
		boolean hovered = isInside(mouseX, mouseY, x, y, BUTTON_SIZE, BUTTON_SIZE);
		boolean unlocked = player != null && SkillData.hasSpecialSkill(player, skillType);
		long cooldownTicks = player == null ? 0L : SpecialSkillFeature.getRemainingCooldownTicks(player, skillType);

		if (hovered) {
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(x - 1.0F, y - 1.0F, 0.0F);
			guiGraphics.pose().scale(1.1F, 1.1F, 1.0F);
			guiGraphics.renderItem(skillType.iconStack(), 2, 2);
			guiGraphics.pose().popPose();
		} else {
			guiGraphics.renderItem(skillType.iconStack(), x + ((BUTTON_SIZE - ICON_SIZE) / 2), y + ((BUTTON_SIZE - ICON_SIZE) / 2));
		}
		if (!unlocked) {
			guiGraphics.fill(x + 2, y + 2, x + BUTTON_SIZE - 2, y + BUTTON_SIZE - 2, 0x8822181A);
			guiGraphics.drawString(this.font, "X", x + 6, y + 6, 0xFFD08989, false);
		}
		if (unlocked && cooldownTicks > 0L) {
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(0.0F, 0.0F, 200.0F);
			guiGraphics.fill(x + 1, y + (BUTTON_SIZE / 2) - 3, x + BUTTON_SIZE - 1, y + (BUTTON_SIZE / 2) + 2,
					0xEE1F6AA5);
			renderScaledCenteredText(guiGraphics,
					String.format(Locale.ROOT, "%.0f", Math.ceil(cooldownTicks / 20.0D)), x + (BUTTON_SIZE / 2),
					y + (BUTTON_SIZE / 2) - 3,
					0xFFF6E7B0);
			guiGraphics.pose().popPose();
		}
	}

	private void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, Player player) {
		if (player == null) {
			return;
		}

		for (int index = 0; index < SpecialSkillType.values().length; index++) {
			SpecialSkillType skillType = SpecialSkillType.values()[index];
			int left = getButtonLeft(index);
			int top = getButtonTop();
			if (!isInside(mouseX, mouseY, left, top, BUTTON_SIZE, BUTTON_SIZE)) {
				continue;
			}

			List<Component> tooltip = new ArrayList<>();
			tooltip.add(Component.translatable(skillType.nameKey()));
			tooltip.add(Component.translatable(skillType.descriptionKey()));
			if (SkillData.hasSpecialSkill(player, skillType)) {
				long cooldownTicks = SpecialSkillFeature.getRemainingCooldownTicks(player, skillType);
				tooltip.add(cooldownTicks > 0L
						? Component.translatable("screen.cbtflex.special_skill.cooldown",
								String.format(Locale.ROOT, "%.1f", cooldownTicks / 20.0D))
						: Component.translatable("screen.cbtflex.special_skill.use_ready"));
			} else {
				tooltip.add(Component.translatable("screen.cbtflex.special_skill.locked_in_tree"));
			}
			guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
			return;
		}
	}

	private SpecialSkillType getHoveredSkill(int mouseX, int mouseY) {
		for (int index = 0; index < SpecialSkillType.values().length; index++) {
			SpecialSkillType skillType = SpecialSkillType.values()[index];
			int left = getButtonLeft(index);
			int top = getButtonTop();
			if (isInside(mouseX, mouseY, left, top, BUTTON_SIZE, BUTTON_SIZE)) {
				return skillType;
			}
		}

		return null;
	}

	private int getButtonLeft(int index) {
		return getRowLeft() + index * (BUTTON_SIZE + BUTTON_GAP);
	}

	private int getButtonTop() {
		return Math.max(8, (this.height / 2) - CROSSHAIR_OFFSET_Y);
	}

	private int getRowLeft() {
		int rowWidth = (SpecialSkillType.values().length * BUTTON_SIZE)
				+ (Math.max(0, SpecialSkillType.values().length - 1) * BUTTON_GAP);
		return (this.width - rowWidth) / 2;
	}

	private int getMouseX() {
		Minecraft minecraft = Minecraft.getInstance();
		return (int) Math.round(minecraft.mouseHandler.xpos() * this.width / minecraft.getWindow().getScreenWidth());
	}

	private int getMouseY() {
		Minecraft minecraft = Minecraft.getInstance();
		return (int) Math.round(minecraft.mouseHandler.ypos() * this.height / minecraft.getWindow().getScreenHeight());
	}

	private boolean isInside(int mouseX, int mouseY, int left, int top, int width, int height) {
		return mouseX >= left && mouseX <= left + width && mouseY >= top && mouseY <= top + height;
	}

	private void renderScaledCenteredText(GuiGraphics guiGraphics, String text, int centerX, int y, int color) {
		guiGraphics.pose().pushPose();
		guiGraphics.pose().scale(COOLDOWN_TEXT_SCALE, COOLDOWN_TEXT_SCALE, 1.0F);
		int scaledCenterX = Math.round(centerX / COOLDOWN_TEXT_SCALE);
		int scaledY = Math.round(y / COOLDOWN_TEXT_SCALE);
		guiGraphics.drawCenteredString(this.font, Component.literal(text), scaledCenterX, scaledY, color);
		guiGraphics.pose().popPose();
	}
}
