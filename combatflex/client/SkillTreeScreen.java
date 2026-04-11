package com.OGAS.combatflex.client;

import com.OGAS.combatflex.SkillData;
import com.OGAS.combatflex.SkillType;
import com.OGAS.combatflex.SpecialSkillFeature;
import com.OGAS.combatflex.SpecialSkillType;
import com.OGAS.combatflex.network.NetworkHandler;
import com.OGAS.combatflex.network.packet.SpendSpecialSkillPointPacket;
import com.OGAS.combatflex.network.packet.SpendSkillPointPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public class SkillTreeScreen extends Screen {
	private static final int PANEL_WIDTH = 320;
	private static final int PANEL_HEIGHT = 252;
	private static final int NODE_SIZE = 32;
	private static final int ICON_OFFSET = 8;
	private static final int ROW_GAP = 22;
	private static final int CONTENT_AREA_LEFT = 14;
	private static final int CONTENT_AREA_WIDTH = PANEL_WIDTH - 28;
	private static final int TAB_WIDTH = 88;
	private static final int TAB_HEIGHT = 18;
	private static final int TAB_GAP = 7;
	private static final int TAB_Y = 28;
	private static final int TAB_VIEWPORT_HEIGHT = 22;
	private static final int TAB_SCROLLBAR_Y = 49;
	private static final int POINTS_Y = 58;
	private static final int PROGRESS_Y = 72;
	private static final int HINT_Y = 86;
	private static final int CONTENT_TOP = 104;
	private static final int CONTENT_BOTTOM = PANEL_HEIGHT - 18;
	private static final int CONTENT_VIEWPORT_HEIGHT = CONTENT_BOTTOM - CONTENT_TOP;
	private static final int CONTENT_TOTAL_HEIGHT = 190;
	private static final int BRANCH_TITLE_Y = 10;
	private static final int ROW1_Y = 42;
	private static final int ROW2_Y = 98;
	private static final int ROW3_Y = 154;
	private static final int SPECIAL_NODE_SIZE = 24;
	private static final int SPECIAL_NODE_RIGHT_PADDING = 18;
	private static final int LINE_COLOR = 0x8892A8C7;
	private static final int ACTIVE_LINE_COLOR = 0xAAEFCB65;
	private float tabScroll;
	private float contentScroll;
	private boolean draggingTabScrollbar;
	private boolean draggingContentScrollbar;
	private float scrollbarGrabOffset;
	private SkillType.SkillBranch activeBranch = SkillType.SkillBranch.COMBAT;
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
		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		if (this.closeButton != null) {
			this.closeButton.setX(left + PANEL_WIDTH - 52);
			this.closeButton.setY(top + 10);
		}
		this.tabScroll = Mth.clamp(this.tabScroll, 0.0F, getMaxTabScroll());
		this.contentScroll = Mth.clamp(this.contentScroll, 0.0F, getMaxContentScroll());
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(guiGraphics);

		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		guiGraphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xD0181E28);
		guiGraphics.fill(left + 2, top + 2, left + PANEL_WIDTH - 2, top + PANEL_HEIGHT - 2, 0xD02A3345);
		guiGraphics.fill(left + 10, top + 50, left + PANEL_WIDTH - 10, top + PANEL_HEIGHT - 14, 0x5B111822);
		renderTabBar(guiGraphics, left, top, mouseX, mouseY);

		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, top + 10, 0xFFF6E7B0);

		Player player = getPlayer();
		int points = player == null ? 0 : SkillData.getPoints(player);
		float progress = player == null ? 0.0F : SkillData.getDamageProgress(player);
		guiGraphics.drawString(this.font, Component.translatable("screen.cbtflex.skill_tree.points", points), left + 16,
				top + POINTS_Y, 0xFFFFFFFF, false);
		guiGraphics.drawString(this.font,
				Component.translatable("screen.cbtflex.skill_tree.progress",
						String.format(Locale.ROOT, "%.1f", progress),
						String.format(Locale.ROOT, "%.0f", SkillData.getDamagePerPointThreshold())),
				left + 16, top + PROGRESS_Y, 0xFFB9D6FF, false);
		guiGraphics.drawString(this.font, Component.translatable("screen.cbtflex.skill_tree.scroll_hint"), left + 16,
				top + HINT_Y, 0xFF9EB4D6, false);
		renderBranchPanel(guiGraphics, left, top, player, mouseX, mouseY);

		super.render(guiGraphics, mouseX, mouseY, partialTick);
		renderHoveredTooltip(guiGraphics, mouseX, mouseY, player);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}

		if (button == 0 && tryStartScrollbarDrag((int) mouseX, (int) mouseY)) {
			return true;
		}

		SkillType.SkillBranch clickedBranch = getClickedBranch((int) mouseX, (int) mouseY);
		if (clickedBranch != null) {
			this.activeBranch = clickedBranch;
			return true;
		}

		Player player = getPlayer();
		if (this.activeBranch == SkillType.SkillBranch.ACTIVE) {
			for (SpecialSkillType specialSkillType : SpecialSkillType.values()) {
				if (!isSpecialNodeHovered((int) mouseX, (int) mouseY, specialSkillType)) {
					continue;
				}

				if (player != null
						&& !SkillData.hasSpecialSkill(player, specialSkillType)
						&& SkillData.meetsSpecialUnlockRequirements(player, specialSkillType)
						&& SkillData.getPoints(player) >= specialSkillType.cost()) {
					purchase(specialSkillType);
				}
				return true;
			}
		}

		for (SkillType skillType : getVisibleSkills()) {
			if (!isNodeHovered((int) mouseX, (int) mouseY, skillType)) {
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
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		if (isInside((int) mouseX, (int) mouseY, left + CONTENT_AREA_LEFT, top + TAB_Y, CONTENT_AREA_WIDTH,
				TAB_VIEWPORT_HEIGHT)) {
			this.tabScroll = Mth.clamp(this.tabScroll - (float) delta * 20.0F, 0.0F, getMaxTabScroll());
			return true;
		}

		if (isInside((int) mouseX, (int) mouseY, left + CONTENT_AREA_LEFT, top + CONTENT_TOP, CONTENT_AREA_WIDTH,
				CONTENT_VIEWPORT_HEIGHT)) {
			this.contentScroll = Mth.clamp(this.contentScroll - (float) delta * 18.0F, 0.0F, getMaxContentScroll());
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (button == 0) {
			if (this.draggingTabScrollbar) {
				updateTabScrollFromDrag((float) mouseX);
				return true;
			}

			if (this.draggingContentScrollbar) {
				updateContentScrollFromDrag((float) mouseY);
				return true;
			}
		}

		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 0 && (this.draggingTabScrollbar || this.draggingContentScrollbar)) {
			this.draggingTabScrollbar = false;
			this.draggingContentScrollbar = false;
			this.scrollbarGrabOffset = 0.0F;
			return true;
		}

		return super.mouseReleased(mouseX, mouseY, button);
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

	private void renderBranchPanel(GuiGraphics guiGraphics, int left, int top, Player player, int mouseX,
			int mouseY) {
		int areaLeft = left + CONTENT_AREA_LEFT;
		int areaRight = areaLeft + CONTENT_AREA_WIDTH;
		int viewportTop = top + CONTENT_TOP;
		int viewportBottom = top + CONTENT_BOTTOM;
		guiGraphics.fill(areaLeft, viewportTop, areaRight, viewportBottom, 0x2E111A24);
		guiGraphics.enableScissor(areaLeft, viewportTop, areaRight, viewportBottom);
		int contentTop = viewportTop - Mth.floor(this.contentScroll);
		guiGraphics.drawCenteredString(this.font, Component.translatable(getBranchTitleKey(this.activeBranch)),
				areaLeft + CONTENT_AREA_WIDTH / 2, contentTop + BRANCH_TITLE_Y, 0xFFE8D8A4);
		renderConnections(guiGraphics, player, contentTop);
		for (SkillType skillType : getVisibleSkills()) {
			renderSkill(guiGraphics, skillType, getNodeX(skillType), getNodeY(skillType, contentTop), player, mouseX, mouseY);
		}
		if (this.activeBranch == SkillType.SkillBranch.ACTIVE) {
			for (SpecialSkillType specialSkillType : SpecialSkillType.values()) {
				renderSpecialSkill(guiGraphics, specialSkillType, getSpecialNodeX(), getSpecialNodeY(specialSkillType, contentTop),
						player, mouseX, mouseY);
			}
		}
		guiGraphics.disableScissor();
		renderContentScrollbar(guiGraphics, areaRight - 5, viewportTop + 4, viewportBottom - 4);
	}

	private void renderSkill(GuiGraphics guiGraphics, SkillType skillType, int x, int y, Player player, int mouseX,
			int mouseY) {
		boolean unlocked = player != null && SkillData.hasSkill(player, skillType);
		boolean requirementsMet = player != null && SkillData.meetsUnlockRequirements(player, skillType);
		boolean hovered = isNodeHovered(mouseX, mouseY, skillType);
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

	private void purchase(SpecialSkillType skillType) {
		NetworkHandler.sendToServer(new SpendSpecialSkillPointPacket(skillType.id()));
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
		if (this.activeBranch == SkillType.SkillBranch.ACTIVE) {
			for (SpecialSkillType specialSkillType : SpecialSkillType.values()) {
				if (!isSpecialNodeHovered(mouseX, mouseY, specialSkillType)) {
					continue;
				}

				boolean unlocked = player != null && SkillData.hasSpecialSkill(player, specialSkillType);
				boolean requirementsMet = player != null && SkillData.meetsSpecialUnlockRequirements(player, specialSkillType);
				List<Component> tooltip = new ArrayList<>();
				tooltip.add(Component.translatable(specialSkillType.nameKey()));
				tooltip.add(Component.translatable(specialSkillType.descriptionKey()));
				tooltip.add(Component.translatable("screen.cbtflex.skill_tree.cost", specialSkillType.cost()));
				tooltip.add(Component.translatable(getStatusTranslationKey(unlocked, requirementsMet)));
				if (!requirementsMet) {
					tooltip.add(Component.translatable("screen.cbtflex.special_skill.require_active_tier", specialSkillType.tier()));
				}
				if (player != null && unlocked) {
					long cooldownTicks = SpecialSkillFeature.getRemainingCooldownTicks(player, specialSkillType);
					tooltip.add(cooldownTicks > 0L
							? Component.translatable("screen.cbtflex.special_skill.cooldown",
									String.format(Locale.ROOT, "%.1f", cooldownTicks / 20.0D))
							: Component.translatable("screen.cbtflex.special_skill.use_ready"));
				}
				guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
				return;
			}
		}

		for (SkillType skillType : getVisibleSkills()) {
			if (!isNodeHovered(mouseX, mouseY, skillType)) {
				continue;
			}

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

	private void renderConnections(GuiGraphics guiGraphics, Player player, int contentTop) {
		if (this.activeBranch == SkillType.SkillBranch.FLIGHT) {
			renderConnection(guiGraphics, SkillType.SLOW_FLIGHT, SkillType.FAST_FLIGHT, player, contentTop);
			return;
		}

		List<SkillType> oneCostSkills = getRowSkills(this.activeBranch, 1);
		List<SkillType> twoCostSkills = getRowSkills(this.activeBranch, 2);
		List<SkillType> threeCostSkills = getRowSkills(this.activeBranch, 3);
		for (SkillType from : oneCostSkills) {
			for (SkillType to : twoCostSkills) {
				renderConnection(guiGraphics, from, to, player, contentTop);
			}
		}
		for (SkillType from : twoCostSkills) {
			for (SkillType to : threeCostSkills) {
				renderConnection(guiGraphics, from, to, player, contentTop);
			}
		}
		if (this.activeBranch == SkillType.SkillBranch.ACTIVE) {
			for (SpecialSkillType specialSkillType : SpecialSkillType.values()) {
				renderSpecialConnection(guiGraphics, getRowSkills(this.activeBranch, specialSkillType.tier()), specialSkillType,
						player, contentTop);
			}
		}
	}

	private void renderSpecialConnection(GuiGraphics guiGraphics, List<SkillType> rowSkills, SpecialSkillType specialSkillType,
			Player player, int contentTop) {
		if (rowSkills.isEmpty()) {
			return;
		}

		int startX = getNodeX(rowSkills.get(rowSkills.size() - 1)) + NODE_SIZE;
		int startY = getNodeY(rowSkills.get(rowSkills.size() - 1), contentTop) + (NODE_SIZE / 2);
		int endX = getSpecialNodeX();
		int endY = getSpecialNodeY(specialSkillType, contentTop) + (SPECIAL_NODE_SIZE / 2);
		int color = player != null && SkillData.getUnlockedSkillCountByTier(player, SkillType.SkillBranch.ACTIVE,
				specialSkillType.tier()) >= 1 ? ACTIVE_LINE_COLOR : LINE_COLOR;
		fillLine(guiGraphics, startX, startY, endX, endY, color);
	}

	private void renderConnection(GuiGraphics guiGraphics, SkillType from, SkillType to, Player player, int contentTop) {
		int startX = getNodeX(from) + NODE_SIZE / 2;
		int startY = getNodeY(from, contentTop) + NODE_SIZE;
		int endX = getNodeX(to) + NODE_SIZE / 2;
		int endY = getNodeY(to, contentTop);
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

	private boolean isNodeHovered(int mouseX, int mouseY, SkillType skillType) {
		int left = getNodeX(skillType);
		int top = getNodeY(skillType,
				(this.height - PANEL_HEIGHT) / 2 + CONTENT_TOP - Mth.floor(this.contentScroll));
		int panelLeft = (this.width - PANEL_WIDTH) / 2;
		int panelTop = (this.height - PANEL_HEIGHT) / 2;
		return mouseX >= left && mouseX <= left + NODE_SIZE
				&& mouseY >= top && mouseY <= top + NODE_SIZE
				&& isInside(mouseX, mouseY, panelLeft + CONTENT_AREA_LEFT, panelTop + CONTENT_TOP, CONTENT_AREA_WIDTH,
						CONTENT_VIEWPORT_HEIGHT);
	}

	private boolean isSpecialNodeHovered(int mouseX, int mouseY, SpecialSkillType specialSkillType) {
		int left = getSpecialNodeX();
		int top = getSpecialNodeY(specialSkillType,
				(this.height - PANEL_HEIGHT) / 2 + CONTENT_TOP - Mth.floor(this.contentScroll));
		int panelLeft = (this.width - PANEL_WIDTH) / 2;
		int panelTop = (this.height - PANEL_HEIGHT) / 2;
		return mouseX >= left && mouseX <= left + SPECIAL_NODE_SIZE
				&& mouseY >= top && mouseY <= top + SPECIAL_NODE_SIZE
				&& isInside(mouseX, mouseY, panelLeft + CONTENT_AREA_LEFT, panelTop + CONTENT_TOP, CONTENT_AREA_WIDTH,
						CONTENT_VIEWPORT_HEIGHT);
	}

	private int getNodeX(SkillType skillType) {
		int left = (this.width - PANEL_WIDTH) / 2;
		List<SkillType> rowSkills = getRowSkills(skillType.branch(), skillType.tier());
		int rowWidth = rowSkills.size() * NODE_SIZE + Math.max(0, rowSkills.size() - 1) * ROW_GAP;
		int rowStart = left + CONTENT_AREA_LEFT + (CONTENT_AREA_WIDTH - rowWidth) / 2;
		return rowStart + rowSkills.indexOf(skillType) * (NODE_SIZE + ROW_GAP);
	}

	private int getNodeY(SkillType skillType, int contentTop) {
		int baseOffset = switch (skillType.tier()) {
			case 1 -> ROW1_Y;
			case 2 -> ROW2_Y;
			default -> ROW3_Y;
		};
		return contentTop + baseOffset;
	}

	private int getSpecialNodeX() {
		int left = (this.width - PANEL_WIDTH) / 2;
		return left + CONTENT_AREA_LEFT + CONTENT_AREA_WIDTH - SPECIAL_NODE_SIZE - SPECIAL_NODE_RIGHT_PADDING;
	}

	private int getSpecialNodeY(SpecialSkillType specialSkillType, int contentTop) {
		return switch (specialSkillType.tier()) {
			case 1 -> contentTop + ROW1_Y + 4;
			case 2 -> contentTop + ROW2_Y + 4;
			default -> contentTop + ROW3_Y + 4;
		};
	}

	private void renderSpecialSkill(GuiGraphics guiGraphics, SpecialSkillType skillType, int x, int y, Player player,
			int mouseX, int mouseY) {
		boolean unlocked = player != null && SkillData.hasSpecialSkill(player, skillType);
		boolean requirementsMet = player != null && SkillData.meetsSpecialUnlockRequirements(player, skillType);
		boolean hovered = isSpecialNodeHovered(mouseX, mouseY, skillType);
		int fillColor = unlocked ? 0xC05F4632 : !requirementsMet ? 0xB8352426 : hovered ? 0xD05D4A39 : 0xC03A2D27;
		int borderColor = unlocked ? 0xFFF0C680 : !requirementsMet ? 0xFFA06363 : hovered ? 0xFFFFE2A1 : 0xFFC49A6C;
		guiGraphics.fill(x, y, x + SPECIAL_NODE_SIZE, y + SPECIAL_NODE_SIZE, fillColor);
		guiGraphics.fill(x, y, x + SPECIAL_NODE_SIZE, y + 1, borderColor);
		guiGraphics.fill(x, y + SPECIAL_NODE_SIZE - 1, x + SPECIAL_NODE_SIZE, y + SPECIAL_NODE_SIZE, borderColor);
		guiGraphics.fill(x, y, x + 1, y + SPECIAL_NODE_SIZE, borderColor);
		guiGraphics.fill(x + SPECIAL_NODE_SIZE - 1, y, x + SPECIAL_NODE_SIZE, y + SPECIAL_NODE_SIZE, borderColor);
		guiGraphics.renderItem(skillType.iconStack(), x + 4, y + 4);
		if (!requirementsMet && !unlocked) {
			guiGraphics.fill(x + 2, y + 2, x + SPECIAL_NODE_SIZE - 2, y + SPECIAL_NODE_SIZE - 2, 0x6622181A);
			guiGraphics.drawString(this.font, "!", x + 9, y + 7, 0xFFFFC766, false);
		}
		if (unlocked) {
			guiGraphics.fill(x + SPECIAL_NODE_SIZE - 6, y + 3, x + SPECIAL_NODE_SIZE - 3, y + 6, 0xFFF0C680);
		}
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

		if (skillType.branch() == SkillType.SkillBranch.ACTIVE) {
			return "screen.cbtflex.skill_tree.require_previous_active";
		}

		return skillType.tier() >= 3
				? "screen.cbtflex.skill_tree.require_all_other_skills"
				: "screen.cbtflex.skill_tree.require_two_basic";
	}

	private Player getPlayer() {
		return Minecraft.getInstance().player;
	}

	private String getBranchTitleKey(SkillType.SkillBranch branch) {
		return switch (branch) {
			case SURVIVAL -> "screen.cbtflex.skill_tree.branch.survival";
			case COMBAT -> "screen.cbtflex.skill_tree.branch.combat";
			case ACTIVE -> "screen.cbtflex.skill_tree.branch.active";
			case FLIGHT -> "screen.cbtflex.skill_tree.branch.flight";
		};
	}

	private List<SkillType> getVisibleSkills() {
		List<SkillType> visibleSkills = new ArrayList<>();
		for (SkillType skillType : SkillType.values()) {
			if (skillType.branch() == this.activeBranch) {
				visibleSkills.add(skillType);
			}
		}
		return visibleSkills;
	}

	private void renderTabBar(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
		int viewportLeft = left + CONTENT_AREA_LEFT;
		int viewportRight = viewportLeft + CONTENT_AREA_WIDTH;
		guiGraphics.fill(viewportLeft, top + TAB_Y + TAB_HEIGHT - 1, viewportRight, top + TAB_Y + TAB_HEIGHT, 0x77485A72);
		guiGraphics.enableScissor(viewportLeft, top + TAB_Y, viewportRight, top + TAB_Y + TAB_VIEWPORT_HEIGHT);
		for (SkillType.SkillBranch branch : SkillType.SkillBranch.values()) {
			renderTab(guiGraphics, left, top, branch, mouseX, mouseY);
		}
		guiGraphics.disableScissor();
		renderTabScrollbar(guiGraphics, viewportLeft, top + TAB_SCROLLBAR_Y, CONTENT_AREA_WIDTH);
	}

	private void renderTab(GuiGraphics guiGraphics, int left, int top, SkillType.SkillBranch branch, int mouseX,
			int mouseY) {
		int tabLeft = getTabLeft(left, branch);
		int tabTop = top + TAB_Y;
		int tabRight = tabLeft + TAB_WIDTH;
		int tabBottom = tabTop + TAB_HEIGHT;
		boolean active = branch == this.activeBranch;
		boolean hovered = isInside(mouseX, mouseY, tabLeft, tabTop, TAB_WIDTH, TAB_HEIGHT);
		int fillColor = active ? 0xD0415166 : hovered ? 0xB7344256 : 0x96212B36;
		int borderColor = active ? 0xFFF3D78A : hovered ? 0xFFAFBED2 : 0xFF69798D;
		int textColor = active ? 0xFFFFF3C6 : hovered ? 0xFFF1F5FA : 0xFFC1CFDE;

		guiGraphics.fill(tabLeft, tabTop + 3, tabRight, tabBottom, fillColor);
		guiGraphics.fill(tabLeft + 4, tabTop + 1, tabRight - 4, tabTop + 3, fillColor);
		guiGraphics.fill(tabLeft + 1, tabTop + 2, tabLeft + 4, tabTop + 4, fillColor);
		guiGraphics.fill(tabRight - 4, tabTop + 2, tabRight - 1, tabTop + 4, fillColor);
		guiGraphics.fill(tabLeft + 4, tabTop, tabRight - 4, tabTop + 1, borderColor);
		guiGraphics.fill(tabLeft + 2, tabTop + 1, tabLeft + 4, tabTop + 2, borderColor);
		guiGraphics.fill(tabRight - 4, tabTop + 1, tabRight - 2, tabTop + 2, borderColor);
		guiGraphics.fill(tabLeft, tabTop + 3, tabLeft + 1, tabBottom, borderColor);
		guiGraphics.fill(tabRight - 1, tabTop + 3, tabRight, tabBottom, borderColor);
		if (!active) {
			guiGraphics.fill(tabLeft, tabBottom - 1, tabRight, tabBottom, borderColor);
		}
		guiGraphics.drawCenteredString(this.font, Component.translatable(getBranchTitleKey(branch)),
				tabLeft + TAB_WIDTH / 2, tabTop + 5, textColor);
	}

	private SkillType.SkillBranch getClickedBranch(int mouseX, int mouseY) {
		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		for (SkillType.SkillBranch branch : SkillType.SkillBranch.values()) {
			if (isInside(mouseX, mouseY, getTabLeft(left, branch), top + TAB_Y, TAB_WIDTH, TAB_HEIGHT)) {
				return branch;
			}
		}
		return null;
	}

	private int getTabLeft(int left, SkillType.SkillBranch branch) {
		return left + CONTENT_AREA_LEFT + getBranchIndex(branch) * (TAB_WIDTH + TAB_GAP) - Mth.floor(this.tabScroll);
	}

	private boolean tryStartScrollbarDrag(int mouseX, int mouseY) {
		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		int[] tabThumb = getTabScrollbarThumb(left + CONTENT_AREA_LEFT, top + TAB_SCROLLBAR_Y, CONTENT_AREA_WIDTH);
		if (isInside(mouseX, mouseY, tabThumb[0], tabThumb[1], tabThumb[2], tabThumb[3])) {
			this.draggingTabScrollbar = true;
			this.draggingContentScrollbar = false;
			this.scrollbarGrabOffset = mouseX - tabThumb[0];
			return true;
		}

		int areaRight = left + CONTENT_AREA_LEFT + CONTENT_AREA_WIDTH;
		int viewportTop = top + CONTENT_TOP;
		int viewportBottom = top + CONTENT_BOTTOM;
		int[] contentThumb = getContentScrollbarThumb(areaRight - 5, viewportTop + 4, viewportBottom - 4);
		if (isInside(mouseX, mouseY, contentThumb[0], contentThumb[1], contentThumb[2], contentThumb[3])) {
			this.draggingContentScrollbar = true;
			this.draggingTabScrollbar = false;
			this.scrollbarGrabOffset = mouseY - contentThumb[1];
			return true;
		}

		return false;
	}

	private int getBranchIndex(SkillType.SkillBranch branch) {
		SkillType.SkillBranch[] branches = SkillType.SkillBranch.values();
		for (int index = 0; index < branches.length; index++) {
			if (branches[index] == branch) {
				return index;
			}
		}
		return 0;
	}

	private float getMaxTabScroll() {
		int totalWidth = SkillType.SkillBranch.values().length * TAB_WIDTH
				+ Math.max(0, SkillType.SkillBranch.values().length - 1) * TAB_GAP;
		return Math.max(0, totalWidth - CONTENT_AREA_WIDTH);
	}

	private float getMaxContentScroll() {
		return Math.max(0, CONTENT_TOTAL_HEIGHT - CONTENT_VIEWPORT_HEIGHT);
	}

	private void renderTabScrollbar(GuiGraphics guiGraphics, int left, int top, int width) {
		guiGraphics.fill(left, top, left + width, top + 3, 0x66344152);
		int[] thumb = getTabScrollbarThumb(left, top, width);
		guiGraphics.fill(thumb[0], thumb[1], thumb[0] + thumb[2], thumb[1] + thumb[3], 0xAAAFBED2);
	}

	private void renderContentScrollbar(GuiGraphics guiGraphics, int x, int top, int bottom) {
		guiGraphics.fill(x, top, x + 3, bottom, 0x66344152);
		int[] thumb = getContentScrollbarThumb(x, top, bottom);
		guiGraphics.fill(thumb[0], thumb[1], thumb[0] + thumb[2], thumb[1] + thumb[3], 0xAAAFBED2);
	}

	private int[] getTabScrollbarThumb(int left, int top, int width) {
		float maxScroll = getMaxTabScroll();
		if (maxScroll <= 0.0F) {
			return new int[] { left, top, width, 3 };
		}

		int thumbWidth = Math.max(48, Math.round(width * (width / (float) (width + maxScroll))));
		int thumbLeft = left + Math.round((width - thumbWidth) * (this.tabScroll / maxScroll));
		return new int[] { thumbLeft, top, thumbWidth, 3 };
	}

	private int[] getContentScrollbarThumb(int x, int top, int bottom) {
		float maxScroll = getMaxContentScroll();
		int height = bottom - top;
		if (maxScroll <= 0.0F) {
			return new int[] { x, top, 3, height };
		}

		int thumbHeight = Math.max(24, Math.round(height * (CONTENT_VIEWPORT_HEIGHT / (float) CONTENT_TOTAL_HEIGHT)));
		int thumbTop = top + Math.round((height - thumbHeight) * (this.contentScroll / maxScroll));
		return new int[] { x, thumbTop, 3, thumbHeight };
	}

	private void updateTabScrollFromDrag(float mouseX) {
		float maxScroll = getMaxTabScroll();
		if (maxScroll <= 0.0F) {
			this.tabScroll = 0.0F;
			return;
		}

		int left = (this.width - PANEL_WIDTH) / 2 + CONTENT_AREA_LEFT;
		int trackWidth = CONTENT_AREA_WIDTH;
		int[] thumb = getTabScrollbarThumb(left, 0, trackWidth);
		float trackRange = Math.max(1.0F, trackWidth - thumb[2]);
		float thumbLeft = Mth.clamp(mouseX - this.scrollbarGrabOffset, left, left + trackWidth - thumb[2]);
		this.tabScroll = ((thumbLeft - left) / trackRange) * maxScroll;
	}

	private void updateContentScrollFromDrag(float mouseY) {
		float maxScroll = getMaxContentScroll();
		if (maxScroll <= 0.0F) {
			this.contentScroll = 0.0F;
			return;
		}

		int top = (this.height - PANEL_HEIGHT) / 2 + CONTENT_TOP + 4;
		int bottom = (this.height - PANEL_HEIGHT) / 2 + CONTENT_BOTTOM - 4;
		int[] thumb = getContentScrollbarThumb(0, top, bottom);
		float trackRange = Math.max(1.0F, (bottom - top) - thumb[3]);
		float thumbTop = Mth.clamp(mouseY - this.scrollbarGrabOffset, top, bottom - thumb[3]);
		this.contentScroll = ((thumbTop - top) / trackRange) * maxScroll;
	}

	private boolean isInside(int mouseX, int mouseY, int left, int top, int width, int height) {
		return mouseX >= left && mouseX <= left + width && mouseY >= top && mouseY <= top + height;
	}
}