package com.OGAS.combatflex.grab;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GrabAndPullFeature.MOD_ID, value = Dist.CLIENT)
public final class ClientForgeEvents {
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

		boolean heavyHeld = ClientModEvents.HEAVY_ATTACK_KEY.isDown();
		if (heavyHeld) {
			heavyHeldTicks++;
			if (heavyHeldTicks >= GrabAndPullFeature.HEAVY_ATTACK_HOLD_TICKS) {
				heavyCharged = true;
			}
		}

		if (!heavyHeld && wasHeavyHeld) {
			if (heavyCharged) {
				GrabAndPullFeature.CHANNEL.sendToServer(new InputActionPacket(InputAction.HEAVY_ATTACK));
			} else if (heavyHeldTicks > 0) {
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
}