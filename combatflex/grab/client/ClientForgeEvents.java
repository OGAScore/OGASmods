package com.OGAS.combatflex.grab;

import com.OGAS.combatflex.SkillData;
import com.OGAS.combatflex.SkillType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GrabAndPullFeature.MOD_ID, value = Dist.CLIENT)
public final class ClientForgeEvents {
	private static final int CHARGE_BAR_WIDTH = 104;
	private static final int CHARGE_BAR_HEIGHT = 8;
	private static final int CHARGE_BAR_Y_OFFSET = 58;
	private static int heavyHeldTicks;
	private static boolean wasHeavyHeld;
	private static boolean heavyCharged;
	private static int heldTicks;
	private static boolean wasHeld;
	private static boolean grabActive;

	private ClientForgeEvents() {
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.level == null) {
			heavyHeldTicks = 0;
			wasHeavyHeld = false;
			heavyCharged = false;
			heldTicks = 0;
			wasHeld = false;
			grabActive = false;
			return;
		}

		boolean hasQuickHeavy = SkillData.hasSkill(minecraft.player, SkillType.ACTIVE_HEAVY_ATTACK);
		boolean hasChargedHeavy = SkillData.hasSkill(minecraft.player, SkillType.ACTIVE_CHARGED_HEAVY);
		boolean heavyHeld = ClientModEvents.HEAVY_ATTACK_KEY.isDown();
		if (!hasQuickHeavy && !hasChargedHeavy) {
			heavyHeldTicks = 0;
			wasHeavyHeld = false;
			heavyCharged = false;
		} else if (heavyHeld) {
			heavyHeldTicks++;
			if (hasChargedHeavy && heavyHeldTicks >= GrabAndPullFeature.HEAVY_ATTACK_HOLD_TICKS) {
				heavyCharged = true;
			}
		}

		if (!heavyHeld && wasHeavyHeld) {
			if (heavyCharged && hasChargedHeavy) {
				GrabAndPullFeature.CHANNEL.sendToServer(new InputActionPacket(InputAction.HEAVY_ATTACK));
			} else if (heavyHeldTicks > 0 && hasQuickHeavy) {
				GrabAndPullFeature.CHANNEL.sendToServer(new InputActionPacket(InputAction.QUICK_HEAVY_ATTACK));
			}

			heavyHeldTicks = 0;
			heavyCharged = false;
		}

		if (!heavyHeld && !wasHeavyHeld) {
			heavyHeldTicks = 0;
		}

		boolean isHeld = ClientModEvents.GRAB_KEY.isDown();
		if (isHeld) {
			heldTicks++;
			if (!grabActive && heldTicks >= GrabAndPullFeature.GRAB_HOLD_TICKS) {
				GrabAndPullFeature.CHANNEL.sendToServer(new InputActionPacket(InputAction.START_GRAB));
				grabActive = true;
			}
		}

		if (!isHeld && wasHeld) {
			if (grabActive) {
				GrabAndPullFeature.CHANNEL.sendToServer(new InputActionPacket(InputAction.STOP_GRAB));
			} else if (heldTicks > 0) {
				GrabAndPullFeature.CHANNEL.sendToServer(new InputActionPacket(InputAction.DIRECT_THROW));
			}

			heldTicks = 0;
			grabActive = false;
		}

		if (!isHeld && !wasHeld) {
			heldTicks = 0;
		}

		wasHeavyHeld = heavyHeld;
		wasHeld = isHeld;
	}

	@SubscribeEvent
	public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.options.hideGui || minecraft.screen != null) {
			return;
		}

		if (!SkillData.hasSkill(minecraft.player, SkillType.ACTIVE_CHARGED_HEAVY)) {
			return;
		}

		if (heavyHeldTicks <= 0 && !ClientModEvents.HEAVY_ATTACK_KEY.isDown()) {
			return;
		}

		renderHeavyChargeBar(event.getGuiGraphics(), minecraft);
	}

	private static void renderHeavyChargeBar(GuiGraphics guiGraphics, Minecraft minecraft) {
		float progress = Math.min(1.0F, heavyHeldTicks / (float) GrabAndPullFeature.HEAVY_ATTACK_HOLD_TICKS);
		int screenWidth = minecraft.getWindow().getGuiScaledWidth();
		int screenHeight = minecraft.getWindow().getGuiScaledHeight();
		int left = (screenWidth - CHARGE_BAR_WIDTH) / 2;
		int top = screenHeight - CHARGE_BAR_Y_OFFSET;
		int fillWidth = Math.max(0, Math.round((CHARGE_BAR_WIDTH - 2) * progress));
		int fillColor = heavyCharged ? 0xFFE4BF57 : 0xFF85BEEA;
		int frameColor = heavyCharged ? 0xFFFFF0C7 : 0xFFBED8F0;
		Component label = Component.translatable(
				heavyCharged ? "hud.cbtflex.heavy_charge_ready" : "hud.cbtflex.heavy_charge");

		guiGraphics.fill(left - 2, top - 14, left + CHARGE_BAR_WIDTH + 2, top + CHARGE_BAR_HEIGHT + 2, 0x66101822);
		guiGraphics.drawCenteredString(minecraft.font, label, screenWidth / 2, top - 11,
				heavyCharged ? 0xFFFFE9A3 : 0xFFD7E8F7);
		guiGraphics.fill(left, top, left + CHARGE_BAR_WIDTH, top + CHARGE_BAR_HEIGHT, 0xCC1D2630);
		guiGraphics.fill(left, top, left + CHARGE_BAR_WIDTH, top + 1, frameColor);
		guiGraphics.fill(left, top + CHARGE_BAR_HEIGHT - 1, left + CHARGE_BAR_WIDTH, top + CHARGE_BAR_HEIGHT,
				frameColor);
		guiGraphics.fill(left, top, left + 1, top + CHARGE_BAR_HEIGHT, frameColor);
		guiGraphics.fill(left + CHARGE_BAR_WIDTH - 1, top, left + CHARGE_BAR_WIDTH, top + CHARGE_BAR_HEIGHT,
				frameColor);
		if (fillWidth > 0) {
			guiGraphics.fill(left + 1, top + 1, left + 1 + fillWidth, top + CHARGE_BAR_HEIGHT - 1, fillColor);
		}
	}
}