package com.OGAS.combatflex.client;

import com.OGAS.combatflex.SkillData;
import com.OGAS.combatflex.SkillType;
import com.OGAS.combatflex.network.NetworkHandler;
import com.OGAS.combatflex.network.packet.SpendSkillPointPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class SkillTreeScreen extends Screen {
	private static final int PANEL_WIDTH = 360;
	private static final int PANEL_HEIGHT = 214;
	private static final int NODE_SIZE = 32;
	private static final int ICON_OFFSET = 8;
	private static final int ROW_GAP = 22;
	private static final int COMBAT_TREE_LEFT = 14;
	private static final int COMBAT_TREE_WIDTH = 224;
	private static final int FLIGHT_TREE_X = 292;
	private static final int FLIGHT_TREE_AREA_LEFT = 258;
	private static final int TOP_ROW_OFFSET = 60;
	private static final int MID_ROW_OFFSET = 112;
	private static final int BOTTOM_ROW_OFFSET = 164;
	private static final int HEADER_Y = 22;
	private static final int SUB_HEADER_Y = 36;
	private static final int LINE_COLOR = 0x8892A8C7;
	private static final int ACTIVE_LINE_COLOR = 0xAAEFCB65;
	private Button closeButton;

	public SkillTreeScreen() {
		super(Component.translatable("screen.cbtflex.skill_tree.title"));
	}

	@Override
	protected void init() {
		super.init();
		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		this.closeButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.cbtflex.skill_tree.close"),
				ignored -> this.onClose()).bounds(left + PANEL_WIDTH - 52, top + 10, 38, 16).build());
	}

	@Override
	public void tick() {
		super.tick();
		if (this.closeButton != null) {
			int left = (this.width - PANEL_WIDTH) / 2;
			int top = (this.height - PANEL_HEIGHT) / 2;
			this.closeButton.setX(left + PANEL_WIDTH - 52);
			this.closeButton.setY(top + 10);
		}
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(guiGraphics);

		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		guiGraphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xD0181E28);
		guiGraphics.fill(left + 2, top + 2, left + PANEL_WIDTH - 2, top + PANEL_HEIGHT - 2, 0xD02A3345);
		guiGraphics.fill(left + 10, top + 50, left + PANEL_WIDTH - 10, top + PANEL_HEIGHT - 14, 0x5B111822);

		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, top + 10, 0xFFF6E7B0);

		Player player = getPlayer();
		int points = player == null ? 0 : SkillData.getPoints(player);
		float progress = player == null ? 0.0F : SkillData.getDamageProgress(player);
		guiGraphics.drawString(this.font, Component.translatable("screen.cbtflex.skill_tree.points", points), left + 16,
				top + HEADER_Y, 0xFFFFFFFF, false);
		guiGraphics.drawString(this.font,
				Component.translatable("screen.cbtflex.skill_tree.progress",
						String.format(Locale.ROOT, "%.1f", progress),
						String.format(Locale.ROOT, "%.0f", SkillData.getDamagePerPointThreshold())),
				left + 122, top + HEADER_Y, 0xFFB9D6FF, false);
		guiGraphics.drawString(this.font, Component.translatable("screen.cbtflex.skill_tree.scroll_hint"), left + 16,
				top + SUB_HEADER_Y, 0xFF9EB4D6, false);
		guiGraphics.fill(left + FLIGHT_TREE_AREA_LEFT, top + 52, left + PANEL_WIDTH - 14, top + PANEL_HEIGHT - 14,
				0x2E111A24);
		guiGraphics.fill(left + FLIGHT_TREE_AREA_LEFT - 9, top + 58, left + FLIGHT_TREE_AREA_LEFT - 8,
				top + PANEL_HEIGHT - 18, 0x55617087);

		renderConnections(guiGraphics, player);
		for (SkillType skillType : SkillType.values()) {
			renderSkill(guiGraphics, skillType, getNodeX(skillType), getNodeY(skillType), player, mouseX, mouseY);
		}

		super.render(guiGraphics, mouseX, mouseY, partialTick);
		renderHoveredTooltip(guiGraphics, mouseX, mouseY, player);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}

		Player player = getPlayer();
		for (SkillType skillType : SkillType.values()) {
			if (!isNodeHovered((int) mouseX, (int) mouseY, getNodeX(skillType), getNodeY(skillType))) {
				continue;
			}

			if (player != null
					&& !SkillData.hasSkill(player, skillType)
					&& SkillData.meetsUnlockRequirements(player, skillType)
					&& SkillData.getPoints(player) >= skillType.cost()) {
				purchase(skillType);
			}
			return true;
		}

		return false;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (ClientKeyMappings.OPEN_SKILL_TREE.matches(keyCode, scanCode)) {
			this.onClose();
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void renderSkill(GuiGraphics guiGraphics, SkillType skillType, int x, int y, Player player, int mouseX,
			int mouseY) {
		boolean unlocked = player != null && SkillData.hasSkill(player, skillType);
		boolean requirementsMet = player != null && SkillData.meetsUnlockRequirements(player, skillType);
		boolean hovered = isNodeHovered(mouseX, mouseY, x, y);
		int fillColor = unlocked ? 0xC02E5A40 : !requirementsMet ? 0xB8352426 : hovered ? 0xD03B4E66 : 0xC0263142;
		int borderColor = unlocked ? 0xFF79D79B : !requirementsMet ? 0xFFA06363 : hovered ? 0xFFEFCB65 : 0xFF7B8FAC;
		guiGraphics.fill(x, y, x + NODE_SIZE, y + NODE_SIZE, fillColor);
		guiGraphics.fill(x, y, x + NODE_SIZE, y + 1, borderColor);
		guiGraphics.fill(x, y + NODE_SIZE - 1, x + NODE_SIZE, y + NODE_SIZE, borderColor);
		guiGraphics.fill(x, y, x + 1, y + NODE_SIZE, borderColor);
		guiGraphics.fill(x + NODE_SIZE - 1, y, x + NODE_SIZE, y + NODE_SIZE, borderColor);
		guiGraphics.renderItem(skillType.iconStack(), x + ICON_OFFSET, y + ICON_OFFSET);

		if (!requirementsMet && !unlocked) {
			guiGraphics.fill(x + 2, y + 2, x + NODE_SIZE - 2, y + NODE_SIZE - 2, 0x6622181A);
			guiGraphics.drawString(this.font, "!", x + 12, y + 10, 0xFFFFC766, false);
		}

		if (unlocked) {
			guiGraphics.fill(x + NODE_SIZE - 7, y + 3, x + NODE_SIZE - 3, y + 7, 0xFF79D79B);
		}
	}

	private void purchase(SkillType skillType) {
		NetworkHandler.sendToServer(new SpendSkillPointPacket(skillType.id()));
	}

	private Component getCooldownText(Player player, SkillType skillType) {
		if (player == null || !SkillData.hasSkill(player, skillType)) {
			return Component.translatable("screen.cbtflex.skill_tree.cooldown_inactive");
		}

		if (skillType.cooldownTicks() <= 0L) {
			return Component.translatable("screen.cbtflex.skill_tree.cooldown_passive");
		}

		long remainingTicks = SkillData.getRemainingCooldownTicks(player, skillType, player.level().getGameTime());
		if (remainingTicks <= 0L) {
			return Component.translatable("screen.cbtflex.skill_tree.cooldown_ready");
		}

		return Component.translatable("screen.cbtflex.skill_tree.cooldown_active",
				String.format(Locale.ROOT, "%.1f", remainingTicks / 20.0D));
	}

	private void renderHoveredTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, Player player) {
		for (SkillType skillType : SkillType.values()) {
			int nodeX = getNodeX(skillType);
			int nodeY = getNodeY(skillType);
			if (isNodeHovered(mouseX, mouseY, nodeX, nodeY)) {
				boolean unlocked = player != null && SkillData.hasSkill(player, skillType);
				boolean requirementsMet = player != null && SkillData.meetsUnlockRequirements(player, skillType);
				List<Component> tooltip = new ArrayList<>();
				tooltip.add(Component.translatable(skillType.nameKey()));
				tooltip.add(Component.translatable(skillType.descriptionKey()));
				tooltip.add(Component.translatable("screen.cbtflex.skill_tree.cost", skillType.cost()));
				tooltip.add(Component.translatable(getStatusTranslationKey(unlocked, requirementsMet)));
				if (player != null && !requirementsMet) {
					tooltip.add(Component.translatable(getRequirementTooltipKey(skillType)));
				}
				tooltip.add(getCooldownText(player, skillType));
				guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
				return;
			}
		}
	}

	private void renderConnections(GuiGraphics guiGraphics, Player player) {
		List<SkillType> oneCostSkills = getRowSkills(SkillType.SkillBranch.COMBAT, 1);
		List<SkillType> twoCostSkills = getRowSkills(SkillType.SkillBranch.COMBAT, 2);
		List<SkillType> threeCostSkills = getRowSkills(SkillType.SkillBranch.COMBAT, 3);

		for (SkillType from : oneCostSkills) {
			for (SkillType to : twoCostSkills) {
				renderConnection(guiGraphics, from, to, player);
			}
		}

		for (SkillType from : twoCostSkills) {
			for (SkillType to : threeCostSkills) {
				renderConnection(guiGraphics, from, to, player);
			}
		}

		renderConnection(guiGraphics, SkillType.SLOW_FLIGHT, SkillType.FAST_FLIGHT, player);
	}

	private void renderConnection(GuiGraphics guiGraphics, SkillType from, SkillType to, Player player) {
		int startX = getNodeX(from) + NODE_SIZE / 2;
		int startY = getNodeY(from) + NODE_SIZE;
		int endX = getNodeX(to) + NODE_SIZE / 2;
		int endY = getNodeY(to);
		int midY = startY + ((endY - startY) / 2);
		int color = isConnectionActive(from, to, player) ? ACTIVE_LINE_COLOR : LINE_COLOR;
		fillLine(guiGraphics, startX, startY, startX, midY, color);
		fillLine(guiGraphics, Math.min(startX, endX), midY, Math.max(startX, endX), midY + 1, color);
		fillLine(guiGraphics, endX, midY, endX, endY, color);
	}

	private void fillLine(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
		guiGraphics.fill(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2) + 1, Math.max(y1, y2) + 1, color);
	}

	private boolean isConnectionActive(SkillType from, SkillType to, Player player) {
		return player != null && SkillData.hasSkill(player, from) && SkillData.meetsUnlockRequirements(player, to);
	}

	private boolean isNodeHovered(int mouseX, int mouseY, int x, int y) {
		return mouseX >= x && mouseX <= x + NODE_SIZE && mouseY >= y && mouseY <= y + NODE_SIZE;
	}

	private int getNodeX(SkillType skillType) {
		int left = (this.width - PANEL_WIDTH) / 2;
		if (skillType.branch() == SkillType.SkillBranch.FLIGHT) {
			return left + FLIGHT_TREE_X;
		}

		List<SkillType> rowSkills = getRowSkills(skillType.branch(), skillType.tier());
		int rowWidth = rowSkills.size() * NODE_SIZE + Math.max(0, rowSkills.size() - 1) * ROW_GAP;
		int rowStart = left + COMBAT_TREE_LEFT + (COMBAT_TREE_WIDTH - rowWidth) / 2;
		return rowStart + rowSkills.indexOf(skillType) * (NODE_SIZE + ROW_GAP);
	}

	private int getNodeY(SkillType skillType) {
		int top = (this.height - PANEL_HEIGHT) / 2;
		if (skillType.tier() == 1) {
			return top + TOP_ROW_OFFSET;
		}

		if (skillType.tier() == 2) {
			return top + MID_ROW_OFFSET;
		}

		return top + BOTTOM_ROW_OFFSET;
	}

	private List<SkillType> getRowSkills(SkillType.SkillBranch branch, int tier) {
		List<SkillType> rowSkills = new ArrayList<>();
		for (SkillType skillType : SkillType.values()) {
			if (skillType.branch() == branch && skillType.tier() == tier) {
				rowSkills.add(skillType);
			}
		}
		return rowSkills;
	}

	private String getStatusTranslationKey(boolean unlocked, boolean requirementsMet) {
		if (unlocked) {
			return "screen.cbtflex.skill_tree.unlocked";
		}

		if (!requirementsMet) {
			return "screen.cbtflex.skill_tree.requirement_locked";
		}

		return "screen.cbtflex.skill_tree.locked";
	}

	private String getRequirementTooltipKey(SkillType skillType) {
		if (skillType == SkillType.FAST_FLIGHT) {
			return "screen.cbtflex.skill_tree.require_slow_flight";
		}

		return skillType.tier() >= 3
				? "screen.cbtflex.skill_tree.require_all_other_skills"
				: "screen.cbtflex.skill_tree.require_two_basic";
	}

	private Player getPlayer() {
		return Minecraft.getInstance().player;
	}
}