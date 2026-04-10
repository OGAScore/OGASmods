package com.OGAS.combatflex.client;

import com.OGAS.combatflex.SkillType;
import com.OGAS.combatflex.CombatFlexMod;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CombatFlexMod.MOD_ID, value = Dist.CLIENT)
public final class ClientForgeEvents {
	private ClientForgeEvents() {
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.screen != null) {
			return;
		}

		while (ClientKeyMappings.OPEN_SKILL_TREE.consumeClick()) {
			if (com.OGAS.combatflex.SkillData.canOpenSkillTree(minecraft.player)) {
				minecraft.setScreen(new SkillTreeScreen());
			} else {
				float remainingDamage = Math.max(0.0F,
						com.OGAS.combatflex.SkillData.getSkillTreeUnlockDamageThreshold()
								- com.OGAS.combatflex.SkillData.getTotalReceivedDamage(minecraft.player));
				minecraft.player.playSound(SoundEvents.VILLAGER_NO, 0.7F, 0.85F);
				minecraft.player.displayClientMessage(
						Component.translatable("message.cbtflex.skill_tree_locked",
								String.format(Locale.ROOT, "%.1f", remainingDamage)),
						true);
			}
		}
	}

	@SubscribeEvent
	public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.options.hideGui) {
			return;
		}

		int activeSkillCount = 0;
		int width = 0;
		for (SkillType skillType : SkillType.values()) {
			if (!shouldRenderHudEntry(minecraft, skillType)) {
				continue;
			}

			activeSkillCount++;
			width = Math.max(width, minecraft.font.width(Component.translatable(skillType.nameKey())) + 52);
		}

		if (activeSkillCount <= 0) {
			return;
		}

		int x = 8;
		int y = 8;
		int rowHeight = 20;
		event.getGuiGraphics().fill(x - 4, y - 4, x + width + 12, y + activeSkillCount * rowHeight + 2, 0x80131A24);

		int index = 0;
		for (SkillType skillType : SkillType.values()) {
			if (!shouldRenderHudEntry(minecraft, skillType)) {
				continue;
			}

			renderHudEntry(event, minecraft, skillType, x, y + index * rowHeight);
			index++;
		}
	}

	private static boolean shouldRenderHudEntry(Minecraft minecraft, SkillType skillType) {
		return skillType.cooldownTicks() > 0L && minecraft.player != null
				&& com.OGAS.combatflex.SkillData.hasSkill(minecraft.player, skillType);
	}

	private static void renderHudEntry(RenderGuiOverlayEvent.Post event, Minecraft minecraft, SkillType skillType,
			int x, int y) {
		event.getGuiGraphics().renderItem(skillType.iconStack(), x, y);
		event.getGuiGraphics().drawString(minecraft.font, Component.translatable(skillType.nameKey()), x + 20, y + 1,
				0xFFF2F4F8, false);
		event.getGuiGraphics().drawString(minecraft.font, getCooldownText(minecraft, skillType), x + 20, y + 10,
				0xFFB9D6FF, false);
	}

	private static Component getCooldownText(Minecraft minecraft, SkillType skillType) {
		long remainingTicks = com.OGAS.combatflex.SkillData.getRemainingCooldownTicks(minecraft.player, skillType,
				minecraft.player.level().getGameTime());
		return Component.translatable("hud.cbtflex.cooldown_seconds",
				String.format(Locale.ROOT, "%.1f", remainingTicks / 20.0D));
	}
}