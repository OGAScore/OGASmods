package com.OGAS.combatflex.client;

import com.OGAS.combatflex.SkillType;
import com.OGAS.combatflex.CombatFlexMod;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CombatFlexMod.MOD_ID, value = Dist.CLIENT)
public final class ClientForgeEvents {
	private static final float FATIGUE_TEXT_SCALE = 0.65F;
	private static final float COOLDOWN_TEXT_SCALE = 0.65F;
	private static final int FATIGUE_ROW_HEIGHT = 15;
	private static final int FATIGUE_PANEL_HEIGHT = 12;
	private static final int FATIGUE_BAR_WIDTH = 52;
	private static final int FATIGUE_MARGIN = 8;
	private static final int PASSIVE_COOLDOWN_ICON_SIZE = 14;
	private static final int PASSIVE_COOLDOWN_SLOT_SIZE = 16;
	private static final int PASSIVE_COOLDOWN_GAP = 4;
	private static final int PASSIVE_COOLDOWN_TOP = 3;

	private ClientForgeEvents() {
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return;
		}

		if (minecraft.screen == null) {
			while (ClientKeyMappings.OPEN_SKILL_TREE.consumeClick()) {
				if (com.OGAS.combatflex.SkillData.canOpenSkillTree(minecraft.player)) {
					minecraft.setScreen(new SkillTreeScreen());
				} else {
					float remainingDamage = Math.max(0.0F,
							com.OGAS.combatflex.SkillData.getSkillTreeUnlockDamageThreshold()
									- com.OGAS.combatflex.SkillData.getSkillTreeUnlockProgress(minecraft.player));
					minecraft.player.playSound(SoundEvents.VILLAGER_NO, 0.7F, 0.85F);
					minecraft.player.displayClientMessage(
							Component.translatable("message.cbtflex.skill_tree_locked",
									String.format(Locale.ROOT, "%.1f", remainingDamage)),
							true);
				}
			}

			if (ClientKeyMappings.OPEN_SPECIAL_SKILLS.consumeClick()
					&& !com.OGAS.combatflex.SkillData.canOpenSkillTree(minecraft.player)) {
				float remainingDamage = Math.max(0.0F,
						com.OGAS.combatflex.SkillData.getSkillTreeUnlockDamageThreshold()
								- com.OGAS.combatflex.SkillData.getSkillTreeUnlockProgress(minecraft.player));
				minecraft.player.playSound(SoundEvents.VILLAGER_NO, 0.7F, 0.85F);
				minecraft.player.displayClientMessage(
						Component.translatable("message.cbtflex.skill_tree_locked",
								String.format(Locale.ROOT, "%.1f", remainingDamage)),
						true);
			}

			if (com.OGAS.combatflex.SkillData.canOpenSkillTree(minecraft.player)
					&& ClientKeyMappings.isSpecialSkillsKeyHeld(minecraft)) {
				minecraft.setScreen(new SpecialSkillScreen());
			}
		}
	}

	@SubscribeEvent
	public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.options.hideGui) {
			return;
		}

		renderPassiveCooldowns(event, minecraft);
		renderDamageFatigueIndicator(event, minecraft);
	}

	private static void renderDamageFatigueIndicator(RenderGuiOverlayEvent.Post event, Minecraft minecraft) {
		List<com.OGAS.combatflex.SkillData.DamageFatigueEntry> entries = com.OGAS.combatflex.SkillData
				.getRecentDamageFatigueEntries(minecraft.player);
		if (entries.isEmpty()) {
			return;
		}

		int screenWidth = minecraft.getWindow().getGuiScaledWidth();
		int maxPanelWidth = Math.max(88, (screenWidth / 4) - FATIGUE_MARGIN);
		int panelWidth = Math.min(132, maxPanelWidth);
		int left = screenWidth - FATIGUE_MARGIN - panelWidth;
		int nameWidth = Math.max(26, panelWidth - FATIGUE_BAR_WIDTH - 12);
		for (int index = 0; index < entries.size(); index++) {
			renderDamageFatigueEntry(event, minecraft, entries.get(index), left,
					FATIGUE_MARGIN + index * FATIGUE_ROW_HEIGHT, panelWidth, nameWidth);
		}
	}

	private static void renderDamageFatigueEntry(RenderGuiOverlayEvent.Post event, Minecraft minecraft,
			com.OGAS.combatflex.SkillData.DamageFatigueEntry entry, int left, int top, int panelWidth, int nameWidth) {
		int barLeft = left + 6 + nameWidth;
		int barTop = top + 3;
		int fillWidth = Math.max(0, Math.round((FATIGUE_BAR_WIDTH - 2) * Math.min(1.0F, entry.fatiguePercent() / 100.0F)));
		event.getGuiGraphics().fill(left, top, left + panelWidth, top + FATIGUE_PANEL_HEIGHT, 0x7A101822);
		renderScaledText(event.getGuiGraphics(), minecraft.font, trimToWidth(minecraft.font, entry.label(), nameWidth),
				left + 4, top + 3, 0xFFF3DFC2);
		event.getGuiGraphics().fill(barLeft, barTop, barLeft + FATIGUE_BAR_WIDTH, barTop + 5, 0xCC1D2630);
		event.getGuiGraphics().fill(barLeft, barTop, barLeft + FATIGUE_BAR_WIDTH, barTop + 1, 0xFFF0C680);
		event.getGuiGraphics().fill(barLeft, barTop + 4, barLeft + FATIGUE_BAR_WIDTH, barTop + 5, 0xFFF0C680);
		event.getGuiGraphics().fill(barLeft, barTop, barLeft + 1, barTop + 5, 0xFFF0C680);
		event.getGuiGraphics().fill(barLeft + FATIGUE_BAR_WIDTH - 1, barTop, barLeft + FATIGUE_BAR_WIDTH, barTop + 5,
				0xFFF0C680);
		if (fillWidth > 0) {
			event.getGuiGraphics().fill(barLeft + 1, barTop + 1, barLeft + 1 + fillWidth, barTop + 4, 0xFFD28734);
		}
	}

	private static void renderScaledText(net.minecraft.client.gui.GuiGraphics guiGraphics, Font font, String text, int x,
			int y, int color) {
		guiGraphics.pose().pushPose();
		guiGraphics.pose().scale(FATIGUE_TEXT_SCALE, FATIGUE_TEXT_SCALE, 1.0F);
		guiGraphics.drawString(font, text, Math.round(x / FATIGUE_TEXT_SCALE), Math.round(y / FATIGUE_TEXT_SCALE), color,
				false);
		guiGraphics.pose().popPose();
	}

	private static String trimToWidth(Font font, String text, int actualWidth) {
		int rawWidth = Math.max(1, Math.round(actualWidth / FATIGUE_TEXT_SCALE));
		if (font.width(text) <= rawWidth) {
			return text;
		}

		String suffix = "...";
		String trimmed = font.plainSubstrByWidth(text, Math.max(1, rawWidth - font.width(suffix)));
		return trimmed + suffix;
	}

	private static boolean shouldRenderHudEntry(Minecraft minecraft, SkillType skillType) {
		return skillType.cooldownTicks() > 0L && minecraft.player != null
				&& com.OGAS.combatflex.SkillData.hasSkill(minecraft.player, skillType);
	}

	private static void renderPassiveCooldowns(RenderGuiOverlayEvent.Post event, Minecraft minecraft) {
		List<SkillType> visibleSkills = new ArrayList<>();
		for (SkillType skillType : SkillType.values()) {
			if (shouldRenderHudEntry(minecraft, skillType)
					&& com.OGAS.combatflex.SkillData.getRemainingCooldownTicks(minecraft.player, skillType,
							minecraft.player.level().getGameTime()) > 0L) {
				visibleSkills.add(skillType);
			}
		}

		if (visibleSkills.isEmpty()) {
			return;
		}

		int totalWidth = visibleSkills.size() * PASSIVE_COOLDOWN_SLOT_SIZE
				+ Math.max(0, visibleSkills.size() - 1) * PASSIVE_COOLDOWN_GAP;
		int left = (minecraft.getWindow().getGuiScaledWidth() - totalWidth) / 2;
		for (int index = 0; index < visibleSkills.size(); index++) {
			renderPassiveCooldownIcon(event, minecraft, visibleSkills.get(index),
					left + index * (PASSIVE_COOLDOWN_SLOT_SIZE + PASSIVE_COOLDOWN_GAP), PASSIVE_COOLDOWN_TOP);
		}
	}

	private static void renderPassiveCooldownIcon(RenderGuiOverlayEvent.Post event, Minecraft minecraft, SkillType skillType,
			int x, int y) {
		int iconLeft = x + ((PASSIVE_COOLDOWN_SLOT_SIZE - PASSIVE_COOLDOWN_ICON_SIZE) / 2);
		int iconTop = y + ((PASSIVE_COOLDOWN_SLOT_SIZE - PASSIVE_COOLDOWN_ICON_SIZE) / 2);
		event.getGuiGraphics().renderItem(skillType.iconStack(), iconLeft, iconTop);
		long remainingTicks = com.OGAS.combatflex.SkillData.getRemainingCooldownTicks(minecraft.player, skillType,
				minecraft.player.level().getGameTime());
		if (remainingTicks > 0L) {
			event.getGuiGraphics().pose().pushPose();
			event.getGuiGraphics().pose().translate(0.0F, 0.0F, 200.0F);
			event.getGuiGraphics().fill(x + 1, y + (PASSIVE_COOLDOWN_SLOT_SIZE / 2) - 3,
					x + PASSIVE_COOLDOWN_SLOT_SIZE - 1, y + (PASSIVE_COOLDOWN_SLOT_SIZE / 2) + 2, 0xEE1F6AA5);
			renderScaledCenteredText(event.getGuiGraphics(), minecraft.font,
					String.format(Locale.ROOT, "%.0f", Math.ceil(remainingTicks / 20.0D)), x + (PASSIVE_COOLDOWN_SLOT_SIZE / 2),
					y + (PASSIVE_COOLDOWN_SLOT_SIZE / 2) - 3, 0xFFF8FCFF, COOLDOWN_TEXT_SCALE);
			event.getGuiGraphics().pose().popPose();
		}
	}

	private static void renderScaledCenteredText(net.minecraft.client.gui.GuiGraphics guiGraphics, Font font, String text,
			int centerX, int y, int color, float scale) {
		guiGraphics.pose().pushPose();
		guiGraphics.pose().scale(scale, scale, 1.0F);
		int scaledCenterX = Math.round(centerX / scale);
		int scaledY = Math.round(y / scale);
		guiGraphics.drawCenteredString(font, Component.literal(text), scaledCenterX, scaledY, color);
		guiGraphics.pose().popPose();
	}
}