package com.OGAS.combatflex.grab;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = GrabAndPullFeature.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientModEvents {
	static final KeyMapping HEAVY_ATTACK_KEY = new KeyMapping(
			"key.cbtflex.heavy_attack",
			InputConstants.Type.MOUSE,
			GLFW.GLFW_MOUSE_BUTTON_4,
			"key.categories.cbtflex"
	);

	static final KeyMapping GRAB_KEY = new KeyMapping(
			"key.cbtflex.grab",
			InputConstants.Type.MOUSE,
			GLFW.GLFW_MOUSE_BUTTON_5,
			"key.categories.cbtflex"
	);

	private ClientModEvents() {
	}

	@SubscribeEvent
	public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(HEAVY_ATTACK_KEY);
		event.register(GRAB_KEY);
	}

	@SubscribeEvent
	public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(ModEntities.THROWN_BLOCK.get(), ThrownBlockRenderer::new);
		event.registerEntityRenderer(ModEntities.THROWN_ITEM.get(), ThrownItemRenderer::new);
	}
}